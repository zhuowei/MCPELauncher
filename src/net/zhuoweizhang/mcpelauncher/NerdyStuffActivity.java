package net.zhuoweizhang.mcpelauncher;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public class NerdyStuffActivity extends Activity implements View.OnClickListener {

	private Button dumpLibMinecraftPeButton;
	private Button restartAppButton;
	private Button setSkinButton;

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.nerdy_stuff);
		dumpLibMinecraftPeButton = (Button) findViewById(R.id.dump_libminecraftpe_button);
		dumpLibMinecraftPeButton.setOnClickListener(this);
		restartAppButton = (Button) findViewById(R.id.restart_app_button);
		restartAppButton.setOnClickListener(this);
		setSkinButton = (Button) findViewById(R.id.set_skin_button);
		setSkinButton.setOnClickListener(this);
	}

	public void onClick(View v) {
		if (v == dumpLibMinecraftPeButton) {
			dumpLib();
		} else if (v == restartAppButton) {
			forceRestart();
		} else if (v == setSkinButton) {
			setSkin();
		}
	}

	public void dumpLib() {
		try {
			FileOutputStream os = new FileOutputStream("/sdcard/libminecraftpe.so.dump");
			FileChannel channel = os.getChannel();
			com.mojang.minecraftpe.MainActivity.minecraftLibBuffer.position(0);
			channel.write(com.mojang.minecraftpe.MainActivity.minecraftLibBuffer);
			channel.close();
			Toast.makeText(this, "/sdcard/libminecraftpe.so.dump", Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** Forces the app to be restarted by calling System.exit(0), 
	 * Android automatically re-launches the activity behind this one when the vm exits, so to go back to the main activity, 
	 * use the AlarmService to launch the home screen intent 1 second later. 
	 */
	public void forceRestart() {

		AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		long timeMillis = SystemClock.elapsedRealtime() + 1000; //5 seconds
		Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		alarmMgr.set(AlarmManager.ELAPSED_REALTIME, timeMillis, PendingIntent.getActivity(this, 0, intent, 0));
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
		Uri derp = Uri.fromFile(new File("/sdcard/skin.png"));
		intent.setDataAndType(derp, "image/png");
		try {
			startActivity(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
