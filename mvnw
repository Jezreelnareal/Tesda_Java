#!/bin/sh
set -eu

if command -v mvn >/dev/null 2>&1; then
  exec mvn "$@"
fi

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
URL=$(sed -n 's/^distributionUrl=//p' "$BASE_DIR/.mvn/wrapper/maven-wrapper.properties")
ARCHIVE=$(basename "$URL")
DIST=${ARCHIVE%-bin.zip}
CACHE_ROOT=${HOME}/.m2/wrapper/dists/jcash
MAVEN_HOME=$CACHE_ROOT/$DIST

if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  mkdir -p "$CACHE_ROOT"
  curl -fsSL "$URL" -o "$CACHE_ROOT/$ARCHIVE"
  unzip -q -o "$CACHE_ROOT/$ARCHIVE" -d "$CACHE_ROOT"
  rm -f "$CACHE_ROOT/$ARCHIVE"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
