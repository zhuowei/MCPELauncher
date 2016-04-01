package net.zhuoweizhang.mcpelauncher.ui;

import java.io.*;
import java.util.*;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
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
	private ImageButton addButton;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		try {
			list = TexturePackLoader.loadDescriptionsWithIcons(this);
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

		addButton = (ImageButton) findViewById(R.id.manage_textures_select);
		addButton.setOnClickListener(this);
	}

	public void onClick(View v) {
		if (v == addButton) {
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
		Utils.getPrefs(0).edit().putBoolean("zz_texture_pack_enable", true).apply();
		for (TexturePackDescription d: list) {
			if (d.path.equals(desc.path)) return;
		}
		if (desc.img == null) {
			try {
				TexturePackLoader.loadIconForDescription(desc);
			} catch (Exception e) {
				e.printStackTrace();
			}
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

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.manage_textures_menu, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.manage_textures_extract) {
			new ExtractTextureTask().execute();
			return true;
		} else if (item.getItemId() == R.id.manage_textures_clear_script_texture_overrides) {
			ScriptManager.clearTextureOverrides();
			Toast.makeText(this, R.string.textures_clear_script_texture_overrides,
					Toast.LENGTH_SHORT).show();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
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
			View buttonParent = v.findViewById(R.id.texture_entry_container);
			buttonParent.setTag(position);
			TextView text = (TextView) v.findViewById(R.id.texture_entry_name);
			text.setText(TexturePackLoader.describeTexturePack(TexturePacksActivity.this, item));
			TextView desc = (TextView) v.findViewById(R.id.texture_entry_desc);
			desc.setText(item.description);
			desc.setVisibility(item.description.length() == 0? View.GONE: View.VISIBLE);
			View up = v.findViewById(R.id.texture_entry_up);
			up.setEnabled(position != 0);
			View down = v.findViewById(R.id.texture_entry_down);
			down.setEnabled(position != this.getCount() - 1);
			ImageView img = (ImageView) v.findViewById(R.id.texture_entry_img);
			img.setImageDrawable(item.img != null? new BitmapDrawable(item.img) : null);
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
			outFile.delete();
			String outPath = outFile.getAbsolutePath().replace(
				Environment.getExternalStorageDirectory().getAbsolutePath(), "/sdcard");
			System.out.println(outPath);
			List<String> suResult = Shell.SU.run("cat \"" + mcpeApkLoc + "\" >\""
					+ outPath + "\"");
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
