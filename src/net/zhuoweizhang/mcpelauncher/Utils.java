package net.zhuoweizhang.mcpelauncher;

import java.io.File;
import java.lang.reflect.Field;

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
}
