@echo off
rem Shared Java runtime resolver for KylGis POS launchers.
rem Caller must define DIR as the absolute installation directory ending in \.

set "KYLGIS_MIN_JAVA_MAJOR=21"
set "KYLGIS_JAVA="
set "KYLGIS_JAVAW="
set "KYLGIS_JAVA_SOURCE="

if exist "%DIR%runtime\bin\java.exe" (
    set "KYLGIS_JAVA=%DIR%runtime\bin\java.exe"
    if exist "%DIR%runtime\bin\javaw.exe" (set "KYLGIS_JAVAW=%DIR%runtime\bin\javaw.exe") else (set "KYLGIS_JAVAW=%DIR%runtime\bin\java.exe")
    set "KYLGIS_JAVA_SOURCE=bundled-runtime"
    goto java_found
)

if defined KYLGIS_JAVA_HOME if exist "%KYLGIS_JAVA_HOME%\bin\java.exe" (
    set "KYLGIS_JAVA=%KYLGIS_JAVA_HOME%\bin\java.exe"
    if exist "%KYLGIS_JAVA_HOME%\bin\javaw.exe" (set "KYLGIS_JAVAW=%KYLGIS_JAVA_HOME%\bin\javaw.exe") else (set "KYLGIS_JAVAW=%KYLGIS_JAVA_HOME%\bin\java.exe")
    set "KYLGIS_JAVA_SOURCE=KYLGIS_JAVA_HOME"
    goto java_found
)

if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" (
    set "KYLGIS_JAVA=%JAVA_HOME%\bin\java.exe"
    if exist "%JAVA_HOME%\bin\javaw.exe" (set "KYLGIS_JAVAW=%JAVA_HOME%\bin\javaw.exe") else (set "KYLGIS_JAVAW=%JAVA_HOME%\bin\java.exe")
    set "KYLGIS_JAVA_SOURCE=JAVA_HOME"
    goto java_found
)

where java.exe >nul 2>&1
if not errorlevel 1 (
    set "KYLGIS_JAVA=java.exe"
    where javaw.exe >nul 2>&1
    if errorlevel 1 (set "KYLGIS_JAVAW=java.exe") else (set "KYLGIS_JAVAW=javaw.exe")
    set "KYLGIS_JAVA_SOURCE=PATH"
    goto java_found
)

echo KylGis POS requiere Java 21 o superior y no se encontro un runtime Java. 1>&2
echo Instale Java 21 LTS o configure KYLGIS_JAVA_HOME/JAVA_HOME. 1>&2
exit /b 2

:java_found
set "KYLGIS_JAVA_VERSION="
for /f "tokens=3" %%V in ('"%KYLGIS_JAVA%" -version 2^>^&1 ^| findstr /i "version"') do if not defined KYLGIS_JAVA_VERSION set "KYLGIS_JAVA_VERSION=%%~V"
if not defined KYLGIS_JAVA_VERSION (
    echo No se pudo determinar la version del runtime Java: %KYLGIS_JAVA% 1>&2
    exit /b 2
)

for /f "tokens=1 delims=." %%M in ("%KYLGIS_JAVA_VERSION%") do set "KYLGIS_JAVA_MAJOR=%%M"
if "%KYLGIS_JAVA_MAJOR%"=="1" for /f "tokens=2 delims=." %%M in ("%KYLGIS_JAVA_VERSION%") do set "KYLGIS_JAVA_MAJOR=%%M"
for /f "tokens=1 delims=-" %%M in ("%KYLGIS_JAVA_MAJOR%") do set "KYLGIS_JAVA_MAJOR=%%M"

set /a KYLGIS_JAVA_MAJOR_NUM=%KYLGIS_JAVA_MAJOR% >nul 2>&1
if errorlevel 1 (
    echo No se pudo interpretar la version Java: %KYLGIS_JAVA_VERSION% 1>&2
    exit /b 2
)
if %KYLGIS_JAVA_MAJOR_NUM% LSS %KYLGIS_MIN_JAVA_MAJOR% (
    echo KylGis POS requiere Java %KYLGIS_MIN_JAVA_MAJOR% o superior. 1>&2
    echo Runtime detectado: %KYLGIS_JAVA% ^(%KYLGIS_JAVA_VERSION%^), origen: %KYLGIS_JAVA_SOURCE%. 1>&2
    echo Configure KYLGIS_JAVA_HOME con un Java 21 LTS compatible. 1>&2
    exit /b 2
)

set "KYLGIS_MODULAR_OPENS=--add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.text=ALL-UNNAMED --add-opens=java.desktop/java.awt.font=ALL-UNNAMED --add-opens=java.desktop/java.awt=ALL-UNNAMED"
echo %JDK_JAVA_OPTIONS% | findstr /c:"--add-opens=java.base/java.util=ALL-UNNAMED" >nul 2>&1
if errorlevel 1 (
    if defined JDK_JAVA_OPTIONS (
        set "JDK_JAVA_OPTIONS=%JDK_JAVA_OPTIONS% %KYLGIS_MODULAR_OPENS%"
    ) else (
        set "JDK_JAVA_OPTIONS=%KYLGIS_MODULAR_OPENS%"
    )
)
exit /b 0
