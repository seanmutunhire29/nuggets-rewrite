import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.HashMap;


/**
 * GridTest- JUnit tests for the Grid class
 * CS50 Fall Professor Kotz
 * 
 * @author Mia Jacobson 11/09/26
 */

public class GridTest {

    private Grid grid;

    @BeforeEach
    void setUp() throws IOException {
        Path mapFile = Files.createTempFile("testmap", ".txt");
        Files.writeString(mapFile, """
        +---------------------------------+          +-----------------+
        |.................................|          |.................|
        |.................................|          |.................|
        |......+---------------+..........|          |.................|
        |......|               |..........|          |.................|
        |......|               |..........############.................|
        |......|               |..........|          |.................|
        |......|               |..........|          +-----------------+
        +------+               +----------+
        """);
        grid = new Grid(mapFile.toString(), new HashMap<>());
    }

   
   @Test
   void missingMapThrowsIOException(){
   assertThrows(IOException.class, () -> new Grid("no/such/map.txt", new HashMap<>()));
   }

    @Test
    void checkMap(){
        assertEquals(9, grid.getNumRows());
        assertEquals(64, grid.getNumCols());

    }

    //placeGold()
    @Test
    void goldTotalsTo250(){
        grid.placeGold(new Random(1));
        assertEquals(250, grid.getGoldRemaining());
    }

    @Test
    void pilesAddUpTo250(){
        grid.placeGold(new Random(1));
        int total = 0;
        for (Point p : grid.goldLocations()){
            assertTrue(grid.goldAt(p) >= 1);   // every pile has at least 1 nugget
            total += grid.goldAt(p);
        }
        assertEquals(250, total);
    }

    @Test
    void validNumberOfPiles(){
        grid.placeGold(new Random(1));
        assertTrue(grid.getNumPiles() >= 10 && grid.getNumPiles() <= 30);
        assertEquals(grid.getNumPiles(), grid.goldLocations().size());
    }

    //takeGold()
    @Test
    void takeGoldReducesGoldRemainingByGoldAtPoint(){
        grid.placeGold(new Random(1));
        Point p = grid.goldLocations().iterator().next();
        int goldAtPoint = grid.goldAt(p);
        grid.takeGold(p);
        assertEquals(250 - goldAtPoint, grid.getGoldRemaining());
    }

    @Test
    void takeGoldRemovesGoldFromMap(){
        grid.placeGold(new Random(1));
        Point p = grid.goldLocations().iterator().next();
        grid.takeGold(p);
        assertEquals(0, grid.goldAt(p));
        assertFalse(grid.goldLocations().contains(p));
    }

    //isBoundary()
    @Test
    void isBoundaryWorks(){
        assertTrue(grid.isBoundary(0, 0)); //corner
        assertTrue(grid.isBoundary(0, 1)); //horizontal wall
        assertTrue(grid.isBoundary(1, 0)); //vertical wall
        assertTrue(grid.isBoundary(3, 10)); //middle wall
        assertFalse(grid.isBoundary(1, 1)); //room spot
        assertFalse(grid.isBoundary(-1, 2)); //rock above
        assertFalse(grid.isBoundary(5, 10)); //rock
        assertFalse(grid.isBoundary(9, 0)); //rock below
    }

    //isRoomSpot()
    @Test
    void isRoomSpotWorks(){
        assertTrue(grid.isRoomSpot(1, 1)); //room spot
        assertFalse(grid.isRoomSpot(0, 0)); //corner
        assertFalse(grid.isRoomSpot(0, 1)); //horizontal wall
        assertFalse(grid.isRoomSpot(1, 0)); //vertical wall
        assertFalse(grid.isRoomSpot(5, 10)); //rock
        assertFalse(grid.isRoomSpot(-1, 2)); //rock above
        assertFalse(grid.isRoomSpot(9, 0)); //rock below
    }

    //isPassageSpot()
    @Test
    void isPassageSpotWorks(){
        assertTrue(grid.isPassageSpot(5, 37)); //passage spot
        assertFalse(grid.isPassageSpot(0, 0)); //corner
        assertFalse(grid.isPassageSpot(0, 1)); //horizontal wall
        assertFalse(grid.isPassageSpot(1, 0)); //vertical wall
        assertFalse(grid.isPassageSpot(3, 10)); //middle wall
        assertFalse(grid.isPassageSpot(1, 1)); //room spot
        assertFalse(grid.isPassageSpot(5, 10)); //rock
        assertFalse(grid.isPassageSpot(-1, 2)); //rock above
        assertFalse(grid.isPassageSpot(9, 0)); //rock below
    }

