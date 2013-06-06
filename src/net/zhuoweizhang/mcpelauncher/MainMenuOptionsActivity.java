package net.zhuoweizhang.mcpelauncher;

import java.io.File;

import java.util.List;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NativeActivity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Build;
import android.os.SystemClock;
import android.widget.Toast;

import android.preference.*;

import com.ipaulpro.afilechooser.FileChooserActivity;
import com.ipaulpro.afilechooser.utils.FileUtils;

import eu.chainfire.libsuperuser.Shell;


public class MainMenuOptionsActivity extends PreferenceActivity implements Preference.OnPreferenceClickListener {

	public static final String PREFERENCES_NAME = "mcpelauncherprefs";
	public static final int REQUEST_SELECT_TEXTURE_PACK = 5;
	public static final int REQUEST_MANAGE_PATCHES = 6;
	public static final int REQUEST_SELECT_SKIN = 7;
	public static final int REQUEST_MANAGE_ADDONS = 8;

	public static final String PRO_APP_ID = "net.zhuoweizhang.mcpelauncher.pro";

	public static final String GOOGLE_PLAY_URL = "market://details?id=";

	public static boolean isManagingAddons = false;

	private Preference texturePackPreference;
	private Preference texturePackEnablePreference;
	private Preference texturePackDemoPreference;
	private Preference managePatchesPreference;
	private Preference safeModePreference;
	private Preference aboutPreference;
	private Preference getProPreference;
	private Preference loadNativeAddonsPreference;
	private Preference extractOriginalTexturesPreference;
	private Preference skinPreference;
	private Preference manageAddonsPreference;
	private Preference goToForumsPreference;
	private boolean needsRestart = false;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(net.zhuoweizhang.mcpelauncher.R.xml.preferences);
		texturePackPreference = findPreference("zz_texture_pack");
		if (texturePackPreference != null) texturePackPreference.setOnPreferenceClickListener(this);
		texturePackEnablePreference = findPreference("zz_texture_pack_enable");
		if (texturePackEnablePreference != null) texturePackEnablePreference.setOnPreferenceClickListener(this);
		texturePackDemoPreference = findPreference("zz_texture_pack_demo");
		if (texturePackDemoPreference != null) texturePackDemoPreference.setOnPreferenceClickListener(this);
		managePatchesPreference = findPreference("zz_manage_patches");
		managePatchesPreference.setOnPreferenceClickListener(this);
		safeModePreference = findPreference("zz_safe_mode");
		safeModePreference.setOnPreferenceClickListener(this);
		aboutPreference = findPreference("zz_about");
		aboutPreference.setOnPreferenceClickListener(this);
		getProPreference = findPreference("zz_get_pro");
		if (getProPreference != null) getProPreference.setOnPreferenceClickListener(this);
		goToForumsPreference = findPreference("zz_go_to_forums");
		if (goToForumsPreference != null) goToForumsPreference.setOnPreferenceClickListener(this);
		loadNativeAddonsPreference = findPreference("zz_load_native_addons");
		if (loadNativeAddonsPreference != null) loadNativeAddonsPreference.setOnPreferenceClickListener(this);
		extractOriginalTexturesPreference = findPreference("zz_extract_original_textures");
		if (extractOriginalTexturesPreference != null) {
			extractOriginalTexturesPreference.setOnPreferenceClickListener(this);
			if (Build.VERSION.SDK_INT < 16 /*Build.VERSION_CODES.JELLY_BEAN*/) { //MCPE original textures not accessible on Jelly Bean
				getPreferenceScreen().removePreference(extractOriginalTexturesPreference);
			}
		}
		skinPreference = findPreference("zz_skin");
		if (skinPreference != null) skinPreference.setOnPreferenceClickListener(this);
		manageAddonsPreference = findPreference("zz_manage_addons");
		if (manageAddonsPreference != null) manageAddonsPreference.setOnPreferenceClickListener(this);
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
		} else if (pref == texturePackDemoPreference || pref == texturePackEnablePreference) {
			//getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0).edit().putBoolean("force_prepatch", true).apply();
			needsRestart = true;
			return false;
		} else if (pref == getProPreference) {
			startGetPro();
			return true;
		} else if (pref == aboutPreference) {
			startAbout();
			return true;
		} else if (pref == loadNativeAddonsPreference) {
			needsRestart = true;
		} else if (pref == extractOriginalTexturesPreference) {
			startExtractTextures();
			return true;
		} else if (pref == skinPreference) {
			chooseSkin();
			return true;
		} else if (pref == manageAddonsPreference) {
			manageAddons();
			return true;
		} else if (pref == goToForumsPreference) {
			goToForums();
			return true;
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

	protected void chooseSkin() {
		Intent target = FileUtils.createGetContentIntent();
		target.setType("image/png");
		//Intent intent = Intent.createChooser(target, "Select texture pack");
		target.setClass(this, FileChooserActivity.class);
		startActivityForResult(target, REQUEST_SELECT_SKIN);
	}

	protected void managePatches() {
		Intent intent = new Intent(this, ManagePatchesActivity.class);
		startActivityForResult(intent, REQUEST_MANAGE_PATCHES);
	}

	protected void manageAddons() {
		isManagingAddons = true;
		Intent intent = new Intent(this, ManageAddonsActivity.class);
		startActivityForResult(intent, REQUEST_MANAGE_ADDONS);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_SELECT_TEXTURE_PACK:  
				if (resultCode == RESULT_OK) {  
					final Uri uri = data.getData();
					File file = FileUtils.getFile(uri);
					setTexturePack(file);
					//forceRestart();

				}
				break;
			case REQUEST_MANAGE_PATCHES:
				if (resultCode == RESULT_OK) {
					forceRestart();
				}
				break;
			case REQUEST_SELECT_SKIN:  
				if (resultCode == RESULT_OK) {  
					final Uri uri = data.getData();
					File file = FileUtils.getFile(uri);
					getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0).edit()
						.putString("player_skin", file.getAbsolutePath()).apply();

				}
				break;
			case REQUEST_MANAGE_ADDONS:
				isManagingAddons = false;
				if (resultCode == RESULT_OK) {
					forceRestart();
				}
				break;
		}
	}

	private void setTexturePack(File file) {
		SharedPreferences prefs = getSharedPreferences(PREFERENCES_NAME, 0);
		SharedPreferences.Editor editor = prefs.edit();
		try {
			editor.putString("texturePack", file.getCanonicalPath());
			//editor.putBoolean("force_prepatch", true);
			editor.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
		needsRestart = true;
		//Toast.makeText(this, "Texture pack set! Restarting Minecraft to load texture pack...", Toast.LENGTH_LONG).show();
		//restartFirstActivity();
	}

	private void startAbout() {
		startActivity(new Intent(this, AboutAppActivity.class));
	}

	private void startGetPro() {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		//intent.setData(Uri.parse(GOOGLE_PLAY_URL + PRO_APP_ID));
		intent.setData(Uri.parse(AboutAppActivity.FORUMS_PAGE_URL));
		try {
			this.startActivity(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void goToForums() {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		//intent.setData(Uri.parse(GOOGLE_PLAY_URL + PRO_APP_ID));
		intent.setData(Uri.parse(AboutAppActivity.FORUMS_PAGE_URL));
		try {
			this.startActivity(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void startExtractTextures() {
		new ExtractTextureTask().execute();
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

	private class ExtractTextureTask extends AsyncTask<Void, Void, Void> {

		private ProgressDialog dialog;
		private String mcpeApkLoc;
		private File outFile;
		private boolean hasSu = true;

		@Override
		protected void onPreExecute() {
			dialog = new ProgressDialog(MainMenuOptionsActivity.this);
			dialog.setMessage(getResources().getString(R.string.extracting_textures));
			dialog.setIndeterminate(true);
			dialog.setCancelable(false);
			dialog.show();
			try {
				ApplicationInfo appInfo = getPackageManager().getApplicationInfo("com.mojang.minecraftpe", 0);
				mcpeApkLoc = appInfo.sourceDir;
			} catch (PackageManager.NameNotFoundException impossible) {
			}
			outFile = new File(getExternalFilesDir(null), "minecraft.apk");
		}

		@Override
		protected Void doInBackground(Void... params) {
			List<String> suResult = Shell.SU.run("cp \"" + mcpeApkLoc + "\" \"" + outFile.getAbsolutePath() + "\"");
			if (suResult == null) {
				hasSu = false;
			}
			
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			dialog.dismiss();			
			if (outFile.exists()) {
				setTexturePack(outFile);
				Toast.makeText(MainMenuOptionsActivity.this, R.string.extract_textures_success, Toast.LENGTH_SHORT).show();
			} else {
				new AlertDialog.Builder(MainMenuOptionsActivity.this).
					setMessage(hasSu? R.string.extract_textures_error : R.string.extract_textures_no_root).
					setPositiveButton(android.R.string.ok, null).
					show();
			}
		}		
	}

}
