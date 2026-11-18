import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * GridPointTest - tests for Grid and Point working together.
 * Grid builds one Point for every spot on the map and uses Points as the keys in
 * its gold map. So we check the Points it hands out are right, and that gold is
 * found by place, which only works because of Point's equals and hashCode.
 *
 * @author Sarah Parigela 11/09/26
 */
public class GridPointTest {

  // small map. the lines are different lengths on purpose so Grid has to fill in the short ones
  private static final String[] SMALL = {
    "+-----+",
    "|.....###",
    "|.....|",
    "+-----+"
  };

  // one big open room, 72 room spots, plenty of space for the gold
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

  private Grid small;
  private Grid big;

  @BeforeEach
  public void setUp() throws IOException {
    small = gridFrom(SMALL);
    big = gridFrom(BIG_ROOM);
  }

  // Grid reads from a file, so write the map to a temp file and load it
  private static Grid gridFrom(String[] lines) throws IOException {
    Path file = Files.createTempFile("map", ".txt");
    file.toFile().deleteOnExit();
    Files.write(file, Arrays.asList(lines));
    return new Grid(file.toString(), new HashMap<>());
  }

  // 1. every Point knows its row, column and symbol, and there's only one Point per spot
  @Test
  public void pointsKnowTheirPlaceAndSymbol() {
    Point p = small.pointAt(1, 3);
    assertEquals(1, p.getRow());
    assertEquals(3, p.getCol());
    assertEquals('.', p.getSymbol());
    assertSame(p, small.pointAt(1, 3)); // asking again gives the very same Point
    assertNull(small.pointAt(-1, 0)); // off the map there's no Point
  }

  // 2. the size comes from the longest line and the short lines get filled in with rock
  @Test
  public void shortLinesArePaddedWithRock() {
    assertEquals(4, small.getNumRows());
    assertEquals(9, small.getNumCols());
    assertEquals(Grid.ROCK, small.pointAt(0, 8).getSymbol()); // row 0 only had 7 characters
  }

  // 3. gold is looked up by place, so a Point built separately still finds it
  @Test
  public void goldIsFoundByPlaceNotByTheObject() {
    big.placeGold(new Random(5));
    Point pile = big.goldLocations().iterator().next();
    Point twin = new Point(pile.getRow(), pile.getCol(), '#'); // same place, different symbol
    assertTrue(big.goldAt(pile) >= 1);
    assertEquals(big.goldAt(pile), big.goldAt(twin));
    assertTrue(big.goldLocations().contains(twin));
  }

  // 4. taking a pile removes it and lowers the total, and taking it again does nothing
  @Test
  public void takingAPileRemovesItAndLowersTheTotal() {
    big.placeGold(new Random(5));
    Point pile = big.goldLocations().iterator().next();
    int size = big.goldAt(pile);

    assertEquals(size, big.takeGold(pile));
    assertEquals(0, big.goldAt(pile));
    assertEquals(250 - size, big.getGoldRemaining());

    assertEquals(0, big.takeGold(pile)); // already gone
    assertEquals(250 - size, big.getGoldRemaining()); // so the total stays put
  }
}
