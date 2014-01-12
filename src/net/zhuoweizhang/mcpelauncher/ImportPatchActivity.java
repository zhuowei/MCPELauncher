package net.zhuoweizhang.mcpelauncher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Redirect all old API requests to new API, handle result and return it.
 */
public class ImportPatchActivity extends Activity {
	public static final int MOVED_PERMANENTLY = 301; // HTTP status code

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		System.out.println("Requested deprecated APIs. Referenced class moved to .api child package.");
		Intent mIntent = new Intent(this, net.zhuoweizhang.mcpelauncher.api.ImportPatchActivity.class);
		mIntent.setAction("net.zhuoweizhang.mcpelauncher.action.IMPORT_PATCH");
		// Copy all data
		mIntent.setDataAndType(getIntent().getData(), getIntent().getType());
		mIntent.putExtras(getIntent());
		startActivityForResult(mIntent, MOVED_PERMANENTLY);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode != MOVED_PERMANENTLY) {
			System.out.println("WTF?");
			setResult(RESULT_CANCELED);
		} else {
			setResult(resultCode, data);
		}
		finish();
	}
}
