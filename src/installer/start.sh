#!/bin/sh
# KylGis POS portable installer launcher
DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
OS=$(uname -s 2>/dev/null || echo unknown)
ARCH=$(uname -m 2>/dev/null || echo unknown)
NATIVE=
case "$OS/$ARCH" in
    Linux/i?86) NATIVE="$DIR/lib/Linux/i686-unknown-linux-gnu" ;;
    Linux/x86_64|Linux/amd64) NATIVE="$DIR/lib/Linux/x86_64-unknown-linux-gnu" ;;
    Darwin/i?86|Darwin/x86_64|Darwin/amd64) NATIVE="$DIR/lib/Mac_OS_X" ;;
esac

if [ -n "$NATIVE" ] && [ -d "$NATIVE" ]; then
    exec java -Xms512m -Xmx1024m "-splash:$DIR/kylgispos_splash_dark.png" \
        "-Djava.library.path=$NATIVE" "-Ddirname.path=$DIR/" \
        -jar "$DIR/kylgispos.jar" "$@"
else
    exec java -Xms512m -Xmx1024m "-splash:$DIR/kylgispos_splash_dark.png" \
        "-Ddirname.path=$DIR/" -jar "$DIR/kylgispos.jar" "$@"
fi
