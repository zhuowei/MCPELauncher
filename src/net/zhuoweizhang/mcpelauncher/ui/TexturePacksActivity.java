package net.zhuoweizhang.mcpelauncher.ui;

import java.io.*;
import java.util.*;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import net.zhuoweizhang.mcpelauncher.*;
import net.zhuoweizhang.mcpelauncher.texture.*;

import com.ipaulpro.afilechooser.FileChooserActivity;
import com.ipaulpro.afilechooser.utils.FileUtils;

import eu.chainfire.libsuperuser.Shell;

public class TexturePacksActivity extends ListActivity implements View.OnClickListener {

	private static final int REQUEST_ADD_TEXTURE = 522;

	private List<TexturePackDescription> list;
	private ArrayAdapter<TexturePackDescription> adapter;
	private Button addButton, extractButton;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		try {
			list = TexturePackLoader.loadDescriptions(this);
		} catch (Exception e) {
			e.printStackTrace();
			list = new ArrayList<TexturePackDescription>();
		}
		if (new File("/sdcard/bl_clearTextures").exists()) {
			list = new ArrayList<TexturePackDescription>();
		}
		setContentView(R.layout.manage_textures);
		adapter = new TexturesAdapter(this);
		this.setListAdapter(adapter);

		extractButton = fb(R.id.manage_textures_extract);
		addButton = fb(R.id.manage_textures_select);
	}

	private Button fb(int id) {
		Button b = (Button) findViewById(id);
		b.setOnClickListener(this);
		return b;
	}

	public void onClick(View v) {
		if (v == extractButton) {
			new ExtractTextureTask().execute();
		} else if (v == addButton) {
			Intent target = FileUtils.createGetContentIntent();
			target.setType("application/zip");
			target.putExtra(FileUtils.EXTRA_MIME_TYPES, new String[] {
				"application/zip", "application/x-appx", "application/vnd.android.package-archive"});
			target.setClass(this, FileChooserActivity.class);
			startActivityForResult(target, REQUEST_ADD_TEXTURE);
		}
	}

	private void saveState() {
		try {
			TexturePackLoader.saveDescriptions(this, list);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void addTexturePack(int index, File f) {
		// get the absolute path
		String path = f.getAbsolutePath();
		TexturePackDescription desc = new TexturePackDescription(TexturePackLoader.TYPE_ZIP, path);
		addTexturePack(index, desc);
	}

	private void addTexturePack(int index, TexturePackDescription desc) {
		for (TexturePackDescription d: list) {
			if (d.path.equals(desc.path)) return;
		}
		list.add(index, desc);
		updateContents();
	}

	private void updateContents() {
		adapter.notifyDataSetChanged();
		saveState();
		setResult(RESULT_OK);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_ADD_TEXTURE && resultCode == RESULT_OK) {
			final Uri uri = data.getData();
			File file = FileUtils.getFile(uri);
			addTexturePack(0, file);
		}
	}

	public void onTextureUpClick(View v) {
		int index = (Integer) ((View) v.getParent()).getTag();
		TexturePackDescription desc = list.remove(index);
		list.add(index - 1, desc);
		updateContents();
	}

	public void onTextureDownClick(View v) {
		int index = (Integer) ((View) v.getParent()).getTag();
		TexturePackDescription desc = list.remove(index);
		list.add(index + 1, desc);
		updateContents();
	}

	public void onTextureRemoveClick(View v) {
		int index = (Integer) ((View) v.getParent()).getTag();
		TexturePackDescription desc = list.remove(index);
		updateContents();
	}

	private class TexturesAdapter extends ArrayAdapter<TexturePackDescription> {
		private LayoutInflater inflater;

		public TexturesAdapter(Context context) {
			super(context, R.layout.texture_pack_entry, R.id.texture_entry_name, list);
			inflater = LayoutInflater.from(context);
		}

		@Override
		public View getView(int position, View v, ViewGroup parent) {
			if (v == null) {
				v = inflater.inflate(R.layout.texture_pack_entry, parent, false);
			}
			TexturePackDescription item = getItem(position);
			v.setTag(position);
			TextView text = (TextView) v.findViewById(R.id.texture_entry_name);
			text.setText(TexturePackLoader.describeTexturePack(TexturePacksActivity.this, item));
			Button up = (Button) v.findViewById(R.id.texture_entry_up);
			up.setEnabled(position != 0);
			Button down = (Button) v.findViewById(R.id.texture_entry_down);
			down.setEnabled(position != this.getCount() - 1);
			return v;
		}
	}

	private class ExtractTextureTask extends AsyncTask<Void, Void, Void> {
		private ProgressDialog dialog;
		private String mcpeApkLoc;
		private File outFile;
		private boolean hasSu = true;

		@Override
		protected void onPreExecute() {
			dialog = new ProgressDialog(TexturePacksActivity.this);
			dialog.setMessage(getResources().getString(R.string.extracting_textures));
			dialog.setIndeterminate(true);
			dialog.setCancelable(false);
			dialog.show();
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				ApplicationInfo appInfo = getPackageManager().getApplicationInfo(
						"com.mojang.minecraftpe", 0);
				mcpeApkLoc = appInfo.sourceDir;
			} catch (PackageManager.NameNotFoundException impossible) {
			}
			outFile = new File(getExternalFilesDir(null), "minecraft.apk");
			List<String> suResult = Shell.SU.run("cat \"" + mcpeApkLoc + "\" >\""
					+ outFile.getAbsolutePath() + "\"");
			if (suResult == null) {
				hasSu = false;
			}

			ScriptManager.clearTextureOverrides();

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			dialog.dismiss();
			if (outFile.exists()) {
				addTexturePack(list.size(), outFile);
				Toast.makeText(TexturePacksActivity.this, R.string.extract_textures_success,
						Toast.LENGTH_SHORT).show();
			} else {
				new AlertDialog.Builder(TexturePacksActivity.this)
						.setMessage(hasSu ? R.string.extract_textures_error
										: R.string.extract_textures_no_root)
						.setPositiveButton(android.R.string.ok, null).show();
			}
		}
	}

}
