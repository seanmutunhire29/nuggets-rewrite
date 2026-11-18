import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Game holds the players and the rules of one Nuggets game.
 * <p>
 * It keeps up to {@value #MAX_PLAYERS} players in fixed slots and the current
 * spectators, and applies joins, quits, moves, swaps, and gold pickup. It builds
 * each client's display string and the game-over summary. It asks {@link Grid}
 * about the board: the map, the gold piles, and what is visible from a point.
 * <p>
 * Game never sends messages. Server calls Game, then sends the results and
 * strings to clients, so Game can be tested with no network.
 * <p>
 * A player who quits keeps its slot, so its letter is never reused and it stays
 * in the final summary. Game is not thread-safe; the server uses one thread.
 * @author Sarah Parigela 11/9/26
 */

public class Game {

  // constants

  /** Longest stored player name, in characters. Longer names are truncated. */
  public static final int MAX_NAME_LENGTH = 50;

  /** Most players in one game; one per letter A to Z. */
  public static final int MAX_PLAYERS = 26;

  /** How a pile of gold is drawn on a display. */
  public static final char GOLD_SYMBOL = '*';

  /** How a player sees itself on its own display. */
  public static final char SELF_SYMBOL = '@';

  // fields

  /** The board: the map, the gold piles, and visibility. Shared by all players. */
  public final Grid grid;

  /** The seeded random generator, so the same seed repeats the same game. */
  private final Random rng;

  /** The players in fixed slots; slot i holds the player whose letter is 'A' + i. */
  private final Player[] players = new Player[MAX_PLAYERS];

  /** Number of slots used so far; also the number of the next letter to give out. */
  private int playerCount = 0;

  /**
   * Finds the player who sent a message, active or not. Players who quit stay in
   * this map, so they cannot rejoin.
   */
  private final Map<InetSocketAddress, Player> playersByAddress = new HashMap<>();

  /** Addresses of the current spectators. A spectator who quits is removed. */
  private final Set<InetSocketAddress> spectators = new LinkedHashSet<>();

  // constructor

  /**
   * Creates a game on the given board and drops the gold piles onto it.
   *
   * @param grid the board, already loaded from the map file
   * @param rng  the seeded random generator; used to place gold now and to choose
   *             where new players start later
   */
  public Game(Grid grid, Random rng) {
    this.grid = grid;
    this.rng = rng;
    grid.placeGold(rng);
  }

  // small helpers

  /**
   * Returns the letter for a slot number. Slot 0 gives 'A', slot 1 gives 'B', and
   * so on.
   *
   * @param index the slot number, 0 to {@code MAX_PLAYERS - 1}
   * @return the letter for that slot
   */
  public static char letterFor(int index) {
    return (char) ('A' + index);
  }

  /**
   * Returns the slot number for a letter. 'A' gives 0, 'B' gives 1, and so on.
   *
   * @param letter a player letter, 'A' to 'Z'
   * @return the slot number for that letter
   */
  public static int indexFor(char letter) {
    return letter - 'A';
  }

  /**
   * Cleans a player's real name for storage and for the game-over summary.
   * Truncates to {@value #MAX_NAME_LENGTH} characters first, then replaces every
   * character that is not graphic, a space, or a tab with an underscore. Server
   * calls this before {@link #addPlayer}.
   *
   * @param raw the name exactly as the client sent it
   * @return the cleaned name
   */
  public static String cleanName(String raw) {
    String name = raw;
    if (name.length() > MAX_NAME_LENGTH) {
      name = name.substring(0, MAX_NAME_LENGTH);
    }
    return name.replaceAll("[^\\p{Graph} \\t]", "_");
  }

  // spectators

  /**
   * Adds a spectator. There is no limit on spectators.
   *
   * @param a the address of the client that sent SPECTATE
   */
  public void addSpectator(InetSocketAddress a) {
    spectators.add(a);
  }

  /**
   * Removes a spectator, such as when it quits. Does nothing if the address is not
   * a spectator.
   *
   * @param a the spectator's address
   */
  public void removeSpectator(InetSocketAddress a) {
    spectators.remove(a);
  }

  /**
   * Tells whether an address belongs to a current spectator.
   *
   * @param a the address to check
   * @return true if the address is a current spectator
   */
  public boolean isSpectator(InetSocketAddress a) {
    return spectators.contains(a);
  }

  /**
   * Returns the current spectators, so Server can send each one an update.
   *
   * @return a read-only set of spectator addresses; callers must not change it
   */
  public Set<InetSocketAddress> getSpectators() {
    return Collections.unmodifiableSet(spectators);
  }

  // players

  /**
   * Tells whether every letter has been given out. Once true, no more players can
   * join, even if some earlier players have quit.
   *
   * @return true if {@value #MAX_PLAYERS} players have ever joined
   */
  public boolean isFull() {
    return playerCount == MAX_PLAYERS;
  }

  /**
   * Creates the next player and drops it on a random empty room spot, meaning a
   * room spot with no gold and no active player. The player gets the next unused
   * letter and an empty purse, and starts with what it can see from that spot. The
   * player is stored in the next free slot and remembered by address. An address that
   * already joined, even one that has quit, cannot join again.
   *
   * @param name    the player's name, already cleaned by {@link #cleanName}
   * @param address the address of the client that sent PLAY
   * @return the new player, or null if the game is full, no room spot is free, or this
   *         address already joined
   */
  public Player addPlayer(String name, InetSocketAddress address) {
    if (isFull()) {
      return null;
    }
    if (playersByAddress.containsKey(address)) { // one player per address, even one that quit
      return null;
    }
    List<Point> empty = new ArrayList<>();
    for (Point spot : grid.getRoomSpotsList()) {
      if (grid.goldAt(spot) == 0 && playerAt(spot) == null) {
        empty.add(spot);
      }
    }
    if (empty.isEmpty()) {
      return null;
    }
    Point start = empty.get(rng.nextInt(empty.size()));
    Player player = new Player(letterFor(playerCount), name, address, start);
    player.updateView(grid.visibleFrom(start));
    players[playerCount] = player;
    playerCount++;
    playersByAddress.put(address, player);
    return player;
  }

  /**
   * Marks a player inactive. The slot and letter stay, so the letter is never
   * reused and the player still appears in the summary. It is no longer drawn on
   * any display and no longer blocks a spot.
   *
   * @param player the player who quit
   */
  public void quitPlayer(Player player) {
    player.quit();
  }

  /**
   * Finds the player who owns an address, whether or not still active.
   *
   * @param a the sender's address
   * @return that player, or null if the address never joined as a player
   */
  public Player playerFor(InetSocketAddress a) {
    return playersByAddress.get(a);
  }

  /**
   * Finds the active player standing on a gridpoint. Players who quit are ignored.
   *
   * @param p the gridpoint to check
   * @return the active player there, or null if none
   */
  public Player playerAt(Point p) {
    for (int i = 0; i < playerCount; i++) {
      if (players[i].isActive() && players[i].getPosition().equals(p)) {
        return players[i];
      }
    }
    return null;
  }

  /**
   * Returns the players who have not quit, in letter order.
   *
   * @return a new list of the active players; empty if there are none
   */
  public List<Player> getActivePlayers() {
    List<Player> list = new ArrayList<>();
    for (int i = 0; i < playerCount; i++) {
      if (players[i].isActive()) {
        list.add(players[i]);
      }
    }
    return list;
  }

  // moving and collecting

  /**
   * Tells whether a gridpoint is a room spot or a passage spot, the only places a
   * player can stand.
   *
   * @param r the row of the gridpoint
   * @param c the column of the gridpoint
   * @return true for a room spot or passage spot; false for a wall, rock, or
   *         anywhere off the map
   */
  private boolean isSpot(int r, int c) {
    return grid.isRoomSpot(r, c) || grid.isPassageSpot(r, c);
  }

  /**
   * Picks up any gold pile at a gridpoint and adds it to the player's purse. The
   * pile is removed from the board by {@link Grid#takeGold(Point)}.
   *
   * @param player the player who just moved onto the point
   * @param target the gridpoint the player moved onto
   * @return the nuggets collected, or 0 if there was no pile
   */
  private int collectGold(Player player, Point target) {
    int n = grid.takeGold(target);
    player.adjustPurse(n);
    return n;
  }

  /**
   * Moves a player one step in a direction, or repeatedly until blocked if
   * sprinting.
   * <p>
   * The direction is {@code (dr, dc)}, row first: {@code (0, 1)} is right and
   * {@code (-1, 0)} is up, because row 0 is the top. A step is allowed only onto a
   * room spot or passage spot. Moving onto another active player swaps the two
   * places, and each player pickpockets a random part of the other's purse (extra credit #3).
   * On every step the player collects any gold there, and its visible and
   * known sets are updated, so gridpoints seen in the middle of a sprint stay on
   * its map. A swapped player's sets are updated too. A sprint also stops when no
   * gold remains.
   *
   * @param player the player to move; must be active
   * @param dr     the row change per step: -1, 0, or 1
   * @param dc     the column change per step: -1, 0, or 1
   * @param sprint true to repeat the step until it is blocked
   * @return the total nuggets collected during this move; 0 if none, or if the
   *         first step was blocked
   */
  public int move(Player player, int dr, int dc, boolean sprint) {
    if (!player.isActive()) {
      return 0;
    }
    int collected = 0;
    do {
      int r = player.getPosition().getRow() + dr;
      int c = player.getPosition().getCol() + dc;
      if (!isSpot(r, c)) {
        break;                                       // wall, rock, or off the map
      }
      Point target = grid.pointAt(r, c);
      Player other = playerAt(target);
      if (other != null) {                           // swap; other takes our spot first
        pickpocket(player, other); // extra credit #3
        other.moveTo(player.getPosition());
        player.moveTo(target);
        other.updateView(grid.visibleFrom(other.getPosition()));
      } else {
        player.moveTo(target);
      }
      collected += collectGold(player, target);
      player.updateView(grid.visibleFrom(target));
      if (grid.getGoldRemaining() == 0) {
        break;                                       // all gold gone, stop moving
      }
    } while (sprint);
    return collected;
  }

  // displays

  /**
   * Makes an empty canvas the size of the grid, filled with rock (blanks).
   *
   * @return a new character array indexed [row][column]
   */
  private char[][] blankCanvas() {
    char[][] canvas = new char[grid.getNumRows()][grid.getNumCols()];
    for (char[] row : canvas) {
      Arrays.fill(row, Grid.ROCK);
    }
    return canvas;
  }

  /**
   * Joins the rows of a canvas into one display string, each row ending in a
   * newline.
   *
   * @param canvas the finished canvas
   * @return nrows lines, each ncols characters plus a newline
   */
  private String rowsToString(char[][] canvas) {
    StringBuilder sb = new StringBuilder();
    for (char[] row : canvas) {
      sb.append(row).append('\n');
    }
    return sb.toString();
  }

  /**
   * Builds one player's display string, drawn in layers. First the map the player
   * has ever seen, then gold piles currently in view, then other active players
   * currently in view, and last the player's own position as
   * {@value #SELF_SYMBOL}. Gold and players that are out of view are not drawn.
   * <p>
   * The string has one line per grid row, each exactly as wide as the grid and
   * ending in a newline, as the DISPLAY message requires.
   *
   * @param player the player whose display to build
   * @return the display string for that player
   * @see #rowsToString(char[][])
   */
  public String displayFor(Player player) {
    char[][] canvas = blankCanvas();
    for (Point p : player.getKnown()) {
      canvas[p.getRow()][p.getCol()] = p.getSymbol();
    }
    Set<Point> visible = player.getVisible();
    for (Point pile : grid.goldLocations()) {
      if (visible.contains(pile)) {
        canvas[pile.getRow()][pile.getCol()] = GOLD_SYMBOL;
      }
    }
    for (Player other : getActivePlayers()) {
      Point at = other.getPosition();
      if (other != player && visible.contains(at)) {
        canvas[at.getRow()][at.getCol()] = other.getLetter();
      }
    }
    Point me = player.getPosition();
    canvas[me.getRow()][me.getCol()] = SELF_SYMBOL;
    return rowsToString(canvas);
  }

  /**
   * Builds the spectator's display string: the whole map, every gold pile, and
   * every active player's letter. There is no {@value #SELF_SYMBOL}, because a
   * spectator is not on the map. Uses the same line format as
   * {@link #displayFor(Player)}.
   *
   * @return the full-view display string
   */
  public String spectatorDisplay() {
    char[][] canvas = blankCanvas();
    for (int r = 0; r < grid.getNumRows(); r++) {
      for (int c = 0; c < grid.getNumCols(); c++) {
        canvas[r][c] = grid.charAt(r, c);
      }
    }
    for (Point pile : grid.goldLocations()) {
      canvas[pile.getRow()][pile.getCol()] = GOLD_SYMBOL;
    }
    for (Player p : getActivePlayers()) {
      Point at = p.getPosition();
      canvas[at.getRow()][at.getCol()] = p.getLetter();
    }
    return rowsToString(canvas);
  }

  // end of game

  /**
   * Builds the rows of the game-over table, one per player who ever joined,
   * including players who quit. Each row is the letter, the purse right-aligned in
   * 10 columns, and the name, in letter order. Rows are joined by newlines with no
   * trailing newline. Server adds the "GAME OVER:" heading.
   *
   * @return the summary rows; an empty string if no one ever joined
   */
  public String summary() {
    List<String> lines = new ArrayList<>();
    for (int i = 0; i < playerCount; i++) {
      Player p = players[i];
      lines.add(String.format("%c %10d %s", p.getLetter(), p.getPurse(), p.getName()));
    }
    return String.join("\n", lines);
  }

  /**
   * Tells whether the game has ended, which happens when all gold has been
   * collected.
   *
   * @return true if no nuggets remain on the board
   */
  public boolean isOver() {
    return grid.getGoldRemaining() == 0;
  }

  /**
   * Returns the nuggets still on the board, which is the third number in every
   * GOLD message.
   *
   * @return the nuggets not yet collected
   */
  public int getGoldRemaining() {
    return grid.getGoldRemaining();
  }

  /**
   * Returns the board, so Server can send its size in the GRID message.
   *
   * @return the grid
   */
  public Grid getGrid() {
    return grid;
  }

  /**
   * Extra credit #3 (pickpocketing): when two players swap places, each takes a random number of nuggets, from 0
   * up to the whole purse, out of the other player's purse. Both amounts are worked out from
   * the purses as they were before the swap, so the two purses together never change and
   * neither goes below 0.
   *
   * @param a one of the two players who swapped
   * @param b the other player
   */
  private void pickpocket(Player a, Player b) {
    int aTakes = rng.nextInt(b.getPurse() + 1); // from 0 up to everything b had
    int bTakes = rng.nextInt(a.getPurse() + 1);
    a.adjustPurse(aTakes - bTakes);
    b.adjustPurse(bTakes - aTakes);
  }
}
