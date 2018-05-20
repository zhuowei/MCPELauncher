package net.zhuoweizhang.mcpelauncher.texture;

import java.io.*;
import java.util.*;

import org.json.*;

import com.mojang.minecraftpe.*;
import net.zhuoweizhang.mcpelauncher.*;

public class PackContentsProvider implements TexturePack {

	public String manifestPath;
	public String contentsPath;
	public boolean hasChanges = false;
	public JSONArray jsonArray;
	public JSONObject contentsObject;
	public PackContentsProvider(String manifestPath, String contentsPath) {
		this.manifestPath = manifestPath;
		this.contentsPath = contentsPath;
	}

	public InputStream getInputStream(String fileName) throws IOException {
		if (!hasChanges) return null;
		if (fileName.equals(manifestPath)) {
			//System.err.println("PackProvide MANIFEST!!!");
			return new ByteArrayInputStream(jsonArray.toString().getBytes("UTF-8"));
		}
		if (fileName.equals(contentsPath)) {
			//System.err.println("PackProvide CONTENTS!!!");
			return new ByteArrayInputStream(contentsObject.toString().getBytes("UTF-8"));
		}
		return null;
	}

	public void dumpAtlas() throws IOException {
		File file = new File("/sdcard/bl_dump_textures_list.txt");
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(jsonArray.toString().getBytes("UTF-8"));
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

	private byte[] getBytesOfFile(MainActivity activity, String manifestPath) throws Exception {
		InputStream metaIs = activity.getInputStreamForAsset(manifestPath);
		if (metaIs == null) {
			return null;
		}
		byte[] a = new byte[0x1000];
		int p;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		while ((p = metaIs.read(a)) != -1) {
			bos.write(a, 0, p);
		}
		metaIs.close();
		return bos.toByteArray();
	}

	private void loadTextureList(MainActivity activity) throws Exception {
		// read the meta file
		byte[] list = getBytesOfFile(activity, manifestPath);
		byte[] contents = getBytesOfFile(activity, contentsPath);
		if (list == null || contents == null) return;

		jsonArray = new JSONArray(new String(list, "UTF-8"));
		contentsObject = new JSONObject(new String(contents, "UTF-8"));
	}


	private void addExtraTextures(MainActivity activity) throws IOException, JSONException {
		Set<String> theSet = new HashSet<String>();
		for (int i = 0; i < jsonArray.length(); i++) {
			String path = jsonArray.getString(i);
			theSet.add(path);
		}
		for (TexturePack pack : activity.textureOverrides) {
			List<String> newFiles = pack.listFiles();
			for (String rawS: newFiles) {
				rawS = TextureUtils.removeExtraDotsFromPath(rawS);
				int lastIndex = rawS.lastIndexOf('.');
				if (lastIndex == -1) continue; // no file extension?
				String extension = rawS.substring(lastIndex + 1);
				if (!(extension.equals("png") || extension.equals("tga"))) continue;  
				String s = rawS.substring(0, lastIndex);
				if (theSet.add(s)) {
					addFile(s, rawS);
					hasChanges = true;
				}
			}
		}
		//if (BuildConfig.DEBUG) System.out.println("it's theSet: " + theSet);
	}

	private void addFile(String file, String rawFile) throws JSONException {
		jsonArray.put(file);
		JSONObject tmpObj = new JSONObject();
		tmpObj.put("path", rawFile);
		contentsObject.getJSONArray("content").put(tmpObj);
	}

	public void close() throws IOException {
		hasChanges = false;
	}

	public long getSize(String name) {
		return 0;
	}

}
