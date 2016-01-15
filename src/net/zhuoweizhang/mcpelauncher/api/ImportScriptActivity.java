package net.zhuoweizhang.mcpelauncher.api;

import static net.zhuoweizhang.mcpelauncher.ScriptManager.SCRIPTS_DIR;

import java.io.File;
import java.io.IOException;

import net.zhuoweizhang.mcpelauncher.R;
import net.zhuoweizhang.mcpelauncher.ScriptManager;
import net.zhuoweizhang.mcpelauncher.patch.PatchUtils;
import android.os.Bundle;
import android.widget.Toast;

public class ImportScriptActivity extends ImportActivity {

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		installConfirmText.setText(R.string.script_import_confirm);
	}

	@Override
	protected void startImport() {
		try {
			File to = new File(getDir(SCRIPTS_DIR, 0), mFile.getName());
			PatchUtils.copy(mFile, to);
			ScriptManager.androidContext = this.getApplicationContext();
			ScriptManager.loadEnabledScriptsNames(this.getApplicationContext());
			ScriptManager.setOriginalLocation(mFile, to);
			ScriptManager.setEnabled(to, true);
			Toast.makeText(this, R.string.script_imported, Toast.LENGTH_LONG)
					.show();
			setResult(RESULT_OK);
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.manage_patches_import_error,
					Toast.LENGTH_LONG).show();
			setResult(RESULT_CANCELED);
		}
		finish();
	}

}
