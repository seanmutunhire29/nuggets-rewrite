import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import java.io.IOException;

/**
 * ClientDisplay - draws the Nuggets screen with lanterna and reads keystrokes.
 *
 * The screen is one banner line on row 0 and then the map, one grid row per
 * screen row. If the window is too small to hold the whole map the display
 * says so and waits, as the Nuggets specification requires.
 *
 * All drawing happens on the thread that calls {@link #redraw(ClientState)};
 * the client calls it only from its main thread.
 *
 * @author Sean Mutunhire
 */
public class ClientDisplay implements AutoCloseable {

  /** Colour of a gold pile. */
  private static final TextColor GOLD_COLOR = TextColor.ANSI.YELLOW_BRIGHT;
  /** Colour of our own '@'. */
  private static final TextColor SELF_COLOR = TextColor.ANSI.WHITE_BRIGHT;
  /** Colour of another player's letter. */
  private static final TextColor OTHER_COLOR = TextColor.ANSI.CYAN_BRIGHT;
  /** Colour of walls, corners, and passages. */
  private static final TextColor WALL_COLOR = TextColor.ANSI.WHITE;
  /** Colour of room floor. */
  private static final TextColor FLOOR_COLOR = TextColor.ANSI.BLACK_BRIGHT;

  private final Terminal terminal;
  private final Screen screen;

  /**
   * Opens the terminal in private mode and hides the cursor.
   *
   * @throws IOException if the terminal cannot be opened
   */
  public ClientDisplay() throws IOException {
    // force a real text terminal: without this, lanterna opens a Swing window
    // when it cannot find a console, which is not what a terminal game wants
    this(new DefaultTerminalFactory()
        .setForceTextTerminal(true)
        .setPreferTerminalEmulator(false)
        .createTerminal());
  }

  /**
   * Opens a display on a terminal that someone else made; used for testing
   * against a virtual terminal.
   *
   * @param terminal the terminal to draw on
   * @throws IOException if the screen cannot be started
   */
  ClientDisplay(Terminal terminal) throws IOException {
    this.terminal = terminal;
    this.screen = new TerminalScreen(terminal);
    screen.startScreen();
    screen.setCursorPosition(null); // no blinking cursor over the map
  }

  /**
   * Gives the underlying screen, so a test can read back what was drawn.
   *
   * @return the lanterna screen
   */
  Screen getScreen() {
    return screen;
  }

  /**
   * Draws the current state: banner on top, map underneath. Picks up any window
   * resize first, and shows a "make the window bigger" message instead of a
   * clipped map when the window is too small.
   *
   * @param state the state to draw
   * @throws IOException if the terminal cannot be written
   */
  public void redraw(ClientState state) throws IOException {
    screen.doResizeIfNecessary(); // adopt a new window size, if the user resized
    screen.clear();

    TerminalSize size = screen.getTerminalSize();
    TextGraphics gfx = screen.newTextGraphics();

    int needRows = state.getNumRows() + 1; // +1 for the banner line
    int needCols = state.getNumCols();

    if (state.isGridKnown() && (size.getRows() < needRows || size.getColumns() < needCols)) {
      drawTooSmall(gfx, size, needRows, needCols);
      screen.refresh(Screen.RefreshType.COMPLETE);
      return;
    }

    drawBanner(gfx, size, state.banner());
    drawMap(gfx, size, state);

    screen.refresh(Screen.RefreshType.DELTA);
  }

  /**
   * Draws the banner line across the top of the window, in reverse video.
   *
   * @param gfx  where to draw
   * @param size the current window size
   * @param text the banner text
   */
  private void drawBanner(TextGraphics gfx, TerminalSize size, String text) {
    String line = text;
    if (line.length() > size.getColumns()) {
      line = line.substring(0, size.getColumns());
    }
    gfx.setForegroundColor(TextColor.ANSI.BLACK);
    gfx.setBackgroundColor(TextColor.ANSI.WHITE);
    gfx.putString(0, 0, padTo(line, size.getColumns()));
    gfx.setForegroundColor(TextColor.ANSI.DEFAULT);
    gfx.setBackgroundColor(TextColor.ANSI.DEFAULT);
  }

