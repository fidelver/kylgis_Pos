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
call "%DIR%kylgis-java.bat"
if errorlevel 1 exit /b %ERRORLEVEL%

set "KYLGIS_NATIVE_DIR="
if /I "%PROCESSOR_ARCHITECTURE%"=="x86" set "KYLGIS_NATIVE_DIR=%DIR%lib\Windows\i686-mingw32"
if /I "%PROCESSOR_ARCHITECTURE%"=="AMD64" set "KYLGIS_NATIVE_DIR=%DIR%lib\Windows\x86_64-mingw32"
if defined KYLGIS_NATIVE_DIR if not exist "%KYLGIS_NATIVE_DIR%" set "KYLGIS_NATIVE_DIR="

if /I "%KYLGIS_LAUNCHER_CHECK_ONLY%"=="1" (
    echo KYLGIS_RUNTIME_CHECK_OK
    echo java=%KYLGIS_JAVA%
    echo java.version=%KYLGIS_JAVA_VERSION%
    echo java.source=%KYLGIS_JAVA_SOURCE%
    echo arch=%PROCESSOR_ARCHITECTURE%
    if defined KYLGIS_NATIVE_DIR (echo native=%KYLGIS_NATIVE_DIR%) else (echo native=none)
    exit /b 0
)

if defined KYLGIS_NATIVE_DIR (
    "%KYLGIS_JAVAW%" -Xms256m -Xmx1024m "-splash:%DIR%kylgispos_splash_dark.png" "-Djava.library.path=%KYLGIS_NATIVE_DIR%" "-Ddirname.path=%DIR%" -jar "%DIR%kylgispos.jar" %*
) else (
    "%KYLGIS_JAVAW%" -Xms256m -Xmx1024m "-splash:%DIR%kylgispos_splash_dark.png" "-Ddirname.path=%DIR%" -jar "%DIR%kylgispos.jar" %*
)
