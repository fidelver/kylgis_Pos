#!/bin/sh
# Shared Java runtime resolver for KylGis POS launchers.
# Caller must define DIR as the absolute installation directory.

KYLGIS_MIN_JAVA_MAJOR=21
KYLGIS_JAVA=
KYLGIS_JAVA_SOURCE=

if [ -x "$DIR/runtime/bin/java" ]; then
    KYLGIS_JAVA="$DIR/runtime/bin/java"
    KYLGIS_JAVA_SOURCE="bundled-runtime"
elif [ -n "${KYLGIS_JAVA_HOME:-}" ] && [ -x "$KYLGIS_JAVA_HOME/bin/java" ]; then
    KYLGIS_JAVA="$KYLGIS_JAVA_HOME/bin/java"
    KYLGIS_JAVA_SOURCE="KYLGIS_JAVA_HOME"
elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    KYLGIS_JAVA="$JAVA_HOME/bin/java"
    KYLGIS_JAVA_SOURCE="JAVA_HOME"
elif command -v java >/dev/null 2>&1; then
    KYLGIS_JAVA=$(command -v java)
    KYLGIS_JAVA_SOURCE="PATH"
else
    echo "KylGis POS requiere Java 21 o superior y no se encontro un runtime Java." >&2
    echo "Instale Java 21 LTS o configure KYLGIS_JAVA_HOME/JAVA_HOME." >&2
    return 2
fi

KYLGIS_JAVA_VERSION_LINE=$("$KYLGIS_JAVA" -version 2>&1 | sed -n '/version "[^"]*"/{p;q;}')
KYLGIS_JAVA_VERSION=$(printf '%s\n' "$KYLGIS_JAVA_VERSION_LINE" | sed -n 's/.*version "\([^"]*\)".*/\1/p')
case "$KYLGIS_JAVA_VERSION" in
    1.*)
        KYLGIS_JAVA_MAJOR=$(printf '%s\n' "$KYLGIS_JAVA_VERSION" | cut -d. -f2)
        ;;
    *)
        KYLGIS_JAVA_MAJOR=${KYLGIS_JAVA_VERSION%%.*}
        KYLGIS_JAVA_MAJOR=${KYLGIS_JAVA_MAJOR%%-*}
        ;;
esac

case "$KYLGIS_JAVA_MAJOR" in
    ''|*[!0-9]*)
        echo "No se pudo determinar la version del runtime Java: $KYLGIS_JAVA_VERSION_LINE" >&2
        return 2
        ;;
esac

if [ "$KYLGIS_JAVA_MAJOR" -lt "$KYLGIS_MIN_JAVA_MAJOR" ]; then
    echo "KylGis POS requiere Java $KYLGIS_MIN_JAVA_MAJOR o superior." >&2
    echo "Runtime detectado: $KYLGIS_JAVA ($KYLGIS_JAVA_VERSION), origen: $KYLGIS_JAVA_SOURCE." >&2
    echo "Configure KYLGIS_JAVA_HOME con un Java 21 LTS compatible." >&2
    return 2
fi

# Legacy WebLaF components still need limited reflective access on modular Java.
KYLGIS_MODULAR_OPENS="--add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.text=ALL-UNNAMED --add-opens=java.desktop/java.awt.font=ALL-UNNAMED --add-opens=java.desktop/java.awt=ALL-UNNAMED --add-opens=java.desktop/javax.swing.table=ALL-UNNAMED"
case " ${JDK_JAVA_OPTIONS:-} " in
    *" --add-opens=java.base/java.util=ALL-UNNAMED "*) ;;
    *)
        if [ -n "${JDK_JAVA_OPTIONS:-}" ]; then
            JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS $KYLGIS_MODULAR_OPENS"
        else
            JDK_JAVA_OPTIONS="$KYLGIS_MODULAR_OPENS"
        fi
        ;;
esac
export JDK_JAVA_OPTIONS
export KYLGIS_JAVA KYLGIS_JAVA_SOURCE KYLGIS_JAVA_VERSION KYLGIS_JAVA_MAJOR
