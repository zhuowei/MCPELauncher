package net.zhuoweizhang.mcpelauncher;

import java.lang.ref.WeakReference;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.util.*;

import com.mojang.minecraftpe.MainActivity;

public class AddonInstallReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {
		System.out.println("MCPELauncher: " + intent.toString());
		System.out.println("Is lib loaded? " + MainActivity.libLoaded);
		if (!MainActivity.libLoaded) return;
		if (MainMenuOptionsActivity.isManagingAddons) return;
		if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED) && intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
			return;
		}
		try {
			PackageManager pm = context.getPackageManager();
			String packageName = intent.getData().toString().substring(8);
			System.out.println("MCPELauncher: " + packageName);
			if (!MainActivity.loadedAddons.contains(packageName)) { //is not currently loaded
				ApplicationInfo appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
				//appInfo.dump(new LogPrinter(4, "MCPELauncher"), "");
				if (appInfo.metaData == null) return;
				String nativeLibName = appInfo.metaData.getString("net.zhuoweizhang.mcpelauncher.api.nativelibname");
				if (nativeLibName == null) return;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		System.out.println("Scheduling MCPELauncher restart");
		if (MainActivity.currentMainActivity != null) {
			MainActivity.currentMainActivity.get().finish();
		}
		new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(200);
				} catch (Exception e) {}
				System.exit(0);
			}
		}).start();
	}
}

