package net.zhuoweizhang.mcpelauncher;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;

import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

import com.mojang.minecraftpe.MainActivity;
import net.zhuoweizhang.mcpelauncher.ui.TrampolineActivity;

public class Utils {
	protected static Context mContext = null;

	public static void setContext(Context context) {
		mContext = context;
	}

	public static void clearDirectory(File dir) {
		File[] fileList = dir.listFiles();
		if (fileList == null) return;
		for (File f : fileList) {
			if (f.isDirectory()) {
				clearDirectory(f);
			}
			f.delete();
		}
	}

	public static Field getDeclaredFieldRecursive(Class<?> clazz, String name) {
		if (clazz == null)
			return null;
		try {
			return clazz.getDeclaredField(name);
		} catch (NoSuchFieldException nsfe) {
			return getDeclaredFieldRecursive(clazz.getSuperclass(), name);
		}
	}

	public static void setLanguageOverride() {
		requireInit();
		String override = getPrefs(0).getString("zz_language_override", "");
		if (override.length() == 0)
			return;
		String[] overrideSplit = override.split("_");
		String langName = overrideSplit[0];
		String countryName = overrideSplit.length > 1 ? overrideSplit[1] : "";
		Resources rez = mContext.getResources();
		Configuration config = new Configuration(rez.getConfiguration());
		DisplayMetrics metrics = rez.getDisplayMetrics();
		config.locale = new Locale(langName, countryName);
		rez.updateConfiguration(config, metrics);
	}

	public static String join(Collection<?> list, String replacement) {
		StringBuilder b = new StringBuilder();
		for (Object item : list) {
			b.append(replacement).append(item.toString());
		}
		String r = b.toString();
		if (r.length() >= replacement.length())
			r = r.substring(replacement.length());
		return r;
	}