  /**
   * Draws the map one character at a time, so each kind of thing gets its own
   * colour. Anything past the edge of the window is skipped.
   *
   * @param gfx   where to draw
   * @param size  the current window size
   * @param state the state holding the map rows
   */
  private void drawMap(TextGraphics gfx, TerminalSize size, ClientState state) {
    String[] rows = state.getDisplayRows();
    for (int r = 0; r < rows.length && r + 1 < size.getRows(); r++) {
      String row = rows[r];
      for (int c = 0; c < row.length() && c < size.getColumns(); c++) {
        char ch = row.charAt(c);
        if (ch == ' ') {
          continue; // solid rock; leave the cell blank
        }
        gfx.setForegroundColor(colorFor(ch));
        if (ch == '@' || ch == '*') {
          gfx.enableModifiers(SGR.BOLD);
        }
        gfx.putString(c, r + 1, String.valueOf(ch));
        gfx.disableModifiers(SGR.BOLD);
      }
    }
    gfx.setForegroundColor(TextColor.ANSI.DEFAULT);
  }

  /**
   * Draws the message shown when the window is too small for the map.
   *
   * @param gfx      where to draw
   * @param size     the current window size
   * @param needRows the rows the map needs, banner included
   * @param needCols the columns the map needs
   */
  private void drawTooSmall(TextGraphics gfx, TerminalSize size, int needRows, int needCols) {
    gfx.setForegroundColor(TextColor.ANSI.DEFAULT);
    gfx.setBackgroundColor(TextColor.ANSI.DEFAULT);
    String[] lines = {
      "Your window is too small for this game.",
      String.format("It must be at least %d rows by %d columns;", needRows, needCols),
      String.format("it is now %d rows by %d columns.", size.getRows(), size.getColumns()),
      "Please resize the window, then press any key."
    };
    for (int i = 0; i < lines.length && i < size.getRows(); i++) {
      String line = lines[i];
      if (line.length() > size.getColumns()) {
        line = line.substring(0, size.getColumns());
      }
      gfx.putString(0, i, line);
    }
  }

  /**
   * Chooses the colour for one map character.
   *
   * @param ch the character from the DISPLAY message
   * @return the colour to draw it in
   */
  private static TextColor colorFor(char ch) {
    if (ch == '*') {
      return GOLD_COLOR;
    }
    if (ch == '@') {
      return SELF_COLOR;
    }
    if (ch >= 'A' && ch <= 'Z') {
      return OTHER_COLOR;
    }
    if (ch == '.') {
      return FLOOR_COLOR;
    }
    return WALL_COLOR; // '-', '|', '+', '#', and anything else
  }

  /**
   * Pads a string with spaces so it fills a whole line.
   *
   * @param s     the text
   * @param width the width to fill
   * @return the padded text
   */
  private static String padTo(String s, int width) {
    if (s.length() >= width) {
      return s;
    }
    return s + " ".repeat(width - s.length());
  }

  /**
   * Returns the next keystroke if one is waiting; does NOT block.
   *
   * @return the keystroke, or null if no key has been pressed
   * @throws IOException if the terminal cannot be read
   */
  public KeyStroke pollKey() throws IOException {
    return screen.pollInput();
  }

  /**
   * Waits for the next keystroke; BLOCKS.
   *
   * @return the keystroke, or null if the input stream ended
   * @throws IOException if the terminal cannot be read
   */
  public KeyStroke readKey() throws IOException {
    return screen.readInput();
  }

  /**
   * Closes the screen and puts the terminal back the way it was. Safe to call
   * more than once.
   */
  @Override
  public void close() {
    try {
      screen.stopScreen();
    } catch (IOException e) {
      // nothing useful left to do; the terminal is on its way out anyway
    }
    try {
      terminal.close();
    } catch (IOException e) {
      // same
    }
  }
}
