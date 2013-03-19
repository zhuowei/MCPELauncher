package net.zhuoweizhang.mcpelauncher;

import java.io.*;

import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import com.ipaulpro.afilechooser.utils.FileUtils;

import com.mojang.minecraftpe.MainActivity;

import net.zhuoweizhang.mcpelauncher.patch.*;

import static net.zhuoweizhang.mcpelauncher.LauncherActivity.PT_PATCHES_DIR;

public class ImportPatchActivity extends Activity implements View.OnClickListener {

	public Button okButton, cancelButton;

	public TextView patchNameText, installConfirmText;

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.import_patch_confirm);
		okButton = (Button) findViewById(R.id.ok_button);
		cancelButton = (Button) findViewById(R.id.cancel_button);
		okButton.setOnClickListener(this);
		cancelButton.setOnClickListener(this);
		patchNameText = (TextView) findViewById(R.id.app_name);
		installConfirmText = (TextView) findViewById(R.id.install_confirm_question);
		
		Intent intent = getIntent();
		if (intent == null) {
			finish();
			return;
		}

		Uri patchUri = intent.getData();

		patchNameText.setText(patchUri.getLastPathSegment());

		if (hasTooManyPatches()) {
			okButton.setEnabled(false);
			patchNameText.setText(R.string.manage_patches_too_many);
		}
		setResult(RESULT_CANCELED);
	}

	public void onClick(View v) {
		if (v == cancelButton) {
			finish();
		} else if (v == okButton) {
			startImport();
		}
	}

	public void startImport() {
		okButton.setEnabled(false);
		cancelButton.setEnabled(false);
		File from = FileUtils.getFile(getIntent().getData());
		File to = new File(getDir(PT_PATCHES_DIR, 0), from.getName());
		try {
			PatchUtils.copy(from, to);
			setPatchListModified();
			Toast.makeText(this, R.string.manage_patches_import_done, Toast.LENGTH_SHORT).show();
			if (MainActivity.libLoaded) {
				new Thread(new Runnable() {
					public void run() {
						try {
							Thread.sleep(100);
						} catch (Exception e) {}
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
		File[] patches = getDir(PT_PATCHES_DIR, 0).listFiles();
		return maxPatchCount >= 0 && patches.length >= maxPatchCount;
	}

	protected void setPatchListModified() {
		setResult(RESULT_OK);
		getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0).edit().putBoolean("force_prepatch", true).apply();
	}


}
