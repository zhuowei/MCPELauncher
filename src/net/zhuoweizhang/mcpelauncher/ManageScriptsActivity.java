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

import static net.zhuoweizhang.mcpelauncher.ScriptManager.SCRIPTS_DIR;
import net.zhuoweizhang.mcpelauncher.patch.*;
import com.mojang.minecraftpe.*;

public class ManageScriptsActivity extends ListActivity implements View.OnClickListener {

	private static final int DIALOG_MANAGE_PATCH = 1;
	private static final int DIALOG_MANAGE_PATCH_CURRENTLY_DISABLED = 2;
	private static final int DIALOG_MANAGE_PATCH_CURRENTLY_ENABLED = 3;
	private static final int DIALOG_PATCH_INFO = 4;
	private static final int DIALOG_IMPORT_SOURCES = 5;

	private static final int REQUEST_IMPORT_PATCH = 212;

	private static String enabledString = "";
	private static String disabledString = " (disabled)";

	private FindScriptsThread findScriptsThread;

	private List<ScriptListItem> patches;

	private ScriptListItem selectedPatchItem;

	private Button importButton;

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
		disabledString = " ".concat(getResources().getString(R.string.manage_patches_disabled));
		ScriptManager.androidContext = this.getApplicationContext();
	}

	@Override
	protected void onStart() {
		super.onStart();
		findScripts();
	}

	public void onClick(View v) {
		if (v == importButton) {
			importPatch();
		}
	}

	public void importPatch() {
		showDialog(DIALOG_IMPORT_SOURCES);
	}

	public void importPatchFromFile() {
		Intent target = FileUtils.createGetContentIntent();
		target.setType("application/javascript");
		target.setClass(this, FileChooserActivity.class);
		startActivityForResult(target, REQUEST_IMPORT_PATCH);
	}

	/** gets the maximum number of patches this application can patch. A negative value indicates unlimited amount. */
	protected int getMaxPatchCount() {
		return this.getResources().getInteger(R.integer.max_num_patches);
	}

	protected void setPatchListModified() {
		setResult(RESULT_OK);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_IMPORT_PATCH:  
				if (resultCode == RESULT_OK) {  
					final Uri uri = data.getData();
					File file = FileUtils.getFile(uri);
					try {
						File to = new File(getDir(SCRIPTS_DIR, 0), file.getName());
						PatchUtils.copy(file, to);
						ScriptManager.setEnabled(to, false);
						int maxPatchCount = getMaxPatchCount();
						if (maxPatchCount >= 0 && getEnabledCount() >= maxPatchCount) {
							Toast.makeText(this, R.string.manage_patches_too_many, Toast.LENGTH_SHORT).show();
						} else {
							ScriptManager.setEnabled(to, true);
							afterPatchToggle(new ScriptListItem(to, true));
						}
						setPatchListModified();
						findScripts();
					} catch (Exception e) {
						e.printStackTrace();
						Toast.makeText(this, R.string.manage_patches_import_error, Toast.LENGTH_LONG).show();
					}
				}
				break;
		}
	}

	private void findScripts() {
		findScriptsThread = new FindScriptsThread();
		new Thread(findScriptsThread).start();
	}

	private void receiveScripts(List<ScriptListItem> patches) {
		this.patches = patches;
		ArrayAdapter<ScriptListItem> adapter = new ArrayAdapter<ScriptListItem>(this, R.layout.patch_list_item, patches);
		adapter.sort(new PatchListComparator());
		setListAdapter(adapter);
		List<String> allPaths = new ArrayList<String>(patches.size());
		for (ScriptListItem i: patches) {
			String name = i.file.getName();
			allPaths.add(name);
		}
		ScriptManager.removeDeadEntries(allPaths);
	}

	private void openManagePatchWindow(ScriptListItem item) {
		this.selectedPatchItem = item;
		showDialog(item.enabled? DIALOG_MANAGE_PATCH_CURRENTLY_ENABLED : DIALOG_MANAGE_PATCH_CURRENTLY_DISABLED);
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
			case DIALOG_IMPORT_SOURCES:
				return createImportSourcesDialog();
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

	public void togglePatch(ScriptListItem patch) {
		int maxPatchCount = getMaxPatchCount();
		if (!patch.enabled && maxPatchCount >= 0 && getEnabledCount() >= maxPatchCount) {
			Toast.makeText(this, R.string.manage_patches_too_many, Toast.LENGTH_SHORT).show();
			return;
		}
		try {
			ScriptManager.setEnabled(patch.file, !patch.enabled);
		} catch (Exception e) {
			e.printStackTrace();
			reportScriptLoadError(e);
		}
		patch.enabled = !patch.enabled;
		afterPatchToggle(patch);
	}

	private void reportScriptLoadError(Exception e) {
		//do nothing for now
	}

	private void afterPatchToggle(ScriptListItem patch) {
	}

	public int getEnabledCount() {
		return ScriptManager.getEnabledScripts().size();
	}

	public void deletePatch(ScriptListItem patch) throws Exception {
		patch.enabled = false;
		try {
			ScriptManager.setEnabled(patch.file, false);
		} catch (Exception e) {e.printStackTrace();}
		setPatchListModified();
		patch.file.delete();
	}

	public void preparePatchInfo(AlertDialog dialog, ScriptListItem patch) {
		dialog.setTitle(patch.toString());
		String patchInfo;
		try {
			patchInfo = getPatchInfo(patch);
		} catch (Exception e) {
			patchInfo = "Cannot show info: " + e.getStackTrace();
		}
		dialog.setMessage(patchInfo);
	}

	private String getPatchInfo(ScriptListItem patchItem) throws IOException {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getResources().getString(R.string.manage_patches_path));
		builder.append(": ");
		builder.append(patchItem.file.getAbsolutePath());
		builder.append('\n');
		/*com.joshuahuelsman.patchtool.PTPatch patch = new com.joshuahuelsman.patchtool.PTPatch();
		patch.loadPatch(patchItem.file);
		String desc = patch.getDescription();
		if (desc.length() > 0) {
			builder.append(this.getResources().getString(R.string.manage_patches_metadata));
			builder.append(": ");
			builder.append(desc);
		} else {
			builder.append(this.getResources().getString(R.string.manage_patches_no_metadata));
		}*/
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
							findScripts();
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else if (button == 1) {
						showDialog(DIALOG_PATCH_INFO);
					} else if (button == 2) {
						togglePatch(selectedPatchItem);
						findScripts();
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

	private AlertDialog createImportSourcesDialog() {
		CharSequence[] options = {"Import local file"};
		return new AlertDialog.Builder(this).setTitle("Import bla bla bla").
			setItems(options, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialogI, int button) {
					if (button == 0) {
						//selectedPatchItem.file.delete();
						importPatchFromFile();
					}
				}
			}).create();
	}

	private boolean isValidPatch(ScriptListItem patch) {
		if (patch.file.length() < 1) {
			return false;
		}
		return true;
	}

	private final class FindScriptsThread implements Runnable {

		public void run() {
			File patchesFolder = getDir(SCRIPTS_DIR, 0);

			final List<ScriptListItem> patches = new ArrayList<ScriptListItem>();

			combOneFolder(patchesFolder, patches);

			ManageScriptsActivity.this.runOnUiThread(new Runnable() {
				public void run() {
					ManageScriptsActivity.this.receiveScripts(patches);
				}
			});
		}

		private void combOneFolder(File patchesFolder, List<ScriptListItem> patches) {
			if (!patchesFolder.exists()) {
				System.err.println("no storage folder");
			} else {
				for (File patchFile : patchesFolder.listFiles()) {
					boolean patchEnabled = ScriptManager.isEnabled(patchFile);
					patches.add(new ScriptListItem(patchFile, patchEnabled));
				}
			}
		}

	}

	private final class ScriptListItem {
		public final File file;
		public final String displayName;
		public boolean enabled = true;
		public ScriptListItem(File file, boolean enabled) {
			this.file = file;
			this.displayName = file.getName();
			this.enabled = enabled;
		}

		public String toString() {
			return displayName + (enabled? enabledString: disabledString);
		}
	}

	private final class PatchListComparator implements Comparator<ScriptListItem> {
		public int compare(ScriptListItem a, ScriptListItem b) {
			//System.out.println(a.toString() + ":" + b.toString() + (a.levelDat.lastModified() - b.levelDat.lastModified()));
			return a.displayName.toLowerCase().compareTo(b.displayName.toLowerCase());
		}
		public boolean equals(ScriptListItem a, ScriptListItem b) {
			return a.displayName.toLowerCase().equals(b.displayName.toLowerCase());
		}
	}

}
