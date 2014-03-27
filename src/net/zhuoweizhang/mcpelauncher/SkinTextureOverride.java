package net.zhuoweizhang.mcpelauncher;

import java.io.*;

public class SkinTextureOverride implements TexturePack {

	public SkinTextureOverride() {
	}

	public InputStream getInputStream(String fileName) throws IOException {
		boolean loadSkin = Utils.getPrefs(0).getBoolean("zz_skin_enable", false);
		if (!loadSkin || Utils.isSafeMode()) return null;
		if (fileName.equals("images/mob/char.png")) {
			String skinPath = Utils.getPrefs(1).getString("player_skin", null);
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
