import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Server - Main Function which starts up a new server and sets up play
 * @author Sean Mutunhire 11/9/26
 */

public class Server{
    private Map<InetSocketAddress, Player> playerSockets;
    private List<InetSocketAddress> spectatorSockets;
    private final Messenger messenger;
    private final Log log;
    private boolean finished = false; // true once the game is over

    // Message constants
    private static final char QUIT_KEY = 'Q';
    private static final String MSG_FULL = "QUIT Game is full: no more players can join.";
    private static final String MSG_ALREADY_JOINED = "QUIT You have already joined this game.";
    private static final String MSG_NO_NAME = "QUIT Sorry - you must provide player's name.";
    private static final String MSG_THANKS_PLAYING = "Thanks for playing!";
    private static final String MSG_THANKS_WATCHING = "Thanks for watching!";
    private static final String MSG_GAME_OVER = "GAME OVER:";

    /**
     * Server - initialises the messenger module for sending and receiving messages
     * @param log - the object into which logs are placed
     * @throws Exception
     */
    public Server(Log log) throws IOException {
        // initialize the Messenger module
        this.log = (log != null) ? log : new Log(); // ensure this.log never null
        messenger = new Messenger(this.log); // may throw Exception upward
        this.playerSockets = new HashMap<>();
        this.spectatorSockets = new ArrayList<>();
    }

    boolean hasPlayer(InetSocketAddress address) {
        return playerSockets.containsKey(address);
    }

    /**
     * run - loops forever, blocking on client messages and handing each one to
     * handleMessage, until the game ends or the socket closes.
     * @param game - the game object which contains all the logic of how the game is played
     */
    public void run(Game game) throws IOException {
        System.out.println("serverPort=" + messenger.getPort());

        playerSockets = new HashMap<>();
        spectatorSockets = new ArrayList<>();

        try {
            while (!finished) {
                Messenger.Message message;

                try { // BLOCK until a message arrives
                    message = messenger.receive();
                    if (message == null) {
                        return; // apparently the socket is closed
                    }
                } catch (IOException e) {
                    log.e("Server: IOException in message receive", e);
                    return; // fatal error
                }

                try { // handle the message
                    log.v("Server: handle inbound message");
                    handleMessage(message.getText(), game, message.getAddr());
                } catch (IOException e) {
                    log.e("server: IOException in message handler", e);
                    throw e; // fatal error
                }
            }
        } finally {
            messenger.close();
        }
    }

    /**
     * isFinished - tells whether the game has ended and the server should stop.
     * @return true once the game-over summary has been sent
     */
    boolean isFinished() {
        return finished;
    }

    /**
     * handleMessage - recieves all the messages comming from clients and decides what to do with them
     * @param message - the message received from a client
     * @param game - the game object
     * @param address - the address of the client that sent the message
     */
    void handleMessage(String message, Game game, InetSocketAddress address) throws IOException {
        String keyWord  = Protocol.firstWord(message);
        switch (keyWord) {
            case "PLAY":
                handlePlay(message, game, address);
                break;
            case "SPECTATE":
                handleSpectate(game, address);
                break;
            case "KEY":
                handleKey(message, game, address);
                break;
            default:
                sendError(address, "unknown key");
        }
    }

    /**
     * handlePlay - adds a new player to the game and sends it the opening messages.
     * @param message - the whole PLAY message, including the player's real name
     * @param game - the game object
     * @param address - the address of the client that sent PLAY
     */
    private void handlePlay(String message, Game game, InetSocketAddress address)
            throws IOException {
        String rawName = Protocol.restOfMessage(message);
        if (rawName.isBlank()) {
            sendTo(address, MSG_NO_NAME);
            return;
        }
        Player player = game.addPlayer(Game.cleanName(rawName), address);

        if (player == null) {
            // addPlayer refuses for two different reasons; say which one it was
            sendTo(address, game.isFull() ? MSG_FULL : MSG_ALREADY_JOINED);
            return;
        }

        playerSockets.put(address, player);
        sendOk(address);
        sendGrid(address, game);
        sendGold(address, 0, player.getPurse(), game.getGoldRemaining());
        sendDisplay(address, game.displayFor(player));
    }

    /**
     * handleSpectate - adds a spectator and sends it the opening messages.
     * @param game - the game object
     * @param address - the address of the client that sent SPECTATE
     */
    private void handleSpectate(Game game, InetSocketAddress address) throws IOException {
        game.addSpectator(address);
        if (!spectatorSockets.contains(address)) {
            spectatorSockets.add(address);
        }
        sendGrid(address, game);
        sendGold(address, 0, 0, game.getGoldRemaining());
        sendDisplay(address, game.spectatorDisplay());
    }

