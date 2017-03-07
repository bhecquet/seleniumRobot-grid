CD /d "%~dp0"

if exist update\seleniumRobot-grid-jar-with-dependencies.jar (
	rem update grid
	xcopy /s /y "update\seleniumRobot-grid-jar-with-dependencies.jar" "seleniumRobot-grid-jar-with-dependencies.jar"
)

java  -cp *;. com.infotel.seleniumrobot.grid.GridStarter %* 
EXIT /B %ERRORLEVEL%