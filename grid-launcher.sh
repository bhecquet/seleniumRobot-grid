while [ true ]
do
        echo "grid"
        java -cp seleniumRobot-grid.jar:lib/drivers/* -Xmx2048m com.infotel.seleniumrobot.grid.GridStarter $@
        sleep 5s
done
