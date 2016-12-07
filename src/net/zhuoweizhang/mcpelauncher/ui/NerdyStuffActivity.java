package net.zhuoweizhang.mcpelauncher.ui;

import java.io.*;
import java.nio.channels.*;
import java.util.*;

import net.zhuoweizhang.mcpelauncher.BuildConfig;
import net.zhuoweizhang.mcpelauncher.PatchManager;
import net.zhuoweizhang.mcpelauncher.R;
import net.zhuoweizhang.mcpelauncher.ScriptManager;
import net.zhuoweizhang.mcpelauncher.Utils;
import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;

public class NerdyStuffActivity extends Activity implements View.OnClickListener {

	private Button dumpLibMinecraftPeButton;
	private Button restartAppButton;
	private Button setSkinButton;
	private Button chefSpecialButton;
	private Button dumpModPEMethodsButton;
	}
	
	public void onClick(View v) {
		if (v == dumpLibMinecraftPeButton) {
			dumpLib();
		} else if (v == restartAppButton) {
			forceRestart(this);
		} else if (v == setSkinButton) {
			setSkin();
		} else if (v == chefSpecialButton && BuildConfig.DEBUG) {
			scriptImport();
		} else if (v == dumpModPEMethodsButton) {
			dumpModPEMethods();
		}
	}

	public void dumpLib() {
		try {
			FileOutputStream os = new FileOutputStream(Environment.getExternalStorageDirectory()
					.getAbsolutePath() + "/libminecraftpe.so.dump");
			FileChannel channel = os.getChannel();
			com.mojang.minecraftpe.MainActivity.minecraftLibBuffer.position(0);
			channel.write(com.mojang.minecraftpe.MainActivity.minecraftLibBuffer);
			channel.close();
			os.close();
			Toast.makeText(
					this,
					Environment.getExternalStorageDirectory().getAbsolutePath()
							+ "libminecraftpe.so.dump", Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Forces the app to be restarted by calling System.exit(0), Android
	 * automatically re-launches the activity behind this one when the vm exits,
	 * so to go back to the main activity, use the AlarmService to launch the
	 * home screen intent 1 second later.
	 */
	public static void forceRestart(Activity activity) {

		AlarmManager alarmMgr = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
		long timeMillis = SystemClock.elapsedRealtime() + 1000; // .5 seconds
		Intent intent = activity.getPackageManager().getLaunchIntentForPackage(
				activity.getPackageName());
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		alarmMgr.set(AlarmManager.ELAPSED_REALTIME, timeMillis,
				PendingIntent.getActivity(activity, 0, intent, 0));
		new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(200);
					System.exit(0);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	public void setSkin() {
		Intent intent = new Intent("net.zhuoweizhang.mcpelauncher.action.SET_SKIN");
		Uri derp = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "/skin.png"));
		intent.setDataAndType(derp, "image/png");
		try {
			startActivity(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void scriptImport() {
		Intent intent = new Intent("net.zhuoweizhang.mcpelauncher.action.IMPORT_SCRIPT");
		Uri derp = Uri.fromFile(new File(Environment.getExternalStorageDirectory(),
				"/winprogress/500ise_everymethod.js"));
		intent.setDataAndType(derp, "text/plain");
		try {
			startActivity(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("deprecation")
	public void dumpModPEMethods() {
		String allMethods = ScriptManager.getAllApiMethodsDescriptions();
		android.text.ClipboardManager cmgr = (android.text.ClipboardManager) this
				.getSystemService(Context.CLIPBOARD_SERVICE);
		cmgr.setText(allMethods);
		try {
			FileWriter w = new FileWriter(new File(Environment.getExternalStorageDirectory(),
					"/modpescript_dump.txt").getAbsolutePath());
			w.write(allMethods);
			w.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		new AlertDialog.Builder(this).setTitle("modpescript_dump.txt").setMessage(allMethods)
				.setPositiveButton(android.R.string.ok, null).show();
	}
}
