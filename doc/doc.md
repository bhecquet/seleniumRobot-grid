# User Manual #
SeleniumRobot-grid aims at adding features to selenium-grid:

- Automatic start of appium
- improved browser choice in mobile => distinction between chrome on android and chrome on desktop
- specific appium provider
- automatic generation of nodeConf.json
- multibrowser support for mobile devices. Matcher has been updated to take several browsers into account
- mobile capabilities are taken into account: platformName, platformVersion, deviceName


## Installation ##
Just copy the jar-with-dependencies in a specific folder

## Running ##

### Running Hub ###
For hub, start grid with `java -cp *.jar org.openqa.grid.selenium.GridLauncher -role hub  -hubConfig hubConf.json`
`hubConf.json` file is the one in this doc directory

### Running node ###
For node, start with `java -cp *;. com.infotel.seleniumrobot.grid.NodeStarter -role node`

This will generate the node configuration file (browser and mobile devices).<br/>
Any options supported by standard selenium grid are also supported (hubHost, hubPort, browser, ...). You can also use your custom json configuration using `-nodeConfig` parameter

When configuration file is automatically generated, all connected mobile devices will be included in grid with an instance number of '1'. 
This implies that ADB is installed for android devices.
The name is the device name returned by ADB (in case of android)

