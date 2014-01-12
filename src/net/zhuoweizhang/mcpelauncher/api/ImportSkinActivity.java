package net.zhuoweizhang.mcpelauncher.api;

import net.zhuoweizhang.mcpelauncher.MainMenuOptionsActivity;
import net.zhuoweizhang.mcpelauncher.R;
import android.os.*;
import android.preference.*;
import android.widget.*;

public class ImportSkinActivity extends ImportActivity {

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		installConfirmText.setText(R.string.skin_import_confirm);
	}

	@Override
	protected void startImport() {
		getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0).edit()
				.putString("player_skin", mFile.getAbsolutePath()).apply();
		PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("zz_skin_enable", true).apply();
		Toast.makeText(this, R.string.skin_imported, Toast.LENGTH_LONG).show();
		finish();
	}
}
