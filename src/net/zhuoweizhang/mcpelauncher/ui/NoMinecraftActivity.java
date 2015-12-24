package net.zhuoweizhang.mcpelauncher.ui;

import net.zhuoweizhang.mcpelauncher.R;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.TextView;

public class NoMinecraftActivity extends Activity {

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.no_minecraft);
		Intent intent = getIntent();
		String message = intent.getStringExtra("message");
		if (message != null) {
			TextView textView = (TextView) findViewById(R.id.no_minecraft_text);
			textView.setText(message);
		}
	}
}
