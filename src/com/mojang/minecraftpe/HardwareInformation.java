package com.mojang.minecraftpe;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import android.os.*;

public class HardwareInformation {
	private static String cpuName = "unknown", cpuFeatures = "unknown";
	private static int numCores = 1;

	public String getSecureId() {
		return "SecureId";
	}

	public static String getSerialNumber() {
		// we can't do that; we don't have the phone permission
		return "SerialNumber";
	}

	public static String getBoard() {
		return Build.BOARD;
	}

	public String getInstallerPackageName() {
		return "installer.package.name";
	}

	public int getSignaturesHashCode() {
		return 0;
	}

	public boolean getIsRooted() {
		return false;
	}

	public static String getDeviceModelName() {
		return Build.MANUFACTURER.toUpperCase() + " " + Build.MODEL;
	}

	public static String getAndroidVersion() {
		return "Android " + Build.VERSION.RELEASE;
	}

	public static String getCPUType() {
		return Build.CPU_ABI;
	}

	public static String getCPUName() {
		return cpuName;
	}

	public static String getCPUFeatures() {
		return cpuFeatures;
	}

	public static int getNumCores() {
		return numCores;
	}

	public static void initHardwareInformation() {
		try {
			numCores = getNumCoresReal();
			parseCpuInfo();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static int getNumCoresReal() {
		// http://stackoverflow.com/questions/10133570/availableprocessors-returns-1-for-dualcore-phones

		FileFilter filter = new FileFilter() {
			public boolean accept(File pathname) {
				return Pattern.matches("cpu[0-9]+", pathname.getName());
			}
		};

		try {
			File dir = new File("/sys/devices/system/cpu/");
			return dir.listFiles(filter).length;
		} catch (Exception e) {
			return Math.max(1, Runtime.getRuntime().availableProcessors());
		}
	}

	private static void parseCpuInfo() throws IOException {
		BufferedReader reader = null;
		boolean cpuNameDone = false, cpuFeaturesDone = false;
		try {
			reader = new BufferedReader(new FileReader("/proc/cpuinfo"));
			String l;
			while ((l = reader.readLine()) != null) {
				if (!l.contains(":")) continue;
				String[] parts = l.split(":");
				String partName = parts[0].trim();
				String result = parts[1].trim();
				if (partName.equals("Hardware") || partName.equals("model name")) {
					cpuName = result;
					cpuNameDone = true;
				} else if (partName.equals("Features") || partName.equals("flags")) {
					cpuFeatures = result;
					cpuFeaturesDone = true;
				}
				if (cpuNameDone && cpuFeaturesDone) break;
			}
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException notanotherone){}
			}
		}
	}

	static {
		initHardwareInformation();
	}

}
