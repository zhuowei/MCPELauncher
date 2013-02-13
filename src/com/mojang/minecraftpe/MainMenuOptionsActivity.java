package com.mojang.minecraftpe;

import java.io.File;

import android.app.Activity;
import android.app.NativeActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import android.preference.*;

import com.ipaulpro.afilechooser.FileChooserActivity;
import com.ipaulpro.afilechooser.utils.FileUtils;


public class MainMenuOptionsActivity extends PreferenceActivity implements Preference.OnPreferenceClickListener {

	public static final String PREFERENCES_NAME = "mcpelauncherprefs";
	public static final int REQUEST_SELECT_TEXTURE_PACK = 5;

	private Preference texturePackPreference;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(net.zhuoweizhang.mcpelauncher.R.xml.preferences);
		texturePackPreference = findPreference("texture_pack");
		texturePackPreference.setOnPreferenceClickListener(this);
	}

	public boolean onPreferenceClick(Preference pref) {
		if (pref == texturePackPreference) {
			chooseTexturePack();
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
		}
		finish();
	}
	/* thanks, http://stackoverflow.com/questions/1397361/how-do-i-restart-an-android-activity */
	private void restartFirstActivity() {
		Intent i = getPackageManager().getLaunchIntentForPackage(getPackageName());
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(i);
	}


}