    //goldAt()
    @Test
        void goldAtWorks(){
        grid.placeGold(new Random(1));

        Point goldPoint = grid.goldLocations().iterator().next();
        assertTrue(grid.goldAt(goldPoint) >= 1);

        Point nogoldPoint = new Point(0, 0,'+');
        assertEquals(0, grid.goldAt(nogoldPoint));
    }

    //charAt()
    @Test
    void charAtWorks(){
        assertEquals(Grid.CORNER_BOUNDARY, grid.charAt(0,0)); //corner
        assertEquals(Grid.HORIZONTAL_BOUNDARY, grid.charAt(0,1)); //horizontal wall
        assertEquals(Grid.VERTICAL_BOUNDARY, grid.charAt(1,0)); //vertical wall
        assertEquals(Grid.ROOM_SPOT, grid.charAt(1,1)); //room spot
        assertEquals(Grid.PASSAGE_SPOT, grid.charAt(5,37)); //passage spot
        assertEquals(Grid.ROCK, grid.charAt(5,10)); //rock
        assertEquals(Grid.ROCK, grid.charAt(-5,10)); //rock
    }

    //pointAt()
    @Test
    void pointAtInBoundsReturnsCorrectPoint(){
        assertEquals(new Point(0,0,Grid.CORNER_BOUNDARY), grid.pointAt(0,0));
    }

    @Test
    void pointAtOutOfBoundsReturnsNull(){
        assertNull(grid.pointAt(-5,10));
    }

    @Test
    void pointAtReturnsRightSymbol(){
        assertEquals(Grid.PASSAGE_SPOT, grid.pointAt(5, 37).getSymbol());
    }

    //goldLocations()
    @Test
    void goldLocationsAreValid(){
        assertEquals(0, grid.goldLocations().size());
        grid.placeGold(new Random(1));
        for (Point p : grid.goldLocations()){
            assertTrue(grid.isRoomSpot(p.getRow(), p.getCol()));
        }
    }

    //isInBounds()
    @Test
    void bottomRightCornerIsInBounds(){
        assertTrue(grid.isInBounds(8, 63));
    }

    @Test
    void columnLeftOfMapIsOutOfBounds(){
        assertFalse(grid.isInBounds(0, -1));
    }

    @Test
    void columnRightOfMapIsOutOfBounds(){
        assertFalse(grid.isInBounds(0,64));
    }

    @Test
    void rowBelowMapIsOutOfBounds(){
        assertFalse(grid.isInBounds(9, 0));
    }

    //isVisible()
    @Test
    void wallsBlockVisibility(){
        Point from = grid.pointAt(2, 20);
        Point to = grid.pointAt(7, 25);
        assertFalse(grid.isVisible(from, to));
    }

    @Test
    void cornersBlockVisibility(){
        Point from = grid.pointAt(2, 22);
        Point to = grid.pointAt(4, 24);
        assertFalse(grid.isVisible(from, to));
    }

    @Test
    void canSeePassageEntrance(){
        Point from = grid.pointAt(3, 30);
        Point to = grid.pointAt(5, 34);
        assertTrue(grid.isVisible(from, to));
    }

    @Test
    void canSeeOneStepAheadInPassage(){
        Point from = grid.pointAt(5, 36);
        Point to = grid.pointAt(5, 37);
        assertTrue(grid.isVisible(from, to));
    }

    @Test
    void canSeeOneStepBehindInPassage(){
        Point from = grid.pointAt(5, 36);
        Point to = grid.pointAt(5, 35);
        assertTrue(grid.isVisible(from, to));
    }

    @Test
    void canNotSeeTwoStepsAheadInPassage(){
        Point from = grid.pointAt(5,36);
        Point to = grid.pointAt(5,38);
        assertFalse(grid.isVisible(from, to));
    }

    @Test
    void wallsAreVisible(){
        Point from = grid.pointAt(7,3);
        Point to = grid.pointAt(0,3);
        assertTrue(grid.isVisible(from, to));
    }

    @Test
    void lineGrazingCornerIsNotBlocked(){
        Point from = grid.pointAt(2,23);
        Point to = grid.pointAt(4, 24);
        assertTrue(grid.isVisible(from, to));
    }


    //visibleFrom()
    @Test
    void visibleFromIncludesItself(){
        Point p = grid.pointAt(5, 36);
        assertTrue(grid.visibleFrom(p).contains(p));
    }

    @Test
    void visibleFromInAPassageContainsDirectNeighbors(){
        Set<Point> visibleFrom = grid.visibleFrom(grid.pointAt(5, 36));
        assertTrue(visibleFrom.contains(grid.pointAt(5, 37)));
        assertTrue(visibleFrom.contains(grid.pointAt(5, 35)));
        assertFalse(visibleFrom.contains(grid.pointAt(5, 38)));
        assertFalse(visibleFrom.contains(grid.pointAt(5, 34)));
    }
}
