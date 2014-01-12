package net.zhuoweizhang.mcpelauncher.api;

import java.io.*;
import java.util.*;

import android.os.Bundle;
import android.widget.*;

import com.mojang.minecraftpe.MainActivity;

import net.zhuoweizhang.mcpelauncher.MainMenuOptionsActivity;
import net.zhuoweizhang.mcpelauncher.PatchManager;
import net.zhuoweizhang.mcpelauncher.R;
import net.zhuoweizhang.mcpelauncher.patch.*;
import static net.zhuoweizhang.mcpelauncher.LauncherActivity.PT_PATCHES_DIR;

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
			PatchManager.getPatchManager(this).setEnabled(to, false);
			if (!hasTooManyPatches()) {
				PatchManager.getPatchManager(this).setEnabled(to, true);
			} else {
				Toast.makeText(this, R.string.manage_patches_too_many, Toast.LENGTH_LONG).show();
			}
			setPatchListModified();
			Toast.makeText(this, R.string.manage_patches_import_done, Toast.LENGTH_SHORT).show();
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
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.manage_patches_import_error, Toast.LENGTH_LONG).show();
		}
		finish();
	}

	public boolean hasTooManyPatches() {
		int maxPatchCount = this.getResources().getInteger(R.integer.max_num_patches);
		Set<String> enabledPatches = PatchManager.getPatchManager(this).getEnabledPatches();
		return maxPatchCount >= 0 && enabledPatches.size() >= maxPatchCount;
	}

	protected void setPatchListModified() {
		setResult(RESULT_OK);
		getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0).edit().putBoolean("force_prepatch", true)
				.apply();
	}

}
