# User Manual #

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

