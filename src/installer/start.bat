@echo off
set "DIR=%~dp0"
if /I "%PROCESSOR_ARCHITECTURE%"=="x86" goto native
if /I "%PROCESSOR_ARCHITECTURE%"=="AMD64" goto native
goto nonative
:native
javaw -Xms256m -Xmx1024m "-splash:%DIR%kylgispos_splash_dark.png" "-Djava.library.path=%DIR%lib\Windows\i368-mingw32" "-Ddirname.path=%DIR%" -jar "%DIR%kylgispos.jar" %*
goto end
:nonative
javaw -Xms256m -Xmx1024m "-splash:%DIR%kylgispos_splash_dark.png" "-Ddirname.path=%DIR%" -jar "%DIR%kylgispos.jar" %*
:end
