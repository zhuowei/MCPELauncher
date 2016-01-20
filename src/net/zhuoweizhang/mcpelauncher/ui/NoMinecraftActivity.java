package net.zhuoweizhang.mcpelauncher.ui;

import net.zhuoweizhang.mcpelauncher.R;
import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class NoMinecraftActivity extends Activity {
	private String learnmoreUri = null;

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.no_minecraft);
		Intent intent = getIntent();
		String message = intent.getStringExtra("message");
		if (message != null) {
			TextView textView = (TextView) findViewById(R.id.no_minecraft_text);
			textView.setText(message);
		}
		learnmoreUri = intent.getStringExtra("learnmore_uri");
		if (learnmoreUri != null) {
			Button theButton = (Button) findViewById(R.id.no_minecraft_learn_more);
			theButton.setVisibility(View.VISIBLE);
		}
	}

	public void learnMoreClicked(View v) {
		Intent learnmoreIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(learnmoreUri));
		try {
			startActivity(learnmoreIntent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
