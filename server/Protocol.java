/**
 * Protocol - helper methods for taking apart the text of a message from a client.
 * A message is a keyword, then a space, then the rest, like "PLAY Alice Smith" or "KEY l".
 * Both methods split at the first space only, so the rest keeps any spaces it has.
 * @author Team Sesami
 */

class Protocol {

  /** Not used: this class only has static methods. */
  private Protocol() {
  }

  /**
   * Gives the keyword at the start of a message: everything before the first space.
   * If there is no space, the whole message is the keyword.
   *
   * @param s the text of a message, such as "PLAY Alice"
   * @return the first word, such as "PLAY"
   */
  public static String firstWord(String s) {
    int space = s.indexOf(' ');
    return space < 0 ? s : s.substring(0, space);
  }

  /**
   * Gives everything after the first space, exactly as it was sent. Nothing is trimmed,
   * so "KEY  " (a key press of the space bar) gives " ".
   *
   * @param s the text of a message, such as "PLAY Alice Smith"
   * @return the rest of the message, such as "Alice Smith"; empty if there is no space
   */
  public static String restOfMessage(String s) {
    int space = s.indexOf(' ');
    return space < 0 ? "" : s.substring(space + 1);
  }
}
