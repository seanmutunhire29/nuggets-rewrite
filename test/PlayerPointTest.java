import java.net.InetSocketAddress;
import java.util.HashSet;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 
 * PlayerPointTest - tests for Player and Point working together.
 * A Player holds a Point for where it stands, and two sets of Points for what it
 * knows and what it sees. These tests use points that are actually instantiated
 * 
 * @author Sarah Parigela 11/09/26
 */
public class PlayerPointTest {

  // fake address not actually using the address
  private static final InetSocketAddress ADDRESS = new InetSocketAddress("127.0.0.1", 5001);

  // makes a player standing on the point we give it
  private static Player playerAt(Point p) {
    return new Player('A', "Alice", ADDRESS, p);
  }

  // makes a set out of whatever points we hand it
  private static HashSet<Point> setOf(Point... points) {
    HashSet<Point> set = new HashSet<>();
    for (Point p : points) {
      set.add(p);
    }
    return set;
  }

  // 1. the player holds the same Point it was given, and moveTo swaps in a new one
  @Test
  public void aPlayerHoldsItsPointAndMoveToChangesIt() {
    Point start = new Point(3, 4, '.');
    Player a = playerAt(start);
    assertSame(start, a.getPosition()); // holds the point we gave it

    Point next = new Point(3, 5, '.');
    a.moveTo(next);
    assertSame(next, a.getPosition()); // now it holds the new one
  }

  // 2. two players on the same Point, only the one we move should change
  @Test
  public void twoPlayersCanShareOnePointAndMoveIndependently() {
    Point samePoint = new Point(1, 2, '.');
    Player a = playerAt(samePoint);
    Player b = playerAt(samePoint);

    Point elsewhere = new Point(1, 1, '.');
    a.moveTo(elsewhere);

    assertSame(elsewhere, a.getPosition()); // a really moved
    assertSame(samePoint, b.getPosition()); // b stayed where it was
  }

  // 3. updateView puts every point in both visible and known
  @Test
  public void updateViewRecordsEachPointAsVisibleAndKnown() {
    Player a = playerAt(new Point(1, 1, '.'));
    assertTrue(a.getKnown().isEmpty()); // a new player hasn't seen anything yet

    a.updateView(setOf(new Point(1, 1, '.'), new Point(1, 2, '.')));

    assertEquals(2, a.getVisible().size()); // right number of points
    assertEquals(2, a.getKnown().size());
    assertTrue(a.getVisible().contains(new Point(1, 2, '.')));
    assertTrue(a.getKnown().contains(new Point(1, 2, '.')));
  }

  // 4. the sets care about the place of the point numerically not the symbol
  @Test
  public void aPointIsFoundByItsPlaceNotByItsSymbol() {
    Player a = playerAt(new Point(1, 1, '.'));
    a.updateView(setOf(new Point(1, 2, '.')));

    // same place but different symbol: should still be found
    assertTrue(a.getVisible().contains(new Point(1, 2, '#')));
    assertTrue(a.getKnown().contains(new Point(1, 2, '#')));

    // different place, should not be found
    assertFalse(a.getVisible().contains(new Point(1, 3, '#')));
    assertFalse(a.getKnown().contains(new Point(1, 3, '#')));
  }

  // 5. when we look at places twice visible is replaced, known keeps everything, no duplicates
  @Test
  public void seeingAgainReplacesVisibleButKeepsKnown() {
    Player a = playerAt(new Point(1, 1, '.'));
    a.updateView(setOf(new Point(1, 1, '.'), new Point(1, 2, '.'))); // sees A and B
    a.updateView(setOf(new Point(1, 2, '.'), new Point(1, 3, '.'))); // sees B and C

    assertEquals(2, a.getVisible().size()); // only B and C now
    assertFalse(a.getVisible().contains(new Point(1, 1, '.'))); // A is out of view
    assertEquals(3, a.getKnown().size()); // A, B, C (B only counted once)
    assertTrue(a.getKnown().contains(new Point(1, 1, '.'))); // A is still remembered
  }

  // 6. the player keeps its own copy so clearing the set afterward doesnt change anything
  @Test
  public void thePlayerKeepsItsOwnCopyOfWhatItSaw() {
    Player a = playerAt(new Point(1, 1, '.'));
    HashSet<Point> seenPoints = setOf(new Point(1, 1, '.'), new Point(1, 2, '.'));
    a.updateView(seenPoints);

    seenPoints.clear(); // wipe our set after handing it over

    assertEquals(2, a.getVisible().size()); // the player still has its points
    assertEquals(2, a.getKnown().size());
    assertTrue(a.getVisible().contains(new Point(1, 2, '.')));
  }
}
