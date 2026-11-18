import java.io.PrintStream;

/**
 * A simple way to log messages to a stream.
 *
 * The constructor takes a PrintStream to log to, or null to discard all
 * messages. The public methods offer simple ways to log a string, integer,
 * or character, using a format string. The log is flushed after each message.
 * Call close() to stop logging.
 * 
 * Note: This class is thread-safe; threads that concurrently call its methods
 * share only its member variable, and Java's PrintStream class is thread-safe.
 *
 * David Kotz, May 2019 (C original);
 * Claude assisted with Java translation 2026.
 */
public class Log implements AutoCloseable {

  private PrintStream out; // null means "do not log"

  // =================== Constructors =================
  /** Create a Log that discards everything. */
  public Log() {
    this(null);
  }

  /**
   * Create a Log to the given stream; if null, discard everything.
   * 
   * @param out the stream to log to, or null to discard everything
   */
  public Log(PrintStream out) {
    this.out = out;
    if (out != null) {
      out.println("START OF LOG");
      out.flush();
    }
  }

  // =================== Logging methods =================
  /**
   * Log a string, using the given format string.
   * A newline is added and the stream is flushed.
   * For example,
   * Log.s("Found name '%s' in the database", name);
   * 
   * @param format a format string containing one %s
   * @param value  the string to be formatted into the format string
   */
  public void s(String format, String value) {
    if (out != null && format != null) {
      out.printf(format + "\n", value);
      out.flush();
    }
  }

  /**
   * Log an integer, using the given format string containing one %d.
   * A newline is added and the stream is flushed. For example,
   * Log.d("Sorting %d records", numRecords);
   * 
   * @param format a format string containing one %d
   * @param value  the integer to be formatted into the format string
   */
  public void d(String format, int value) {
    if (out != null && format != null) {
      out.printf(format + "\n", value);
      out.flush();
    }
  }

  /**
   * Log a character, using the given format string containing one %c.
   * A newline is added and the stream is flushed. For example,
   * Log.c("Unknown letter '%c' in command", nextChar);
   * 
   * @param format a format string containing one %c
   * @param value  the character to be formatted into the format string
   */
  public void c(String format, char value) {
    if (out != null && format != null) {
      out.printf(format + "\n", value);
      out.flush();
    }
  }

  /**
   * Log a message verbatim; used when no format argument is needed.
   * A newline is added and the stream is flushed. For example,
   * Log.v("Starting the breadth-first search");
   * 
   * @param message the message to be logged
   */
  public void v(String message) {
    if (out != null && message != null) {
      out.println(message);
      out.flush();
    }
  }

  /**
   * Log a message about an internal error, along with the cause's detail
   * message. Best used immediately after catching an exception from a
   * failed I/O or system operation. The stream is flushed. For example,
   * Log.e("Exception in opening input file", e);
   * 
   * @param message the message to be logged
   * @param cause   the exception that caused the error
   */
  public void e(String message, Throwable cause) {
    if (out != null && message != null) {
      out.println(message + ": " + (cause != null ? cause.getMessage() : "unknown error"));
      out.flush();
    }
  }

  /**
   * Shut down the module; Safe to call more than once.
   * Called as a result of AutoCloseable ... when this instance is destroyed.
   */
  @Override
  public void close() {
    v("END OF LOG");
    out = null;
  }
}
