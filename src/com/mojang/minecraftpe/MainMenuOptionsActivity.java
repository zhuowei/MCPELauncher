package com.mojang.minecraftpe;

import java.io.File;

import android.app.Activity;
import android.app.NativeActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.ipaulpro.afilechooser.FileChooserActivity;
import com.ipaulpro.afilechooser.utils.FileUtils;


public class MainMenuOptionsActivity extends Activity
{
	public static final String PREFERENCES_NAME = "mcpelauncherprefs";
	public static final int REQUEST_SELECT_TEXTURE_PACK = 5;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.main);

		Intent target = FileUtils.createGetContentIntent();
		target.setType("application/zip");
		Intent intent = Intent.createChooser(target, "Select texture pack");
		startActivityForResult(intent, REQUEST_SELECT_TEXTURE_PACK);

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_SELECT_TEXTURE_PACK:  
				if (resultCode == RESULT_OK) {  
					final Uri uri = data.getData();
					File file = FileUtils.getFile(uri);
					SharedPreferences prefs = getSharedPreferences(PREFERENCES_NAME, 0);
					SharedPreferences.Editor editor = prefs.edit();
					try {
						editor.putString("texturePack", file.getCanonicalPath());
						editor.commit();
					} catch (Exception e) {
						e.printStackTrace();
					}
					Toast.makeText(this, "Texture pack set! Restarting Minecraft to load texture pack...", Toast.LENGTH_LONG).show();
					finish();
					restartFirstActivity();
					//System.exit(0);

				}
				break;
		}
		finish();
	}
	/* thanks, http://stackoverflow.com/questions/1397361/how-do-i-restart-an-android-activity */
	private void restartFirstActivity() {
		Intent i = getPackageManager().getLaunchIntentForPackage(getPackageName());
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK );
		startActivity(i);
	}


}
