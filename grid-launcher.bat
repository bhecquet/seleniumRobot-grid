title "grid_launcher"
:grid
	start /wait "grid-hub" "%JAVA_HOME%\bin\java.exe" -cp seleniumRobot-grid.jar;lib/drivers/* -Xmx2048m com.infotel.seleniumrobot.grid.GridStarter %*
	timeout /t 5
goto grid