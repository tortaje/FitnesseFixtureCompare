#!/usr/bin/env bash
# Starts a FitNesse server on the project's wiki so that CompareSuite and CompareErrorSuite can be run by hand
# from the FitNesse UI; the Slim JVM gets the project classpath and runs from the repository root, so the
# relative paths in the pages (src/test/resources/e2e/...) resolve. The server only listens on localhost (-lh):
# anyone reaching it could edit a page and run commands through COMMAND_PATTERN.
# Usage: scripts/fitnesse-ui.sh start | stop
set -euo pipefail
cd "$(dirname "$0")/.."

FITNESSE_PORT=${FITNESSE_PORT:-9090}
RUN_DIR=target/fitnesse-ui

stop() {
  if [ -f "$RUN_DIR/fitnesse.pid" ]; then
    kill "$(cat "$RUN_DIR/fitnesse.pid")" 2>/dev/null || true
    rm -f "$RUN_DIR/fitnesse.pid"
  fi
  echo "Stopped."
}

start() {
  if [ -f "$RUN_DIR/fitnesse.pid" ] && kill -0 "$(cat "$RUN_DIR/fitnesse.pid")" 2>/dev/null; then
    echo "Already running: http://localhost:$FITNESSE_PORT/CompareSuite  (stop it with: $0 stop)"
    return
  fi
  if curl -s -o /dev/null "http://localhost:$FITNESSE_PORT/"; then
    echo "Port $FITNESSE_PORT is already in use by another server: stop it, or set FITNESSE_PORT." >&2
    exit 1
  fi
  mkdir -p "$RUN_DIR"
  echo "Building classes and classpath..."
  mvn -q test-compile dependency:build-classpath -Dmdep.outputFile="$RUN_DIR/classpath.txt"
  CP="$PWD/target/test-classes:$PWD/target/classes:$(cat "$RUN_DIR/classpath.txt")"

  echo "Starting FitNesse on port $FITNESSE_PORT..."
  nohup java -cp "$CP" \
      "-DCOMMAND_PATTERN=java -cp $CP -Djava.util.logging.config.file=$PWD/src/test/resources/logging.properties %m" \
      fitnesseMain.FitNesseMain -p "$FITNESSE_PORT" -lh -d src/test/fitnesse -r FitNesseRoot -e 0 -o \
      > "$RUN_DIR/fitnesse.log" 2>&1 &
  echo $! > "$RUN_DIR/fitnesse.pid"
  for _ in $(seq 1 30); do
    curl -sf -o /dev/null "http://localhost:$FITNESSE_PORT/CompareSuite" && break
    sleep 1
  done
  if ! kill -0 "$(cat "$RUN_DIR/fitnesse.pid")" 2>/dev/null \
      || ! curl -sf -o /dev/null "http://localhost:$FITNESSE_PORT/CompareSuite"; then
    cat "$RUN_DIR/fitnesse.log"; stop; exit 1
  fi

  echo
  echo "Ready: http://localhost:$FITNESSE_PORT/CompareSuite       (click Suite: every row green)"
  echo "       http://localhost:$FITNESSE_PORT/CompareErrorSuite  (click Suite: every row yellow, as expected)"
  echo "Log:   $RUN_DIR/fitnesse.log"
  echo "Stop:  scripts/fitnesse-ui.sh stop"
}

case "${1:-}" in
  start) start ;;
  stop) stop ;;
  *) echo "usage: $0 start | stop" >&2; exit 2 ;;
esac
