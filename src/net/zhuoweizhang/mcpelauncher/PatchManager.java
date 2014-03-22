package net.zhuoweizhang.mcpelauncher;

import android.content.*;

import java.util.*;
import java.io.*;

import net.zhuoweizhang.mcpelauncher.ui.MainMenuOptionsActivity;

public class PatchManager {

	public static PatchManager patchMgr;

	private Context context;

	private Set<String> enabledPatches;

	public static final String[] blankArray = new String[0];

	public static PatchManager getPatchManager(Context context) {
		if (patchMgr == null) {
			patchMgr = new PatchManager(context);
		}
		return patchMgr;
	}

	public PatchManager(Context context) {
		this.context = context;
		loadEnabledPatches();
	}

	public Set<String> getEnabledPatches() {
		return enabledPatches;
	}

	private void setEnabled(String name, boolean state) {
		if (state) {
			enabledPatches.add(name);
		} else {
			enabledPatches.remove(name);
		}
		saveEnabledPatches();
	}

	public void setEnabled(File[] files, boolean state) {
		for (File file: files) {
			String name = file.getAbsolutePath();
			if (name == null || name.length() <= 0) continue;
			if (state) {
				enabledPatches.add(name);
			} else {
				enabledPatches.remove(name);
			}
		}
		saveEnabledPatches();
	}

	public void setEnabled(File file, boolean state) {
		setEnabled(file.getAbsolutePath(), state);
	}

	private boolean isEnabled(String name) {
		return enabledPatches.contains(name);
	}

	public boolean isEnabled(File file) {
		return isEnabled(file.getAbsolutePath());
	}

	public void removeDeadEntries(Collection<String> allPossibleFiles) {
		enabledPatches.retainAll(allPossibleFiles);
		saveEnabledPatches();
	}

	protected void loadEnabledPatches() {
		SharedPreferences sharedPrefs = context.getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0);
		String enabledPatchesStr = sharedPrefs.getString("enabledPatches", "");
		enabledPatches = new HashSet<String>(Arrays.asList(enabledPatchesStr.split(";")));
	}

	protected void saveEnabledPatches() {
		SharedPreferences sharedPrefs = context.getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0);
		SharedPreferences.Editor edit = sharedPrefs.edit();
		edit.putString("enabledPatches", join(enabledPatches.toArray(blankArray), ";"));
		edit.putBoolean("force_prepatch", true);
		edit.putInt("patchManagerVersion", 1);
		edit.apply();
	}

	public static String join(String[] list, String token) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < list.length; i++) {
			builder.append(list[i]);
			if (i < list.length - 1) {
				builder.append(token);
			}
		}
		return builder.toString();
	}

	public void disableAllPatches() {
		enabledPatches.clear();
		saveEnabledPatches();
	}
}
