import com.googlecode.lanterna.input.KeyStroke;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Client - the Nuggets client: joins a game as a player or a spectator, draws
 * the map with lanterna, and sends every keystroke to the server.
 *
 * <pre>
 * usage: Client hostname port [playername] [--log logfile]
 * </pre>
 *
 * With a player name the client sends PLAY and joins the game; without one it
 * sends SPECTATE and only watches. The server prints the port to use when it
 * starts, on a line reading "serverPort=NNNNN".
 *
 * <p>Two threads share one {@link ClientState}: a network thread blocks on
 * {@link Messenger#receive()} and writes what the server says into the state,
 * while the main thread polls the keyboard and redraws the screen. Nothing but
 * the main thread ever touches the terminal.
 *
 * <p>Note that stderr is useless once lanterna owns the terminal, so --log is
 * the only way to see what is going over the wire.
 *
 * @author Sean Mutunhire
 */
public class Client {

  /** How long the main loop waits between keyboard polls, in milliseconds. */
  private static final int POLL_MILLIS = 20;

  /** How long we wait for the server's QUIT reply after we press Q, in millis. */
  private static final int QUIT_WAIT_MILLIS = 1000;

  private final Messenger messenger;
  private final InetSocketAddress server;
  private final ClientState state;
  private final Log log;

  /**
   * Builds a client that talks to one server.
   *
   * @param messenger the already-open UDP messenger
   * @param server    the server's address
   * @param state     the state this client will fill in and draw
   * @param log       where to log traffic; never null
   */
  public Client(Messenger messenger, InetSocketAddress server, ClientState state, Log log) {
    this.messenger = messenger;
    this.server = server;
    this.state = state;
    this.log = log;
  }

  // ======================= handling what the server says =======================

  /**
   * Acts on one message from the server, filling in the state. Unknown keywords
   * are logged and ignored, so a newer server cannot crash an older client.
   *
   * @param message the whole message, exactly as it arrived
   */
  void handleMessage(String message) {
    String keyword = keywordOf(message);
    String rest = bodyOf(message);

    switch (keyword) {
      case "OK":
        if (!rest.isEmpty()) {
          state.setLetter(rest.charAt(0));
        }
        break;

      case "GRID": {
        String[] parts = rest.trim().split("\\s+");
        if (parts.length >= 2) {
          try {
            state.setGrid(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
          } catch (NumberFormatException e) {
            log.v("Client: malformed GRID message: " + message);
          }
        }
        break;
      }

      case "GOLD": {
        String[] parts = rest.trim().split("\\s+");
        if (parts.length >= 3) {
          try {
            state.setGold(Integer.parseInt(parts[0]),
                          Integer.parseInt(parts[1]),
                          Integer.parseInt(parts[2]));
          } catch (NumberFormatException e) {
            log.v("Client: malformed GOLD message: " + message);
          }
        }
        break;
      }

      case "DISPLAY":
        // the keyword is followed by a newline, then one line per grid row
        state.setDisplay(rest);
        break;

      case "QUIT":
        state.setQuit(rest);
        break;

      case "ERROR":
        state.setStatusNote(rest);
        break;

      default:
        log.v("Client: ignoring unknown message: " + message);
    }
  }

  /**
   * Finds the keyword at the start of a message from the server.
   *
   * <p>This cannot use {@link Protocol#firstWord(String)}, which splits at the
   * first space: a DISPLAY message's keyword is followed by a newline, and the
   * map that follows it is full of spaces.
   *
   * @param message the whole message
   * @return the keyword, such as "DISPLAY"
   */
  static String keywordOf(String message) {
    int end = endOfKeyword(message);
    return message.substring(0, end);
  }

  /**
   * Gives everything after the keyword and the one space or newline that ends
   * it, exactly as it arrived.
   *
   * @param message the whole message
   * @return the body of the message; empty if there is nothing after the keyword
   */
  static String bodyOf(String message) {
    int end = endOfKeyword(message);
    return (end < message.length()) ? message.substring(end + 1) : "";
  }

  /**
   * Finds where the keyword ends: at the first space or newline, or at the end
   * of the message if it holds neither.
   *
   * @param message the whole message
   * @return the index just past the keyword
   */
  private static int endOfKeyword(String message) {
    for (int i = 0; i < message.length(); i++) {
      char ch = message.charAt(i);
      if (ch == ' ' || ch == '\n') {
        return i;
      }
    }
    return message.length();
  }

  /**
   * The network thread's body: block on receive, hand each message to
   * {@link #handleMessage(String)}, and stop once the server says QUIT.
   */
  private void receiveLoop() {
    try {
      while (!state.isQuit()) {
        Messenger.Message message = messenger.receive();
        if (message == null) {
          return; // socket closed
        }
        handleMessage(message.getText());
      }
    } catch (IOException e) {
      log.e("Client: error receiving from server", e);
      state.setQuit("Lost contact with the server: " + e.getMessage());
    }
  }

  // ============================== the main loop ==============================

  /**
   * Joins the game and then runs until the server says QUIT or the player
   * presses Q. Always restores the terminal before returning.
   *
   * @param joinMessage the PLAY or SPECTATE message to send first
   * @throws IOException if the terminal or the socket fails
   */
  public void play(String joinMessage) throws IOException {
    messenger.send(server, joinMessage);

    Thread network = new Thread(this::receiveLoop, "nuggets-client-network");
    network.setDaemon(true);
    network.start();

    String quitMessage;
    if (System.console() == null) {
      throw new IOException("no terminal found; run the client from a terminal window");
    }
    try (ClientDisplay display = new ClientDisplay()) {
      quitMessage = loop(display);
    }

    // the terminal is back to normal; now it is safe to print
    if (quitMessage != null && !quitMessage.isEmpty()) {
      System.out.println(quitMessage);
    }
  }

  /**
   * Polls the keyboard and redraws the screen until the game ends.
   *
   * @param display the open lanterna display
   * @return the server's parting message, or null if there was none
   * @throws IOException if the terminal fails
   */
  private String loop(ClientDisplay display) throws IOException {
    display.redraw(state);

    while (!state.isQuit()) {
      KeyStroke stroke = display.pollKey();
      if (stroke != null) {
        char key = KeyHandler.toProtocolKey(stroke);
        if (key != 0) {
          state.clearTransient();       // the old "GOLD received" note is stale now
          messenger.send(server, "KEY " + key);
          if (KeyHandler.isQuit(key)) {
            waitForQuit();              // give the server a moment to answer
            break;
          }
        }
      }
      display.redraw(state);

      try {
        Thread.sleep(POLL_MILLIS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }

    String message = state.getQuitMessage();
    return (message != null) ? message : "Thanks for playing!";
  }

  /** Waits a short while for the server's QUIT reply after we send KEY Q. */
  private void waitForQuit() {
    long deadline = System.currentTimeMillis() + QUIT_WAIT_MILLIS;
    while (!state.isQuit() && System.currentTimeMillis() < deadline) {
      try {
        Thread.sleep(POLL_MILLIS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
    }
  }

  // ================================== main ==================================

  /** Prints how to run the client and exits with a failure status. */
  private static void usage(String problem) {
    System.err.println("error: " + problem);
    System.err.println("usage: Client hostname port [playername] [--log logfile]");
    System.exit(1);
  }

  /**
   * main - starts one client.
   *
   * @param args hostname, port, an optional player name, and an optional
   *             "--log logfile" pair
   */
  public static void main(String[] args) {
    if (args.length < 2) {
      usage("provide at least a hostname and a port");
    }

    String host = args[0];
    int port = 0;
    try {
      port = Integer.parseInt(args[1]);
    } catch (NumberFormatException e) {
      usage("port must be a number, not '" + args[1] + "'");
    }

    String playerName = null;
    String logFile = null;
    int i = 2;
    while (i < args.length) {
      if (args[i].equals("--log")) {
        if (i + 1 >= args.length) {
          usage("--log needs a file name");
        }
        logFile = args[i + 1];
        i += 2;
      } else if (playerName == null) {
        playerName = args[i];
        i++;
      } else {
        usage("unexpected argument '" + args[i] + "'");
      }
    }

    Log log = new Log();
    if (logFile != null) {
      try {
        log = new Log(new PrintStream(new FileOutputStream(logFile, true),
                                      true, StandardCharsets.UTF_8));
      } catch (IOException e) {
        usage("cannot write the log file '" + logFile + "'");
      }
    }

    try (Messenger messenger = new Messenger(log)) {
      InetSocketAddress server = Messenger.resolveAddress(host, port);
      boolean spectator = (playerName == null);
      ClientState state = new ClientState(spectator);
      Client client = new Client(messenger, server, state, log);
      client.play(spectator ? "SPECTATE" : "PLAY " + playerName);
    } catch (IllegalArgumentException e) {
      usage(e.getMessage());
    } catch (IOException e) {
      System.err.println("error: " + e.getMessage());
      System.exit(1);
    } finally {
      log.close();
    }
  }
}
