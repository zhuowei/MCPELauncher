package net.zhuoweizhang.mcpelauncher;

import android.app.Application;
import android.os.Build;

public class BlockLauncher extends Application {
	@Override
	public void onCreate() {
		Utils.setContext(getApplicationContext());
		super.onCreate();
		if (Build.VERSION.SDK_INT >= 14) {
			registerActivityLifecycleCallbacks(new ThemeLifecycleCallbacks());
		}
	}
}
