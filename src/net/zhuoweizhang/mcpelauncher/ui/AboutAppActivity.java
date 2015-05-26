package net.zhuoweizhang.mcpelauncher.ui;

import net.zhuoweizhang.mcpelauncher.R;
import net.zhuoweizhang.mcpelauncher.Utils;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public final class AboutAppActivity extends Activity implements View.OnLongClickListener, View.OnClickListener {

	public static final String FORUMS_PAGE_URL = 
		"http://www.minecraftforum.net/topic/1675581-blocklauncher-an-android-app-that-patches-minecraft-pe-without-reinstall/";

	public static final String LICENSES_URL = "https://gist.github.com/zhuowei/da4c2fec46d4d23050bf";

	public static final int SLEEP_INTERVAL = 120;


	public TextView appNameText;

	public TextView appVersionText;

	public Button gotoForumsButton;

	public Button ossLicensesButton;

	public int frame;

	protected void onCreate(Bundle savedInstanceState) {
		Utils.setLanguageOverride();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		appNameText = (TextView) findViewById(R.id.about_appnametext);
		appNameText.setOnLongClickListener(this);
		gotoForumsButton = (Button) findViewById(R.id.about_go_to_forums_button);
		gotoForumsButton.setOnClickListener(this);
		ossLicensesButton = (Button) findViewById(R.id.about_oss_license_info_button);
		ossLicensesButton.setOnClickListener(this);
		appVersionText = (TextView) findViewById(R.id.about_appversiontext);
		String appVersion = "Top secret alpha pre-prerelease";
		try {
			appVersion = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
		} catch (Exception e) {
			e.printStackTrace();
		}
		appVersionText.setText(appVersion);
	}

	public boolean onLongClick(View v) {
		if (v == appNameText) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse("http://www.youtube.com/watch?v=6K7VaIdttkw"));
			startActivity(intent); //no easter egg for you!
			return true;
		}
		return false;
	}

	public void onClick(View v) {
		if (v == gotoForumsButton) {
			openForumsPage();
		} else if (v == ossLicensesButton) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(LICENSES_URL));
			startActivity(intent);
		}
	}

	protected void openForumsPage() {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(FORUMS_PAGE_URL));
		startActivity(intent);
	}
}
