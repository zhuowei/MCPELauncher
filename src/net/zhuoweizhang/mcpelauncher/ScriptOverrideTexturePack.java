package net.zhuoweizhang.mcpelauncher;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import android.content.Context;

public class ScriptOverrideTexturePack implements TexturePack {

	private Context context;

	public ScriptOverrideTexturePack(Context ctx) {
		this.context = ctx;
	}

	public InputStream getInputStream(String fileName) throws IOException {
		if (ScriptManager.androidContext == null) return null;
		File file = ScriptManager.getTextureOverrideFile(fileName);
		if (!file.exists()) return null;
		return new FileInputStream(file);
	}

	public void close() throws IOException {
		//do nothing
	}

}
