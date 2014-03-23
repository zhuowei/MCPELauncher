package net.zhuoweizhang.mcpelauncher;

import android.app.Application;

public class BlockLauncher extends Application {
	@Override
	public void onCreate() {
		Utils.setContext(getApplicationContext());
		super.onCreate();
	}
}
