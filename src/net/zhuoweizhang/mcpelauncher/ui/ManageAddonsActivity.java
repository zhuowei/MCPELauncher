package net.zhuoweizhang.mcpelauncher.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.zhuoweizhang.mcpelauncher.R;
import net.zhuoweizhang.mcpelauncher.Utils;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;

import net.zhuoweizhang.mcpelauncher.AddonManager;

@SuppressWarnings("deprecation")
public class ManageAddonsActivity extends ListActivity {

	private static final int DIALOG_MANAGE_PATCH = 1;
	private static final int DIALOG_MANAGE_PATCH_CURRENTLY_DISABLED = 2;
	private static final int DIALOG_MANAGE_PATCH_CURRENTLY_ENABLED = 3;

	private static String enabledString = "";
	private static String disabledString = " (disabled)";

	private List<AddonListItem> addons;

	private AddonListItem selectedAddonItem;

	protected CompoundButton master = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Utils.setLanguageOverride();
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);
		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				openManageAddonWindow(addons.get(position));
			}
		});
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getMenuInflater().inflate(R.menu.ab_master, menu);
			master = (CompoundButton) menu.findItem(R.id.ab_switch_container).getActionView()
					.findViewById(R.id.ab_switch);
			if (master != null) {
				master.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						if (isChecked) {
							findAddons();
						} else {
							((ArrayAdapter<?>) getListAdapter()).clear();
						}
						SharedPreferences.Editor sh = Utils.getPrefs(0).edit();
						sh.putBoolean("zz_load_native_addons", isChecked);
						sh.apply();
						refreshABToggle();
					}
				});
				refreshABToggle();
			} else {
				System.err.println("WTF?");
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		findAddons();
		refreshABToggle();
	}

	@Override
	protected void onPause() {
		super.onPause();
		refreshABToggle();
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshABToggle();
	}

	protected void refreshABToggle() {
		if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) && (master != null)) {
			master.setChecked(Utils.getPrefs(0).getBoolean("zz_load_native_addons", false));
		}
	}

	protected void setAddonListModified() {
		setResult(RESULT_OK);
		//Utils.getPrefs(1).edit().putBoolean("force_prepatch", true).apply();
	}

	private void findAddons() {
		PackageManager pm = getPackageManager();
		List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
		List<AddonListItem> addonListItems = new ArrayList<AddonListItem>();
		AddonManager manager = AddonManager.getAddonManager(this);
		for (ApplicationInfo app : apps) {
			if (app.metaData == null)
				continue;
			String nativeLibName = app.metaData
					.getString("net.zhuoweizhang.mcpelauncher.api.targetmcpeversion");
			if (nativeLibName != null) {
				boolean enabled = manager.isEnabled(app.packageName);
				AddonListItem itm = new AddonListItem(app, enabled);
				itm.displayName = pm.getApplicationLabel(app).toString() + " " + itm.displayName;
				addonListItems.add(itm);
			}
		}
		receiveAddons(addonListItems);
	}

	private void receiveAddons(List<AddonListItem> addons) {
		this.addons = addons;

		List<String> allPaths = new ArrayList<String>(addons.size());
		for (AddonListItem i : addons) {
			String name = i.appInfo.packageName;
			allPaths.add(name);
		}
		AddonManager.getAddonManager(this).removeDeadEntries(allPaths);

		ArrayAdapter<AddonListItem> adapter = new ArrayAdapter<AddonListItem>(this,
				R.layout.patch_list_item, addons);
		adapter.sort(new AddonListComparator());
		setListAdapter(adapter);
	}

	private void openManageAddonWindow(AddonListItem item) {
		this.selectedAddonItem = item;
		showDialog(item.enabled? DIALOG_MANAGE_PATCH_CURRENTLY_ENABLED : DIALOG_MANAGE_PATCH_CURRENTLY_DISABLED);
	}

	public Dialog onCreateDialog(int dialogId) {
		switch (dialogId) {
		case DIALOG_MANAGE_PATCH:
			return createManageAddonDialog(-1);
		case DIALOG_MANAGE_PATCH_CURRENTLY_ENABLED:
			return createManageAddonDialog(1);
		case DIALOG_MANAGE_PATCH_CURRENTLY_DISABLED:
			return createManageAddonDialog(0);
		default:
			return super.onCreateDialog(dialogId);
		}
	}

	public void onPrepareDialog(int dialogId, Dialog dialog) {
		switch (dialogId) {
		case DIALOG_MANAGE_PATCH:
		case DIALOG_MANAGE_PATCH_CURRENTLY_ENABLED:
		case DIALOG_MANAGE_PATCH_CURRENTLY_DISABLED:
			AlertDialog aDialog = (AlertDialog) dialog;
			aDialog.setTitle(selectedAddonItem.toString());
			break;
		default:
			super.onPrepareDialog(dialogId, dialog);
		}
	}

	public void toggleAddon(AddonListItem addon) {
		AddonManager.getAddonManager(this).setEnabled(addon.appInfo.packageName, !addon.enabled);
		addon.enabled = !addon.enabled;
		afterAddonToggle(addon);
	}

	private void afterAddonToggle(AddonListItem patch) {
		setAddonListModified(); // should really be called requestPrePatch
	}

	public void deleteAddon(AddonListItem addon) throws Exception {
		Uri packageURI = Uri.parse("package:" + addon.appInfo.packageName);
		Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
		startActivityForResult(uninstallIntent, 123);
		setAddonListModified();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 123) {
			findAddons();
		}
	}

	/**
	 * @param enableStatus
	 *            -1 = can't disable, 0 = currently disabled, 1 = currently
	 *            enabled
	 */

	protected AlertDialog createManageAddonDialog(int enableStatus) {
		CharSequence[] options = null;
		if (enableStatus == -1) {
			options = new CharSequence[] { this.getResources().getText(
					R.string.manage_patches_delete) };
		} else {
			options = new CharSequence[] {
					this.getResources().getText(R.string.manage_patches_delete),
					(enableStatus == 0 ? this.getResources()
							.getText(R.string.manage_patches_enable) : this.getResources().getText(
							R.string.manage_patches_disable)) };
		}

		return new AlertDialog.Builder(this).setTitle("Addon name goes here")
				.setItems(options, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialogI, int button) {
						if (button == 0) {
							// selectedAddonItem.file.delete();
							try {
								deleteAddon(selectedAddonItem);
								findAddons();
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else if (button == 1) {
							toggleAddon(selectedAddonItem);
							findAddons();
						}
					}
				}).create();
	}

	private final class AddonListItem {
		public final ApplicationInfo appInfo;
		public String displayName;
		public boolean enabled = true;

		public AddonListItem(ApplicationInfo appInfo, boolean enabled) {
			this.appInfo = appInfo;
			this.displayName = appInfo.packageName;
			this.enabled = enabled;
		}

		public String toString() {
			return displayName + (enabled ? enabledString : disabledString);
		}
	}

	@SuppressLint("DefaultLocale")
	private final class AddonListComparator implements Comparator<AddonListItem> {
		public int compare(AddonListItem a, AddonListItem b) {
			return a.displayName.toLowerCase().compareTo(b.displayName.toLowerCase());
		}

		public boolean equals(AddonListItem a, AddonListItem b) {
			return a.displayName.toLowerCase().equals(b.displayName.toLowerCase());
		}
	}

}
