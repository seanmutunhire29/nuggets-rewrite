import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * GameTest - tests for the Game class.
 * Checks the gold at the start, names, spectators, players, movement, gold,
 * swapping, displays, the summary, and the end of the game.
 *
 * @author Sarah Parigela 11/09/26
 */
public class GameTest {

  // one open room, rows 1-6 and columns 1-12. 72 room spots, plenty of space for
  // 26 players and up to 30 piles of gold
  private static final String[] BIG_ROOM = {
    "+------------+",
    "|............|",
    "|............|",
    "|............|",
    "|............|",
    "|............|",
    "|............|",
    "+------------+"
  };

  // two rooms joined by a passage on row 2. room A is columns 1-6, room B is columns 11-16
  private static final String[] TWO_ROOMS = {
    "+------+  +------+",
    "|......|  |......|",
    "|......####......|",
    "|......|  |......|",
    "|......|  |......|",
    "|......|  |......|",
    "|......|  |......|",
    "+------+  +------+"
  };

  private Grid grid;
  private Game game;

  // runs before every test so each one gets a fresh grid and game
  @BeforeEach
  public void setUp() throws IOException {
    grid = gridFrom(BIG_ROOM);
    game = new Game(grid, new Random(42)); // fixed seed so every run is the same
  }

  // Grid reads from a file, so write the map to a temp file and load it
  private static Grid gridFrom(String[] lines) throws IOException {
    Path file = Files.createTempFile("map", ".txt");
    file.toFile().deleteOnExit();
    Files.write(file, Arrays.asList(lines));
    return new Grid(file.toString(), new HashMap<>());
  }

  // a fake client address, each number gives a different one
  private static InetSocketAddress addr(int n) {
    return new InetSocketAddress("127.0.0.1", 5000 + n);
  }

  // puts a player on a spot and has it look around from there. moveTo never collects gold
  private void placeAt(Player p, int r, int c) {
    Point at = grid.pointAt(r, c);
    p.moveTo(at);
    p.updateView(grid.visibleFrom(at));
  }

  // removes the gold at one spot, if there is any
  private void clearGoldAt(int r, int c) {
    grid.takeGold(grid.pointAt(r, c));
  }

