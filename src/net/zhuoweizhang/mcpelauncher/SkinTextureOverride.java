package net.zhuoweizhang.mcpelauncher;

import java.io.*;
import android.content.*;
import android.preference.*;

public class SkinTextureOverride implements TexturePack {

	private Context context;

	public SkinTextureOverride(Context ctx) {
		this.context = ctx;
	}

	public InputStream getInputStream(String fileName) throws IOException {
		boolean loadSkin = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("zz_skin_enable", false);
		if (!loadSkin) return null;
		if (fileName.equals("mob/char.png")) {
			String skinPath = context.getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0).getString("player_skin", null);
			if (skinPath == null) return null;
			File file = new File(skinPath);
			if (!file.exists()) return null;
			return new FileInputStream(file);
		}
		return null;
	}

	public void close() throws IOException {
		//do nothing
	}

}
