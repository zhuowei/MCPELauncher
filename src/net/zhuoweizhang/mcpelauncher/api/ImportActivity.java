package net.zhuoweizhang.mcpelauncher.api;

import java.io.File;

import net.zhuoweizhang.mcpelauncher.R;
import net.zhuoweizhang.mcpelauncher.Utils;
import com.ipaulpro.afilechooser.utils.FileUtils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public abstract class ImportActivity extends Activity implements View.OnClickListener {
	public Button okButton, cancelButton;

	public TextView patchNameText, installConfirmText;

	public File mFile = null;

	public void onCreate(Bundle icicle) {
		Utils.setLanguageOverride();
		super.onCreate(icicle);
		setContentView(R.layout.import_confirm);
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

		mFile = FileUtils.getFile(getIntent().getData());

		if (mFile == null || !mFile.canRead()) {
			finish();
			return;
		}

		patchNameText.setText(mFile.getName());

		setResult(RESULT_CANCELED);
	}

	@Override
	public void onClick(View v) {
		if (v.equals(cancelButton)) {
			finish();
		} else if (v.equals(okButton)) {
			okButton.setEnabled(false);
			cancelButton.setEnabled(false);
			startImport();
		}
	}

	protected abstract void startImport();
}