  // how many times a character shows up in a string
  private static int count(String text, char ch) {
    int n = 0;
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == ch) {
        n++;
      }
    }
    return n;
  }

  // 250 nuggets split into 10 to 30 piles, every pile has at least 1
  @Test
  public void constructorDrops250NuggetsInto10To30PilesOfAtLeastOne() {
    int total = 0;
    for (Point pile : grid.goldLocations()) {
      assertTrue(grid.goldAt(pile) >= 1); // no empty piles
      total += grid.goldAt(pile);
    }
    int piles = grid.goldLocations().size();
    assertTrue(piles >= 10 && piles <= 30, "piles: " + piles); // message shows the count if it fails
    assertEquals(250, total); // the piles really add up
    assertEquals(250, game.getGoldRemaining()); // and so does the counter
    assertFalse(game.isOver());
  }

  // same seed should give the exact same gold, so a game can be repeated
  @Test
  public void theSameSeedGivesTheSameGold() throws IOException {
    Grid g1 = gridFrom(BIG_ROOM);
    Grid g2 = gridFrom(BIG_ROOM);
    new Game(g1, new Random(7)); // making the game is what drops the gold
    new Game(g2, new Random(7));
    assertEquals(g1.goldLocations(), g2.goldLocations());
    for (Point pile : g1.goldLocations()) {
      assertEquals(g1.goldAt(pile), g2.goldAt(pile));
    }
  }

  // normal names stay the same (spaces and tabs are fine), and things you can't print
  // like a bell or a newline turn into underscores
  @Test
  public void cleanNameKeepsGoodNamesAndReplacesUnprintableCharacters() {
    assertEquals("Alice Smith", Game.cleanName("Alice Smith"));
    assertEquals("a b\tc", Game.cleanName("a b\tc"));
    assertEquals("Ali_ce", Game.cleanName("Ali\u0007ce"));
    assertEquals("a_b", Game.cleanName("a\nb"));
  }

  // long names get cut to 50 characters, and the cut happens before the replacing
  @Test
  public void cleanNameCutsToFiftyCharactersBeforeReplacing() {
    assertEquals(50, Game.cleanName("x".repeat(60)).length());
    String raw = "a".repeat(49) + "\u0007" + "zzz"; // the 50th character is the bell
    assertEquals("a".repeat(49) + "_", Game.cleanName(raw));
  }

  // add, add the same one again (still just one), then remove
  @Test
  public void spectatorsCanBeAddedAndRemovedAndDuplicatesCountOnce() {
    game.addSpectator(addr(1));
    game.addSpectator(addr(1));
    assertTrue(game.isSpectator(addr(1)));
    assertEquals(1, game.getSpectators().size());
    game.removeSpectator(addr(1));
    assertFalse(game.isSpectator(addr(1)));
  }

  // letters go out in order, and every new player starts on a room spot with no gold on it
  @Test
  public void thePlayersGetLettersInOrderAndStartOnFreeRoomSpots() {
    Player a = game.addPlayer("Alice", addr(1));
    Player b = game.addPlayer("Bob", addr(2));
    Player c = game.addPlayer("Carol", addr(3));
    assertEquals('A', a.getLetter());
    assertEquals('B', b.getLetter());
    assertEquals('C', c.getLetter());
    for (Player p : new Player[] {a, b, c}) {
      Point at = p.getPosition();
      assertTrue(grid.isRoomSpot(at.getRow(), at.getCol())); // starts on floor
      assertEquals(0, grid.goldAt(at)); // not on top of gold
    }
  }

  // after 26 players the game is full and the 27th gets turned away
  @Test
  public void theGameRefusesThe27thPlayer() {
    for (int i = 0; i < Game.MAX_PLAYERS; i++) {
      assertNotNull(game.addPlayer("P" + i, addr(i)));
    }
    assertTrue(game.isFull());
    assertNull(game.addPlayer("TooLate", addr(100)));
  }

  // a player who quits is inactive but still found by its address, and its letter
  // never gets handed out again
  @Test
  public void aPlayerWhoQuitsKeepsItsLetterAndIsStillFoundByAddress() {
    Player a = game.addPlayer("Alice", addr(1));
    game.addPlayer("Bob", addr(2));
    game.quitPlayer(a);
    assertFalse(a.isActive());
    assertSame(a, game.playerFor(addr(1)));
    assertEquals('C', game.addPlayer("Carol", addr(3)).getLetter()); // not 'A' again
  }

  // walls block. up from the top row of the room is the wall, so nothing happens
  @Test
  public void aStepIntoAWallDoesNotMoveThePlayer() {
    Player a = game.addPlayer("Alice", addr(1));
    placeAt(a, 1, 5); // the top row
    Point before = a.getPosition();
    assertEquals(0, game.move(a, -1, 0, false)); // nothing collected
    assertEquals(before, a.getPosition()); // and we didn't move
  }

  // the direction is (row change, column change): up is (-1, 0), right is (0, 1),
  // and a diagonal changes both
  @Test
  public void directionsAreRowFirstAndDiagonalsMoveBoth() {
    Player a = game.addPlayer("Alice", addr(1));
    clearGoldAt(2, 5); // nothing to pick up on the way
    clearGoldAt(2, 6);
    clearGoldAt(3, 7);
    placeAt(a, 3, 5);
    game.move(a, -1, 0, false);
    assertEquals(grid.pointAt(2, 5), a.getPosition()); // up one row
    game.move(a, 0, 1, false);
    assertEquals(grid.pointAt(2, 6), a.getPosition()); // right one column
    game.move(a, 1, 1, false);
    assertEquals(grid.pointAt(3, 7), a.getPosition()); // down and right
  }

  // stepping onto a pile puts it in the purse, removes the pile and lowers the gold remaining
  @Test
  public void steppingOntoAPileCollectsIt() {
    Player a = game.addPlayer("Alice", addr(1));
    Point pile = grid.goldLocations().iterator().next();
    int size = grid.goldAt(pile);
    int before = game.getGoldRemaining();
    // stand next to the pile: on its left if that's floor, otherwise on its right
    int dc = grid.isRoomSpot(pile.getRow(), pile.getCol() - 1) ? 1 : -1;
    placeAt(a, pile.getRow(), pile.getCol() - dc);
    assertEquals(size, game.move(a, 0, dc, false));
    assertEquals(size, a.getPurse());
    assertEquals(pile, a.getPosition());
    assertEquals(0, grid.goldAt(pile));
    assertEquals(before - size, game.getGoldRemaining());
  }

  // a sprint picks up every pile it passes and stops at the wall.
  // we start at column 1 so only the piles in columns 2 to 12 count
  @Test
  public void aSprintCollectsTheGoldAlongTheWayAndStopsAtTheWall() {
    Player a = game.addPlayer("Alice", addr(1));
    int row = -1;
    int expected = 0;
    for (int r = 1; r <= 6 && row < 0; r++) { // find a row with gold, while some gold is left elsewhere
      int inRow = 0;
      for (int c = 2; c <= 12; c++) {
        inRow += grid.goldAt(grid.pointAt(r, c));
      }
      if (inRow > 0 && inRow < game.getGoldRemaining()) {
        row = r;
        expected = inRow;
      }
    }
    assertTrue(row > 0, "needs gold in one row and gold left elsewhere");
    placeAt(a, row, 1);
    assertEquals(expected, game.move(a, 0, 1, true));
    assertEquals(expected, a.getPurse());
    assertEquals(grid.pointAt(row, 12), a.getPosition()); // the last spot before the wall
  }

  // moving onto another player swaps the two places
  @Test
  public void movingOntoAnotherPlayerSwapsThePlaces() {
    Player a = game.addPlayer("Alice", addr(1));
    Player b = game.addPlayer("Bob", addr(2));
    clearGoldAt(3, 5);
    clearGoldAt(3, 6);
    placeAt(a, 3, 5);
    placeAt(b, 3, 6);
    assertEquals(0, game.move(a, 0, 1, false));
    assertEquals(grid.pointAt(3, 6), a.getPosition());
    assertEquals(grid.pointAt(3, 5), b.getPosition());
    assertSame(a, game.playerAt(grid.pointAt(3, 6)));
  }

  // the spectator display has one full-width line per row, shows every pile and every
  // player's letter, and has no @ since a spectator isn't on the map
  @Test
  public void spectatorDisplayHasOneFullWidthLinePerRowAndNoAtSign() {
    game.addPlayer("Alice", addr(1));
    String d = game.spectatorDisplay();
    String[] lines = d.split("\n", -1);
    assertEquals(grid.getNumRows() + 1, lines.length); // the last newline leaves one empty piece
    for (int i = 0; i < grid.getNumRows(); i++) {
      assertEquals(grid.getNumCols(), lines[i].length());
    }
    assertEquals(grid.goldLocations().size(), count(d, '*')); // every pile shows
    assertEquals(1, count(d, 'A')); // the player shows as its letter
    assertEquals(0, count(d, '@'));
  }

  // gold in another room is out of sight, so the player's display doesn't draw it
  // even though it's on the board
  @Test
  public void goldInAnotherRoomIsNotDrawn() throws IOException {
    Grid g = gridFrom(TWO_ROOMS);
    Game two = new Game(g, new Random(42));
    Player a = two.addPlayer("Alice", addr(1));
    for (Point p : new ArrayList<>(g.goldLocations())) { // a copy, since we remove while looping
      if (p.getCol() <= 6) {
        g.takeGold(p); // clear room A so only room B has gold
      }
    }
    assertTrue(g.getGoldRemaining() > 0, "some gold must remain in room B");
    Point inRoomA = g.pointAt(3, 3);
    a.moveTo(inRoomA);
    a.updateView(g.visibleFrom(inRoomA));
    assertEquals(0, count(two.displayFor(a), '*')); // none of room B's gold in view
    assertTrue(count(two.spectatorDisplay(), '*') > 0); // but it's on the board
  }

  // the game-over table has to match the example in the Requirements Spec exactly
  @Test
  public void summaryMatchesTheRequirementsSpecExample() {
    Player a = game.addPlayer("Alice", addr(1));
    Player b = game.addPlayer("Bob", addr(2));
    Player c = game.addPlayer("Carol", addr(3));
    a.adjustPurse(4);
    b.adjustPurse(16);
    c.adjustPurse(230);
    String expected = "A          4 Alice\n"
                    + "B         16 Bob\n"
                    + "C        230 Carol";
    assertEquals(expected, game.summary());
  }

  // taking a pile lowers the gold remaining, and when every pile is gone the game is over
  @Test
  public void theGameIsOverWhenAllTheGoldIsGone() {
    Point pile = grid.goldLocations().iterator().next();
    int size = grid.goldAt(pile);
    grid.takeGold(pile);
    assertEquals(250 - size, game.getGoldRemaining());
    assertFalse(game.isOver());
    for (Point p : new ArrayList<>(grid.goldLocations())) { // a copy, since we remove while looping
      grid.takeGold(p);
    }
    assertEquals(0, game.getGoldRemaining());
    assertTrue(game.isOver());
  }

  // when two players swap each one takes some of the other's purse. the total stays the same
  // and nobody goes below 0
  @Test
  public void aSwapKeepsTheTotalAndNobodyGoesBelowZero() {
    Player a = game.addPlayer("Alice", addr(1));
    Player b = game.addPlayer("Bob", addr(2));
    a.adjustPurse(20);
    b.adjustPurse(30);
    clearGoldAt(3, 5);
    clearGoldAt(3, 6);
    placeAt(a, 3, 5);
    placeAt(b, 3, 6);
    assertEquals(0, game.move(a, 0, 1, false)); // a pickpocket doesn't count as collecting gold
    assertEquals(50, a.getPurse() + b.getPurse());
    assertTrue(a.getPurse() >= 0 && b.getPurse() >= 0);
  }

  // keep swapping and nuggets should move both ways, so each player can steal from the other
  @Test
  public void bothPlayersCanStealWhenTheyKeepSwapping() {
    Player a = game.addPlayer("Alice", addr(1));
    Player b = game.addPlayer("Bob", addr(2));
    a.adjustPurse(20);
    b.adjustPurse(30);
    clearGoldAt(3, 5);
    clearGoldAt(3, 6);
    placeAt(a, 3, 5);
    placeAt(b, 3, 6);
    boolean aGained = false;
    boolean bGained = false;
    for (int i = 0; i < 20; i++) { // each move swaps them again
      int aBefore = a.getPurse();
      int bBefore = b.getPurse();
      game.move(a, 0, i % 2 == 0 ? 1 : -1, false);
      assertTrue(a.getPurse() >= 0 && b.getPurse() >= 0); // nobody goes below 0
      if (a.getPurse() > aBefore) {
        aGained = true;
      }
      if (b.getPurse() > bBefore) {
        bGained = true;
      }
    }
    assertTrue(aGained); // the player who moved can steal
    assertTrue(bGained); // and so can the player who was moved onto
  }

  // a second PLAY from the same address is refused, so no ghost player is left on the map
  @Test
  public void aRepeatPlayFromTheSameAddressGetsNull() {
    assertNotNull(game.addPlayer("Alice", addr(1)));
    assertNull(game.addPlayer("Alice again", addr(1)));
    assertEquals(1, game.getActivePlayers().size()); // still just one player on the map
  }

  // quitting doesn't free the address, so a quit player can't come back under a new letter
  @Test
  public void aPlayerWhoQuitCannotRejoin() {
    Player a = game.addPlayer("Alice", addr(1));
    game.quitPlayer(a);
    assertNull(game.addPlayer("Alice", addr(1)));
    assertEquals('B', game.addPlayer("Bob", addr(2)).getLetter()); // A stays taken, so Bob is B
  }

  // 13 room spots and 10 piles of gold leave 3 free spots, and gold and players both take a spot
  @Test
  public void addPlayerReturnsNullWhenNoRoomSpotIsFree() throws IOException {
    Path file = Files.createTempFile("map", ".txt");
    file.toFile().deleteOnExit();
    Files.write(file, Arrays.asList("+-------------+", "|.............|", "+-------------+"));
    Grid tiny = new Grid(file.toString(), Map.of("gold", 10, "minpiles", 10, "maxpiles", 10));
    Game small = new Game(tiny, new Random(1));
    assertNotNull(small.addPlayer("A", addr(1)));
    assertNotNull(small.addPlayer("B", addr(2)));
    assertNotNull(small.addPlayer("C", addr(3)));
    assertNull(small.addPlayer("D", addr(4)));
    assertFalse(small.isFull()); // so it was the spots that ran out, not the 26 letters
  }

  // a player who quit should vanish from the spectator's view and from what others see
  @Test
  public void aPlayerWhoQuitDisappearsFromTheDisplays() {
    Player a = game.addPlayer("Alice", addr(1));
    Player b = game.addPlayer("Bob", addr(2));
    clearGoldAt(3, 5);
    clearGoldAt(3, 6);
    placeAt(a, 3, 5);
    placeAt(b, 3, 6);
    assertTrue(game.displayFor(b).indexOf('A') >= 0); // Bob sees Alice at first
    assertTrue(game.spectatorDisplay().indexOf('A') >= 0);
    game.quitPlayer(a);
    assertEquals(-1, game.displayFor(b).indexOf('A'));
    assertEquals(-1, game.spectatorDisplay().indexOf('A'));
    assertTrue(game.spectatorDisplay().indexOf('B') >= 0); // Bob is still there
  }
}
