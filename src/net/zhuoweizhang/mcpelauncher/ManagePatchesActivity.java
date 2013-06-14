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
import net.zhuoweizhang.mcpelauncher.patch.*;
import com.mojang.minecraftpe.*;

public class ManagePatchesActivity extends ListActivity implements View.OnClickListener {

	private static final int DIALOG_MANAGE_PATCH = 1;
	private static final int DIALOG_MANAGE_PATCH_CURRENTLY_DISABLED = 2;
	private static final int DIALOG_MANAGE_PATCH_CURRENTLY_ENABLED = 3;
	private static final int DIALOG_PATCH_INFO = 4;

	private static final int REQUEST_IMPORT_PATCH = 212;

	private static String enabledString = "";
	private static String disabledString = " (disabled)";

	private FindPatchesThread findPatchesThread;

	private List<PatchListItem> patches;

	private PatchListItem selectedPatchItem;

	private Button importButton;

	private byte[] libBytes = null;

	private boolean prePatchConfigure = true;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)	{
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);
		setContentView(R.layout.manage_patches);
		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				openManagePatchWindow(patches.get(position));
			}
		});
		importButton = (Button) findViewById(R.id.manage_patches_import_button);
		importButton.setOnClickListener(this);
		prePatchConfigure = getIntent().getBooleanExtra("prePatchConfigure", true);
		disabledString = " ".concat(getResources().getString(R.string.manage_patches_disabled));
	}

	@Override
	protected void onStart() {
		super.onStart();
		findPatches();
	}

	@Override
	protected void onPause(){
		super.onPause();
		libBytes = null;
	}

	public void onClick(View v) {
		if (v == importButton) {
			importPatch();
		}
	}

	public void importPatch() {
		Intent target = FileUtils.createGetContentIntent();
		target.setType("application/x-ptpatch");
		target.setClass(this, FileChooserActivity.class);
		startActivityForResult(target, REQUEST_IMPORT_PATCH);
	}

	/** gets the maximum number of patches this application can patch. A negative value indicates unlimited amount. */
	protected int getMaxPatchCount() {
		return this.getResources().getInteger(R.integer.max_num_patches);
	}

	protected void setPatchListModified() {
		setResult(RESULT_OK);
		getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0).edit().putBoolean("force_prepatch", true).apply();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_IMPORT_PATCH:  
				if (resultCode == RESULT_OK) {  
					final Uri uri = data.getData();
					File file = FileUtils.getFile(uri);
					try {
						File to = new File(getDir(PT_PATCHES_DIR, 0), file.getName());
						PatchUtils.copy(file, to);
						PatchManager.getPatchManager(this).setEnabled(to, false);
						int maxPatchCount = getMaxPatchCount();
						if (maxPatchCount >= 0 && getEnabledCount() >= maxPatchCount) {
							Toast.makeText(this, R.string.manage_patches_too_many, Toast.LENGTH_SHORT).show();
						} else {
							PatchManager.getPatchManager(this).setEnabled(to, true);
							afterPatchToggle(new PatchListItem(to, true));
						}
						setPatchListModified();
						findPatches();
					} catch (Exception e) {
						e.printStackTrace();
						Toast.makeText(this, R.string.manage_patches_import_error, Toast.LENGTH_LONG).show();
					}
				}
				break;
		}
	}

	private void findPatches() {
		findPatchesThread = new FindPatchesThread();
		new Thread(findPatchesThread).start();
	}

	private void receivePatches(List<PatchListItem> patches) {
		this.patches = patches;
		ArrayAdapter<PatchListItem> adapter = new ArrayAdapter<PatchListItem>(this, R.layout.patch_list_item, patches);
		adapter.sort(new PatchListComparator());
		setListAdapter(adapter);
		List<String> allPaths = new ArrayList<String>(patches.size());
		for (PatchListItem i: patches) {
			String name = i.file.getAbsolutePath();
			allPaths.add(name);
		}
		PatchManager.getPatchManager(this).removeDeadEntries(allPaths);
	}

	private void openManagePatchWindow(PatchListItem item) {
		this.selectedPatchItem = item;
		if (prePatchConfigure || canLivePatch(item)) {
			showDialog(item.enabled? DIALOG_MANAGE_PATCH_CURRENTLY_ENABLED : DIALOG_MANAGE_PATCH_CURRENTLY_DISABLED);
		} else {
			Toast.makeText(this, "This patch cannot be disabled in game.", Toast.LENGTH_SHORT).show();
		}
	}

	public Dialog onCreateDialog(int dialogId) {
		switch (dialogId) {
			case DIALOG_MANAGE_PATCH:
				return createManagePatchDialog(-1);
			case DIALOG_MANAGE_PATCH_CURRENTLY_ENABLED:
				return createManagePatchDialog(1);
			case DIALOG_MANAGE_PATCH_CURRENTLY_DISABLED:
				return createManagePatchDialog(0);
			case DIALOG_PATCH_INFO:
				return createPatchInfoDialog();
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
				aDialog.setTitle(selectedPatchItem.toString());
				break;
			case DIALOG_PATCH_INFO:
				preparePatchInfo((AlertDialog) dialog, selectedPatchItem);
				break;
			default:
				super.onPrepareDialog(dialogId, dialog);
		}
	}

	public void togglePatch(PatchListItem patch) {
		int maxPatchCount = getMaxPatchCount();
		if (!patch.enabled && maxPatchCount >= 0 && getEnabledCount() >= maxPatchCount) {
			Toast.makeText(this, R.string.manage_patches_too_many, Toast.LENGTH_SHORT).show();
			return;
		}
		PatchManager.getPatchManager(this).setEnabled(patch.file, !patch.enabled);
		patch.enabled = !patch.enabled;
		afterPatchToggle(patch);
	}

	private void afterPatchToggle(PatchListItem patch) {
		if (!isValidPatch(patch)) {
			PatchManager.getPatchManager(this).setEnabled(patch.file, false);
			new AlertDialog.Builder(this).setMessage(getResources().getString(R.string.manage_patches_invalid_patches) +
				" " + patch.displayName).setPositiveButton(android.R.string.ok, null).show();
			return;
		}
		if (prePatchConfigure) {
			setPatchListModified(); //should really be called requestPrePatch
		} else if (canLivePatch(patch)) {
			try {
				livePatch(patch);
				getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0).edit().putBoolean("force_prepatch", true).apply();
				/* defer the prepatch until it is needed next time */
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			setPatchListModified(); //should really be called requestPrePatch
		}
	}

	public void livePatch(PatchListItem patchItem) throws Exception {
		ApplicationInfo mcAppInfo = getPackageManager().getApplicationInfo("com.mojang.minecraftpe", 0);
		File patched = getDir("patched", 0);
		File originalLibminecraft = new File(mcAppInfo.nativeLibraryDir + "/libminecraftpe.so");
		File newMinecraft = new File(patched, "libminecraftpe.so");
		com.joshuahuelsman.patchtool.PTPatch patch = new com.joshuahuelsman.patchtool.PTPatch();
		patch.loadPatch(patchItem.file);
		boolean enabled = patchItem.enabled;
		if (enabled) {
			patch.applyPatch(MainActivity.minecraftLibBuffer);
		} else {
			if (libBytes == null) {
				libBytes = new byte[(int) originalLibminecraft.length()];

				InputStream is = new FileInputStream(originalLibminecraft);
				is.read(libBytes);
				is.close();
			}

			patch.removePatch(MainActivity.minecraftLibBuffer, libBytes);
		}
	}

	public boolean canLivePatch(PatchListItem patch) {
		try {
			return PatchUtils.canLivePatch(patch.file);
		} catch (Exception e) {
			return false;
		}
	}

	public int getEnabledCount() {
		return PatchManager.getPatchManager(this).getEnabledPatches().size();
	}

	public void deletePatch(PatchListItem patch) throws Exception {
		patch.enabled = false;
		if (!prePatchConfigure) {
			livePatch(patch);
			getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0).edit().putBoolean("force_prepatch", true).apply();
		}
		setPatchListModified();
		patch.file.delete();
	}

	public void preparePatchInfo(AlertDialog dialog, PatchListItem patch) {
		dialog.setTitle(patch.toString());
		String patchInfo;
		try {
			patchInfo = getPatchInfo(patch);
		} catch (Exception e) {
			patchInfo = "Cannot show info: " + e.getStackTrace();
		}
		dialog.setMessage(patchInfo);
	}

	private String getPatchInfo(PatchListItem patchItem) throws IOException {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getResources().getString(R.string.manage_patches_path));
		builder.append(": ");
		builder.append(patchItem.file.getAbsolutePath());
		builder.append('\n');
		com.joshuahuelsman.patchtool.PTPatch patch = new com.joshuahuelsman.patchtool.PTPatch();
		patch.loadPatch(patchItem.file);
		String desc = patch.getDescription();
		if (desc.length() > 0) {
			builder.append(this.getResources().getString(R.string.manage_patches_metadata));
			builder.append(": ");
			builder.append(desc);
		} else {
			builder.append(this.getResources().getString(R.string.manage_patches_no_metadata));
		}
		return builder.toString();
	}

	/**
	 * @param enableStatus -1 = can't disable, 0 = currently disabled, 1 = currently enabled 
	 */

	protected AlertDialog createManagePatchDialog(int enableStatus) {
		CharSequence patchInfoStr = this.getResources().getText(R.string.manage_patches_info);
		CharSequence[] options = null;
		if (enableStatus == -1) {
			options = new CharSequence[] {this.getResources().getText(R.string.manage_patches_delete), 
				patchInfoStr};
		} else {
			options = new CharSequence[] {this.getResources().getText(R.string.manage_patches_delete), patchInfoStr,
				(enableStatus == 0? this.getResources().getText(R.string.manage_patches_enable): 
					this.getResources().getText(R.string.manage_patches_disable))};
		}

		return new AlertDialog.Builder(this).setTitle("Patch name goes here").
			setItems(options, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialogI, int button) {
					if (button == 0) {
						//selectedPatchItem.file.delete();
						try {
							deletePatch(selectedPatchItem);
							findPatches();
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else if (button == 1) {
						showDialog(DIALOG_PATCH_INFO);
					} else if (button == 2) {
						togglePatch(selectedPatchItem);
						findPatches();
					}
				}
			}).create();
	}

	private AlertDialog createPatchInfoDialog() {
		return new AlertDialog.Builder(this)
			.setTitle("Whoops! info fail") //will get filled in by prepare
			.setMessage("Whoops - try again, this is a tiny fail")
			.setPositiveButton(android.R.string.ok, null)
			.create();
	}

	private boolean isValidPatch(PatchListItem patch) {
		if (patch.file.length() < 6) {
			return false;
		}
		return true;
	}

	private final class FindPatchesThread implements Runnable {

		public void run() {
			File patchesFolder = getDir(PT_PATCHES_DIR, 0);

			final List<PatchListItem> patches = new ArrayList<PatchListItem>();

			combOneFolder(patchesFolder, patches);

			combOneFolder(new File(Environment.getExternalStorageDirectory(), "Android/data/com.snowbound.pockettool.free/Patches"), patches);

			combOneFolder(new File(Environment.getExternalStorageDirectory(), "Android/data/com.joshuahuelsman.pockettool/Patches"), patches);

			ManagePatchesActivity.this.runOnUiThread(new Runnable() {
				public void run() {
					ManagePatchesActivity.this.receivePatches(patches);
				}
			});
		}

		private void combOneFolder(File patchesFolder, List<PatchListItem> patches) {
			if (!patchesFolder.exists()) {
				System.err.println("no storage folder");
			} else {
				PatchManager patchMgr = PatchManager.getPatchManager(ManagePatchesActivity.this);
				for (File patchFile : patchesFolder.listFiles()) {
					boolean patchEnabled = patchMgr.isEnabled(patchFile);
					patches.add(new PatchListItem(patchFile, patchEnabled));
				}
			}
		}

	}

	private final class PatchListItem {
		public final File file;
		public final String displayName;
		public boolean enabled = true;
		public PatchListItem(File file, boolean enabled) {
			this.file = file;
			this.displayName = file.getName();
			this.enabled = enabled;
		}

		public String toString() {
			return displayName + (enabled? enabledString: disabledString);
		}
	}

	private final class PatchListComparator implements Comparator<PatchListItem> {
		public int compare(PatchListItem a, PatchListItem b) {
			//System.out.println(a.toString() + ":" + b.toString() + (a.levelDat.lastModified() - b.levelDat.lastModified()));
			return a.displayName.toLowerCase().compareTo(b.displayName.toLowerCase());
		}
		public boolean equals(PatchListItem a, PatchListItem b) {
			return a.displayName.toLowerCase().equals(b.displayName.toLowerCase());
		}
	}

}
