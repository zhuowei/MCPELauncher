package net.zhuoweizhang.mcpelauncher;

import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import net.zhuoweizhang.mcpelauncher.RefreshContentListThread.OnRefreshContentList;
import net.zhuoweizhang.mcpelauncher.patch.PatchUtils;
import static net.zhuoweizhang.mcpelauncher.PatchManager.blankArray;
import static net.zhuoweizhang.mcpelauncher.PatchManager.join;
import static net.zhuoweizhang.mcpelauncher.ScriptManager.SCRIPTS_DIR;

public class ImportScriptActivity extends ImportActivity implements OnRefreshContentList {
	protected Thread refreshThread;
	protected CountDownLatch mLatch;

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		installConfirmText.setText(R.string.script_import_confirm);
	}

	@Override
	protected void startImport() {
		try {
			File to = new File(getDir(SCRIPTS_DIR, 0), mFile.getName());
			PatchUtils.copy(mFile, to);
			int maxPatchCount = this.getResources().getInteger(R.integer.max_num_scripts);
			if (maxPatchCount >= 0 && getEnabledScripts().size() >= maxPatchCount) {
				setResult(RESULT_CANCELED);
				Toast.makeText(this, R.string.script_import_too_many, Toast.LENGTH_SHORT).show();
				finish();
			} else {
				setResult(RESULT_OK);
				findScripts();
			}
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.manage_patches_import_error, Toast.LENGTH_LONG).show();
		}
		finish();
	}

	private void findScripts() {
		mLatch = new CountDownLatch(1);
		refreshThread = new Thread(new RefreshContentListThread(this, this));
		refreshThread.start();
		try {
			mLatch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	protected Set<String> getEnabledScripts() {
		SharedPreferences sharedPrefs = getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0);
		String enabledScriptsStr = sharedPrefs.getString("enabledScripts", "");
		return new HashSet<String>(Arrays.asList(enabledScriptsStr.split(";")));
	}

	@Override
	public void onRefreshComplete(List<ContentListItem> items) {
		// There is a bug - we should enable script on import, but it is not
		// working, so comment this to not to cause any awful exceptions,
		// crashes, or more bad things.

		List<String> allPaths = new ArrayList<String>(items.size());
		for (ContentListItem i : items) {
			String name = i.file.getName();
			allPaths.add(name);
		}
		Set<String> enabledScripts = getEnabledScripts();
		enabledScripts.retainAll(allPaths);
		// SharedPreferences sharedPrefs =
		// getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0);
		// SharedPreferences.Editor edit = sharedPrefs.edit();
		// edit.putString("enabledScripts",
		// join(enabledScripts.toArray(blankArray), ";"));
		// edit.putInt("scriptManagerVersion", 1);
		// edit.apply();
		mLatch.countDown();
	}

	@Override
	public List<File> getFolders() {
		List<File> folders = new ArrayList<File>();
		folders.add(getDir(SCRIPTS_DIR, 0));
		return folders;
	}

	@Override
	public boolean isEnabled(File f) {
		return getEnabledScripts().contains(f.getName());
	}

}
