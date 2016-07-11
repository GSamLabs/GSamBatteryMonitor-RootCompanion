package com.gsamlabs.bbm.GSamBatteryMonitorRootHelper;

interface IBatteryStatsService
{
	byte[] getStatistics();
	boolean hasBatteryStatsPermission();
	byte[] readProcFile(String fileName);
}