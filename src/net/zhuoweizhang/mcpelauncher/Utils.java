package net.zhuoweizhang.mcpelauncher;

import java.io.File;
import java.lang.reflect.Field;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import java.util.Collection;
import java.util.Locale;

public class Utils {
	protected static Context mContext = null;

	public Utils(Context con) {
		mContext = con;
	}

	public static void clearDirectory(File dir) {
		for (File f : dir.listFiles()) {
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
		String override = PreferenceManager.getDefaultSharedPreferences(mContext).getString(
				"zz_language_override", "");
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
		}
		return null;
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
