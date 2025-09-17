This file aims at listing additional tests that should be performed when a new version of Selenium grid is about to be released

# Execute integration tests of SeleniumRobot #

Start a grid 
- hub `java -cp seleniumRobot-grid.jar -Dselenium.debug=true com.infotel.seleniumrobot.grid.GridStarter hub --host <host> --port 4445 --session-request-timeout 30 --publish-events tcp://<host>:24445 --subscribe-events tcp://<host>:34445 --tracing false`
- node `java -cp seleniumRobot-grid.jar -Dselenium.debug=true com.infotel.seleniumrobot.grid.GridStarter node --host <host> --max-sessions 1 --grid-url http://<host>:4445 --publish-events tcp://<host>:24445 --subscribe-events tcp://<host>:34445 --port 6666 --devMode true --tracing false`

Then execute SeleniumRobot integration tests in `com.seleniumtests.it.connector.selenium.TestSeleniumRobotGridConnector` with VM options `-DhubUrl=http://<host>:4445/wd/hub -DnodeUrl=http://<host>:6666`

# Check LocalNewSessionQueue has been applied #

Start the hub and node with the same options as above
Then execute a SeleniumRobot grid test
In Hub logs, you should see a line containing "getNextAvailable:"