    /**
     * handleKey - acts on one keystroke from a player or a spectator.
     * @param message - the whole KEY message
     * @param game - the game object
     * @param address - the address of the client that pressed the key
     */
    private void handleKey(String message, Game game, InetSocketAddress address)
            throws IOException {
        String rest = Protocol.restOfMessage(message);
        if (rest.length() != 1) {
            sendError(address, "wrong key pressed");
            return;
        }
        char key = rest.charAt(0);

        if (!playerSockets.containsKey(address)) {       // a spectator, or a stranger
            if (key == QUIT_KEY) {
                sendQuit(address, MSG_THANKS_WATCHING);
                game.removeSpectator(address);           // stop sending it updates
                spectatorSockets.remove(address);
            } else {
                sendError(address, "You can only use Q");
            }
            return;
        }

        Player player = playerSockets.get(address);

        if (key == QUIT_KEY) {
            sendQuit(address, MSG_THANKS_PLAYING);
            game.quitPlayer(player);
            playerSockets.remove(address);
            broadcastState(game, null, 0);               // its letter leaves every display
            return;
        }

        int[] dir = directionFor(key);
        if (dir[0] == 0 && dir[1] == 0) {
            sendError(address, "unknown keystroke");
            return;
        }

        boolean sprint = (dir[2] == 1);
        int goldCollected = game.move(player, dir[0], dir[1], sprint);

        // show everyone the result of the move BEFORE announcing game over,
        // so the last pickup is on screen when the summary arrives
        broadcastState(game, address, goldCollected);

        if (game.isOver()) {
            gameOver(game.summary());
        }
    }

    /**
     * broadcastState - sends every player and spectator a fresh GOLD and DISPLAY.
     * @param game - the game object
     * @param mover - the address that just collected gold, or null if nobody did
     * @param goldCollected - the nuggets that address just collected
     */
    private void broadcastState(Game game, InetSocketAddress mover, int goldCollected)
            throws IOException {
        int remaining = game.getGoldRemaining();

        for (InetSocketAddress sock : new ArrayList<>(playerSockets.keySet())) {
            Player p = playerSockets.get(sock);
            if (p == null) {
                continue;
            }
            sendGold(sock, sock.equals(mover) ? goldCollected : 0, p.getPurse(), remaining);
            sendDisplay(sock, game.displayFor(p));
        }

        String spectatorView = game.spectatorDisplay();
        for (InetSocketAddress addr : new ArrayList<>(spectatorSockets)) {
            sendGold(addr, 0, 0, remaining);
            sendDisplay(addr, spectatorView);
        }
    }

    /**
     * notifyClients - sends a message to all the clients
     * @param message - the String message to be sent to all clients
     */
    private void notifyClients(String message) throws IOException {
        for (InetSocketAddress addr : new ArrayList<>(playerSockets.keySet())) {
            sendTo(addr, message);
        }
        for (InetSocketAddress addr : new ArrayList<>(spectatorSockets)) {
            sendTo(addr, message);
        }
    }

    /**
     * gameOver - tells every client the game has ended and asks run() to stop.
     * @param message - the results of the game
     */
    private void gameOver(String message) throws IOException {
        String m = MSG_GAME_OVER + "\n" + message;
        notifyClients("QUIT " + m);
        System.out.println(m);
        finished = true;   // run() closes the messenger and returns
    }

    /**
     * directionFor - dictates the direction which a charactor moves depending on the key
     * @param key - the charactor clicked by the client
     * @return a list of the direction which the client moves
     */
    static int[] directionFor(char key){
        //returns (dr, dc) for h j k l y u b n, otherwise nothing.
        switch (key) {
            case 'k':
                return new int[] {-1, 0, 0};
            case 'j':
                return new int[] {1, 0, 0};
            case 'h':
                return new int[] {0, -1, 0};
            case 'l':
                return new int[] {0, 1, 0};
            case 'y':
                return new int[] {-1, -1, 0};
            case 'u':
                return new int[] {-1, 1, 0};
            case 'b':
                return new int[] {1, -1, 0};
            case 'n':
                return new int[] {1, 1, 0};
            case 'K':
                return new int[] {-1, 0, 1};
            case 'J':
                return new int[] {1, 0, 1};
            case 'H':
                return new int[] {0, -1, 1};
            case 'L':
                return new int[] {0, 1, 1};
            case 'Y':
                return new int[] {-1, -1, 1};
            case 'U':
                return new int[] {-1, 1, 1};
            case 'B':
                return new int[] {1, -1, 1};
            case 'N':
                return new int[] {1, 1, 1};
            default:
                return new int[] {0, 0, 0};
        }
    }

    /**
     * sendOk - sends the message OK to clients
     * @param to - the address of the client
     */
    void sendOk(InetSocketAddress to) throws IOException {
        sendTo(to, "OK " + playerSockets.get(to).getLetter());
    }
    
