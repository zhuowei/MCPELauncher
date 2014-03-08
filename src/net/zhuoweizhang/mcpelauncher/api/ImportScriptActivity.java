package net.zhuoweizhang.mcpelauncher.api;

import java.io.*;
import java.util.*;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import net.zhuoweizhang.mcpelauncher.MainMenuOptionsActivity;
import net.zhuoweizhang.mcpelauncher.R;
import net.zhuoweizhang.mcpelauncher.patch.PatchUtils;
import static net.zhuoweizhang.mcpelauncher.ScriptManager.SCRIPTS_DIR;

public class ImportScriptActivity extends ImportActivity {

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		installConfirmText.setText(R.string.script_import_confirm);
	}

	@Override
	protected void startImport() {
		try {
			int maxPatchCount = getResources().getInteger(R.integer.max_num_scripts);
			if (maxPatchCount >= 0 && getEnabledScripts().size() >= maxPatchCount) {
				setResult(RESULT_CANCELED);
				Toast.makeText(this, R.string.script_import_too_many, Toast.LENGTH_SHORT).show();
				finish();
			} else {
				File to = new File(getDir(SCRIPTS_DIR, 0), mFile.getName());
				PatchUtils.copy(mFile, to);
				setResult(RESULT_OK);
				Toast.makeText(this, R.string.script_imported, Toast.LENGTH_LONG).show();
			}
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.manage_patches_import_error, Toast.LENGTH_LONG).show();
		}
		finish();
	}

	protected List<String> getEnabledScripts() {
		SharedPreferences sharedPrefs = getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0);
		String enabledScriptsStr = sharedPrefs.getString("enabledScripts", "");
		return Arrays.asList(enabledScriptsStr.split(";"));
	}

}
