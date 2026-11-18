import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 
 * Tests for server.java
 * 
 * @author Sean Mutunhire 11/9/26
 */

class ServerTest {

    private static final String[] SMALL_ROOM = {
    "+------+  +------+",
    "|......|  |......|",
    "|......####......|",
    "|......|  |......|",
    "|......|  |......|",
    "|......|  |......|",
    "|......|  |......|",
    "+------+  +------+"
  };

    private TestableServer server;
    private Game game;
    private InetSocketAddress address;

    private static class TestableServer extends Server {
        public List<String> sentMessages = new ArrayList<>();

        public TestableServer(Log log) throws IOException {
            super(log);
        }

        @Override
        void sendTo(InetSocketAddress to, String text) throws IOException {
            sentMessages.add(text);
        }

        public void clear() {
            sentMessages.clear();
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        Log log = new Log();
        server = new TestableServer(log);
        Grid grid = gridFrom(SMALL_ROOM);
        game = new Game(grid, new Random(12345));
        address = new InetSocketAddress("127.0.0.1", 8080);
    }

    private static Grid gridFrom(String[] lines) throws IOException {
        Path file = Files.createTempFile("map", ".txt");
        file.toFile().deleteOnExit();
        Files.write(file, Arrays.asList(lines));
        return new Grid(file.toString(), new HashMap<>());
    }

    @Test
    void testHandleMessageUnknownKeyword() throws IOException {
        server.handleMessage("UNKNOWN_CMD", game, address);
        assertEquals("ERROR unknown key", server.sentMessages.get(0));
    }

    @Test
    void testPlayCommandBlankName() throws IOException {
        server.handleMessage("PLAY   ", game, address);
        assertTrue(server.sentMessages.get(0).contains("must provide"));
    }

    @Test
    void testSpectateCommandAddsSpectator() throws IOException {
        server.handleMessage("SPECTATE", game, address);
        // Verify grid and initial display messages are sent
        assertTrue(server.sentMessages.get(0).startsWith("GRID"));
        assertTrue(server.sentMessages.get(1).startsWith("GOLD"));
        assertTrue(server.sentMessages.get(2).startsWith("DISPLAY"));
    }

    @Test
    void testKeyQuitUnregisteredUser() throws IOException {
        server.handleMessage("KEY Q", game, address);
        assertEquals("QUIT Thanks for watching!", server.sentMessages.get(0));
    }

    @Test
    void testKeyNonQuitUnregisteredUser() throws IOException {
        server.handleMessage("KEY k", game, address);
        assertEquals("ERROR You can only use Q", server.sentMessages.get(0));
    }

    @Test
    void testKeyUnknownKeystrokeRegisteredPlayer() throws IOException {
        // Register a player first
        server.handleMessage("PLAY Alice", game, address);
        server.clear();

        // Send an invalid movement key
        server.handleMessage("KEY x", game, address);
        assertEquals("ERROR unknown keystroke", server.sentMessages.get(0));
    }

    @Test
    void testKeyQuitRegisteredPlayer() throws IOException {
        server.handleMessage("PLAY Alice", game, address);
        server.clear();

        server.handleMessage("KEY Q", game, address);
        assertEquals("QUIT Thanks for playing!", server.sentMessages.get(0));
        assertFalse(server.hasPlayer(address));
    }

    @Test
    void testSendOkFormat() throws IOException {
        server.handleMessage("PLAY Alice", game, address);
        server.clear();

        server.sendOk(address);
        assertTrue(server.sentMessages.get(0).startsWith("OK "));
    }

    @Test
    void testSendGoldFormat() throws IOException {
        server.sendGold(address, 10, 50, 100);
        assertEquals("GOLD 10 50 100", server.sentMessages.get(0));
    }

    @Test
    void testSendErrorFormat() throws IOException {
        server.sendError(address, "invalid action");
        assertEquals("ERROR invalid action", server.sentMessages.get(0));
    }

    @Test
    void testSendQuitFormat() throws IOException {
        server.sendQuit(address, "Goodbye");
        assertEquals("QUIT Goodbye", server.sentMessages.get(0));
    }

    @Test
    void testSendToRecordsPayload() throws IOException {
        server.sendTo(address, "DIRECT_MESSAGE");
        assertEquals("DIRECT_MESSAGE", server.sentMessages.get(0));
    }
}
