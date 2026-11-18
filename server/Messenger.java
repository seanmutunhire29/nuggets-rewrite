import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

/**
 * A UDP-based messaging module that provides a simple interface for sending and receiving messages.
 * It supports sending messages to a specified address, receiving messages from any address.
 * The module is designed to be used in both server and client applications.
 *
 * Example server sequence, for a server object that expects only to react to messages:
 * 
 *    Messenger messenger = new Messenger(null);
 *    int port = messenger.getPort(); // advertise this port to clients
 *    Messenger.Message message = messenger.receive();
 *    System.out.println("Message from " + message.getAddr() + ": " + message.getText());
 *    messenger.send(from, "Thanks!");
 *    messenger.close();
 * 
 * Example client sequence for a client object:
 * 
 *    Messenger messenger = new Messenger(null);
 *    InetSocketAddress server = Messenger.resolveAddress(serverHost, serverPort);
 *    messenger.send(server, "hello!");
 *    Messenger.Message message = messenger.receive();
 *    System.out.println("Response from " + message.getAddr() + ": " + message.getText());
 *    messenger.close();
 * 
 * Note: This class is thread-safe; threads that concurrently call its methods
 * share only its member variables. Java's DatagramSocket class is thread-safe.
 * My Log class is thread-safe.
 *
 * David Kotz - Fall 2026, derived from his original C module
 * (with Claude suggesting some of the Socket details).
 */
public class Messenger implements AutoCloseable {

  // ========================== Constants ==========================
  /** Maximum payload size for a UDP message (RFC-limited datagram size). */
  public static final int MAX_BYTES = 65507;

  private static final int MIN_PORT = 1024;
  private static final int MAX_PORT = 65535;

  // ========================== Instance Variables ==========================
  private final Log log;               // Log to which we write messages
  private DatagramSocket socket; // UDP socket on which we operate
  private final int port;              // port number for that socket

  // ========================== Constructors ==========================
  /** Create a Messenger module with no logging. */
  public Messenger() throws SocketException {
    this(null);
  }

  /** Create a Messenger module that logs to the given Log (may be a no-op Log). */
  public Messenger(Log log) throws SocketException {
    // log to the given Log, or to a no-op Log if null
    this.log = (log != null) ? log : new Log();

    try {
      socket = new DatagramSocket(0); // wildcard address, OS-assigned port
    } catch (SocketException e) {
      this.log.e("Messenger: error opening/binding datagram socket", e);
      // throw new SocketException("Messenger: unable to create socket", e);
      throw new SocketException("Messenger: unable to create socket");
    }
    // record the port number assigned to the socket
    port = socket.getLocalPort();
    this.log.d("Messenger: ready at port %d", port);
  }

  // =========================== Inner class ============================
  /** represents a message from or to the given address
   */
  public class Message {
    private final InetSocketAddress addr; // to/from address
    private final String text;            // text of this message

    public Message(InetSocketAddress addr, String text) {
      this.addr = addr;
      this.text = text;
    }
    public InetSocketAddress getAddr() { return addr; }
    public String            getText() { return text; }
  }

  // ========================== Public Methods ==========================
  /**
   * send - Send a message to the given address. 
   * Overlength messages are silently truncated.
   * 
   * @param to - the Internet address to which to send the message
   * @param text - the string message to send
   * @throws IllegalArgumentException if the address or message is null
   * @throws RuntimeException if the socket is closed
   * @throws IOException if an I/O error occurs while sending the message
   */
  public void send(InetSocketAddress to, String text) 
    throws IllegalArgumentException, RuntimeException, IOException {

    if (to == null) {
      log.v("Messenger.send: called with null address");
      throw new IllegalArgumentException("Messenger.send: address must not be null");
    }
    if (text == null) {
      log.v("Messenger.send: called with null message");
      throw new IllegalArgumentException("Messenger.send: message must not be null");
    }
    if (socket.isClosed()) {
      log.v("Messenger.send: called after socket closed");
      throw new RuntimeException("Messenger.send: socket is closed");
    }

    byte[] data = text.getBytes(StandardCharsets.UTF_8);
    int sendLength = Math.min(data.length, MAX_BYTES);
    try {
      socket.send(new DatagramPacket(data, sendLength, to));
      log.s("Messenger.send: TO %s", stringAddr(to) + ": " + text);
    } catch (IOException e) {
      log.e("Messenger.send: error sending to datagram socket", e);
      throw e; // our caller must know the message did not go out
    }
  }

  /**
   * receive - receive one messages from the socket; MAY BLOCK.
   * Returns null if socket is closed or thread is interrupted.
   * 
   * @return Message(from, text), or null
   * @throws IOException if any errors on the socket
   */
  public Message receive() throws IOException {
    if (socket.isClosed()) {
      // socket is closed - don't try to receive
      return null;
    }
    
    // internal buffer for receiving messages
    byte[] buf = new byte[MAX_BYTES];
    DatagramPacket packet = new DatagramPacket(buf, buf.length);

    try {
      socket.receive(packet);
    } catch (SocketException e) {
      log.e("Messenger: thread interrupted", e);
      return null;
    } catch (IOException e) {
      if (socket.isClosed()) {
        return null; // normal shutdown via close()
      } else {
        throw e;     // fatal error: throw exception upward
      }
    }

    
    // extract the message and sender address from the packet
    String text = new String(packet.getData(), packet.getOffset(), packet.getLength(),
    StandardCharsets.UTF_8);
    InetSocketAddress from = (InetSocketAddress) packet.getSocketAddress();
    log.s("Messenger.receive: FROM %s", stringAddr(from) + ": " + text);
    
    // return the message to our caller
    return new Message(from, text);
  }
  
  /** 
   * close - Shut down the module: close the socket. Safe to call more than once.
   */
  @Override
  public void close() {
    socket.close();
    log.v("Messenger.close: message module closing down.");
  }

  // ========================== Public getters and helpers ==========================

  /**
   * getPort - Return the port number assigned to the socket.
   * @return the port number assigned to the socket
   */
  public int getPort() {
    return port;
  }

  /**
   * resolveAddress - Resolve a hostname/port pair into an address suitable for send().
   * It throws exceptions for invalid input.
   * 
   * @param hostname the hostname to resolve
   * @param port the port number as an integer
   * @throws IllegalArgumentException if the hostname is null
   * @throws IllegalArgumentException if the port number is not in the unreserved range (1024-65535)
   * @throws UnknownHostException if the hostname cannot be resolved
   * @return an InetSocketAddress representing the resolved address
   */
  public static InetSocketAddress resolveAddress(String hostname, int port)
      throws IllegalArgumentException, UnknownHostException {
    if (hostname == null) {
      throw new IllegalArgumentException("resolveAddress: hostname must not be null");
    }
    if (port < MIN_PORT || port > MAX_PORT) {
      throw new IllegalArgumentException("resolveAddress: invalid port number: " + port);
    }
     
    try {
      InetAddress address = InetAddress.getByName(hostname);
      return new InetSocketAddress(address, port);
    } catch (UnknownHostException e) {
      throw new UnknownHostException("resolveAddress: " + e.getMessage());
    }
  }

  /**
   * stringAddr - Produce a string representation of an address,
   * e.g., "128.119.245.12:45678".
   * 
   * @param addr the InetSocketAddress to convert to a string
   * @return string representation of the address, or "(no address)" for null.
  */
  public static String stringAddr(InetSocketAddress addr) {
    if (addr == null) {
      return "(no address)";
    } else {
      return String.format("%s:%d", addr.getAddress().getHostAddress(), addr.getPort());
    }
  }

  // ========================== Private helpers ==========================

}

