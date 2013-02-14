package net.zhuoweizhang.mcpelauncher;

import java.io.File;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NativeActivity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Toast;

import android.preference.*;

import com.ipaulpro.afilechooser.FileChooserActivity;
import com.ipaulpro.afilechooser.utils.FileUtils;


public class MainMenuOptionsActivity extends PreferenceActivity implements Preference.OnPreferenceClickListener {

	public static final String PREFERENCES_NAME = "mcpelauncherprefs";
	public static final int REQUEST_SELECT_TEXTURE_PACK = 5;
	public static final int REQUEST_MANAGE_PATCHES = 6;

	private Preference texturePackPreference;
	private Preference managePatchesPreference;
	private Preference safeModePreference;
	private boolean needsRestart = false;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(net.zhuoweizhang.mcpelauncher.R.xml.preferences);
		texturePackPreference = findPreference("zz_texture_pack");
		texturePackPreference.setOnPreferenceClickListener(this);
		managePatchesPreference = findPreference("zz_manage_patches");
		managePatchesPreference.setOnPreferenceClickListener(this);
		safeModePreference = findPreference("zz_safe_mode");
		safeModePreference.setOnPreferenceClickListener(this);
	}

	@Override
	public void onBackPressed() {
		if (needsRestart) {
			forceRestart();
		} else {
			super.onBackPressed();
		}
	}

	public boolean onPreferenceClick(Preference pref) {
		if (pref == texturePackPreference) {
			chooseTexturePack();
			return true;
		} else if (pref == managePatchesPreference) {
			managePatches();
			return true;
		} else if (pref == safeModePreference) {
			needsRestart = true;
			return false; //Don't eat it
		}
		return false;
	}

	protected void chooseTexturePack() {
		Intent target = FileUtils.createGetContentIntent();
		target.setType("application/zip");
		//Intent intent = Intent.createChooser(target, "Select texture pack");
		target.setClass(this, FileChooserActivity.class);
		startActivityForResult(target, REQUEST_SELECT_TEXTURE_PACK);
	}

	protected void managePatches() {
		Intent intent = new Intent(this, ManagePatchesActivity.class);
		startActivityForResult(intent, REQUEST_MANAGE_PATCHES);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_SELECT_TEXTURE_PACK:  
				if (resultCode == RESULT_OK) {  
					final Uri uri = data.getData();
					File file = FileUtils.getFile(uri);
					SharedPreferences prefs = getSharedPreferences(PREFERENCES_NAME, 0);
					SharedPreferences.Editor editor = prefs.edit();
					try {
						editor.putString("texturePack", file.getCanonicalPath());
						editor.commit();
					} catch (Exception e) {
						e.printStackTrace();
					}
					Toast.makeText(this, "Texture pack set! Restarting Minecraft to load texture pack...", Toast.LENGTH_LONG).show();
					finish();
					restartFirstActivity();
					//System.exit(0);

				}
				break;
			case REQUEST_MANAGE_PATCHES:
				if (resultCode == RESULT_OK) {
					forceRestart();
				}
				break;
		}
	}
	/* thanks, http://stackoverflow.com/questions/1397361/how-do-i-restart-an-android-activity */
	private void restartFirstActivity() {
		Intent i = getPackageManager().getLaunchIntentForPackage(getPackageName());
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(i);
	}

	/** Forces the app to be restarted by calling System.exit(0), 
	 * Android automatically re-launches the activity behind this one when the vm exits, so to go back to the main activity, 
	 * use the AlarmService to launch the home screen intent 1 second later. 
	 */
	private void forceRestart() {

/*		AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		long timeMillis = SystemClock.elapsedRealtime() + 1000; //1 second
		Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		alarmMgr.set(AlarmManager.ELAPSED_REALTIME, timeMillis, PendingIntent.getActivity(this, 0, intent, 0));*/
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

}
