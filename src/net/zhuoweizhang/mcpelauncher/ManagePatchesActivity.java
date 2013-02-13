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

public class ManagePatchesActivity extends ListActivity implements View.OnClickListener {

	private static final int DIALOG_MANAGE_PATCH = 1;

	private static final int REQUEST_IMPORT_PATCH = 212;

	private FindPatchesThread findPatchesThread;

	private List<PatchListItem> patches;

	private PatchListItem selectedPatchItem;

	private Button importButton;

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
	}

	@Override
	protected void onStart() {
		super.onStart();
		findPatches();
	}

	public void onClick(View v) {
		if (v == importButton) {
			importPatch();
		}
	}

	public void importPatch() {
		int maxPatchCount = getIntent().getIntExtra("maxNumPatches", 3);
		if (maxPatchCount >= 0 && patches.size() >= maxPatchCount) {
			Toast.makeText(this, R.string.manage_patches_too_many, Toast.LENGTH_SHORT).show();
			return;
		}
		Intent target = FileUtils.createGetContentIntent();
		target.setType("application/x-ptpatch");
		target.setClass(this, FileChooserActivity.class);
		startActivityForResult(target, REQUEST_IMPORT_PATCH);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_IMPORT_PATCH:  
				if (resultCode == RESULT_OK) {  
					final Uri uri = data.getData();
					File file = FileUtils.getFile(uri);
					try {
						PatchUtils.copy(file, new File(getDir(PT_PATCHES_DIR, 0), file.getName()));
						setResult(RESULT_OK);
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
	}

	private void openManagePatchWindow(PatchListItem item) {
		this.selectedPatchItem = item;
		showDialog(DIALOG_MANAGE_PATCH);
	}

	public Dialog onCreateDialog(int dialogId) {
		switch (dialogId) {
			case DIALOG_MANAGE_PATCH:
				return createManagePatchDialog();
			default:
				return super.onCreateDialog(dialogId);
		}
	}

	public void onPrepareDialog(int dialogId, Dialog dialog) {
		switch (dialogId) {
			case DIALOG_MANAGE_PATCH:
				AlertDialog aDialog = (AlertDialog) dialog;
				aDialog.setTitle(selectedPatchItem.toString());
				break;
			default:
				super.onPrepareDialog(dialogId, dialog);
		}
	}

	protected AlertDialog createManagePatchDialog() {
		CharSequence[] options = {this.getResources().getText(R.string.manage_patches_delete)};
		return new AlertDialog.Builder(this).setTitle("Patch name goes here").
			setItems(options, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialogI, int button) {
					if (button == 0) {
						selectedPatchItem.file.delete();
						setResult(RESULT_OK);
						findPatches();
					}
				}
			}).create();
	}

	private final class FindPatchesThread implements Runnable {

		public void run() {
			File patchesFolder = getDir(PT_PATCHES_DIR, 0);

			final List<PatchListItem> patches = new ArrayList<PatchListItem>();

			if (!patchesFolder.exists()) {
				System.err.println("no storage folder");
			} else {
				for (File patchFile : patchesFolder.listFiles()) {
					patches.add(new PatchListItem(patchFile));
				}
			}
			ManagePatchesActivity.this.runOnUiThread(new Runnable() {
				public void run() {
					ManagePatchesActivity.this.receivePatches(patches);
				}
			});
		}

	}

	private final class PatchListItem {
		public final File file;
		public final String displayName;
		public PatchListItem(File file) {
			this.file = file;
			this.displayName = file.getName();
		}

		public String toString() {
			return displayName;
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
