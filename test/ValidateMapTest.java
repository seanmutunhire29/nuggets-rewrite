import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * ValidateMapTest- JUnit tests for ValidateMap
 * CS50 Fall Professor Kotz
 * 
 * @author Mia Jacobson 11/09/26
 */

public class ValidateMapTest {

    private Grid grid;

    @Test
    public void tooSmallMapIsRejected() throws IOException {
        Path mapFile = Files.createTempFile("testmap", ".txt");
        Files.writeString(mapFile, """
        +------+
        |......|
        |......|
        +------+
        """);
        grid = new Grid(mapFile.toString(), new HashMap<>());
        ValidateMap validator = new ValidateMap(grid);

        assertFalse(validator.hasEnoughSpots());
    }

    @Test
    public void horizontalAndVerticalBoundariesMeeetingDirectlyIsRejected() throws IOException {
        Path mapFile = Files.createTempFile("testmap", ".txt");
        Files.writeString(mapFile, """
        +---------------------------------+          +-----------------+
        |.................................|          |.................|
        |.................................|          |.................|
        |......+---------------+..........|          |.................|
        |......|               |..........|          |.................|
        |......|               |..........############.................|
        |......|               |..........|          |.................|
        +------+               +----------|          +-----------------+
        """);
        grid = new Grid(mapFile.toString(), new HashMap<>());
        ValidateMap validator = new ValidateMap(grid);

        assertFalse(validator.horizontalAndVerticalBoundariesMeetAtCorners());
    }

    @Test
    public void invalidCharAtOnePointIsRejected() throws IOException {
        Path mapFile = Files.createTempFile("testmap", ".txt");
        Files.writeString(mapFile, """
        +---------------------------------+          +-----------------+
        |.................................|          |.................|
        |.................................|          |.................|
        |......+---------------+..........|          |.................|
        |......|               |....p.....|          |.................|
        |......|               |..........############.................|
        |......|               |..........|          |.................|
        +------+               +----------+          +-----------------+
        """);
        grid = new Grid(mapFile.toString(), new HashMap<>());
        ValidateMap validator = new ValidateMap(grid);

        assertFalse(validator.everyPointIsValidChar());
    }

    @Test
    public void passageAtCornerIsRejected() throws IOException {
        Path mapFile = Files.createTempFile("testmap", ".txt");
        Files.writeString(mapFile, """
        +---------------------------------+          +-----------------+
        |.................................|          |.................|
        |.................................|          |.................|
        |......+---------------+..........|          |.................|
        |......|               |..........|          |.................|
        |......|               |..........|          |.................|
        |......|               |..........|          |.................|
        +------+               +----------############-----------------+
        """);
        grid = new Grid(mapFile.toString(), new HashMap<>());
        ValidateMap validator = new ValidateMap(grid);

        assertFalse(validator.passagesNeverMeetCorners());
    }

    @Test
    public void nonRectangularGridIsRejected()throws IOException {
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
        ValidateMap validator = new ValidateMap(grid);

        assertFalse(validator.gridIsRectangular(mapFile.toString()));
    }

    @Test
    public void gridThatWontFitInAMessageIsRejected() throws IOException {
        Path mapFile = Files.createTempFile("testmap", ".txt");
        Files.writeString(mapFile,".".repeat(70000));

        grid = new Grid(mapFile.toString(), new HashMap<>());
        ValidateMap validator = new ValidateMap(grid);

        assertFalse(validator.gridFitsInADisplayMessage());

    }

    @Test
    public void passagesWiderThanOneSpotAreRejected() throws IOException {
        Path mapFile = Files.createTempFile("testmap", ".txt");
        Files.writeString(mapFile, """
        +---------------------------------+          +-----------------+
        |.................................|          |.................|
        |.................................|          |.................|
        |......+---------------+..........|          |.................|
        |......|               |..........############.................|
        |......|               |..........############.................|
        |......|               |..........|          |.................|
        +------+               +----------+          +-----------------+
        """);

        grid = new Grid(mapFile.toString(), new HashMap<>());
        ValidateMap validator = new ValidateMap(grid);

        assertFalse(validator.passagesAreOnlyOneSpotWide());
    }

    @Test
    public void disconnectedMapIsRejected()  throws IOException {
        Path mapFile = Files.createTempFile("testmap", ".txt");
        Files.writeString(mapFile, """
        +---------------------------------+          +-----------------+
        |.................................|          |.................|
        |.................................|          |.................|
        |......+---------------+..........|          |.................|
        |......|               |..........|          |.................|
        |......|               |..........|          |.................|
        |......|               |..........|          |.................|
        +------+               +----------+          +-----------------+
        """);

        grid = new Grid(mapFile.toString(), new HashMap<>());
        ValidateMap validator = new ValidateMap(grid);

        assertFalse(validator.mapIsOneConnectedComponent());

    }

    @Test
    public void validMapIsAccepted()  throws IOException {
        Path mapFile = Files.createTempFile("testmap", ".txt");
        Files.writeString(mapFile, """
        +---------------------------------+          +-----------------+
        |.................................|          |.................|
        |.................................|          |.................|
        |......+---------------+..........|          |.................|
        |......|               |..........############.................|
        |......|               |..........|          |.................|
        |......|               |..........|          |.................|
        +------+               +----------+          +-----------------+
        """);

        grid = new Grid(mapFile.toString(), new HashMap<>());
        ValidateMap validator = new ValidateMap(grid);

        assertTrue(validator.hasEnoughSpots());
        assertTrue(validator.horizontalAndVerticalBoundariesMeetAtCorners());
        assertTrue(validator.everyPointIsValidChar());
        assertTrue(validator.passagesNeverMeetCorners());
        assertTrue(validator.gridIsRectangular(mapFile.toString()));
        assertTrue(validator.gridFitsInADisplayMessage());
        assertTrue(validator.passagesAreOnlyOneSpotWide());
        assertTrue(validator.mapIsOneConnectedComponent());
    }

}
