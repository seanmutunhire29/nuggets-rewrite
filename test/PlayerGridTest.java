import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PlayerGridTest - tests for Player and Grid working together.
 * Grid works out what can be seen from a spot and Player keeps track of it. Game just
 * asks Grid.visibleFrom and hands the answer to Player.updateView. Rooms are lit and
 * passages are dark, so you can't see down a passage.
 *
 * @author Sarah Parigela 11/09/26
 */
public class PlayerGridTest {

  // one open room, rows 1-6 and columns 1-12
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

  // fake address, not actually used
  private static final InetSocketAddress ADDRESS = new InetSocketAddress("127.0.0.1", 5001);

  private Grid room;
  private Grid rooms;

  @BeforeEach
  public void setUp() throws IOException {
    room = gridFrom(BIG_ROOM);
    rooms = gridFrom(TWO_ROOMS);
  }

  // Grid reads from a file, so write the map to a temp file and load it
  private static Grid gridFrom(String[] lines) throws IOException {
    Path file = Files.createTempFile("map", ".txt");
    file.toFile().deleteOnExit();
    Files.write(file, Arrays.asList(lines));
    return new Grid(file.toString(), new HashMap<>());
  }

  // puts a new player on (r, c) and has it look around from there
  private static Player lookFrom(Grid g, int r, int c) {
    Point at = g.pointAt(r, c);
    Player p = new Player('A', "Alice", ADDRESS, at);
    p.updateView(g.visibleFrom(at));
    return p;
  }

  // 1. in an open room you see every room spot and the walls around it
  @Test
  public void aPlayerInAnOpenRoomSeesTheWholeRoom() {
    Player p = lookFrom(room, 3, 5);
    for (Point spot : room.getRoomSpotsList()) {
      assertTrue(p.getVisible().contains(spot));
    }
    assertTrue(p.getVisible().contains(room.pointAt(0, 5))); // top wall
    assertTrue(p.getVisible().contains(room.pointAt(3, 13))); // right wall
  }

  // 2. passages are dark: from the edge of a room you see the opening but not past it,
  // and inside the passage you only see one step each way
  @Test
  public void passagesAreDark() {
    Player atEdge = lookFrom(rooms, 2, 6);
    assertTrue(atEdge.getVisible().contains(rooms.pointAt(2, 7))); // the opening
    assertFalse(atEdge.getVisible().contains(rooms.pointAt(2, 8))); // but not the next spot

    Player inside = lookFrom(rooms, 2, 8);
    assertTrue(inside.getVisible().contains(rooms.pointAt(2, 7))); // one step back
    assertTrue(inside.getVisible().contains(rooms.pointAt(2, 9))); // one step ahead
    assertFalse(inside.getVisible().contains(rooms.pointAt(2, 6))); // two steps is too far
    assertFalse(inside.getVisible().contains(rooms.pointAt(2, 10)));
  }

  // 3. walk from room A through the passage into room B, looking at every step.
  // what's visible changes but what's known only grows
  @Test
  public void walkingThroughAPassageKeepsTheMapButChangesWhatIsVisible() {
    Player p = lookFrom(rooms, 2, 5); // start in room A
    for (int col = 6; col <= 12; col++) { // one step at a time along row 2
      Point next = rooms.pointAt(2, col);
      p.moveTo(next);
      p.updateView(rooms.visibleFrom(next));
    }
    // now in room B: we see room B and no longer see room A
    assertTrue(p.getVisible().contains(rooms.pointAt(3, 14)));
    assertFalse(p.getVisible().contains(rooms.pointAt(3, 3)));
    // but we still remember room A, and the passage spots we only saw while walking through
    assertTrue(p.getKnown().contains(rooms.pointAt(3, 3)));
    assertTrue(p.getKnown().contains(rooms.pointAt(2, 8)));
    assertTrue(p.getKnown().contains(rooms.pointAt(2, 9)));
  }
}
