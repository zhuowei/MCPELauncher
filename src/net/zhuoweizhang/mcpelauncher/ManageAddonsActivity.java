package net.zhuoweizhang.mcpelauncher;

import java.io.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import static android.widget.AdapterView.OnItemClickListener;
import android.widget.*;

import com.ipaulpro.afilechooser.FileChooserActivity;
import com.ipaulpro.afilechooser.utils.FileUtils;

import static net.zhuoweizhang.mcpelauncher.LauncherActivity.PT_PATCHES_DIR;
import com.mojang.minecraftpe.*;

public class ManageAddonsActivity extends ListActivity/* implements View.OnClickListener*/ {

	private static final int DIALOG_MANAGE_PATCH = 1;
	private static final int DIALOG_MANAGE_PATCH_CURRENTLY_DISABLED = 2;
	private static final int DIALOG_MANAGE_PATCH_CURRENTLY_ENABLED = 3;

	private static final int REQUEST_IMPORT_PATCH = 212;

	private static String enabledString = "";
	private static String disabledString = " (disabled)";

	private List<AddonListItem> addons;

	private AddonListItem selectedAddonItem;

	private Button importButton;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)	{
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

	@Override
	protected void onStart() {
		super.onStart();
		findAddons();
	}

	protected void setAddonListModified() {
		setResult(RESULT_OK);
		getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0).edit().putBoolean("force_prepatch", true).apply();
	}

	private void findAddons() {
		PackageManager pm = getPackageManager();
		List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
		List<AddonListItem> addonListItems = new ArrayList<AddonListItem>();
		for (ApplicationInfo app: apps) {
			if (app.metaData == null) continue;
			String nativeLibName = app.metaData.getString("net.zhuoweizhang.mcpelauncher.api.nativelibname");
			if (nativeLibName != null) {
				AddonListItem itm = new AddonListItem(app, true);
				itm.displayName = pm.getApplicationLabel(app).toString() + " " + itm.displayName;
				addonListItems.add(itm);
			}
		}
		receiveAddons(addonListItems);
	}

	private void receiveAddons(List<AddonListItem> addons) {
		this.addons = addons;
		ArrayAdapter<AddonListItem> adapter = new ArrayAdapter<AddonListItem>(this, R.layout.patch_list_item, addons);
		adapter.sort(new AddonListComparator());
		setListAdapter(adapter);
	}

	private void openManageAddonWindow(AddonListItem item) {
		this.selectedAddonItem = item;
		//showDialog(item.enabled? DIALOG_MANAGE_PATCH_CURRENTLY_ENABLED : DIALOG_MANAGE_PATCH_CURRENTLY_DISABLED);
		showDialog(DIALOG_MANAGE_PATCH);
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
	}

	public void deleteAddon(AddonListItem addon) throws Exception {
		Uri packageURI = Uri.parse("package:" + addon.appInfo.packageName);
		Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
		startActivityForResult(uninstallIntent, 0);
		setAddonListModified();
	}


	/**
	 * @param enableStatus -1 = can't disable, 0 = currently disabled, 1 = currently enabled 
	 */

	protected AlertDialog createManageAddonDialog(int enableStatus) {
		CharSequence[] options = null;
		if (enableStatus == -1) {
			options = new CharSequence[] {this.getResources().getText(R.string.manage_patches_delete)};
		} else {
			options = new CharSequence[] {this.getResources().getText(R.string.manage_patches_delete), 
				(enableStatus == 0? this.getResources().getText(R.string.manage_patches_enable): 
					this.getResources().getText(R.string.manage_patches_disable))};
		}

		return new AlertDialog.Builder(this).setTitle("Addon name goes here").
			setItems(options, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialogI, int button) {
					if (button == 0) {
						//selectedAddonItem.file.delete();
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
			return displayName + (enabled? enabledString: disabledString);
		}
	}

	private final class AddonListComparator implements Comparator<AddonListItem> {
		public int compare(AddonListItem a, AddonListItem b) {
			//System.out.println(a.toString() + ":" + b.toString() + (a.levelDat.lastModified() - b.levelDat.lastModified()));
			return a.displayName.toLowerCase().compareTo(b.displayName.toLowerCase());
		}
		public boolean equals(AddonListItem a, AddonListItem b) {
			return a.displayName.toLowerCase().equals(b.displayName.toLowerCase());
		}
	}

}
