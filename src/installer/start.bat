@echo off
setlocal EnableExtensions
set "DIR=%~dp0"

rem WebLaF 1.2.9 embeds XStream 1.4.9 and needs reflective access to a
rem handful of JDK internals on modular Java (9+). Java 8 ignores
rem JDK_JAVA_OPTIONS, while Java 9+ consumes these options before startup.
set "KYLGIS_MODULAR_OPENS=--add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.text=ALL-UNNAMED --add-opens=java.desktop/java.awt.font=ALL-UNNAMED --add-opens=java.desktop/java.awt=ALL-UNNAMED"
if defined JDK_JAVA_OPTIONS (
    set "JDK_JAVA_OPTIONS=%JDK_JAVA_OPTIONS% %KYLGIS_MODULAR_OPENS%"
) else (
    set "JDK_JAVA_OPTIONS=%KYLGIS_MODULAR_OPENS%"
)

if /I "%PROCESSOR_ARCHITECTURE%"=="x86" goto native
if /I "%PROCESSOR_ARCHITECTURE%"=="AMD64" goto native
goto nonative

:native
javaw -Xms256m -Xmx1024m "-splash:%DIR%kylgispos_splash_dark.png" "-Djava.library.path=%DIR%lib\Windows\i368-mingw32" "-Ddirname.path=%DIR%" -jar "%DIR%kylgispos.jar" %*
goto end

:nonative
javaw -Xms256m -Xmx1024m "-splash:%DIR%kylgispos_splash_dark.png" "-Ddirname.path=%DIR%" -jar "%DIR%kylgispos.jar" %*

:end
endlocal
