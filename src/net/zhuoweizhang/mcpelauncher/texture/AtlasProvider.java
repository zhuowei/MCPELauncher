// needs:
// map of name to position
// map of empty positions
// some way to add and remove positions from the atlas
// some sort of do init function that pulls all files from textures loaded currently, and adds/removes the meta files into the atlas
package net.zhuoweizhang.mcpelauncher.texture;

import java.io.*;
import java.util.*;

import android.graphics.*;

import org.json.*;

import com.mojang.minecraftpe.*;
import net.zhuoweizhang.mcpelauncher.*;

public class AtlasProvider implements TexturePack {

	public String metaName;
	public String importDir;
	public String textureNamePrefix;
	public List<String[]> addedTextureNames = new ArrayList<String[]>();
	public JSONObject metaObj;
	public boolean hasChanges = false;
	public AtlasProvider(String metaName, String importDir, String textureNamePrefix) {
		this.metaName = metaName;
		this.importDir = importDir;
		this.textureNamePrefix = textureNamePrefix;
	}

	public InputStream getInputStream(String fileName) throws IOException {
		if (!hasChanges) return null;
		if (fileName.equals(metaName)) {
			return new ByteArrayInputStream(metaObj.toString().getBytes("UTF-8"));
		}
		return null;
	}

	public void dumpAtlas() throws IOException {
		File file = new File("/sdcard/bl_dump_" + textureNamePrefix + "json");
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(metaObj.toString().getBytes("UTF-8"));
		fos.close();
	}

	public List<String> listFiles() throws IOException {
		return new ArrayList<String>();
	}

	public void initAtlas(MainActivity activity) throws Exception {
		hasChanges = false;
		loadAtlas(activity);
		hasChanges = addAllToMeta(activity);
	}

	private void loadAtlas(MainActivity activity) throws Exception {
		// read the meta file
		InputStream metaIs = activity.getInputStreamForAsset(metaName);
		byte[] a = new byte[0x1000];
		int p;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		while ((p = metaIs.read(a)) != -1) {
			bos.write(a, 0, p);
		}
		metaIs.close();

		metaObj = new JSONObject(new String(bos.toByteArray(), "UTF-8"));
	}

	private boolean addAllToMeta(MainActivity activity) throws Exception {
		// list all files to be added
		// for each,
		// call AtlasMeta.getIconPosition to get icon position to edit into
		// then load icon and copy into texture meta
		List<String> pathsForMeta = TextureUtils.getAllFilesFilter(activity.textureOverrides, importDir);
		if (pathsForMeta.size() == 0) return false;
		Object[] nameParts = new Object[2];
		for (int i = pathsForMeta.size() - 1; i >= 0; i--) {
			String filePath = pathsForMeta.get(i);
			if (!filePath.toLowerCase().endsWith(".png")) continue;
			parseNameParts(filePath, nameParts);
			if (nameParts[0] == null) continue;
			// place into meta obj
			addFileIntoObj(filePath, nameParts);
		}
		return true;
	}

	private void parseNameParts(String filePath, Object[] nameParts) {
		nameParts[0] = null;
		String fileName = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."));
		int underscoreIndex = fileName.lastIndexOf("_");
		if (underscoreIndex < 0) return;
		String name = fileName.substring(0, underscoreIndex);
		String number = fileName.substring(underscoreIndex + 1);
		try {
			nameParts[1] = Integer.parseInt(number);
		} catch (NumberFormatException nfe) {
			return;
		}
		nameParts[0] = name;
	}

	private void addFileIntoObj(String filePath, Object[] nameParts) throws JSONException {
		filePath = TextureUtils.removeExtraDotsFromPath(filePath);
		String texName = (String) nameParts[0];
		int texIndex = (Integer) nameParts[1];
		int index = filePath.lastIndexOf(".");
		String textureResName = (index != -1? filePath.substring(0, index): filePath);
		JSONObject obj = metaObj.getJSONObject("texture_data").optJSONObject(texName);
		if (obj == null) {
			obj = new JSONObject();
			metaObj.getJSONObject("texture_data").put(texName, obj);
		}
		JSONArray arr = obj.optJSONArray("textures");
		if (arr == null) {
			arr = new JSONArray();
			obj.put("textures", arr);
		}
		if (texIndex < arr.length()) {
			arr.put(texIndex, textureResName);
		} else {
			for (int i = arr.length(); i <= texIndex; i++) {
				arr.put(i, textureResName);
			}
		}
		addedTextureNames.add(new String[] {textureResName, filePath});
	}

	public boolean hasIcon(String name, int index) {
		try {
			JSONObject obj = metaObj.getJSONObject("texture_data").optJSONObject(name);
			if (obj == null) return false;
			JSONArray arr = obj.optJSONArray("textures");
			if (arr == null) {
				return index == 0 && obj.optString("textures") != null;
			}
			return index < arr.length();
		} catch (JSONException je) {
			je.printStackTrace();
			return false;
		}
	}

	public String getIcon(String name, int index) {
		try {
			JSONObject obj = metaObj.getJSONObject("texture_data").optJSONObject(name);
			if (obj == null) return null;
			JSONArray arr = obj.optJSONArray("textures");
			if (arr == null) {
				// just one texture.
				if (index == 0) {
					return obj.optString("textures");
				}
				return null;
			}
			return arr.optString(index);
		} catch (JSONException je) {
			je.printStackTrace();
			return null;
		}
	}

	public void setIcon(String texName, String[] textures) throws JSONException {
		JSONObject obj = metaObj.getJSONObject("texture_data").optJSONObject(texName);
		if (obj == null) {
			obj = new JSONObject();
			metaObj.getJSONObject("texture_data").put(texName, obj);
		}
		JSONArray arr = new JSONArray(Arrays.asList(textures));
		obj.put("textures", arr);
		hasChanges = true;
	}

	public void close() throws IOException {
		metaObj = null;
		hasChanges = false;
	}

	public long getSize(String name) {
		return 0;
	}

}
