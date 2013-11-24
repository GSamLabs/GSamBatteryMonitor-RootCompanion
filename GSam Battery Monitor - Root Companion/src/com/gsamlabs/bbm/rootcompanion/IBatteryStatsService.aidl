package com.gsamlabs.bbm.rootcompanion;

interface IBatteryStatsService
{
	byte[] getStatistics();
	boolean hasBatteryStatsPermission();
}