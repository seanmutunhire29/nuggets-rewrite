import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

/**
 * KeyHandler - turns a lanterna keystroke into the character the Nuggets
 * protocol expects in a KEY message.
 *
 * The movement keys are the usual rogue-like set: h j k l for left, down, up,
 * right, and y u b n for the four diagonals. The same letter in upper case
 * means "sprint": keep going in that direction until something blocks the way.
 * Q quits. Arrow keys are offered as a convenience and are translated into
 * their h/j/k/l equivalents.
 *
 * @author Sean Mutunhire
 */
public final class KeyHandler {

  /** The character the server reads as "quit". */
  public static final char QUIT_KEY = 'Q';

  /** Every key the server will act on. */
  private static final String MOVE_KEYS = "hjklyubnHJKLYUBN";

  /** Not used: this class only has static methods. */
  private KeyHandler() {
  }

  /**
   * Translates a keystroke into the character to put in a KEY message.
   *
   * @param stroke the keystroke lanterna read, may be null
   * @return the protocol character, or 0 if this key means nothing to the game
   */
  public static char toProtocolKey(KeyStroke stroke) {
    if (stroke == null) {
      return 0;
    }

    switch (stroke.getKeyType()) {
      case ArrowLeft:
        return 'h';
      case ArrowDown:
        return 'j';
      case ArrowUp:
        return 'k';
      case ArrowRight:
        return 'l';
      case EOF:
        return QUIT_KEY; // the terminal closed under us; leave the game politely
      case Character:
        break;           // fall through to the character test below
      default:
        return 0;
    }

    Character c = stroke.getCharacter();
    if (c == null) {
      return 0;
    }
    char ch = c;

    // control-C and control-D also mean "I am done here"
    if (stroke.isCtrlDown() && (ch == 'c' || ch == 'd' || ch == 3 || ch == 4)) {
      return QUIT_KEY;
    }
    if (ch == QUIT_KEY || MOVE_KEYS.indexOf(ch) >= 0) {
      return ch;
    }
    return 0;
  }

  /**
   * Tells whether a protocol character asks the server to end this client's game.
   *
   * @param key the character from {@link #toProtocolKey(KeyStroke)}
   * @return true if the key is the quit key
   */
  public static boolean isQuit(char key) {
    return key == QUIT_KEY;
  }
}
