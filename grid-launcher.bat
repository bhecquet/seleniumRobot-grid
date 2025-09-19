title "grid_launcher"

for /f "delims=" %%i in ('dir /s /b /a-d "%ANDROID_AVD%\emulator*.bat"') do (start /MIN %%i && timeout /t 60)

:grid
	taskkill /F /FI "windowtitle eq grid-process"
	taskkill /F /FI "IMAGENAME eq chrome*"
	taskkill /F /FI "IMAGENAME eq *edge*"
	timeout /t 5

	start /MIN /wait "grid-process" "%JAVA_HOME%\bin\java.exe" -cp seleniumRobot-grid.jar;lib/drivers/* -Xmx2048m com.infotel.seleniumrobot.grid.GridStarter %*
	timeout /t 5
goto grid