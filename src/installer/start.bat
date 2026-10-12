@echo off
setlocal EnableExtensions
set "DIR=%~dp0"
call "%DIR%kylgis-java.bat"
if errorlevel 1 exit /b %ERRORLEVEL%

set "KYLGIS_NATIVE_DIR="
if /I "%PROCESSOR_ARCHITECTURE%"=="x86" set "KYLGIS_NATIVE_DIR=%DIR%lib\Windows\i686-mingw32"
if /I "%PROCESSOR_ARCHITECTURE%"=="AMD64" set "KYLGIS_NATIVE_DIR=%DIR%lib\Windows\x86_64-mingw32"
if defined KYLGIS_NATIVE_DIR if not exist "%KYLGIS_NATIVE_DIR%" set "KYLGIS_NATIVE_DIR="

if defined KYLGIS_NATIVE_DIR (
    "%KYLGIS_JAVAW%" -Xms256m -Xmx1024m "-splash:%DIR%kylgispos_splash_dark.png" "-Djava.library.path=%KYLGIS_NATIVE_DIR%" "-Ddirname.path=%DIR%" -jar "%DIR%kylgispos.jar" %*
) else (
    "%KYLGIS_JAVAW%" -Xms256m -Xmx1024m "-splash:%DIR%kylgispos_splash_dark.png" "-Ddirname.path=%DIR%" -jar "%DIR%kylgispos.jar" %*
)
endlocal
