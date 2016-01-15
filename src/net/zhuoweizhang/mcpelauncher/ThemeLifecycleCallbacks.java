package net.zhuoweizhang.mcpelauncher;

import android.app.Application;
import android.app.Activity;
import android.os.Bundle;

public class ThemeLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
	public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
		Utils.setupTheme(activity, false);
	}
	public void onActivityDestroyed(Activity activity) {}
	public void onActivityPaused(Activity activity) {}
	public void onActivityResumed(Activity activity) {}
	public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
	public void onActivityStarted(Activity activity) {}
	public void onActivityStopped(Activity activity) {}
}
