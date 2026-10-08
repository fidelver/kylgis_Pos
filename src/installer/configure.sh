#!/bin/sh
# KylGis POS portable configuration launcher
DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec java "-Ddirname.path=$DIR/" "-splash:$DIR/kylgispos_splash_dark.png" \
    -cp "$DIR/kylgispos.jar" com.mx.kylgis.pos.config.JFrmConfig "$@"