    /**
     * sendGold - sends a message about the gold picked by a user
     * @param to - the address of the client
     * @param goldCollected - the amount of gold picked
     * @param goldRemaining - the goid amount that is yet to be collected
     */
    void sendGold(InetSocketAddress to, int goldCollected, int goldState, int goldRemaining)
            throws IOException {
        sendTo(to, "GOLD " + goldCollected + " " + goldState + " " + goldRemaining);
    }

    /**
     * sendQuit - forces a client to quit
     * @param to - the address of the client who is forced to quit
     * */
    void sendQuit(InetSocketAddress to, String message) throws IOException {
        sendTo(to, "QUIT " + message);
    }
    
    /**
     * sendError - sends an error messages to a client if one arises
     * @param to - the address of the client receiving the error message
     * @param error - the String error message
     */
    void sendError(InetSocketAddress to, String error) throws IOException {
        sendTo(to, "ERROR " + error);
    }

    /**
     * sendGrid - tells a client the size of the grid
     * @param to - the address of the client
     * @param game - the game whose grid size to report
     */
    void sendGrid(InetSocketAddress to, Game game) throws IOException {
        sendTo(to, "GRID " + game.getGrid().getNumRows() + " " + game.getGrid().getNumCols());
    }

    /**
     * sendDisplay - sends a client the map it should draw
     * @param to - the address of the client
     * @param display - the display string, one line per grid row
     */
    void sendDisplay(InetSocketAddress to, String display) throws IOException {
        sendTo(to, "DISPLAY\n" + display);
    }

    /**
     * sendTo - helper function to send a message to a client from the server
     * @param to - the address of the client
     * @param text - the string to be sent
     */
    void sendTo(InetSocketAddress to, String text) throws IOException {
        messenger.send(to, text);
    }
   
    /** The command-line flags this server understands. */
    private static final List<String> OPTIONS =
        List.of("gold", "minpiles", "maxpiles", "seed");

    /**
     * usage - prints how to start the server and exits with a failure status.
     * @param problem - what was wrong with the command line
     */
    private static void usage(String problem) {
        System.err.println("error: " + problem);
        System.err.println("usage: Server map.txt "
            + "[--gold N] [--minpiles N] [--maxpiles N] [--seed N]");
        System.exit(1);
    }

    /**
     * parseOptions - reads the --key value flags that follow the map file.
     * Every flag must be one of OPTIONS and must be followed by an integer.
     * @param args - the whole command line
     * @return a map from option name to value; never null
     */
    static Map<String, Integer> parseOptions(String[] args) {
        Map<String, Integer> options = new HashMap<>();
        int argInd = 1; // args[0] is the map file
        while (argInd < args.length) {
            String arg = args[argInd];
            if (!arg.startsWith("--")) {
                usage("unexpected argument '" + arg + "'");
                return options; // unreachable; keeps the compiler happy
            }
            String key = arg.substring(2);
            if (!OPTIONS.contains(key)) {
                usage("unknown option '" + arg + "'");
            }
            if (argInd + 1 >= args.length) {
                usage("option '" + arg + "' needs a value");
            }
            if (options.containsKey(key)) {
                usage("option '" + arg + "' given twice");
            }
            try {
                options.put(key, Integer.parseInt(args[argInd + 1]));
            } catch (NumberFormatException e) {
                usage("option '" + arg + "' needs an integer, not '" + args[argInd + 1] + "'");
            }
            argInd += 2;
        }
        return options;
    }

    /**
     * main - the main function running the server itself
     * @param args - map.txt, then optional --gold, --minpiles, --maxpiles, --seed flags
     */
    public static void main(String[] args) throws IOException {
        Grid grid;
        Game game;
        Random rand;

        if (args.length == 0) {
            usage("provide the map file");
        }

        Map<String, Integer> gameDefaults = parseOptions(args);

        try {
            grid = new Grid(args[0], gameDefaults);
        } catch (IOException e) {
            System.err.println("error: cannot read map file " + args[0]);
            System.exit(1);
            return;
        }

        // input arg checks
        if (grid.getMinPiles() < 1) {
            usage("minpiles must be at least 1");
        }
        if (grid.getMinPiles() > grid.getMaxPiles()) {
            usage("minpiles must not be larger than maxpiles");
        }
        if (grid.getMaxPiles() > grid.getGoldTotal()) {
            usage("maxpiles cannot be larger than goldtotal");
        }
        if (grid.getGoldTotal() < 1) {
            usage("gold must be at least 1");
        }

        ValidateMap validator = new ValidateMap(grid); // validate map
        if (!validator.isValid(args[0])) {
            System.err.println("error: " + args[0] + " is not a valid map");
            System.exit(1);
        }

        if (gameDefaults.containsKey("seed")) {
            rand = new Random(gameDefaults.get("seed"));
        } else {
            rand = new Random(); // a different game every run
        }
        game = new Game(grid, rand);

        Log log = new Log(System.err);
        Server server = new Server(log);
        server.run(game);
    }

}
