#!/bin/bash
#
# runtests.sh - compile the server classes and the JUnit tests, then run the tests.
# usage: ./test/runtests.sh   (run it from anywhere; it finds the repository itself)
# Author: Sarah Parigela
#
set -euo pipefail
cd "$(dirname "$0")/.."

JUNIT_VERSION=1.11.4
JUNIT_JAR="lib/junit-platform-console-standalone-${JUNIT_VERSION}.jar"
JUNIT_URL="https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/${JUNIT_VERSION}/junit-platform-console-standalone-${JUNIT_VERSION}.jar"

# the jar used to live in the CS50 shared folder; use that copy if it is there
if [ -f "$HOME/cs50/shared/junit-platform-console-standalone-6.1.3.jar" ]; then
  JUNIT_JAR="$HOME/cs50/shared/junit-platform-console-standalone-6.1.3.jar"
elif [ ! -f "$JUNIT_JAR" ]; then
  echo "runtests.sh: $JUNIT_JAR not found; downloading from Maven Central..."
  mkdir -p lib
  if ! curl -fsSL "$JUNIT_URL" -o "$JUNIT_JAR.tmp"; then
    rm -f "$JUNIT_JAR.tmp"
    echo "runtests.sh: could not download the JUnit console jar; fetch it by hand:" >&2
    echo "  $JUNIT_URL" >&2
    exit 1
  fi
  mv "$JUNIT_JAR.tmp" "$JUNIT_JAR"
fi

mkdir -p build
javac -Xlint:all -cp "$JUNIT_JAR" -d build server/*.java test/*.java
java -jar "$JUNIT_JAR" execute --class-path build --scan-class-path --details=summary
