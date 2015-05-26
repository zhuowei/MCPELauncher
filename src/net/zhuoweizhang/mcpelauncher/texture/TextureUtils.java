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
}
