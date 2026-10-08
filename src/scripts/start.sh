#!/bin/sh
#    KylGis POS Punto de Venta Táctil
#    Copyright (c) 2026 KylGis POS
#    Portions Copyright (c) 2015-2021 John Lewis (Chromis POS / ChromisKitchenScreen)
#    Portions Copyright (c) 2010-2021 Hugh Clayson / uniCenta (https://unicenta.com)
#    Portions Copyright (c) 2006-2010 Adrián Romero / Openbravo S.L.
#
#    This file is part of KylGis POS
#
#    KylGis POS is free software: you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    KylGis POS is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with KylGis POS.  If not, see <http://www.gnu.org/licenses/>.

DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
OS=$(uname -s 2>/dev/null || echo unknown)
ARCH=$(uname -m 2>/dev/null || echo unknown)
NATIVE=
case "$OS/$ARCH" in
    Linux/i?86) NATIVE="$DIR/lib/Linux/i686-unknown-linux-gnu" ;;
    Linux/x86_64|Linux/amd64) NATIVE="$DIR/lib/Linux/x86_64-unknown-linux-gnu" ;;
    Linux/ia64) NATIVE="$DIR/lib/Linux/ia64-unkown-linux-gnu" ;;
    Darwin/i?86|Darwin/x86_64|Darwin/amd64) NATIVE="$DIR/lib/Mac_OS_X" ;;
esac

if [ -n "$NATIVE" ] && [ -d "$NATIVE" ]; then
    exec java "-Djava.library.path=$NATIVE" "-Ddirname.path=$DIR/" -jar "$DIR/kylgispos.jar" "$@"
else
    exec java "-Ddirname.path=$DIR/" -jar "$DIR/kylgispos.jar" "$@"
fi
