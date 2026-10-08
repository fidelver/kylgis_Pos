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

set "DIR=%~dp0"
if /I "%PROCESSOR_ARCHITECTURE%"=="ARM64" goto nonative
java "-Djava.library.path=%DIR%lib\Windows\i368-mingw32" "-Ddirname.path=%DIR%" -jar "%DIR%kylgispos.jar" %*
goto end
:nonative
java "-Ddirname.path=%DIR%" -jar "%DIR%kylgispos.jar" %*
:end
