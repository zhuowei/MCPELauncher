package net.zhuoweizhang.mcpelauncher;

import android.content.*;

import java.util.*;
import java.io.*;

import net.zhuoweizhang.mcpelauncher.ui.MainMenuOptionsActivity;

import static net.zhuoweizhang.mcpelauncher.PatchManager.join;
import static net.zhuoweizhang.mcpelauncher.PatchManager.blankArray;

public class AddonManager {

	public static AddonManager addonMgr;

	private Context context;

	private Set<String> disabledAddons;

	public static AddonManager getAddonManager(Context context) {
		if (addonMgr == null) {
			addonMgr = new AddonManager(context.getApplicationContext());
		}
		return addonMgr;
	}

	public AddonManager(Context context) {
		this.context = context;
		loadDisabledAddons();
	}

	public Set<String> getDisabledAddons() {
		return disabledAddons;
	}

	public void setEnabled(String name, boolean state) {
		if (!state) {
			disabledAddons.add(name);
		} else {
			disabledAddons.remove(name);
		}
		saveDisabledAddons();
	}

	public boolean isEnabled(String name) {
		return !disabledAddons.contains(name);
	}

	public void removeDeadEntries(Collection<String> allPossibleFiles) {
		disabledAddons.retainAll(allPossibleFiles);
		saveDisabledAddons();
	}

	protected void loadDisabledAddons() {
		String[] parts = Utils.getPrefs(1).getString("disabledAddons", "").split(";");
		disabledAddons = new HashSet<String>(Arrays.asList(parts));
	}

	protected void saveDisabledAddons() {
		SharedPreferences sharedPrefs = Utils.getPrefs(1);
		SharedPreferences.Editor edit = sharedPrefs.edit();
		edit.putString("disabledAddons", join(disabledAddons.toArray(blankArray), ";"));
		edit.putInt("addonManagerVersion", 1);
		edit.apply();
	}
}
