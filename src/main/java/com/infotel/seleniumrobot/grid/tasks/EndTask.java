package com.infotel.seleniumrobot.grid.tasks;

import org.apache.log4j.Logger;

/**
 * Task to end the current grid node/hub
 * @author s047432
 *
 */
public class EndTask implements Task {

	private static final Logger logger = Logger.getLogger(EndTask.class);
	
	@Override
	public void execute() {
		logger.info("requested node stop");
		System.exit(0);
	}

}
