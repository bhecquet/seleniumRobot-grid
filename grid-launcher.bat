title "grid_launcher"
:grid
	taskkill /F /FI "windowtitle eq grid-process"
	start /wait "grid-process" "%JAVA_HOME%\bin\java.exe" -cp seleniumRobot-grid.jar;lib/drivers/* -Xmx2048m com.infotel.seleniumrobot.grid.GridStarter %*
	timeout /t 5
goto grid