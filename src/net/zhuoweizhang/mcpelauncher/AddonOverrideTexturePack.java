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

	private MainActivity activity;
	private Map<String, ZipFile> assets = new HashMap<String, ZipFile>();

	public AddonOverrideTexturePack(MainActivity activity) {
		this.activity = activity;
		initAddons();
	}

	private void initAddons() {
		for (String packageName: activity.loadedAddons) {
			System.out.println("Addon textures: " + packageName);
			try {
				addPackage(new File(activity.getPackageManager().getPackageInfo(packageName, 0).
					applicationInfo.publicSourceDir));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void addPackage(File myZip) throws IOException {
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
	}

	public InputStream getInputStream(String fileName) throws IOException {
		String name = "assets/" + fileName;
		ZipFile file = assets.get(name);
		if (file == null) return null;
		return file.getInputStream(file.getEntry(name));
	}

	public void close() throws IOException {
		//do nothing
	}
}
