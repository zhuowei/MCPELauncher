package net.zhuoweizhang.mcpelauncher;

import java.io.*;
import java.util.*;
import java.nio.charset.Charset;
import org.json.*;
import android.os.Environment;

public class ResourcePack {
	public File file;
	public ResourcePack(File file) {
		this.file = file;
	}
	public InputStream getInputStream(String name) throws IOException {
		File theFile = new File(this.file, name);
		if (!theFile.exists()) return null;
		return new FileInputStream(theFile);
	}
	public String getName() {
		return file.getName();
	}

	public static List<String> readAllIds() throws IOException {
		List<String> list = new ArrayList<String>();
		File configFile = new File(Environment.getExternalStorageDirectory(), "games/com.mojang/minecraftpe/resource_packs.txt");
		if (!configFile.exists()) return list;
		byte[] b = new byte[(int)configFile.length()];
		FileInputStream fis = new FileInputStream(configFile);
		fis.read(b);
		fis.close();
		String[] strs = new String(b, Charset.forName("UTF-8")).split("\n");
		for (String s: strs) {
			if (s.length() != 0) list.add(s);
		}
		return list;
	}
	public static List<ResourcePack> getAllResourcePacks() throws IOException {
		List<ResourcePack> list = new ArrayList<ResourcePack>();
		File resPackDir = new File(Environment.getExternalStorageDirectory(), "games/com.mojang/resource_packs");
		if (!resPackDir.exists()) return list;
		List<String> ids = readAllIds();
		if (ids.size() == 0) return list;
		// build a ID to path mapping
		Map<String, File> mapping = new HashMap<String, File>();
		Charset utf8Charset = Charset.forName("UTF-8");
		for (File f: resPackDir.listFiles()) {
			File manifestFile = new File(f, "resources.json");
			if (!manifestFile.exists()) continue;
			try {
				byte[] b = new byte[(int)manifestFile.length()];
				FileInputStream fis = new FileInputStream(manifestFile);
				fis.read(b);
				fis.close();
				String stringRead = new String(b, utf8Charset);
				JSONObject obj = new JSONObject(stringRead);
				String id = obj.getString("pack_id");
				mapping.put(id, f);
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
		}
		for (String id: ids) {
			File f = mapping.get(id);
			if (f == null) continue;
			list.add(new ResourcePack(f));
		}
		return list;
	}
}
