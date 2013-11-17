package net.zhuoweizhang.mcpelauncher;

import java.io.*;
import java.util.*;

import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.preference.*;
import android.view.*;
import android.widget.*;

import com.ipaulpro.afilechooser.utils.FileUtils;

public class ImportSkinActivity extends Activity {

	public void onCreate(Bundle icicle) {
		Utils.setLanguageOverride(this);
		super.onCreate(icicle);
		
		Intent intent = getIntent();
		if (intent == null) {
			finish();
			return;
		}

		File skinFile = FileUtils.getFile(getIntent().getData());
		if (skinFile.exists()) {
			getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0).edit().putString("player_skin", skinFile.getAbsolutePath()).apply();
			PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("zz_skin_enable", true).apply();
		}
		Toast.makeText(this, R.string.skin_imported, Toast.LENGTH_LONG).show();
		finish();
	}
}
