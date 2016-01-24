package net.zhuoweizhang.mcpelauncher;

import java.io.*;
import org.json.*;

public class WorldData {
	File mFile;
	File mDir;
	JSONObject mData;
	boolean dirty = false;
	public WorldData(File dir) throws IOException {
		mDir = dir;
		mFile = new File(dir, "blocklauncher_data.json");
		load();
	}

	protected void load() throws IOException {
		if (!mFile.exists() || mFile.length() == 0) {
			loadDefaults();
			return;
		}
		byte[] buf = new byte[(int) mFile.length()];
		FileInputStream fis = null;
		boolean success = false;
		try {
			fis = new FileInputStream(mFile);
			fis.read(buf);
			success = true;
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException ie) {
					// screw it
					ie.printStackTrace();
				}
			}
		}
		if (success) {
			try {
				mData = new JSONObject(new String(buf, "UTF-8"));
			} catch (JSONException je) {
				je.printStackTrace();
				loadDefaults();
			}
		} else {
			loadDefaults();
		}
	}

	protected void loadDefaults() {
		try {
			mData = new JSONObject();
			mData.put("entities", new JSONObject());
		} catch (JSONException je) {
			throw new RuntimeException(je); // literally impossible
		}
	}

	public void setEntityData(long entityId, String key, String value) {
		if (key.indexOf(".") == -1) {
			throw new RuntimeException("Entity data keys must be in format of author.modname.keyname;" +
				" for example, coolmcpemodder.sz.favoritecolor");
		}
		try {
			JSONObject obj = mData.getJSONObject("entities");
			JSONObject entityData = mData.optJSONObject(Long.toString(entityId));
			if (entityData == null) {
				entityData = new JSONObject();
				obj.put(Long.toString(entityId), entityData);
			}
			entityData.put(key, value);
			if (BuildConfig.DEBUG) System.out.println("world data: set entity data " + entityId + ":" + key + ":" + value);
			dirty = true;
		} catch (JSONException je) {
			throw new RuntimeException(je);
		}
	}

	public String getEntityData(long entityId, String key) {
		try {
			JSONObject obj = mData.getJSONObject("entities");
			JSONObject entityData = obj.optJSONObject(Long.toString(entityId));
			if (entityData == null) return null;
			return entityData.optString(key);
		} catch (JSONException je) {
			throw new RuntimeException(je);
		}
	}

	public void clearEntityData(long entityId) {
		try {
			JSONObject obj = mData.getJSONObject("entities");
			dirty = (null != obj.remove(Long.toString(entityId)));
			if (dirty && BuildConfig.DEBUG) System.out.println("world data: cleared entity data " + entityId);
		} catch (JSONException je) {
			throw new RuntimeException(je);
		}
	}

	public void save() throws IOException {
		if (BuildConfig.DEBUG) System.out.println(dirty? "Saving world data": "Not saving world data");
		if (!dirty) return;
		FileOutputStream fis = null;
		try {
			fis = new FileOutputStream(mFile);
			fis.write(mData.toString().getBytes("UTF-8"));
			dirty = false;
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException ie) {
					// screw it
					ie.printStackTrace();
				}
			}
		}
	}
}
