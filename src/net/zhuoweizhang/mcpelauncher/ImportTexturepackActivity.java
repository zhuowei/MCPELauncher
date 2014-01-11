package net.zhuoweizhang.mcpelauncher;

import android.os.*;
import android.preference.*;
import android.widget.*;

public class ImportTexturepackActivity extends ImportActivity {

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		installConfirmText.setText(R.string.texturepack_import_confirm);
	}

	@Override
	protected void startImport() {
		getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0).edit()
				.putString("texturePack", mFile.getAbsolutePath()).apply();
		PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("zz_texture_pack_enable", true).apply();
		Toast.makeText(this, R.string.texturepack_imported, Toast.LENGTH_LONG).show();
		finish();
	}
}