	public static String joinArray(Object[] arr, String sep) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < arr.length; i++) {
			if (i != 0) b.append(sep);
			b.append(arr[i] == null? "null": arr[i].toString());
		}
		return b.toString();
	}

	public static boolean hasTooManyPatches() {
		int maxPatchCount = getMaxPatches();
		return maxPatchCount >= 0 && getEnabledPatches().size() >= maxPatchCount;
	}

	public static boolean hasTooManyScripts() {
		int maxPatchCount = getMaxScripts();
		return maxPatchCount >= 0 && getEnabledScripts().size() >= maxPatchCount;
	}

	public static int getMaxPatches() {
		return mContext.getResources().getInteger(R.integer.max_num_patches);
	}

	public static int getMaxScripts() {
		return mContext.getResources().getInteger(R.integer.max_num_scripts);
	}

	public static Set<String> getEnabledPatches() {
		String theStr = getPrefs(1).getString("enabledPatches", "");
		if (theStr.equals("")) return new HashSet<String>();
		return new HashSet<String>(Arrays.asList(theStr.split(";")));
	}

	public static Set<String> getEnabledScripts() {
		String theStr = getPrefs(1).getString("enabledScripts", "");
		if (theStr.equals("")) return new HashSet<String>();
		return new HashSet<String>(Arrays.asList(theStr.split(";")));
	}

	public static boolean isSafeMode() {
		return (MainActivity.libLoaded && MainActivity.tempSafeMode) || getPrefs(0).getBoolean("zz_safe_mode", false);
	}

	public static boolean isPro() {
		return mContext.getPackageName().equals("net.zhuoweizhang.mcpelauncher.pro");
	}

	/**
	 * Returns SharedPreferences of type in argument
	 * 
	 * @param type
	 *            0 - PreferenceManager.getDefaultSharedPreferences <br />
	 *            1 - mcpelauncherprefs
	 * @return SharedPreferences of given type or null, when type is unknown
	 */
	public static SharedPreferences getPrefs(int type) {
		requireInit();
		switch (type) {
		case 0:
			return PreferenceManager.getDefaultSharedPreferences(mContext);
		case 1:
			return mContext.getSharedPreferences("mcpelauncherprefs", 0);
		case 2:
			return mContext.getSharedPreferences("safe_mode_counter", 0);

		}
		return null;
	}

	public static boolean hasExtrasPackage(Context context) {
		return context.getPackageName().equals("net.zhuoweizhang.mcpelauncher.pro");
	}

	public static long parseMemInfo() throws IOException {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader("/proc/meminfo"));
			String l;
			while ((l = reader.readLine()) != null) {
				if (!l.contains(":")) continue;
				String[] parts = l.split(":");
				String partName = parts[0].trim();
				String[] result = parts[1].trim().split(" ");
				if (partName.equals("MemTotal")) {
					return Long.parseLong(result[0]) * 1024;
				}
			}
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException notanotherone){}
			}
		}
		return 0x400000000L; // 16 GB
	}

	/**
	 * Gets the architecture of an ELF library.
	 */
	public static int getElfArch(File file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		byte[] header = new byte[2];
		fis.skip(18);
		fis.read(header, 0, 2);
		int arch = header[0] | (header[1] << 8);
		fis.close();
		if (arch == 0x28) {
			return ScriptManager.ARCH_ARM;
		} else if (arch == 0x3) {
			return ScriptManager.ARCH_I386;
		} else {
			// I have no clue
			System.err.println(file + " has unknown architecture 0x" + Integer.toString(arch, 16));
			return ScriptManager.ARCH_ARM;
		}
	}

	public static String getArchName(int arch) {
		switch (arch) {
			case ScriptManager.ARCH_ARM:
				return "ARM";
			case ScriptManager.ARCH_I386:
				return "Intel";
			default:
				return "Unknown";
		}
	}

	public static void setupTheme(Context context, boolean fullscreen) {
		boolean darkTheme = getPrefs(0).getBoolean("zz_theme_dark", false);
		if (darkTheme) {
			context.setTheme(fullscreen? R.style.FullscreenDarkTheme: R.style.BlockLauncherDarkTheme);
		}
	}

	public static void extractFromZip(File zipFilePath, File outputDir, String pathMatching) throws IOException {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(zipFilePath);
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.isDirectory()) continue;
				if (!entry.getName().startsWith(pathMatching)) continue;
				extractZipEntry(zipFile, outputDir, entry);
			}
		} finally {
			if (zipFile != null) {
				zipFile.close();
			}
		}
	}

	private static void extractZipEntry(ZipFile zipFile, File outputDir, ZipEntry entry) throws IOException {
		BufferedInputStream is = null;
		FileOutputStream fos = null;
		try {
			byte[] bytes = new byte[(int)entry.getSize()];
			is = new BufferedInputStream(zipFile.getInputStream(entry));
			is.read(bytes);
			// yes, this is vulnerable to zip traversal.
			// if this ever gets used for non-apk, you'll have a bad time
			File targetFile = new File(outputDir, entry.getName());
			targetFile.getParentFile().mkdirs();
			fos = new FileOutputStream(targetFile);
			fos.write(bytes);
		} finally {
			if (is != null) is.close();
			if (fos != null) fos.close();
		}
	}
	public static void popStayInForegroundTasks(Activity activity) {
		Intent trampolineIntent = new Intent(activity, TrampolineActivity.class);
		activity.startActivity(trampolineIntent);
		Intent chooserIntent = Intent.createChooser(new Intent(activity.getPackageName() + ".action.TRAMPOLINE"), "Loading...");
        	activity.startActivity(chooserIntent);
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void forceRestartIntoDifferentAbi(Activity activity, Intent targetIntent) {
		final Intent theIntent = targetIntent != null? targetIntent:
			activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
		boolean needsToStayInForeground = Build.VERSION.SDK_INT >= 29; // Android 10 requires alarm
		if (needsToStayInForeground) {
			popStayInForegroundTasks(activity);
		}
		rebootIntoInstrumentation(activity, theIntent);
	}

	private static String getOverriddenNativeAbi() {
		String arch = System.getProperty("os.arch");
		String targetAbi = "armeabi-v7a";
		if (arch.startsWith("armv") || arch.startsWith("aarch")) {
			// already arm
		} else if (arch.equals("x86") || arch.equals("i686") || arch.equals("x86_64")) {
			targetAbi = "x86";
		}
		return targetAbi;
	}

	private static void rebootIntoInstrumentation(Activity activity, Intent targetIntent) {
		boolean launchViaAlarm = Build.VERSION.SDK_INT >= 29; // Android 10 requires alarm
		if (launchViaAlarm) {
			setupAlarm(activity, targetIntent);
		}
		String abiOverride = getOverriddenNativeAbi();
		Bundle bundle = new Bundle();
		if (launchViaAlarm) {
			bundle.putByte("skipLaunch", (byte)1);
		} else {
			bundle.putParcelable("launchIntent", targetIntent);
		}
		ComponentName componentName = new ComponentName(activity, RelaunchInstrumentation.class);
		try {
			Object iActivityManager = null;
			try {
				Method ActivityManager_getService = ActivityManager.class.getMethod("getService");
				iActivityManager = ActivityManager_getService.invoke(null);
			} catch (NoSuchMethodException nme) {
				System.err.println(nme);
				// Android 7.1 and below
				Method ActivityManagerNative_getDefault =
					Class.forName("android.app.ActivityManagerNative").getMethod("getDefault");
				iActivityManager = ActivityManagerNative_getDefault.invoke(null);
			}
			Method IActivityManager_startInstrumentation = iActivityManager.getClass().getMethod("startInstrumentation",
					ComponentName.class, String.class, Integer.TYPE, Bundle.class,
					Class.forName("android.app.IInstrumentationWatcher"),
					Class.forName("android.app.IUiAutomationConnection"),
					Integer.TYPE, String.class);
			IActivityManager_startInstrumentation.invoke(iActivityManager,
					/* className= */ componentName,
					/* profileFile= */ null, /* flags= */ 0, /* arguments= */ bundle,
					/* watcher= */ null, /* connection= */ null, /* userId= */ 0,
					/* abiOverride= */ abiOverride);
			// this call will force-close the process momentarily.
			while (true) {
				Thread.sleep(10000);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void setupAlarm(Activity activity, Intent targetIntent) {
		// borrowed from the existing restart code
		int delay = 1000;
		AlarmManager alarmMgr = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
		long timeMillis = SystemClock.elapsedRealtime() + delay;
		Intent intent = new Intent(targetIntent);
		// trying to close the stupid chooser activity...
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
		alarmMgr.set(AlarmManager.ELAPSED_REALTIME, timeMillis,
				PendingIntent.getActivity(activity, 0, intent, 0));
	}

	/**
	 * Throws an exception when you try to call methods, calling this class to
	 * prevent NPE, without Utils initialization.
	 */
	protected static void requireInit() {
		if (mContext == null)
			throw new RuntimeException("Tried to work with Utils class without context");
	}
}
