package net.zhuoweizhang.mcpelauncher.api;

import java.util.*;

import net.zhuoweizhang.mcpelauncher.R;
import net.zhuoweizhang.mcpelauncher.Utils;
import net.zhuoweizhang.mcpelauncher.texture.*;
import android.content.*;
import android.os.*;
import android.widget.*;

public class ImportTexturepackActivity extends ImportActivity {

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		installConfirmText.setText(R.string.texturepack_import_confirm);
	}

	@Override
	protected void startImport() {
		try {
			List<TexturePackDescription> list = TexturePackLoader.loadDescriptionsWithIcons(this);
			TexturePackDescription desc = new TexturePackDescription(TexturePackLoader.TYPE_ZIP, mFile.getAbsolutePath());
			int minecraftVersionCode = getPackageManager().getPackageInfo("com.mojang.minecraftpe", 0).versionCode;
			SharedPreferences myprefs = Utils.getPrefs(1);
			int lastVersionCode = myprefs.getInt("last_version", -1);
			boolean needReplaceAll = lastVersionCode != minecraftVersionCode;

			if (needReplaceAll) {
				myprefs.edit().putInt("last_version", minecraftVersionCode).apply();
			}

			boolean replacePack = getIntent().getAction().equals("net.zhuoweizhang.mcpelauncher.action.REPLACE_TEXTUREPACK")
				|| needReplaceAll;
			if (!replacePack) {
				boolean already = false;
				for (TexturePackDescription d: list) {
					if (d.path.equals(desc.path)) {
						already = true;
						break;
					}
				}
				if (!already) list.add(0, desc);
			} else {
				list.clear();
				list.add(desc);
			}
			TexturePackLoader.saveDescriptions(this, list);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Utils.getPrefs(0).edit().putBoolean("zz_texture_pack_enable", true).apply();
		Toast.makeText(this, R.string.texturepack_imported, Toast.LENGTH_LONG).show();
		finish();
	}
}
