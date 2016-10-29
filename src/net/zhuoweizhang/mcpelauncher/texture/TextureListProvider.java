package net.zhuoweizhang.mcpelauncher.texture;

import java.io.*;
import java.util.*;

import com.mojang.minecraftpe.*;
import net.zhuoweizhang.mcpelauncher.*;

public class TextureListProvider implements TexturePack {

	public String manifestPath;
	public List<String> files;
	public boolean hasChanges = false;
	public TextureListProvider(String manifestPath) {
		this.manifestPath = manifestPath;
	}

	public InputStream getInputStream(String fileName) throws IOException {
		if (!hasChanges) return null;
		if (fileName.equals(manifestPath)) {
			System.out.println("new textures list!");
			StringBuilder builder = new StringBuilder();
			builder.append("texture/blocks/harambe\n");
			for (String s: files) {
				builder.append(s);
				builder.append('\n');
			}
			return new ByteArrayInputStream(builder.toString().getBytes("UTF-8"));
		}
		return null;
	}

	public void dumpAtlas() throws IOException {
		File file = new File("/sdcard/bl_dump_textures_list.txt");
		FileOutputStream fos = new FileOutputStream(file);
		StringBuilder builder = new StringBuilder();
		for (String s: files) {
			builder.append(s);
			builder.append('\n');
		}
		fos.write(builder.toString().getBytes("UTF-8"));
		fos.close();
	}

	public List<String> listFiles() throws IOException {
		return new ArrayList<String>();
	}

	public void init(MainActivity activity) throws Exception {
		hasChanges = false;
		loadTextureList(activity);
		addExtraTextures(activity);
	}

	private void loadTextureList(MainActivity activity) throws Exception {
		// read the meta file
		InputStream metaIs = activity.getInputStreamForAsset(manifestPath);
		byte[] a = new byte[0x1000];
		int p;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		while ((p = metaIs.read(a)) != -1) {
			bos.write(a, 0, p);
		}
		metaIs.close();

		String[] strs = new String(bos.toByteArray(), "UTF-8").split("\n");
		files = new ArrayList<String>(strs.length);
		for (String str: strs) {
			if (str.length() > 0) files.add(str);
		}
	}

	private void addExtraTextures(MainActivity activity) throws IOException {
		Set<String> theSet = new HashSet<String>(files);
		for (TexturePack pack : activity.textureOverrides) {
			List<String> newFiles = pack.listFiles();
			for (String rawS: newFiles) {
				int lastIndex = rawS.lastIndexOf('.');
				if (lastIndex == -1) continue; // no file extension?
				String s = rawS.substring(0, lastIndex);
				if (theSet.add(s)) {
					files.add(s);
					hasChanges = true;
				}
			}
		}
	}

	public void close() throws IOException {
		files = null;
		hasChanges = false;
	}

	public long getSize(String name) {
		return 0;
	}

}
