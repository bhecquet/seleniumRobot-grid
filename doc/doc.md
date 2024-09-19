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
Just copy the zip in a specific folder and unzip it

**/!\ GRID NODE SHOULD NEVER BE EXECUTED ON THE SAME MACHINE AS SELENIUMROBOT CORE**
This is due to the fact that grid node clean temp directory regularly and this temp directory is used by core to write openCV dll/so file.
Error could be: `C:\Users\selenium\AppData\Local\Temp\opencv_openpnp8056450660102574107\nu\pattern\opencv\windows\x86_64\opencv_java320.dll: Access denied`

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
	
### start as a windows service ###

grid cannot be directly started as a windows service, but, you can use 2 ways described here [http://ethertubes.com/make-a-program-run-as-a-windows-service-on-boot/](http://ethertubes.com/make-a-program-run-as-a-windows-service-on-boot/)

#### use cmd.exe /C ####

Use the command: `sc create selenium-grid-hub DisplayName= "Selenium Grid Hub" binPath= "cmd /C java.exe -cp <path_to_grid>\seleniumRobot-grid.jar com.infotel.seleniumrobot.grid.GridStarter hub" start= "auto"`

When service will be started, grid will start, but then service control manager will not detect the start and will terminate the command. But grid will be still running. 

#### Use nssm.exe ####

See: [https://nssm.cc/usage](https://nssm.cc/usage)

Path is 'java.exe'
Arguments are `-cp <path_to_grid>\seleniumRobot-grid.jar com.infotel.seleniumrobot.grid.GridStarter hub`
Startup directory: path where grid hub is installed

#### Install appium ####

Install lastest release of npm
[http://appium.io/docs/en/about-appium/getting-started/?lang=fr](http://appium.io/docs/en/about-appium/getting-started/?lang=fr)

##### Appium 1.x #####

```
npm install -g appium 
```

##### Appium 2.x #####

```
npm install --global appium --drivers=xcuitest,uiautomator2
```

## Running ##

Typical usage
```
java -cp seleniumRobot-grid.jar com.infotel.seleniumrobot.grid.GridStarter node --grid-url http://localhost:4444 --host 127.0.0.1 --port 5555 --max-sessions 1 --devMode true --nodeTags toto --restrictToTags false --proxyConfig auto
```

### Options ###

SeleniumRobot-grid supports all command line options of the standard Selenium-grid:

 | option  	| comment 				|
 |--------------|----------------------|
 | --grid-url	| URL of the HUB or Distributor |
 | --host	| host for the node	|
 | --port		| port on which node will listen |
 
 
Other options are specific to SeleniumRobot-grid
 
 | option  		| comment 				|
 |----------------------|------------------|
 | --devMode		| If value is "true", browsers will not be killed when grid starts. Useful when developing tests or grid	|
 | --nodeTags	| Name of the tags, this node provides (see below). When starting test with seleniumRobot, you can choose which node to use, with this tag. e.g: `--nodeTags foo,bar`. Is seleniumRobot does not specify the option, this node may still be used. Except if `--restrictToTags` option is set |
 | --restrictToTags	| If set to "true", combined with `-nodeTags`, this node will only be called if seleniumRobot requests it explicitly |
 | --extProgramWhiteList | comma separated list of programs that are allowed to be started remotely by SeleniumRobot. |
 | --proxyConfig	| "auto" to reset proxy configuration to AUTO when stopping a test |

### Running Hub ###
For hub, start grid with `java -cp seleniumRobot-grid.jar com.infotel.seleniumrobot.grid.GridStarter hub --host 127.0.0.1 --port 4444`


### Running node ###

**Driver arfifact** must be deployed, for example, using (for windows node, for others, replace seleniumRobot-windows-driver by 'seleniumRobot-linux-driver' or 'seleniumRobot-mac-driver'): `mvn -U org.apache.maven.plugins:maven-dependency-plugin:2.8:copy -Dartifact=com.infotel.seleniumRobot:seleniumRobot-windows-driver:RELEASE:jar -DoutputDirectory=<path_to_deployed_selenium_robot>/lib/drivers  -Dmdep.overWriteReleases=true -Dmdep.stripVersion=true`

For node, start with `java -cp seleniumRobot-grid.jar;lib/drivers/* com.infotel.seleniumrobot.grid.GridStarter node --grid-url http://localhost:4444` (use `-cp seleniumRobot-grid.jar:lib/drivers/*` on linux)


This will generate the node configuration file (browser and mobile devices).<br/>

When configuration file is automatically generated, all connected mobile devices will be included in grid with an instance number of '1'. 
This implies that ADB is installed for android devices.
The name is the device name returned by ADB (in case of android)

### Running SeleniumRobot tests on grid ###
Start SeleniumRobot test with the parameters `-DrunMode=grid -DwebDriverGrid=http://<server>:4444/wd/hub` or their equivalent in XML configuration

### Running tests on a specific set of nodes on grid ###
Apart from playing with capabilities to select a node based on browser or operating system, you are allowed to tell that your node handles specific features (a specific network config, an installed program or whatever)
To do so, start your node with `-nodeTags <tag1>,<tag2>,...`

When starting your test, add the following option: `-DnodeTags=<tag1>` (a comma seperated list can be provided) to seleniumRobot
Only a node which is set with the tag `tag1` will be used

### Restrict test session on a particular node
If you need that a node only accept test sessions that are addressed to it through nodeTags option, then add `--restrictToTags true` parameter.
For example, if options are `--restrictToTags true --nodeTags foo` then, only tests having option `-DnodeTags=foo` will be routed to this node

### Running mobile tests ###
For mobile tests, set the following environment variables:
- APPIUM_PATH: path to Appium installation path (e.g: where Appium.exe/appium.ps1 resides on Windows, /usr/local/lib on Mac OS when installed with NPM). Grid searches a path `<APPIUM_PATH>/node_modules/appium/package.json`
- ANDROID_HOME: path to Android SDK (e.g: where SDK Manager resides. We search `ANDROID_HOME/platform-tools/adb` )
- ANDROID_AVD: path where android emulator AVD are located

To start automatically android emulators (windows only for now) on grid startup, place a file "%ANDROID_AVD%/emulator-xxx.bat" which contains
```
cmd /C %ANDROID_HOME%\emulator\emulator.exe -avd <name> -netdelay none -netspeed full -port <port> -no-snapshot-load
```

Appium server is started automatically if mobile devices are detected


/!\ **Android emulators or physical devices** MUST be available on startup

### Do not kill browser processes automatically ###
By default, grid will kill all browser and driver processes when tests are not running
To avoid this behaviour, add option `-devMode true` when launching a node

### Allow external programs to be run ###

From a selenium test, it's possible to write

```java
public void myTest() {
	...
	executeCommand("myProgram", "arg1", "arg2");
	...
}
```
By default, seleniumRobot-grid will refuse to execute the program to avoid malicious usages
You can allow this program by:

- starting grid node with option: `-extProgramWhiteList myProgram`
- add "myProgram" to PATH so that it can be recognized without providing the full path

## Upgrading grid ##

Upgrading grid may be done by stopping each component and updating them before restart. It's easy but you need to stop your test before this phase so that they don't fail when you stop the grid.

SeleniumRobot grid offers a more robust way, the StatusServlet API (see below). Setting the hub to inactive will not stop currently running test but grid won't accept new test sessions. Steps will be:
- call POST StatusServlet API to set hub and nodes inactive: `wget --post-data=status=INACTIVE <hub_url>/grid/admin/StatusServlet`
- reinstall each node. You can wait for the node to be available by calling `<hub_url>/grid/admin/StatusServlet?jsonpath=$['http://<node_address>:<node_port>']['busy']`. This returns 'true' if node has running test sessions.
- reinstall the hub.

No need to reset the hub into 'ACTIVE' as restart reset it.

## API ##

### seleniumRobot grid API ###

SeleniumRobot grid defines other entry points

#### hub entry points ####

##### GuiServlet (/grid/admin/GuiServlet) #####

- GET `/grid/admin/GuiServlet`: an other grid GUI offering screenshot of nodes, CPU and memory information, list of active sessions

##### StatusServlet (/grid/admin/StatusServlet) #####

- GET `/grid/admin/StatusServlet`: returns JSON information about the state of the hub and nodes

```json
	{
	  "http:\u002f\u002fnode-machine:5554": {
	    "busy": false,
	    "lastSessionStart": -1,
	    "version": "3.14.0-SNAPSHOT",
	    "usedTestSlots": 0,
	    "testSlots": 1,
	    "status": "ACTIVE"
	  },
	  "hub": {
	    "version": "3.14.0-SNAPSHOT",
	    "status": "ACTIVE"
	  },
	  "success": true
	}
```
	
- GET `/grid/admin/StatusServlet?jsonpath=$['http://node-machine:5554']` param: returns partial information of the above status. 
	
```json
	{
	    "busy": false,
	    "lastSessionStart": -1,
	    "version": "3.14.0-SNAPSHOT",
	    "usedTestSlots": 0,
	    "testSlots": 1,
	    "status": "ACTIVE"
	  }
```
	
- POST `/grid/admin/StatusServlet?status=INACTIVE`: allow to disable hub and nodes. Allowed values are 'ACTIVE' and 'INACTIVE'. If 'INACTIVE' is given, then all running test sessions will continue, but no new session will be allowed, reporting that no node is available to process request. This allows to update gracefuly the whole grid, waiting for current tests to end.

##### INTERNAL USE! FileServlet (/grid/admin/FileServlet) #####

- POST a zip file content to `/grid/admin/FileServlet` with `output` parameter will unzip the file to the hub. `output` values can be 'upgrade' to place file to `<grid_root>/upgrade` folder, or any path value which will unzip file under `<grid_root>/upload/<your_path>/<some_random_id>`. This returns the path where files where unzipped
- POST `/grid/admin/FileServlet?output=mydir&localPath=true` will upload file to `<grid_root>/upload/mydir/<some_random_id>` and return the local path (e.g: 'D:\grid-node\upload\mydir\123454\')
- GET `/grid/admin/FileServlet?file=file:<filePath>` will download file present in the `upload` directory only. The path is relative to this folder.
- GET `/grid/admin/FileServlet/<filePath>` will download file present in the `upload` directory only. The path is relative to this folder. This is used by mobile tests to make application file available to appium.

#### Node entry points ####

##### NodeStatusServlet (/extra/NodeStatusServlet) #####
 - GET `/extra/NodeStatusServlet`: returns a partial GUI which is used by hub GuiServlet
 - GET `/extra/NodeStatusServlet?format=json`: returns the node information in json format
 
 ```json
	{
	  "memory": {
	    "totalMemory": 17054,
	    "class": "com.infotel.seleniumrobot.grid.utils.MemoryInfo",
	    "freeMemory": 4629
	  },
	  "maxSessions": 1,
	  "port": 5554,
	  "ip": "node_machine",
	  "cpu": 25.2,
	  "version": "3.14.0-SNAPSHOT",
	  "status": "ACTIVE"
	}
```
	
- POST `/extra/NodeStatusServlet?status=INACTIVE`: disable this node. It won't accept any new session, but current test session will continue. Allowed values are 'ACTIVE' and 'INACTIVE'

##### INTERNAL USE! FileServlet (/extra/FileServlet) #####

Same as FileServlet on hub

##### INTERNAL USE! NodeTaskServlet (/extra/NodeTaskServlet) #####

Several actions are available. **Most of them could be dangerous** if used in an unsecured environment as it gives access to the node itself
They are all used internaly by seleniumRobot to perform specific actions

POST `/extra/NodeTaskServlet?action=<action>` supports several actions

- `action=restart`: restart node computer
- `action=kill&process=<process_name>`: kill a process by name without extension
- `action=killPid&pid=<pid>`: kill a process by pid
- `action=leftClic&x=<x_coordinate>&y=<y_coordinate>`: perform a left click at point x,y
- `action=doubleClic&x=<x_coordinate>&y=<y_coordinate>`: perform a double click at point x,y
- `action=rightClic&x=<x_coordinate>&y=<y_coordinate>`: perform a right click at point x,y
- `action=sendKeys&keycodes=<kc1>,<kc2>` where kcX is a key code. Sends keys to desktop. Used to send non alphanumeric keys
- `action=writeText&text=<text>`: write text to desktop.
- `action=displayRunningStep&stepName=<step_name>`: display step name on desktop for video recording
- `action=uploadFile&name=<file_name>&content=<base64_string>` use browser to upload a file when a upload file window is displayed. The base64 content is copied to a temps file which will then be read by browser.
- `action=setProperty&key=<key>&value=<value>` set java property for the node
- `action=command&name<program>&arg0=<arg0>&arg1=<arg1>` execute program with arguments. Only programs allowed by parameter 'extProgramWhiteList' can be run
- `action=command&name<program>&timeout=10&session=<sessionId>&arg0=<arg0>&arg1=<arg1>` execute program with arguments with time limit of 10 secs. Only programs allowed by parameter 'extProgramWhiteList' can be run. 'SessionId' will be used to maintain driver session above standard timeout (9 mins) if program runs very long

GET `/extra/NodeTaskServlet?action=<action>` supports several actions

- `action=version`: returns the version of the node
- `action=screenshot`: returns a base64 string of the node screen (PNG format)
- `action=startVideoCapture&session=<test_session_id>`: start video capture on the node. SessionId is used to store the video file
- `action=stopVideoCapture&session=<test_session_id>`: stop video capture previously created (use the provided sessionId)
- `action=driverPids&browserName=<browser>&browserVersion=<version>&existingPids=<some_pids>`: Returns list of PIDS for this driver exclusively. This allows the hub to know which browser has been recently started. If existingPids is not empty, these pids won't be returned by the command. Browser name and version refers to installed browsers, declared in grid node
- `action=browserAndDriverPids&browserName=<browser>&browserVersion=<version>&parentPids=<some_pid>`: Returns list of PIDs for this driver and for all subprocess created (driver, browser and other processes). This allows to kill any process created by a driver. parentPids are the processs for which we should search child processes.
- `action=keepAlive`: move mouse from 1 pixel so that windows session does not lock

##### INTERNAL USE! MobileNodeServlet (/extra/MobileNodeServlet) #####

Servlet for getting all mobile devices information
This helps the hub to update capabilities with information on the connected device

GET `/extra/MobileNodeServlet?caps=<capabilities_as_json_string>`: returns updated capabilities as Json. this will allow to add missing caps, for example when client requests an android device without specifying it precisely

## Q&A ##

### Errors ###

#### AnnotationTypeMismatchException ####

When debugging from eclipse, this error may appear

`Exception in thread "main" java.lang.annotation.AnnotationTypeMismatchException: Incorrectly typed data found for annotation element public abstract java.lang.Class[] com.beust.jcommander.Parameter.validateValueWith() (Found data of type class java.lang.Class[class org.openqa.grid.internal.utils.configuration.validators.FileExistsValueValidator])`

This is due to project 'core' being configured to be a dependency of seleniumRobot-grid inside eclipse. There may be a library conflict.
Solution is to remove this eclipse dependency and use repository artifacts. In case you need updated code from `core`, then, install it in local repository before using it. 

## Development ##

To test developments on grid, first use the unit tests
Then, you can execute tests on the read grid bu launching it locally and executing tests from SeleniumRobot
- TestSeleniumRobotGridConnector       => tests servlets
- TestSeleniumRobotGridConnector2       => test with a driver

Test with beta browser
Test with default profile (start with '-DchromeUserProfilePath=default')
Test with Edge in IE mode
Test that drivers / browsers are closed automatically when session is closed, even if, from client side, program is killed
