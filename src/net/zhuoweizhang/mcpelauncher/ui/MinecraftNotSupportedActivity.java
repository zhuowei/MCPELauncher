package net.zhuoweizhang.mcpelauncher.ui;

import net.zhuoweizhang.mcpelauncher.R;
import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.widget.*;

public class MinecraftNotSupportedActivity extends Activity {

	public TextView theText;

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.no_minecraft);
		theText = (TextView) findViewById(R.id.no_minecraft_text);
		Intent intent = getIntent();
		String minecraftVersion = intent.getStringExtra("minecraftVersion");
		String supportedVersion = intent.getStringExtra("supportedVersion");
		String textToDisplay = getResources().getString(R.string.minecraft_version_not_supported).
			toString();
		String textWithReplacements = textToDisplay.replaceAll("MINECRAFT_VERSION", minecraftVersion).
			replaceAll("SUPPORTED_VERSION", supportedVersion);
		theText.setText(textWithReplacements);
	}
}
