package net.zhuoweizhang.mcpelauncher.texture;

import java.io.*;
import java.util.*;
import org.json.*;
import android.content.*;
import android.graphics.*;

import net.zhuoweizhang.mcpelauncher.*;

public class TexturePackLoader {

	public final static String TYPE_ZIP = "zip";
	public final static String TYPE_MPKG = "mpkg";
	public final static String TYPE_ADDON = "addon";

	public static List<TexturePackDescription> loadDescriptions(Context context) throws JSONException {
		// we only handle zips for now.
		String descriptionList = context.getSharedPreferences("mcpelauncherprefs", 0).getString("texture_packs", "[]");
		JSONArray arr = new JSONArray(descriptionList);

		int arrLength = arr.length();
		List<TexturePackDescription> descs = new ArrayList<TexturePackDescription>(arrLength);
		for (int i = 0; i < arrLength; i++) {
			descs.add(TexturePackDescription.fromJson(arr.getJSONObject(i)));
		}
		return descs;
	}

	public static List<TexturePack> loadTexturePacks(Context context) throws Exception {
		List<TexturePackDescription> descs = loadDescriptions(context);
		List<TexturePack> packs = new ArrayList<TexturePack>(descs.size());
		for (TexturePackDescription d: descs) {
			packs.add(loadTexturePack(d));
		}
		return packs;
	}

	public static List<TexturePackDescription> loadDescriptionsWithIcons(Context context) throws JSONException {
		List<TexturePackDescription> descs = loadDescriptions(context);
		for (TexturePackDescription d: descs) {
			try {
				loadIconForDescription(d);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return descs;
	}

	public static void loadIconForDescription(TexturePackDescription d) throws Exception {
		TexturePack pack = loadTexturePack(d);
		doLoadIcon(pack, d);
		doLoadMeta(pack, d);
	}
	private static void doLoadIcon(TexturePack pack, TexturePackDescription d) throws Exception {
		InputStream is = pack.getInputStream("pack.png");
		if (is == null) return;
		d.img = BitmapFactory.decodeStream(is);
		is.close();
	}

	private static void doLoadMeta(TexturePack pack, TexturePackDescription d) throws Exception {
		InputStream is = pack.getInputStream("pack.mcmeta");
		if (is == null) return;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] a = new byte[0x1000];
		int p;
		while ((p = is.read(a)) != -1) {
			bos.write(a, 0, p);
		}
		is.close();
		JSONObject jsonObj = new JSONObject(new String(a, "UTF-8"));
		d.description = jsonObj.getJSONObject("pack").getString("description");
	}

	private static TexturePack loadTexturePack(TexturePackDescription desc) throws Exception {
		if (desc.type.equals(TYPE_ZIP) || desc.type.equals(TYPE_MPKG)) {
			return new ZipTexturePack(new File(desc.path));
		} else {
			throw new RuntimeException("Unsupported texture pack type: " + desc);
		}
	}

	public static String describeTexturePack(Context context, TexturePackDescription desc) {
		String name = desc.path;
		if (desc.type.equals(TYPE_ZIP) || desc.type.equals(TYPE_MPKG)) {
			name = name.substring(name.lastIndexOf("/") + 1);
		}
		return name;
	}

	public static void saveDescriptions(Context context, List<TexturePackDescription> descs) throws JSONException {
		JSONArray arr = new JSONArray();
		for (int i = 0; i < descs.size(); i++) {
			arr.put(i, descs.get(i).toJson());
		}
		context.getSharedPreferences("mcpelauncherprefs", 0).edit().putString("texture_packs", arr.toString()).
			commit();
	}
}
