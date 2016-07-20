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

public class ResourcePackManifestProvider implements TexturePack {

	public String manifestPath;
	public JSONObject metaObj;
	public boolean hasChanges = false;
	public ResourcePackManifestProvider(String manifestPath) {
		this.manifestPath = manifestPath;
	}

	public InputStream getInputStream(String fileName) throws IOException {
		if (!hasChanges) return null;
		if (fileName.equals(manifestPath)) {
			return new ByteArrayInputStream(metaObj.toString().getBytes("UTF-8"));
		}
		return null;
	}

	public List<String> listFiles() throws IOException {
		return new ArrayList<String>();
	}

	public void init(MainActivity activity) throws Exception {
		hasChanges = false;
		loadAtlas(activity);
	}

	private void loadAtlas(MainActivity activity) throws Exception {
		// read the meta file
		InputStream metaIs = activity.getInputStreamForAsset(manifestPath);
		byte[] a = new byte[0x1000];
		int p;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		while ((p = metaIs.read(a)) != -1) {
			bos.write(a, 0, p);
		}
		metaIs.close();

		metaObj = new JSONObject(new String(bos.toByteArray(), "UTF-8"));
	}

	public void addTextures(List<String[]> textures) throws JSONException {
		if (textures.size() == 0) return;
		JSONObject textureObject = metaObj.getJSONObject("resources").getJSONObject("textures");
		for (String[] pair: textures) {
			textureObject.put(pair[0], pair[1]);
		}
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
