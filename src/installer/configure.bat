@echo off
set "DIR=%~dp0"
javaw "-Ddirname.path=%DIR%" "-splash:%DIR%kylgispos_splash_dark.png" -cp "%DIR%kylgispos.jar" com.mx.kylgis.pos.config.JFrmConfig %*
