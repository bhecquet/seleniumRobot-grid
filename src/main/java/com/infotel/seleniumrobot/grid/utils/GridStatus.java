package com.infotel.seleniumrobot.grid.utils;


public enum GridStatus {

	ACTIVE ("active"),
    INACTIVE ("inactive"),
	UNKNOWN ("unknwon");
	
	String[] gStatus;
	
	GridStatus(final String... gridStatus) {
        this.gStatus = gridStatus;
    }
	
	public static GridStatus fromString(String status) {
		try {
			return GridStatus.valueOf(status);
		} catch (NullPointerException e) {
			throw new IllegalArgumentException("Unrecognized null grid status");
		} catch (IllegalArgumentException ex) {
			for (GridStatus gridStatus : GridStatus.values()) {
		        for (String matcher : gridStatus.gStatus) {
		          if (status.equalsIgnoreCase(matcher)) {
		            return gridStatus;
		          }
		        }
		      }
		      throw new IllegalArgumentException("Unrecognized grid status: " + status);
		}
	}	
}
