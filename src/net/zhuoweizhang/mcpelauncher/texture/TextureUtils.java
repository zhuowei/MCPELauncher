package net.zhuoweizhang.mcpelauncher.texture;

import java.io.*;
import java.util.*;
import net.zhuoweizhang.mcpelauncher.*;

public class TextureUtils {
	public static List<String> getAllFilesFilter(List<TexturePack> packs, String filter) throws IOException {
		List<String> allFiles = new ArrayList<String>();
		for (TexturePack p: packs) {
			List<String> fileList = p.listFiles();
			List<String> temp = new ArrayList<String>();
			for (String s: fileList) {
				if (s.contains(filter)) temp.add(s);
			}
			Collections.sort(temp);
			allFiles.addAll(temp);
		}
		return allFiles;
	}
	/**
	 * test 2.4/what.the/cheese.png -> test 2_4/what_the/cheese.png
	 */
	public static String removeExtraDotsFromPath(String inStr) {
		int endDot = inStr.lastIndexOf('.');
		if (endDot == -1) return inStr;
		int startDot = inStr.indexOf('.');
		if (startDot == endDot) return inStr;
		return inStr.substring(0, endDot).replace('.', '_') + inStr.substring(endDot);
	}
}
