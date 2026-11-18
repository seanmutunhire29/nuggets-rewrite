/**
 * ClientState - everything the client knows about the game right now.
 *
 * The network thread writes into this object as messages arrive from the server,
 * and the main thread reads it to draw the screen, so every method is
 * synchronized on the instance.
 *
 * @author Sean Mutunhire
 */
public class ClientState {

  /** Shown on the banner until the player letter arrives in the OK message. */
  public static final char NO_LETTER = '\0';

  private final boolean spectator; // true if we joined with SPECTATE

  private char letter = NO_LETTER; // our own letter, from OK
  private int numRows = 0;         // grid height, from GRID
  private int numCols = 0;         // grid width, from GRID
  private boolean gridKnown = false;

  private int purse = 0;           // nuggets we hold, from GOLD
  private int collected = 0;       // nuggets just picked up, from GOLD
  private int remaining = 0;       // nuggets still on the map, from GOLD

  private String[] displayRows = new String[0]; // the map, one string per row
  private String statusNote = null;             // an ERROR or other transient note
  private String quitMessage = null;            // set when QUIT arrives
  private boolean quit = false;

  /**
   * Creates the state for one client session.
   *
   * @param spectator true if this client sent SPECTATE rather than PLAY
   */
  public ClientState(boolean spectator) {
    this.spectator = spectator;
  }

  /** @return true if this client is watching rather than playing */
  public synchronized boolean isSpectator() {
    return spectator;
  }

  /** @return our player letter, or {@link #NO_LETTER} if we have no OK yet */
  public synchronized char getLetter() {
    return letter;
  }

  /**
   * Records the letter from the OK message.
   *
   * @param letter the letter the server gave us
   */
  public synchronized void setLetter(char letter) {
    this.letter = letter;
  }

  /** @return the number of rows in the grid, or 0 if GRID has not arrived */
  public synchronized int getNumRows() {
    return numRows;
  }

  /** @return the number of columns in the grid, or 0 if GRID has not arrived */
  public synchronized int getNumCols() {
    return numCols;
  }

  /** @return true once a GRID message has told us the size of the map */
  public synchronized boolean isGridKnown() {
    return gridKnown;
  }

  /**
   * Records the grid size from the GRID message.
   *
   * @param rows the number of rows
   * @param cols the number of columns
   */
  public synchronized void setGrid(int rows, int cols) {
    this.numRows = rows;
    this.numCols = cols;
    this.gridKnown = true;
  }

  /** @return the nuggets in our purse */
  public synchronized int getPurse() {
    return purse;
  }

  /** @return the nuggets we picked up on the last move; 0 if none */
  public synchronized int getCollected() {
    return collected;
  }

  /** @return the nuggets still waiting on the map */
  public synchronized int getRemaining() {
    return remaining;
  }

  /**
   * Records the three numbers from a GOLD message. A new GOLD message always
   * replaces the old transient note on the banner.
   *
   * @param collected nuggets just collected
   * @param purse     nuggets now held
   * @param remaining nuggets still unclaimed
   */
  public synchronized void setGold(int collected, int purse, int remaining) {
    this.collected = collected;
    this.purse = purse;
    this.remaining = remaining;
    this.statusNote = null;
  }

  /** @return the map, one string per grid row; empty until DISPLAY arrives */
  public synchronized String[] getDisplayRows() {
    return displayRows.clone();
  }

  /**
   * Records the map from a DISPLAY message.
   *
   * @param body everything after the "DISPLAY\n" keyword line
   */
  public synchronized void setDisplay(String body) {
    // the server ends every row with a newline, so the final split piece is empty
    String[] rows = body.split("\n", -1);
    int count = rows.length;
    if (count > 0 && rows[count - 1].isEmpty()) {
      count--;
    }
    String[] kept = new String[count];
    System.arraycopy(rows, 0, kept, 0, count);
    this.displayRows = kept;
  }

  /** @return a transient note for the banner, such as an error; null if none */
  public synchronized String getStatusNote() {
    return statusNote;
  }

  /**
   * Sets a transient note shown at the end of the banner.
   *
   * @param note the text to show, or null to clear it
   */
  public synchronized void setStatusNote(String note) {
    this.statusNote = note;
  }

  /** Clears the "GOLD received" note; called when the player presses a key. */
  public synchronized void clearTransient() {
    this.collected = 0;
    this.statusNote = null;
  }

  /** @return true once the server has told us to quit */
  public synchronized boolean isQuit() {
    return quit;
  }

  /** @return the server's explanation of why we quit, or null */
  public synchronized String getQuitMessage() {
    return quitMessage;
  }

  /**
   * Records a QUIT message from the server.
   *
   * @param message the explanation the server sent
   */
  public synchronized void setQuit(String message) {
    this.quitMessage = message;
    this.quit = true;
  }

  /**
   * Builds the one-line banner at the top of the screen.
   *
   * @return the banner text, already including any transient note
   */
  public synchronized String banner() {
    StringBuilder sb = new StringBuilder();
    if (spectator) {
      sb.append(String.format("Spectator: %d nuggets unclaimed.", remaining));
    } else if (letter == NO_LETTER) {
      sb.append("Waiting to join...");
    } else {
      sb.append(String.format("Player %c has %d nuggets (%d nuggets unclaimed).",
                              letter, purse, remaining));
    }
    if (collected > 0) {
      sb.append(String.format("  GOLD received: %d", collected));
    }
    if (statusNote != null && !statusNote.isEmpty()) {
      sb.append("  ").append(statusNote);
    }
    return sb.toString();
  }
}
