package com.infotel.seleniumrobot.grid.utils;

public class MemoryInfo {
	
	private long totalMemory;
	private long freeMemory;
	
	public MemoryInfo(long total, long free) {
		totalMemory = total;
		freeMemory = free;
	}
	
	public long getTotalMemory() {
		return totalMemory / 1000000;
	}

	public long getFreeMemory() {
		return freeMemory / 1000000;
	}
}
