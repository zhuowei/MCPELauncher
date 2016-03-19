package net.zhuoweizhang.mcpelauncher.ui;

import java.io.*;
import java.util.List;
import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.*;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.View;
import static android.widget.AdapterView.OnItemClickListener;
import android.widget.*;

import com.ipaulpro.afilechooser.FileChooserActivity;
import com.ipaulpro.afilechooser.utils.FileUtils;

import static net.zhuoweizhang.mcpelauncher.ui.LauncherActivity.PT_PATCHES_DIR;
import net.zhuoweizhang.mcpelauncher.MinecraftVersion;
import net.zhuoweizhang.mcpelauncher.PatchManager;
import net.zhuoweizhang.mcpelauncher.R;
import net.zhuoweizhang.mcpelauncher.Utils;
import net.zhuoweizhang.mcpelauncher.patch.*;
import net.zhuoweizhang.mcpelauncher.ui.RefreshContentListThread.OnRefreshContentList;

import com.mojang.minecraftpe.*;

@SuppressWarnings("deprecation")
public class ManagePatchesActivity extends ListActivity implements View.OnClickListener,
		OnRefreshContentList {

	private static final int DIALOG_MANAGE_PATCH = 1;
	private static final int DIALOG_MANAGE_PATCH_CURRENTLY_DISABLED = 2;
	private static final int DIALOG_MANAGE_PATCH_CURRENTLY_ENABLED = 3;
	private static final int DIALOG_PATCH_INFO = 4;

	private static final int REQUEST_IMPORT_PATCH = 212;

	private Thread refreshThread;

	private List<ContentListItem> patches;

	private ContentListItem selectedPatchItem;

	private ImageButton importButton;

	private byte[] libBytes = null;

	private boolean prePatchConfigure = true;

	protected CompoundButton master = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Utils.setLanguageOverride();
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
		importButton = (ImageButton) findViewById(R.id.manage_patches_import_button);
		importButton.setOnClickListener(this);
		prePatchConfigure = getIntent().getBooleanExtra("prePatchConfigure", true);
		PatchUtils.minecraftVersion = MinecraftVersion.get(this);
	}

	@Override
	protected void onStart() {
		super.onStart();
		findPatches();
	}

	@Override
	protected void onPause() {
		super.onPause();
		libBytes = null;
	}

	public void onClick(View v) {
		if (v.equals(importButton)) {
			importPatch();
		}
	}

	public void importPatch() {
		Intent target = FileUtils.createGetContentIntent();
		target.setType("application/x-ptpatch");
		target.setClass(this, FileChooserActivity.class);
		target.putExtra(FileUtils.EXTRA_SORT_METHOD, FileUtils.SORT_LAST_MODIFIED);
		startActivityForResult(target, REQUEST_IMPORT_PATCH);
	}

	/**
	 * gets the maximum number of patches this application can patch. A negative
	 * value indicates unlimited amount.
	 */

	protected void setPatchListModified() {
		setResult(RESULT_OK);
		Utils.getPrefs(1).edit().putBoolean("force_prepatch", true).apply();
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
					if (Utils.hasTooManyPatches()) {
						Toast.makeText(this, R.string.manage_patches_too_many, Toast.LENGTH_SHORT)
								.show();
					} else {
						PatchManager.getPatchManager(this).setEnabled(to, true);
						afterPatchToggle(new ContentListItem(to, true));
					}
					setPatchListModified();
					findPatches();
				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(this, R.string.manage_patches_import_error, Toast.LENGTH_LONG)
							.show();
				}
			}
			break;
		}
	}

	private void findPatches() {
		refreshThread = new Thread(new RefreshContentListThread(this, this));
		refreshThread.start();
	}

	private void openManagePatchWindow(ContentListItem item) {
		this.selectedPatchItem = item;
		if (prePatchConfigure || canLivePatch(item)) {
			showDialog(item.enabled ? DIALOG_MANAGE_PATCH_CURRENTLY_ENABLED
					: DIALOG_MANAGE_PATCH_CURRENTLY_DISABLED);
		} else {
			Toast.makeText(this, "This patch cannot be disabled in game.", Toast.LENGTH_SHORT)
					.show();
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
			aDialog.setTitle(selectedPatchItem.toString(getResources()));
			break;
		case DIALOG_PATCH_INFO:
			preparePatchInfo((AlertDialog) dialog, selectedPatchItem);
			break;
		default:
			super.onPrepareDialog(dialogId, dialog);
		}
	}

	public void togglePatch(ContentListItem patch) {
		if (!patch.enabled && Utils.hasTooManyPatches()) {
			Toast.makeText(this, R.string.manage_patches_too_many, Toast.LENGTH_SHORT).show();
			return;
		}
		PatchManager.getPatchManager(this).setEnabled(patch.file, !patch.enabled);
		patch.enabled = !patch.enabled;
		afterPatchToggle(patch);
	}

	private void afterPatchToggle(ContentListItem patch) {
		if (!isValidPatch(patch)) {
			PatchManager.getPatchManager(this).setEnabled(patch.file, false);
			new AlertDialog.Builder(this)
					.setMessage(
							getResources().getString(R.string.manage_patches_invalid_patches) + " "
									+ patch.displayName)
					.setPositiveButton(android.R.string.ok, null).show();
			return;
		}
		if (prePatchConfigure) {
			setPatchListModified(); // should really be called requestPrePatch
		} else if (canLivePatch(patch)) {
			try {
				livePatch(patch);
				Utils.getPrefs(1).edit().putBoolean("force_prepatch", true).apply();
				/* defer the prepatch until it is needed next time */
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			setPatchListModified(); // should really be called requestPrePatch
		}
	}

	public void livePatch(ContentListItem patchItem) throws Exception {
		ApplicationInfo mcAppInfo = getPackageManager().getApplicationInfo(
				"com.mojang.minecraftpe", 0);
		File patched = getDir("patched", 0);
		File originalLibminecraft = new File(mcAppInfo.nativeLibraryDir + "/libminecraftpe.so");
		new File(patched, "libminecraftpe.so");
		com.joshuahuelsman.patchtool.PTPatch patch = new com.joshuahuelsman.patchtool.PTPatch();
		patch.loadPatch(patchItem.file);
		boolean enabled = patchItem.enabled;
		if (enabled) {
			// patch.applyPatch(MainActivity.minecraftLibBuffer);
			PatchUtils.patch(MainActivity.minecraftLibBuffer, patch);
		} else {
			if (libBytes == null) {
				libBytes = new byte[(int) originalLibminecraft.length()];

				InputStream is = new FileInputStream(originalLibminecraft);
				is.read(libBytes);
				is.close();
			}

			PatchUtils.unpatch(MainActivity.minecraftLibBuffer, libBytes, patch);
		}
	}

	public boolean canLivePatch(ContentListItem patch) {
		try {
			return PatchUtils.canLivePatch(patch.file);
		} catch (Exception e) {
			return false;
		}
	}

	public void deletePatch(ContentListItem patch) throws Exception {
		patch.enabled = false;
		if (!prePatchConfigure) {
			livePatch(patch);
			Utils.getPrefs(1).edit().putBoolean("force_prepatch", true).apply();
		}
		setPatchListModified();
		patch.file.delete();
	}

	public void preparePatchInfo(AlertDialog dialog, ContentListItem patch) {
		dialog.setTitle(patch.toString(getResources()));
		String patchInfo;
		try {
			patchInfo = getPatchInfo(patch);
		} catch (Exception e) {
			patchInfo = "Cannot show info: " + e.getStackTrace();
		}
		dialog.setMessage(patchInfo);
	}

	private String getPatchInfo(ContentListItem patchItem) throws IOException {
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
	 * @param enableStatus
	 *            -1 = can't disable, 0 = currently disabled, 1 = currently
	 *            enabled
	 */

	protected AlertDialog createManagePatchDialog(int enableStatus) {
		CharSequence patchInfoStr = this.getResources().getText(R.string.manage_patches_info);
		CharSequence[] options = null;
		if (enableStatus == -1) {
			options = new CharSequence[] {
					this.getResources().getText(R.string.manage_patches_delete), patchInfoStr };
		} else {
			options = new CharSequence[] {
					this.getResources().getText(R.string.manage_patches_delete),
					patchInfoStr,
					(enableStatus == 0 ? this.getResources()
							.getText(R.string.manage_patches_enable) : this.getResources().getText(
							R.string.manage_patches_disable)) };
		}

		return new AlertDialog.Builder(this).setTitle("Patch name goes here")
				.setItems(options, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialogI, int button) {
						if (button == 0) {
							// selectedPatchItem.file.delete();
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
		return new AlertDialog.Builder(this).setTitle("Whoops! info fail")
				// will get filled in by prepare
				.setMessage("Whoops - try again, this is a tiny fail")
				.setPositiveButton(android.R.string.ok, null).create();
	}

	private boolean isValidPatch(ContentListItem patch) {
		if (patch.file.length() < 6) {
			return false;
		}
		return true;
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
							findPatches();
						} else {
							((ArrayAdapter<?>) getListAdapter()).clear();
						}
						SharedPreferences.Editor sh = Utils.getPrefs(0).edit();
						sh.putBoolean("zz_manage_patches", isChecked);
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

	protected void refreshABToggle() {
		if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) && (master != null)) {
			master.setChecked(Utils.getPrefs(0).getBoolean("zz_manage_patches", true));
		}
	}

	@Override
	public void onRefreshComplete(final List<ContentListItem> items) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				ContentListItem.sort(items);
				ManagePatchesActivity.this.patches = items;
				ArrayAdapter<ContentListItem> adapter = new ContentListAdapter(
						ManagePatchesActivity.this, R.layout.patch_list_item, patches);
				setListAdapter(adapter);
				List<String> allPaths = new ArrayList<String>(patches.size());
				for (ContentListItem i : patches) {
					String name = i.file.getAbsolutePath();
					allPaths.add(name);
				}
				PatchManager.getPatchManager(ManagePatchesActivity.this)
						.removeDeadEntries(allPaths);
			}
		});
	}

	@Override
	public List<File> getFolders() {
		List<File> folders = new ArrayList<File>();
		folders.add(getDir(PT_PATCHES_DIR, 0));
		folders.add(new File(Environment.getExternalStorageDirectory(),
				"Android/data/com.snowbound.pockettool.free/Patches"));
		folders.add(new File(Environment.getExternalStorageDirectory(),
				"Android/data/com.joshuahuelsman.pockettool/Patches"));
		return folders;
	}

	@Override
	public boolean isEnabled(File f) {
		PatchManager patchMgr = PatchManager.getPatchManager(ManagePatchesActivity.this);
		return patchMgr.isEnabled(f);
	}

}
