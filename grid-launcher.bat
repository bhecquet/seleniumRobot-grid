:grid
	start /wait "grid-hub" java -cp seleniumRobot-grid.jar;lib/drivers/* -Xmx2048m com.infotel.seleniumrobot.grid.GridStarter -role hub
	timeout /t 5
goto grid