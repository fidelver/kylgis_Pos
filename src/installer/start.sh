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

# WebLaF 1.2.9 embeds XStream 1.4.9 and needs reflective access to a
# handful of JDK internals on modular Java (9+). JDK_JAVA_OPTIONS is ignored
# by Java 8, but Java 9+ consumes these flags before the application starts,
# so one launcher remains compatible with both runtime families.
KYLGIS_MODULAR_OPENS="--add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.text=ALL-UNNAMED --add-opens=java.desktop/java.awt.font=ALL-UNNAMED --add-opens=java.desktop/java.awt=ALL-UNNAMED"
if [ -n "${JDK_JAVA_OPTIONS:-}" ]; then
    JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS $KYLGIS_MODULAR_OPENS"
else
    JDK_JAVA_OPTIONS="$KYLGIS_MODULAR_OPENS"
fi
export JDK_JAVA_OPTIONS

if [ -n "$NATIVE" ] && [ -d "$NATIVE" ]; then
    exec java -Xms512m -Xmx1024m "-splash:$DIR/kylgispos_splash_dark.png" \
        "-Djava.library.path=$NATIVE" "-Ddirname.path=$DIR/" \
        -jar "$DIR/kylgispos.jar" "$@"
else
    exec java -Xms512m -Xmx1024m "-splash:$DIR/kylgispos_splash_dark.png" \
        "-Ddirname.path=$DIR/" -jar "$DIR/kylgispos.jar" "$@"
fi
