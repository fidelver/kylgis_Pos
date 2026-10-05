@echo off

REM    KylGis POS Punto de Venta Táctil
REM    Copyright (c) 2026 KylGis POS
REM    Portions Copyright (c) 2015-2021 John Lewis (Chromis POS / ChromisKitchenScreen)
REM    Portions Copyright (c) 2010-2021 Hugh Clayson / uniCenta (https://unicenta.com)
REM    Portions Copyright (c) 2006-2010 Adrián Romero / Openbravo S.L.
REM
REM    This file is part of KylGis POS
REM
REM    KylGis POS is free software: you can redistribute it and/or modify
REM    it under the terms of the GNU General Public License as published by
REM    the Free Software Foundation, either version 3 of the License, or
REM    (at your option) any later version.
REM
REM    KylGis POS is distributed in the hope that it will be useful,
REM    but WITHOUT ANY WARRANTY; without even the implied warranty of
REM    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
REM    GNU General Public License for more details.
REM
REM    You should have received a copy of the GNU General Public License
REM    along with KylGis POS.  If not, see <http://www.gnu.org/licenses/>.

set DIRNAME=%~dp0
set CP="%DIRNAME%kylgispos.jar"

set CP=%CP%;"%DIRNAME%lib/bsh-core-2.0b4.jar"
set CP=%CP%;"%DIRNAME%lib/barcode4j-2.0.jar"
set CP=%CP%;"%DIRNAME%lib/commons-beanutils-1.8.3.jar"
set CP=%CP%;"%DIRNAME%lib/commons-codec-1.4.jar"
set CP=%CP%;"%DIRNAME%lib/commons-collections-3.2.1.jar"
set CP=%CP%;"%DIRNAME%lib/commons-configuration2-2.1.jar"
set CP=%CP%;"%DIRNAME%lib/commons-dbcp2-2.1.1.jar"
set CP=%CP%;"%DIRNAME%lib/commons-digester-2.1.jar"
set CP=%CP%;"%DIRNAME%lib/commons-discovery-0.5.0.jar"
set CP=%CP%;"%DIRNAME%lib/commons-lang3-3.5.jar"
set CP=%CP%;"%DIRNAME%lib/commons-logging-1.2.jar"
set CP=%CP%;"%DIRNAME%lib/iText-4.2.1.jar"
set CP=%CP%;"%DIRNAME%lib/jasperreports-6.4.0.jar"
set CP=%CP%;"%DIRNAME%lib/jcl_editor.jar"
set CP=%CP%;"%DIRNAME%lib/jcommon-1.0.15.jar"
set CP=%CP%;"%DIRNAME%lib/jdt-compiler-3.1.1.jar"
set CP=%CP%;"%DIRNAME%lib/jfreechart-1.0.19.jar"
set CP=%CP%;"%DIRNAME%lib/jpos-1.13.0.jar"
set CP=%CP%;"%DIRNAME%lib/jpos-2.0.10.jar"
set CP=%CP%;"%DIRNAME%lib/oro-2.0.8.jar"
set CP=%CP%;"%DIRNAME%lib/poi-3.10.1.jar"
set CP=%CP%;"%DIRNAME%lib/RXTXcomm-2.2.jar"
set CP=%CP%;"%DIRNAME%lib/swingx-all-1.6.4.jar"
set CP=%CP%;"%DIRNAME%lib/velocity-1.7-dep.jar"

REM Apache Axis SOAP libraries.
set CP=%CP%;"%DIRNAME%lib/axis.jar"
set CP=%CP%;"%DIRNAME%lib/jaxrpc-1.4.0.jar"
set CP=%CP%;"%DIRNAME%lib/saaj-1.4.0.jar"
set CP=%CP%;"%DIRNAME%lib/wsdl4j-1.6.3.jar"

set CP=%CP%;"%DIRNAME%locales/"
set CP=%CP%;"%DIRNAME%reports/"

start /B javaw -Xms512m -Xmx1024m -cp %CP% -Djava.library.path="%DIRNAME%lib/Windows/i368-mingw32" -Ddirname.path="%DIRNAME%./" -splash:kylgispos_splash_dark.png com.mx.kylgis.pos.forms.StartPOS %1