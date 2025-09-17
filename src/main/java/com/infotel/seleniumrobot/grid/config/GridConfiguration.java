package com.infotel.seleniumrobot.grid.config;

import com.infotel.seleniumrobot.grid.utils.GridStatus;

public abstract class GridConfiguration {


	private GridStatus status = GridStatus.ACTIVE;

	public GridStatus getStatus() {
		return status;
	}

	public void setStatus(GridStatus nodeStatus) {
		this.status = nodeStatus;
	}
}
