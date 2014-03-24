package net.zhuoweizhang.mcpelauncher.api;

import net.zhuoweizhang.mcpelauncher.R;
import net.zhuoweizhang.mcpelauncher.Utils;
import android.os.*;
import android.widget.*;

public class ImportSkinActivity extends ImportActivity {

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		installConfirmText.setText(R.string.skin_import_confirm);
	}

	@Override
	protected void startImport() {
		Utils.getPrefs(1).edit().putString("player_skin", mFile.getAbsolutePath()).apply();
		Utils.getPrefs(0).edit().putBoolean("zz_skin_enable", true).apply();
		Toast.makeText(this, R.string.skin_imported, Toast.LENGTH_LONG).show();
		finish();
	}
}
