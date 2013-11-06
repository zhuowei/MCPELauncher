package net.zhuoweizhang.mcpelauncher;

import java.io.File;

public class Utils {
	public static void clearDirectory(File dir) {
		for (File f: dir.listFiles()) {
			if (f.isDirectory()) {
				clearDirectory(f);
			}
			f.delete();
		}
	}
}
