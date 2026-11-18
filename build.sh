#!/bin/bash
#
# build.sh - compile the Nuggets server and client with plain javac.
#
# This is the fallback build for machines without Maven; `mvn package` using the
# pom.xml at the top of the repo does the same job. Compiled classes land in
# build/ and the lanterna jar is vendored into lib/.
#
# usage: ./build.sh [--clean]
#
set -euo pipefail
cd "$(dirname "$0")"

LANTERNA_VERSION=3.1.3
LANTERNA_JAR="lib/lanterna-${LANTERNA_VERSION}.jar"
BASE_URL="https://repo1.maven.org/maven2/com/googlecode/lanterna/lanterna/${LANTERNA_VERSION}"

if [ "${1:-}" = "--clean" ]; then
  rm -rf build
fi

# ---------------------------------------------------------------- dependency
if [ ! -f "$LANTERNA_JAR" ]; then
  echo "build.sh: $LANTERNA_JAR not found; downloading from Maven Central..."
  mkdir -p lib
  tmp=$(mktemp -d)
  trap 'rm -rf "$tmp"' EXIT

  if ! curl -fsSL "${BASE_URL}/lanterna-${LANTERNA_VERSION}.jar" -o "$tmp/lanterna.jar"; then
    echo "build.sh: could not download lanterna." >&2
    echo "  Fetch it by hand and save it as $LANTERNA_JAR:" >&2
    echo "  ${BASE_URL}/lanterna-${LANTERNA_VERSION}.jar" >&2
    exit 1
  fi

  # verify against the SHA-1 published next to the jar
  if curl -fsSL "${BASE_URL}/lanterna-${LANTERNA_VERSION}.jar.sha1" -o "$tmp/lanterna.sha1"; then
    expected=$(tr -d '[:space:]' < "$tmp/lanterna.sha1" | cut -c1-40)
    if command -v shasum >/dev/null 2>&1; then
      actual=$(shasum -a 1 "$tmp/lanterna.jar" | cut -d' ' -f1)
    else
      actual=$(sha1sum "$tmp/lanterna.jar" | cut -d' ' -f1)
    fi
    if [ "$expected" != "$actual" ]; then
      echo "build.sh: checksum mismatch for lanterna jar." >&2
      echo "  expected $expected" >&2
      echo "  actual   $actual" >&2
      exit 1
    fi
    echo "build.sh: checksum verified ($actual)."
  else
    echo "build.sh: warning - could not fetch the published SHA-1; jar left unverified." >&2
  fi

  mv "$tmp/lanterna.jar" "$LANTERNA_JAR"
  echo "build.sh: saved $LANTERNA_JAR"
fi

# ---------------------------------------------------------------- compile
mkdir -p build
echo "build.sh: compiling server/ and client/ ..."
javac -Xlint:all -cp "$LANTERNA_JAR" -d build server/*.java client/*.java

cat <<MSG

build.sh: done. Classes are in build/.

  start a server:    java -cp build Server maps/main.txt [--seed N] [--gold N] [--minpiles N] [--maxpiles N]
  join as a player:  java -cp build:$LANTERNA_JAR Client <host> <port> <playername>
  watch as a spectator:
                     java -cp build:$LANTERNA_JAR Client <host> <port>
MSG
