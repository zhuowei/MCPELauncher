package com.mojang.minecraftpe;

import android.os.*;

public class HardwareInformation {

	public static String getDeviceModelName() {
		return Build.MODEL;
	}

	public static String getAndroidVersion() {
		return "999999";
	}

	public static String getCPUType() {
		return "CPU type";
	}

	public static String getCPUName() {
		return "CPU name";
	}

	public static String getCPUFeatures() {
		return "CPU features";
	}

	public static int getNumCores() {
		return 4;
	}

}
