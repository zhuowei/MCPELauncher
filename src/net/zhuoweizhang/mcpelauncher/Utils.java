package net.zhuoweizhang.mcpelauncher;

import java.io.File;
import java.lang.reflect.Field;
import android.content.Context;
import android.preference.PreferenceManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import java.util.Locale;

public class Utils {
	public static void clearDirectory(File dir) {
		for (File f: dir.listFiles()) {
			if (f.isDirectory()) {
				clearDirectory(f);
			}
			f.delete();
		}
	}
	public static Field getDeclaredFieldRecursive(Class<?> clazz, String name) {
		if (clazz == null) return null;
		try {
			return clazz.getDeclaredField(name);
		} catch (NoSuchFieldException nsfe) {
			return getDeclaredFieldRecursive(clazz.getSuperclass(), name);
		}
	}

	public static void setLanguageOverride(Context ctx) {
		String override = PreferenceManager.getDefaultSharedPreferences(ctx).getString("zz_language_override", "");
		if (override.length() == 0) return;
		String[] overrideSplit = override.split("_");
		String langName = overrideSplit[0];
		String countryName = overrideSplit.length > 1? overrideSplit[1]: "";
		Resources rez = ctx.getResources();
		Configuration config = new Configuration(rez.getConfiguration());
		DisplayMetrics metrics = rez.getDisplayMetrics();
		config.locale = new Locale(langName, countryName);
		rez.updateConfiguration(config, metrics);
	}
}
