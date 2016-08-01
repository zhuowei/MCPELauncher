package net.zhuoweizhang.mcpelauncher;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.*;
import android.content.Context;
import android.content.res.*;

import com.mojang.minecraftpe.MainActivity;

public class AddonOverrideTexturePack implements TexturePack {

	private final MainActivity activity;
	private final Map<String, ZipFile> assets = new HashMap<String, ZipFile>();
	private final Map<String, ZipFile> zipsByPackage = new HashMap<String, ZipFile>();
	private final String prefix;

	public AddonOverrideTexturePack(MainActivity activity, String prefix) {
		this.activity = activity;
		this.prefix = prefix;
		initAddons();
	}

	private void initAddons() {
		for (String packageName: activity.loadedAddons) {
			System.out.println("Addon textures: " + packageName);
			try {
				addPackage(new File(activity.getPackageManager().getPackageInfo(packageName, 0).
					applicationInfo.publicSourceDir), packageName);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public Map<String, ZipFile> getZipsByPackage() {
		return zipsByPackage;
	}

	public void addPackage(File myZip, String packageName) throws IOException {
		ZipFile zipFile = new ZipFile(myZip);
		Enumeration<? extends ZipEntry> i = zipFile.entries();
		while (i.hasMoreElements()) {
			ZipEntry entry = i.nextElement();
			String name = entry.getName();
			if (name.contains("__MACOSX")) continue; //A pox on ye, Mac OS X
			if (name.indexOf("assets/") != 0) continue;
			if (name.charAt(name.length() - 1) == '/') continue;
			assets.put(name, zipFile);
		}
		zipsByPackage.put(packageName, zipFile);
	}

	public InputStream getInputStream(String fileName) throws IOException {
		if (fileName.startsWith(prefix)) fileName = fileName.substring(prefix.length());
		String name = "assets/" + fileName;
		ZipFile file = assets.get(name);
		if (file == null) return null;
		return file.getInputStream(file.getEntry(name));
	}

	public long getSize(String fileName) throws IOException {
		if (fileName.startsWith(prefix)) fileName = fileName.substring(prefix.length());
		String name = "assets/" + fileName;
		ZipFile file = assets.get(name);
		if (file == null) return -1;
		return file.getEntry(name).getSize();
	}

	public void close() throws IOException {
		//do nothing
	}

	public List<String> listFiles() throws IOException {
		List<String> list = new ArrayList<String>();
		for (Map.Entry<String, ZipFile> e: assets.entrySet()) {
			list.add(e.getKey().substring("assets/".length()));
		}
		return list;
	}
}
