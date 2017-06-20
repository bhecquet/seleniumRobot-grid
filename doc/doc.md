# User Manual #
SeleniumRobot-grid aims at adding features to selenium-grid:

- Automatic start of appium
- improved browser choice in mobile => distinction between chrome on android and chrome on desktop
- specific appium provider
- automatic generation of nodeConf.json
- multibrowser support for mobile devices. Matcher has been updated to take several browsers into account
- mobile capabilities are taken into account: platformName, platformVersion, deviceName
- automatic update of seleniumGrid nodes when hub has been updated

## Installation ##
Just copy the jar-with-dependencies in a specific folder

### start as a service on Linux ###

To start SeleniumRobot-grid as a service on Linux (sysV), copy this file to /etc/init.d/selenium-grid-hub
	
	#!/bin/bash
	#
	# seleniumGridHub
	#
	# chkconfig:
	# description:  Start Selenium grid hub.
	
	# Source function library.
	#. /etc/init.d/functions
	
	DESC="Selenium Grid Server"
	RUN_AS="tomcat"
	JAVA_BIN="/usr/lib/jvm/java-8-openjdk-amd64/bin/java"
	
	SELENIUM_DIR="/opt/selenium-grid"
	PID_FILE="$SELENIUM_DIR/selenium-grid.pid"
	JAR_FILE="$SELENIUM_DIR/selenium-server.jar"
	LOG_DIR="/var/log/selenium"
	LOG_FILE="${LOG_DIR}/selenium-grid.log"
	
	USER="tomcat"
	GROUP="tomcat"
	
	MAX_MEMORY="-Xmx256m"
	STACK_SIZE="-Xss8m"
	
	DAEMON_OPTS=" $MAX_MEMORY $STACK_SIZE -cp $SELENIUM_DIR/*.jar org.openqa.grid.selenium.GridLauncher -role hub  -hubConfig $SELENIUM_DIR/hubConf.json -log $LOG_FILE"
	
	NAME="selenium"
	
	if [ "$1" != status ]; then
	    if [ ! -d ${LOG_DIR} ]; then
	        mkdir --mode 750 --parents ${LOG_DIR}
	        chown ${USER}:${GROUP} ${LOG_DIR}
	    fi
	fi
	
	
	. /lib/lsb/init-functions
	case "$1" in
	    start)
	        echo -n "Starting $DESC: "
	        if start-stop-daemon -c $RUN_AS --start --background --pidfile $PID_FILE --make-pidfile --exec $JAVA_BIN -- $DAEMON_OPTS ; then
	            log_end_msg 0
	        else
	            log_end_msg 1
	        fi
	        ;;
	
	    stop)
	        echo -n "Stopping $DESC: "
	        start-stop-daemon --stop --pidfile $PID_FILE
	        echo "$NAME."
	

## Running ##

### Running Hub ###
For hub, start grid with `java -cp *.jar org.openqa.grid.selenium.GridLauncher -role hub`
The hub configuration will be automatically generated. It's also possible to give your custom configuration or any other arguments accepted by selenium-grid

### Running node ###
For node, start with `java -cp *;. com.infotel.seleniumrobot.grid.GridStarter -role node` (use `cp *:.` on linux)

This will generate the node configuration file (browser and mobile devices).<br/>
Any options supported by standard selenium grid are also supported (hubHost, hubPort, browser, ...). You can also use your custom json configuration using `-nodeConfig` parameter

When specifying custom browser (not detected by grid) and this browser needs a specific driver, add the driver path to capabilities
e.g: `-browser -browser browserName=chrome,version=40.0,chrome_binary=/home/myhomedir/chrome,maxInstances=4,platform=LINUX,webdriver.chrome.driver=/home/myhomedir/chromedriver`
The keys for drivers are:
- `webdriver.chrome.driver` for chrome
- `webdriver.gecko.driver` for firefox > 47.0
- `webdriver.ie.driver` for internet explorer
- `webdriver.edge.driver` for edge
Else, no driver will be specified and test will fail

When configuration file is automatically generated, all connected mobile devices will be included in grid with an instance number of '1'. 
This implies that ADB is installed for android devices.
The name is the device name returned by ADB (in case of android)

### Running SeleniumRobot tests on grid ###
Start SeleniumRobot test with the parameters `-DrunMode=grid -DwebDriverGrid=http://<server>:4444/wd/hub` or their equivalent in XML configuration

### Running mobile tests ###
For mobile tests, set the following environment variables:
- APPIUM_HOME: path to Appium installation path (e.g: where Appium.exe/node.exe resides on Windows)
- ANDROID_HOME: path to Android SDK (e.g: where SDK Manager resides)