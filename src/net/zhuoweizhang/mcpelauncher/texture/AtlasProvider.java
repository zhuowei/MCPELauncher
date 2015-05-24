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

public abstract class AtlasProvider implements TexturePack {

	public String metaName, atlasName, importDir;
	public boolean hasChanges = false;
	public Bitmap atlasImg;
	public AtlasMeta metaObj;
	public ImageLoader loader;
	public AtlasProvider(String metaName, String atlasName, String importDir, ImageLoader loader) {
		this.metaName = metaName;
		this.atlasName = atlasName;
		this.importDir = importDir;
		this.loader = loader;
	}

	public InputStream getInputStream(String fileName) throws IOException {
		if (!hasChanges) return null;
		if (fileName.equals(metaName)) {
			return new ByteArrayInputStream(metaObj.data.toString().getBytes("UTF-8"));
		} else if (fileName.equals(atlasName)) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			loader.save(atlasImg, bos);
			return new ByteArrayInputStream(bos.toByteArray());
		} else {
			return null;
		}
	}

	public List<String> listFiles() throws IOException {
		return new ArrayList<String>();
	}

	public void initAtlas(MainActivity activity) throws IOException {
		hasChanges = false;
		// read the meta file
		InputStream metaIs = activity.getInputStreamForAsset(metaName);
		// read and decode the original atlas
		InputStream atlasIs = activity.getInputStreamForAsset(atlasName);
		atlasImg = loader.load(atlasIs);
		atlasIs.close();
		// use meta to find out which spots are not occupied
		// list all extra files that need to be added
		// add them to meta
		hasChanges = true;
	}

}
