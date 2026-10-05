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

DIRNAME=`dirname $0`
CP=$DIRNAME/kylgispos.jar

CP=$CP:$DIRNAME/locales/
CP=$CP:$DIRNAME/reports/

# Select the library folder
case "`uname -s`" in
    Linux)
    case "`uname -m`" in
    i686) LIBRARYPATH=/lib/Linux/i686-unknown-linux-gnu;;
    ia64) LIBRARYPATH=/lib/Linux/ia64-unknown-linux-gnu;;
    x86_64|amd64) LIBRARYPATH=/lib/Linux/x86_64-unknown-linux-gnu;;
    esac;;
    SunOS)
    case "`uname -m`" in
    sparc32) LIBRARYPATH=/Solaris/sparc-solaris/sparc32-sun-solaris2.8;;
    sparc64) LIBRARYPATH=/Solaris/sparc-solaris/sparc64-sun-solaris2.8;;
    esac;;
Darwin) LIBRARYPATH=/lib/Mac_OS_X;;
CYGWIN*|MINGW32*) LIBRARYPATH=/lib/Windows/i368-mingw32;;
esac

# start uniCenta oPOS
java -cp $CP -Xms512m -Xmx1024m -splash:kylgispos_splash_dark.png -Djava.library.path=$DIRNAME$LIBRARYPATH -Ddirname.path=$DIRNAME/ com.mx.kylgis.pos.forms.StartPOS