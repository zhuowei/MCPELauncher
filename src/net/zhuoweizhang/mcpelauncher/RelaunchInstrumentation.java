package net.zhuoweizhang.mcpelauncher;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class RelaunchInstrumentation extends Instrumentation {
	@Override
	public void onCreate(Bundle arguments) {
		super.onCreate(arguments);
		if (arguments.getByte("skipLaunch") == 0) {
			reLaunch(arguments);
		}
	}
	private void reLaunch(Bundle arguments) {
		Context context = getContext();
		Intent launchIntent = arguments.getParcelable("launchIntent");
		if (launchIntent == null) {
			launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
		}
		launchIntent.setPackage(context.getPackageName());
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(launchIntent);
	}
}
