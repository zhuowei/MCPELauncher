package net.zhuoweizhang.mcpelauncher.texture;

import java.io.*;
import java.util.*;

import org.json.*;

import com.mojang.minecraftpe.*;
import net.zhuoweizhang.mcpelauncher.*;

// I write these blocks in JSON, for anything not written in JSON cannot be trusted.
// -- Brandon Sanderson, probably
public class ClientBlocksJsonProvider implements TexturePack {

	public String manifestPath;
	public JSONObject metaObj;
	public boolean hasChanges = false;
	public ClientBlocksJsonProvider(String manifestPath) {
		this.manifestPath = manifestPath;
	}

	public InputStream getInputStream(String fileName) throws IOException {
		if (!hasChanges) return null;
		if (fileName.equals(manifestPath)) {
			dumpAtlas();
			return new ByteArrayInputStream(metaObj.toString().getBytes("UTF-8"));
		}
		return null;
	}

	public void dumpAtlas() throws IOException {
		File file = new File("/sdcard/bl_dump_client_blocks.json");
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(metaObj.toString().getBytes("UTF-8"));
		fos.close();
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

	private static final String[] sidesNames = {"up", "down", "north", "south", "west", "east"};
	public void setBlockTextures(String blockName, int blockId, String[] textureNames, int[] textureOffsets) throws JSONException {
		String blockHash = blockName + "." + blockId;

		// in the atlas, create six mappings, one for each side of the block
		// for each side: for each damage value
		//	find existing mapping from textureNames, grab the texture filename from textureOffsets
		//	(note: yes, this means each modPE defined set of textures will be placed in a useless block texture)
		//	put the filename into the new mapping at the right position
		for (int side = 0; side < 6; side++) {
			String[] textureFiles = new String[16];
			for (int damage = 0; damage < 16; damage++) {
				String tn = textureNames[damage*6 + side];
				int to = textureOffsets[damage*6 + side];
				String textureFile = ScriptManager.terrainMeta.getIcon(tn, to);
				if (textureFile == null) {
					throw new RuntimeException("Can't find texture " + tn + ":" + to + 
						" when constructing block " + blockName + " (" + blockId + ")");
				}
				textureFiles[damage] = textureFile;
			}
			ScriptManager.terrainMeta.setIcon(blockHash + "_" + sidesNames[side], textureFiles);
		}

		// all the sides are in; now create the actual block.
		JSONObject obj = metaObj.optJSONObject(blockHash);
		if (obj == null) {
			obj = new JSONObject();
			metaObj.put(blockHash, obj);
		}

		JSONObject textureObj = new JSONObject();
		for (int side = 0; side < 6; side++) {
			textureObj.put(sidesNames[side], blockHash + "_" + sidesNames[side]);
		}
		obj.put("textures", textureObj);

		hasChanges = true;
		System.out.println("Client blocks.json: " + blockName + ":" + blockId + ":" +
			Arrays.toString(textureNames) + ":" + Arrays.toString(textureOffsets));
	}

	public void close() throws IOException {
		metaObj = null;
		hasChanges = false;
	}

	public long getSize(String name) {
		return 0;
	}

}
