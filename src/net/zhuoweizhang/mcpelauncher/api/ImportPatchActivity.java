package net.zhuoweizhang.mcpelauncher.api;

import static com.mojang.minecraftpe.MainActivity.PT_PATCHES_DIR;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import net.zhuoweizhang.mcpelauncher.PatchManager;
import net.zhuoweizhang.mcpelauncher.R;
import net.zhuoweizhang.mcpelauncher.Utils;
import net.zhuoweizhang.mcpelauncher.patch.PatchUtils;
import net.zhuoweizhang.mcpelauncher.ui.MainMenuOptionsActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.mojang.minecraftpe.MainActivity;

public class ImportPatchActivity extends ImportActivity {

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		installConfirmText.setText(R.string.manage_patches_import_confirm);
	}

	@Override
	protected void startImport() {
		File to = new File(getDir(PT_PATCHES_DIR, 0), mFile.getName());
		try {
			PatchUtils.copy(mFile, to);
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.manage_patches_import_error, Toast.LENGTH_LONG).show();
			return;
		}
		setResult(RESULT_OK);
		boolean hasTooManyPatches = hasTooManyPatches();
		PatchManager.getPatchManager(this).setEnabled(to, false);
		if (hasTooManyPatches) {
			Toast.makeText(this, R.string.manage_patches_too_many, Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		PatchManager.getPatchManager(this).setEnabled(to, true);
		Utils.getPrefs(1).edit().putBoolean("force_prepatch", true).apply();
		if (MainActivity.libLoaded) {
			new Thread(new Runnable() {
				public void run() {
					try {
						Thread.sleep(100);
					} catch (Exception e) {
					}
					System.exit(0);
				}
			}).start();
		}
		Toast.makeText(this, R.string.manage_patches_import_done, Toast.LENGTH_SHORT).show();
		finish();
	}

	public boolean hasTooManyPatches() {
		int maxPatchCount = this.getResources().getInteger(R.integer.max_num_patches);
		Set<String> enabledPatches = PatchManager.getPatchManager(this).getEnabledPatches();
		return maxPatchCount >= 0 && enabledPatches.size() >= maxPatchCount;
	}

}
