import java.net.InetSocketAddress;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * PlayerTest- JUnit tests for the Player class
 * CS50 Fall Professor Kotz
 * 
 * @author Mia Jacobson 11/09/26
 */

public class PlayerTest {
    private Player player;
    
    @BeforeEach
    void setUp(){
        player = new Player('M', "Mia", new InetSocketAddress("localhost", 12345), new Point(1, 1, Grid.ROOM_SPOT));
    }

    @Test
    void purseIncreasesByGoldAmount(){
        player.adjustPurse(10);
        assertEquals(10, player.getPurse());
    }

    @Test
    void moveToUpdatesPosition(){
        Point newPosition = new Point(2, 2, Grid.PASSAGE_SPOT);
        player.moveTo(newPosition);
        assertEquals(newPosition, player.getPosition());
    }

      @Test
    void quitSetsActiveToFalse(){
        player.quit();
        assertFalse(player.getStatus());
    }

    @Test
    void isActiveReturnsTrueWhenActive(){
        assertTrue(player.getStatus());
    }

    @Test
    void isActiveReturnsFalseWhenInactive(){
        player.quit();
        assertFalse(player.getStatus());
    }


    @Test
    void whatPlayerSeesFillsKnownAndVisible(){
        Point p = new Point(1, 1, Grid.ROOM_SPOT);

        HashSet<Point> visiblePoints = new HashSet<>();
        visiblePoints.add(p);

        player.updateView(visiblePoints);

        assertTrue(player.getKnown().contains(p));
        assertTrue(player.getVisible().contains(p));
    }

}