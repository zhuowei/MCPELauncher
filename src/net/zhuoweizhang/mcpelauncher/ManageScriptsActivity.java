package net.zhuoweizhang.mcpelauncher;

import java.io.*;

import java.net.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.pm.*;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import static android.widget.AdapterView.OnItemClickListener;
import android.widget.*;
import android.util.Base64;

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
	private static final int DIALOG_IMPORT_FROM_CFGY = 6;
	private static final int DIALOG_IMPORT_FROM_URL = 7;
	private static final int DIALOG_VERSION_INCOMPATIBLE = 8;

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
		if (!versionIsSupported()) {
			showDialog(DIALOG_VERSION_INCOMPATIBLE);
		}
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
		return this.getResources().getInteger(R.integer.max_num_scripts);
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
							Toast.makeText(this, R.string.script_import_too_many, Toast.LENGTH_SHORT).show();
						} else {
							ScriptManager.setEnabled(to, true);
							afterPatchToggle(new ScriptListItem(to, true));
						}
						setPatchListModified();
						findScripts();
					} catch (Exception e) {
						e.printStackTrace();
						reportError(e);
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
			case DIALOG_IMPORT_FROM_CFGY:
				return createImportFromCfgyDialog();
			case DIALOG_IMPORT_FROM_URL:
				return createImportFromUrlDialog();
			case DIALOG_VERSION_INCOMPATIBLE:
				return createVersionIncompatibleDialog();
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
			Toast.makeText(this, R.string.script_import_too_many, Toast.LENGTH_SHORT).show();
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
		Resources res = this.getResources();
		CharSequence[] options = {res.getString(R.string.script_import_from_local), res.getString(R.string.script_import_from_cfgy), 
			res.getString(R.string.script_import_from_url)};
		return new AlertDialog.Builder(this).setTitle(R.string.script_import_from).
			setItems(options, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialogI, int button) {
					if (button == 0) {
						//selectedPatchItem.file.delete();
						importPatchFromFile();
					} else if (button == 1) {
						showDialog(DIALOG_IMPORT_FROM_CFGY);
					} else if (button == 2) {
						showDialog(DIALOG_IMPORT_FROM_URL);
					}
				}
			}).create();
	}

	private AlertDialog createImportFromCfgyDialog() {
		final EditText view = new EditText(this);
		return new AlertDialog.Builder(this).setTitle(R.string.script_import_from_cfgy_id).
			setView(view).
			setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialogI, int button) {
					importFromCfgy(view.getText().toString());
				}
			}).
			setNegativeButton(android.R.string.cancel, null).
			create();
	}

	private AlertDialog createImportFromUrlDialog() {
		final EditText view = new EditText(this);
		return new AlertDialog.Builder(this).setTitle(R.string.script_import_from_url_enter).
			setView(view).
			setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialogI, int button) {
					importFromUrl(view.getText().toString());
				}
			}).
			setNegativeButton(android.R.string.cancel, null).
			create();
	}

	private AlertDialog createVersionIncompatibleDialog() {
		return new AlertDialog.Builder(this).setMessage(R.string.script_minecraft_version_incompatible).
			setPositiveButton(android.R.string.ok, null).
			create();
	}

	private boolean isValidPatch(ScriptListItem patch) {
		if (patch.file.length() < 1) {
			return false;
		}
		return true;
	}

	private void importFromCfgy(String id) {
		ImportScriptFromCfgyTask task = new ImportScriptFromCfgyTask();
		task.execute(id);
	}

	//the following method and array is adapted from http://modpe.cf.gy/modEditor.html
	/**
	 * Method to convert a Cfgy ID to a filename for downloading scripts off Treebl's server.
	 * Each cfgy ID is a base 36 number.
	 * Each cfgy number is the base10 representation of the ID with replaced digits. You know, because logic.
	 */
	private static String cfgyIdToFilename(String str) {
		int cfgyID = Integer.parseInt(str, 36);
		char[] cfgyFilename = Integer.toString(cfgyID).toCharArray();
		for (int i = 0; i < cfgyFilename.length; i++) {
			cfgyFilename[i] = cfgyMappings[(char) ((int)cfgyFilename[i] - 48)];
		}

		String retval = new String(cfgyFilename);
		System.out.println(retval);
		return retval;

	}

	/** The mapping table used to map between normal digits and Treebl digits. */
	private static char[] cfgyMappings = new char[] {'f', 't', 'a', 'm', 'b', 'q', 'g', 'r', 'z', 'o'};

	//end adapted
	private void importFromUrl(String url) {
		ImportScriptFromUrlTask task = new ImportScriptFromUrlTask();
		task.execute(url);
	}

	private void reportError(final Throwable t) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				StringWriter strWriter = new StringWriter();
				PrintWriter pWriter = new PrintWriter(strWriter);
				t.printStackTrace(pWriter);
				new AlertDialog.Builder(ManageScriptsActivity.this).setTitle(R.string.manage_patches_import_error).setMessage(strWriter.toString()).
					setPositiveButton(android.R.string.ok, null).
					show();
			}
		});
	}

	private boolean versionIsSupported() {
		try {
			return getPackageManager().getPackageInfo("com.mojang.minecraftpe", 0).versionCode == MinecraftConstants.MINECRAFT_VERSION_CODE;
		} catch (PackageManager.NameNotFoundException ex) {
			return false; //Not possible
		}
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

	private abstract class ImportScriptTask extends AsyncTask<String, Void, File> {

		protected void onPostExecute(File file) {
			if (file == null) {
				Toast.makeText(ManageScriptsActivity.this, R.string.manage_patches_import_error, Toast.LENGTH_SHORT).show();
				return;
			}
			try {
				ScriptManager.setEnabled(file, false);
				int maxPatchCount = getMaxPatchCount();
				if (maxPatchCount >= 0 && getEnabledCount() >= maxPatchCount) {
					Toast.makeText(ManageScriptsActivity.this, R.string.script_import_too_many, Toast.LENGTH_SHORT).show();
				} else {
					ScriptManager.setEnabled(file, true);
				}
			} catch (Exception e) {
				e.printStackTrace();
				reportError(e);
				Toast.makeText(ManageScriptsActivity.this, R.string.manage_patches_import_error, Toast.LENGTH_SHORT).show();
			}
			findScripts();
		}
		
	}

	/** Import a script from Treebl's repo. */
	private class ImportScriptFromCfgyTask extends ImportScriptTask {
		protected File doInBackground(String... ids) {
			String id = ids[0];

			InputStream is = null;
			byte[] content = null;
			int response = 0;
			FileOutputStream fos = null;
			File file = new File(getDir(SCRIPTS_DIR, 0), id + ".js");
			HttpURLConnection conn;
			URL url;

			try {
				//Hurl the file to a byte array
				url = new URL("http://modpe.cf.gy/mods/" + cfgyIdToFilename(id) + ".js");
				conn = (HttpURLConnection) url.openConnection();
				conn.setRequestProperty("User-Agent", "BlockLauncher");
				conn.setDoInput(true);
				conn.connect();
				try {
					response = conn.getResponseCode();
					is = conn.getInputStream();
				} catch (Exception e) {
					is = conn.getErrorStream();
				}

				if (response >= 400) return null;

				if (is != null) {
					content = bytesFromInputStream(is, conn.getContentLength() > 0 ? conn.getContentLength() : 1024);
				}
				String contentStr = new String(content).replaceAll(" ", "+"); //WTF, Treebl.
				//now we have the bytes, let's base64-decode it (Why, treebl?) then write it to a file
				byte[] decoded = Base64.decode(contentStr, Base64.DEFAULT);
				fos = new FileOutputStream(file);
				fos.write(decoded);
				fos.flush();
				return file;
			} catch (Exception e) {
				e.printStackTrace();
				reportError(e);
				return null;
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (Exception e) {
					}
				}
				if (fos != null) {
					try {
						fos.close();
					} catch (Exception e) {
					}
				}
			}
			
		}
		
	}

	/** Import a script from a url. */
	private class ImportScriptFromUrlTask extends ImportScriptTask {
		protected File doInBackground(String... ids) {

			InputStream is = null;
			byte[] content = null;
			int response = 0;
			FileOutputStream fos = null;
			File file = null;
			HttpURLConnection conn;
			URL url;

			try {
				//Hurl the file to a byte array
				url = new URL(ids[0]);
				System.out.println(url);
				String urlPath = url.getPath();
				String fileName = urlPath.substring(urlPath.lastIndexOf("/") + 1);
				if (fileName.length() < 1) fileName = "nameless_script.js";
				file = new File(getDir(SCRIPTS_DIR, 0), fileName);
				conn = (HttpURLConnection) url.openConnection();
				conn.setRequestProperty("User-Agent", "BlockLauncher");
				conn.setDoInput(true);
				conn.connect();
				try {
					response = conn.getResponseCode();
					is = conn.getInputStream();
				} catch (Exception e) {
					is = conn.getErrorStream();
				}

				if (response >= 400) return null;
				if (is == null) return null;
				content = bytesFromInputStream(is, conn.getContentLength() > 0 ? conn.getContentLength() : 1024);
				//no processing done on URL scripts
				fos = new FileOutputStream(file);
				fos.write(content);
				fos.flush();
				return file;
			} catch (Exception e) {
				reportError(e);
				e.printStackTrace();
				return null;
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (Exception e) {
					}
				}
				if (fos != null) {
					try {
						fos.close();
					} catch (Exception e) {
					}
				}
			}
			
		}

	}

	private static byte[] bytesFromInputStream(InputStream in, int startingLength) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream(startingLength);
		try {
			byte[] buffer = new byte[1024];
			int count;
			while ((count = in.read(buffer)) != -1) {
				bytes.write(buffer, 0, count);
			}
			return bytes.toByteArray();
		} finally {
			bytes.close();
		}
	}

}
