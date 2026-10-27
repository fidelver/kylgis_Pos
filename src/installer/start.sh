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

# KylGis POS requires Java 21. WebLaF 1.2.9 embeds XStream and needs
# explicit reflective access to selected JDK internals on the modular runtime.
KYLGIS_MODULAR_OPENS="--add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.text=ALL-UNNAMED --add-opens=java.desktop/java.awt.font=ALL-UNNAMED --add-opens=java.desktop/java.awt=ALL-UNNAMED"
if [ -n "${JDK_JAVA_OPTIONS:-}" ]; then
    JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS $KYLGIS_MODULAR_OPENS"
else
    JDK_JAVA_OPTIONS="$KYLGIS_MODULAR_OPENS"
fi
export JDK_JAVA_OPTIONS

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    PATH="$JAVA_HOME/bin:$PATH"
fi
JAVA_MAJOR=$(java -version 2>&1 | awk -F'[".]' '/version/ {print ($2 == 1 ? $3 : $2); exit}')
case "$JAVA_MAJOR" in
    21) ;;
    *) echo "ERROR: KylGis POS requires Java 21; detected ${JAVA_MAJOR:-unknown}" >&2; exit 64 ;;
esac

if [ -n "$NATIVE" ] && [ -d "$NATIVE" ]; then
    exec java -Xms512m -Xmx1024m "-splash:$DIR/kylgispos_splash_dark.png" \
        "-Djava.library.path=$NATIVE" "-Ddirname.path=$DIR/" \
        -jar "$DIR/kylgispos.jar" "$@"
else
    exec java -Xms512m -Xmx1024m "-splash:$DIR/kylgispos_splash_dark.png" \
        "-Ddirname.path=$DIR/" -jar "$DIR/kylgispos.jar" "$@"
fi
