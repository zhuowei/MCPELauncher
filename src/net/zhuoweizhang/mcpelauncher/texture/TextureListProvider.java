package net.zhuoweizhang.mcpelauncher.texture;

import java.io.*;
import java.util.*;

import com.mojang.minecraftpe.*;
import net.zhuoweizhang.mcpelauncher.*;

public class TextureListProvider implements TexturePack {

	public String manifestPath;
	public List<String> files;
	public List<String> addedFiles = new ArrayList<String>();
	public Set<String> addedFilesSet = new HashSet<String>();
	public boolean hasChanges = false;
	public TextureListProvider(String manifestPath) {
		this.manifestPath = manifestPath;
	}

	public InputStream getInputStream(String fileName) throws IOException {
		if (!hasChanges) return null;
		if (fileName.equals(manifestPath)) {
			dumpAtlas();
			StringBuilder builder = new StringBuilder();
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
		if (metaIs == null) {
			files = new ArrayList<String>();
			return;
		}
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
				rawS = TextureUtils.removeExtraDotsFromPath(rawS);
				addedFiles.add(rawS);
				addedFilesSet.add(rawS.substring(rawS.lastIndexOf("/") + 1));
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
	public boolean containsFile(String file) {
		return addedFilesSet.contains(file.substring(file.lastIndexOf("/") + 1));
	}
	public Set<String> listDir(String dirPath) {
		String prefix = dirPath + "/";
		Set<String> outList = new HashSet<String>();
		for (String path: addedFiles) {
			if (!path.startsWith(prefix) || path.indexOf("/", prefix.length()) != -1) continue;
			outList.add(path.substring(path.lastIndexOf("/")));
		}
		return outList;
	}

	public void close() throws IOException {
		files = null;
		hasChanges = false;
	}

	public long getSize(String name) {
		return 0;
	}

}
