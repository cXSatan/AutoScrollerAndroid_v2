#!/bin/sh
# Gradle startup script
DIR="$( cd "$( dirname "$0" )" && pwd )"
exec "$DIR/gradlew.bat" "$@"
