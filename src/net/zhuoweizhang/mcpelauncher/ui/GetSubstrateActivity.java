package net.zhuoweizhang.mcpelauncher.ui;

import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.view.*;

import net.zhuoweizhang.mcpelauncher.*;

public class GetSubstrateActivity extends Activity {

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.get_substrate);
	}

	public void downloadClicked(View v) {
		String url = isPlay()? "market://details?id=com.saurik.substrate" : "http://www.cydiasubstrate.com/";
		Intent downloadIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		downloadIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(downloadIntent);
		finish();
	}

	private boolean isPlay() {
		try {
			return getPackageManager().getInstallerPackageName(getPackageName()).equals("com.android.vending");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}
