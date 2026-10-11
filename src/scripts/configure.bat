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

if /I "%KYLGIS_LAUNCHER_CHECK_ONLY%"=="1" (
    echo KYLGIS_CONFIG_RUNTIME_CHECK_OK
    echo java=%KYLGIS_JAVA%
    echo java.version=%KYLGIS_JAVA_VERSION%
    echo java.source=%KYLGIS_JAVA_SOURCE%
    exit /b 0
)

"%KYLGIS_JAVAW%" "-Ddirname.path=%DIR%" "-splash:%DIR%kylgispos_splash_dark.png" -cp "%DIR%kylgispos.jar" com.mx.kylgis.pos.config.JFrmConfig %*
