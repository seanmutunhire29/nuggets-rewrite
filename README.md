# Nuggets

A Java implementation of the CS50 *Nuggets* game: a multi-player, terminal-based
treasure hunt. A server holds the map and the gold; players connect over UDP,
walk around collecting nuggets, and see only what their line of sight allows.
Spectators watch the whole map.

```
server/   the game server (Server, Game, Grid, Player, Point, ValidateMap,
          Messenger, Protocol, Log)
client/   the lanterna terminal client (Client, ClientState, ClientDisplay,
          KeyHandler)
test/     JUnit 5 tests, plus runtests.sh
maps/     map files
lib/      third-party jars, downloaded on demand by the build scripts
```

All classes are in the default package, matching the original layout.

## Building

Two equivalent builds. Either one works; both need a JDK 16 or newer.

**With Maven** (if `mvn` is installed):

```sh
mvn package          # compiles, runs the tests, builds target/nuggets.jar
```

**Without Maven** — `build.sh` downloads the lanterna jar into `lib/` the first
time it runs (verifying it against the SHA-1 published on Maven Central) and
then compiles with plain `javac`:

```sh
./build.sh           # classes land in build/
./build.sh --clean   # throw away build/ first
```

## Running

Start the server with a map file. It prints the UDP port it is listening on:

```sh
java -cp build Server maps/main.txt
serverPort=57082
```

Server options, each needing an integer, may follow the map file:

| option | meaning | default |
| --- | --- | --- |
| `--gold N` | total nuggets to scatter | 250 |
| `--minpiles N` | fewest piles | 10 |
| `--maxpiles N` | most piles | 30 |
| `--seed N` | random seed, for a repeatable game | a new game each run |

Then join, from this or any other machine, giving the host and that port:

```sh
# play
java -cp build:lib/lanterna-3.1.3.jar Client localhost 57082 Alice

# watch
java -cp build:lib/lanterna-3.1.3.jar Client localhost 57082
```

With a Maven build, use `java -cp target/nuggets.jar Client localhost 57082 Alice`
instead; the lanterna classes are already inside that jar.

The client takes an optional `--log logfile` so you can see the traffic — once
lanterna owns the terminal, stderr is no longer readable.

## Playing

| key | what it does |
| --- | --- |
| `h` `j` `k` `l` | move left, down, up, right |
| `y` `u` `b` `n` | move diagonally |
| arrow keys | same as `h` `j` `k` `l` |
| any of those in capitals | sprint: keep going until something blocks the way |
| `Q` | quit |

You are the `@`. Other players are their letters, gold piles are `*`, and the
map darkens where you cannot currently see. The banner across the top shows your
purse and how many nuggets are still out there. Walking into another player
swaps your places — and each of you pickpockets a random share of the other's
purse. The game ends when the last nugget is collected, and everyone gets the
final score table.

If your window is smaller than the map, the client says so and waits for you to
resize it.

## Testing

```sh
./test/runtests.sh    # or: mvn test
```

`runtests.sh` downloads the JUnit console-standalone jar into `lib/` if it is not
already there (or uses `~/cs50/shared/` if that copy exists).

## The protocol

UDP datagrams, one message per packet, UTF-8, no trailing newline.

Client to server:

```
PLAY <real name>
SPECTATE
KEY <single character>
```

Server to client:

```
OK <letter>
GRID <nrows> <ncols>
GOLD <collected> <purse> <remaining>
DISPLAY\n<nrows lines, each ncols characters, each ending in a newline>
QUIT <explanation>
ERROR <explanation>
```

A joining player gets `OK`, `GRID`, `GOLD`, `DISPLAY`, in that order; a spectator
gets the same without the `OK`. After every move the server sends a fresh `GOLD`
and `DISPLAY` to every player and spectator.
