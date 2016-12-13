package net.zhuoweizhang.mcpelauncher.ui;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.mojang.minecraftpe.MainActivity;

import net.zhuoweizhang.mcpelauncher.KamcordConstants;
import net.zhuoweizhang.mcpelauncher.R;
import net.zhuoweizhang.mcpelauncher.ScriptManager;
import net.zhuoweizhang.mcpelauncher.Utils;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.*;
import de.ankri.views.Switch;

public class MainMenuOptionsActivity extends PreferenceActivity implements
		Preference.OnPreferenceClickListener, SwitchPreference.OnCheckedChangeListener {

	public static final String PREFERENCES_NAME = "mcpelauncherprefs";
	public static final int REQUEST_MANAGE_TEXTURES = 5;
	public static final int REQUEST_MANAGE_PATCHES = 6;
	public static final int REQUEST_MANAGE_SKINS = 7;
	public static final int REQUEST_MANAGE_ADDONS = 8;
	public static final int REQUEST_SERVER_LIST = 9;
	public static final int REQUEST_MANAGE_SCRIPTS = 10;

	public static final String PRO_APP_ID = "net.zhuoweizhang.mcpelauncher.pro";

	public static final String GOOGLE_PLAY_URL = "market://details?id=";

	private boolean needsRestart = false;
	public static boolean isManagingAddons = false;

	private Set<Switch> hasInflatedSwitches = new HashSet<Switch>();

	private SwitchPreference patchesPreference;
	private SwitchPreference texturepackPreference;
	private SwitchPreference addonsPreference;
	private SwitchPreference scriptsPreference;
	private SwitchPreference skinPreference;
	private SwitchPreference safeModePreference;
	private ListPreference languagePreference;
	private Preference goToForumsPreference;
	private Preference getProPreference;
	private Preference aboutPreference;
	private Preference paranoidPreference;
	private Preference legacyLivePatchPreference;
	private SwitchPreference useControllerPreference;
	private SwitchPreference enableKamcordPreference;
	private SwitchPreference themeDarkPreference;

	private Preference recorderWatchPreference;
	private Preference recorderReshareLastPreference;
	private SwitchPreference reimportScriptsPreference;
	private SwitchPreference desktopGuiPreference;

	protected Thread ui = new Thread(new Runnable() {
		protected WeakReference<MainMenuOptionsActivity> activity = null;

		@Override
		public void run() {
			activity = new WeakReference<MainMenuOptionsActivity>(MainMenuOptionsActivity.this);
			while (activity.get() != null) {
				updateStates();
				updateTP();
				updateSkin();
				updatePatches();
				updateScripts();
				synchronized (ui) {
					try {
						ui.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			System.gc();
		}

		protected void updateTP() {
			/*
			MainMenuOptionsActivity a = activity.get();
			final SwitchPreference p = a.texturepackPreference;
			String sum = null;
			if ((p.content != null) && (p.content.isChecked())) {
				SharedPreferences prefs = Utils.getPrefs(1);
				if (prefs.getBoolean("zz_texture_pack_demo", false)) {
					sum = a.getString(R.string.textures_demo);
				} else {
					prefs = Utils.getPrefs(1);
					sum = prefs.getString("texturePack", null);
				}
			}
			final String sm = sum;
			a.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					p.setSummary(sm);
				}
			});
			a = null;
			*/
		}

		protected void updateSkin() {
			/*
			MainMenuOptionsActivity a = activity.get();
			final SwitchPreference p = a.skinPreference;
			String sum = null;
			if ((p.content != null) && (p.content.isChecked())) {
				SharedPreferences prefs = Utils.getPrefs(1);
				sum = prefs.getString("player_skin", null);
			}
			final String sm = sum;
			a.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					p.setSummary(sm);
				}
			});
			a = null;
			*/
		}

		protected void updatePatches() {
			MainMenuOptionsActivity a = activity.get();
			final SwitchPreference p = a.patchesPreference;
			String sum = null;
			if ((p.content != null) && (p.content.isChecked())) {
				int count = getDir(MainActivity.PT_PATCHES_DIR, 0).listFiles().length;
				if (!Utils.isPro() && (Utils.getMaxPatches() != -1)) {
					count = Math.min(Utils.getMaxPatches(), count);
				}
				String descr = getString(R.string.plurals_patches_more);
				if (count == 1)
					descr = getString(R.string.plurals_patches_one);
				if (count == 0) {
					sum = getString(R.string.plurals_patches_no);
				} else {
					sum = Utils.getEnabledPatches().size() + "/" + count + " " + descr;
				}
			}
			final String sm = sum;
			a.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					p.setSummary(sm);
				}
			});
			a = null;
		}

		protected void updateScripts() {
			MainMenuOptionsActivity a = activity.get();
			final SwitchPreference p = a.scriptsPreference;
			String sum = null;
			if ((p.content != null) && (p.content.isChecked())) {
				int count = getDir(ScriptManager.SCRIPTS_DIR, 0).listFiles().length;
				if (!Utils.isPro() && (Utils.getMaxScripts() != -1)) {
					count = Math.min(Utils.getMaxScripts(), count);
				}
				String descr = getString(R.string.plurals_scripts_more);
				if (count == 1)
					descr = getString(R.string.plurals_scripts_one);
				if (count == 0) {
					sum = getString(R.string.plurals_scripts_no);
				} else {
					sum = Utils.getEnabledScripts().size() + "/" + count + " " + descr;
				}
			}
			final String sm = sum;
			a.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					p.setSummary(sm);
				}
			});
			a = null;
		}
		
		protected void updateStates() {
			activity.get().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					togglePrefs();
				}
			});
		}
	});

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Utils.setLanguageOverride();
		super.onCreate(savedInstanceState);
		
		setUp();

		ui.start();
	}

	@Override
	public void onBackPressed() {
		if (needsRestart) {
			forceRestart();
		} else {
			super.onBackPressed();
		}
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onRestart() {
		super.onRestart();
		setPreferenceScreen(null);
		setUp();
		if (!ui.isAlive()) ui.start();
	}

	@Override
	protected void onStart() {
		super.onStart();
		getWindow().getDecorView().post(new Runnable() {
			public void run() {
				synchronized (ui) {
					ui.notifyAll();
				}
			}
		});
	}
	
	@SuppressWarnings("deprecation")
	protected void setUp() {
		addPreferencesFromResource(R.xml.preferences);

		texturepackPreference = (SwitchPreference) findPreference("zz_texture_pack_enable");
		if (texturepackPreference != null) {
			texturepackPreference.setListener(this);
			texturepackPreference.setOnPreferenceClickListener(this);
		}

		patchesPreference = (SwitchPreference) findPreference("zz_manage_patches");
		if (patchesPreference != null) {
			patchesPreference.setListener(this);
			patchesPreference.setOnPreferenceClickListener(this);
		}

		safeModePreference = (SwitchPreference) findPreference("zz_safe_mode");
		if (safeModePreference != null) {
			safeModePreference.setListener(this);
			safeModePreference.setOnPreferenceClickListener(this);
		}

		addonsPreference = (SwitchPreference) findPreference("zz_load_native_addons");
		if (addonsPreference != null) {
			addonsPreference.setOnPreferenceClickListener(this);
		}

		skinPreference = (SwitchPreference) findPreference("zz_skin_enable");
		if (skinPreference != null) {
			skinPreference.setListener(this);
			skinPreference.setOnPreferenceClickListener(this);
		}

		scriptsPreference = (SwitchPreference) findPreference("zz_script_enable");
		if (scriptsPreference != null) {
			scriptsPreference.setListener(this);
			scriptsPreference.setOnPreferenceClickListener(this);
			if (ScriptManager.isRemote)
				scriptsPreference.setEnabled(false);
			/*
			try {
				if (!getPackageManager().getPackageInfo("com.mojang.minecraftpe", 0)
					.versionName.startsWith(MainActivity.SCRIPT_SUPPORT_VERSION)) scriptsPreference.setEnabled(false);
			} catch (Exception e) {
				e.printStackTrace();
			}
			*/
		}

		languagePreference = (ListPreference) findPreference("zz_language_override");
		if (languagePreference != null) {
			initLanguagePreference();
			languagePreference.setOnPreferenceClickListener(this);
		}

		paranoidPreference = findPreference("zz_script_paranoid_mode");
		if (paranoidPreference != null) {
			paranoidPreference.setOnPreferenceClickListener(this);
		}

		legacyLivePatchPreference = findPreference("zz_legacy_live_patch");
		if (legacyLivePatchPreference != null) {
			legacyLivePatchPreference.setOnPreferenceClickListener(this);
		}

		aboutPreference = findPreference("zz_about");
		if (aboutPreference != null) {
			aboutPreference.setOnPreferenceClickListener(this);
		}

		getProPreference = findPreference("zz_get_pro");
		if (getProPreference != null) {
			getProPreference.setOnPreferenceClickListener(this);
		}

		goToForumsPreference = findPreference("zz_go_to_forums");
		if (goToForumsPreference != null) {
			goToForumsPreference.setOnPreferenceClickListener(this);
		}

		Preference immersiveModePreference = findPreference("zz_immersive_mode");
		if (immersiveModePreference != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
			getPreferenceScreen().removePreference(immersiveModePreference);
			immersiveModePreference.setEnabled(false);
		}

		boolean hasRecorder = hasRecorder();// && Kamcord.isEnabled();
		System.out.println("Has recorder: " + hasRecorder);

		recorderWatchPreference = findPreference("zz_watch_recording");
		if (recorderWatchPreference != null) {
			if (!hasRecorder) {
				recorderWatchPreference.setEnabled(false);
			} else {
				recorderWatchPreference.setOnPreferenceClickListener(this);
			}
		}

		recorderReshareLastPreference = findPreference("zz_reshare_last_recording");
		if (recorderReshareLastPreference != null) {
			if (!hasRecorder) {
				recorderReshareLastPreference.setEnabled(false);
			} else {
				recorderReshareLastPreference.setOnPreferenceClickListener(this);
			}
		}
		useControllerPreference = (SwitchPreference) findPreference("zz_use_controller");
		if (useControllerPreference != null) {
			if (Build.VERSION.SDK_INT < 12) {
				getPreferenceScreen().removePreference(useControllerPreference);
			} else {
				useControllerPreference.setListener(this);
			}
		}
		enableKamcordPreference = (SwitchPreference) findPreference("zz_enable_kamcord");
		if (enableKamcordPreference != null) {
			if (Build.VERSION.SDK_INT < 16 || Build.VERSION.SDK_INT >= 23) { // Android 4.1, 6.0
				getPreferenceScreen().removePreference(enableKamcordPreference);
			} else {
				enableKamcordPreference.setListener(this);
			}
		}
		themeDarkPreference = (SwitchPreference) findPreference("zz_theme_dark");
		if (themeDarkPreference != null) {
			themeDarkPreference.setListener(this);
		}
		reimportScriptsPreference = (SwitchPreference) findPreference("zz_reimport_scripts");
		if (reimportScriptsPreference != null) {
			reimportScriptsPreference.setListener(this);
		}
		desktopGuiPreference = (SwitchPreference) findPreference("zz_desktop_gui");
		if (desktopGuiPreference != null) {
			desktopGuiPreference.setListener(this);
		}
	}

	public boolean onPreferenceClick(Preference pref) {
		synchronized (ui) {
			ui.notifyAll();
		}
		if (pref == patchesPreference) {
			managePatches();
			return true;
		} else if (pref == texturepackPreference) {
			manageTexturepacks();
			return true;
		} else if (pref == getProPreference) {
			startGetPro();
			return true;
		} else if (pref == aboutPreference) {
			startAbout();
			return true;
		} else if (pref == addonsPreference) {
			manageAddons();
			return true;
		} else if (pref == scriptsPreference) {
			manageScripts();
			return true;
		} else if (pref == skinPreference) {
			manageSkins();
			return true;
		} else if (pref == goToForumsPreference) {
			goToForums();
			return true;
		} else if (pref == languagePreference || pref == paranoidPreference || pref == legacyLivePatchPreference) {
			needsRestart = true;
			return false;
		} else if (pref == recorderWatchPreference) {
			try {
				Class.forName("com.kamcord.android.Kamcord").getDeclaredMethod("showWatchView", (Class[]) null).
					invoke(null);
			} catch (Exception e) {
				e.printStackTrace();
			}
			finish();
			return false;
		} else if (pref == recorderReshareLastPreference) {
			reshareLast();
			return false;
		}
		return false;
	}

	@Override
	public void onCheckedChange(Switch data) {
		needsRestart = true;
		if (useControllerPreference != null && data == useControllerPreference.content) {
			controllerChange(data);
			return;
		} else if (reimportScriptsPreference != null && data == reimportScriptsPreference.content) {
			if (data.isChecked()) {
				new AlertDialog.Builder(this).setMessage(R.string.manage_scripts_reimport_enable_dialog).
					setPositiveButton(android.R.string.ok, null).show();
			}
			return;
		}
		synchronized (ui) {
			ui.notifyAll();
		}
		if (data == texturepackPreference.content) {
			File f = ManageTexturepacksActivity.REQUEST_ENABLE;
			if (!data.isChecked())
				f = ManageTexturepacksActivity.REQUEST_DISABLE;
			ManageTexturepacksActivity.setTexturepack(f, null);
		}
		/*
		} else if (data == skinPreference.content) {
			File f = ManageSkinsActivity.REQUEST_ENABLE;
			if (!data.isChecked())
				f = ManageSkinsActivity.REQUEST_DISABLE;
			ManageSkinsActivity.setSkin(f, null);
		}
		*/
	}

	private void controllerChange(Switch sw) {
		if (!sw.isChecked()) return;
		if (Utils.hasExtrasPackage(this)) return;
		sw.setChecked(false);
		Utils.getPrefs(0).edit().putBoolean("zz_use_controller", false).apply();
		new AlertDialog.Builder(this).setMessage(R.string.purchase_extras_package)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialogI, int button) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse("https://gist.github.com/zhuowei/4538923d1963524b71fc#file-getextras-md"));
					startActivity(intent);
				}
			})
			.setNegativeButton(android.R.string.cancel, null)
			.show();
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

	protected void manageScripts() {
		Intent intent = new Intent(this, ManageScriptsActivity.class);
		startActivityForResult(intent, REQUEST_MANAGE_SCRIPTS);
	}

	protected void manageTexturepacks() {
		//Intent intent = new Intent(this, ManageTexturepacksActivity.class);
		Intent intent = new Intent(this, TexturePacksActivity.class);
		startActivityForResult(intent, REQUEST_MANAGE_TEXTURES);
	}

	protected void manageSkins() {
		Intent intent = new Intent(this, ManageSkinsActivity.class);
		startActivityForResult(intent, REQUEST_MANAGE_SKINS);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		synchronized (ui) {
			ui.notifyAll();
		}
		switch (requestCode) {
		case REQUEST_MANAGE_PATCHES:
			if (resultCode == RESULT_OK) {
				needsRestart = true;
			}
			break;
		case REQUEST_MANAGE_ADDONS:
			isManagingAddons = false;
			if (resultCode == RESULT_OK) {
				needsRestart = true;
			}
			break;
		case REQUEST_SERVER_LIST:
			if (resultCode == RESULT_OK) {
				onBackPressed();
			}
			break;
		case REQUEST_MANAGE_SCRIPTS:
		case REQUEST_MANAGE_TEXTURES:
		case REQUEST_MANAGE_SKINS:
			if (resultCode == RESULT_OK) {
				needsRestart = true;
			}
			break;
		}
	}

	private void startAbout() {
		startActivity(new Intent(this, AboutAppActivity.class));
	}

	private void startGetPro() {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		// intent.setData(Uri.parse(GOOGLE_PLAY_URL + PRO_APP_ID));
		intent.setData(Uri.parse(AboutAppActivity.FORUMS_PAGE_URL));
		try {
			this.startActivity(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void goToForums() {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(AboutAppActivity.FORUMS_PAGE_URL));
		try {
			this.startActivity(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void reshareLast() {
		// find the last Kamcord video, if any, and call share on it
		File file = findLastKamcordVideo();
		if (file == null) {
			new AlertDialog.Builder(this).setMessage(R.string.recorder_no_recording).
				setPositiveButton(android.R.string.ok, null).
				show();
			return;
		}
		Uri theUri = Uri.fromFile(file);
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("video/mp4");
		intent.putExtra(Intent.EXTRA_STREAM, theUri);
		Intent chooser = Intent.createChooser(intent, "Share video");
		startActivity(chooser);
	}

	private File findLastKamcordVideo() {
		File kamcordDir = new File(Environment.getExternalStorageDirectory(), "Kamcord");
		File myDir = new File(kamcordDir, KamcordConstants.DEV_KEY + "-" + getPackageName());
		if (!myDir.exists()) return null;
		for (File theDir: myDir.listFiles()) {
			if (!theDir.isDirectory()) continue;
			if (new File(theDir, "thumbnail.jpg").exists() && new File(theDir, "video.mp4").exists()) {
				return new File(theDir, "video.mp4");
			}
		}
		return null;
	}

	/**
	 * Forces the app to be restarted by calling System.exit(0), Android
	 * automatically re-launches the activity behind this one when the vm exits,
	 * so to go back to the main activity, use the AlarmService to launch the
	 * home screen intent 1 second later.
	 */
	private void forceRestart() {
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

	private void initLanguagePreference() {
		String[] langList = getResources().getString(R.string.languages_supported).split(",");
		List<String> languageNames = new ArrayList<String>();
		languageNames.add(getResources().getString(R.string.pref_zz_language_override_default));
		Locale currentLocale = getResources().getConfiguration().locale;
		for (String override : langList) {
			if (override.length() == 0)
				continue;
			String[] overrideSplit = override.split("_");
			String langName = overrideSplit[0];
			String countryName = overrideSplit.length > 1 ? overrideSplit[1] : "";
			Locale locale = new Locale(langName, countryName);
			languageNames.add(locale.getDisplayName(currentLocale));
		}
		languagePreference.setEntries(languageNames.toArray(new String[] {}));
		languagePreference.setEntryValues(langList);
	}

	protected void togglePrefs() {
		/*boolean safeMode = Utils.isSafeMode();
		if (safeMode == skinPreference.isEnabled()) {
			patchesPreference.setEnabled(!safeMode);
			skinPreference.setEnabled(!safeMode);
			texturepackPreference.setEnabled(!safeMode);
			addonsPreference.setEnabled(!safeMode);
			scriptsPreference.setEnabled(!safeMode);
			needsRestart = true;
		}*/
	}

	protected boolean hasRecorder() {
/*
		try {
			getPackageManager().getPackageInfo("net.zhuoweizhang.mcpelauncher.recorder", 0);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}
*/
		return this.getPackageName().equals("net.zhuoweizhang.mcpelauncher.pro");
	}

}
