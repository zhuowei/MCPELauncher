package net.zhuoweizhang.mcpelauncher.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.ipaulpro.afilechooser.FileChooserActivity;
import com.ipaulpro.afilechooser.utils.FileUtils;

import eu.chainfire.libsuperuser.Shell;
import net.zhuoweizhang.mcpelauncher.R;
import net.zhuoweizhang.mcpelauncher.Utils;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import net.zhuoweizhang.mcpelauncher.ScriptManager;

public class ManageTexturepacksActivity extends ListActivity {
	protected TexturesAdapter adapter;
	public static final File REQUEST_DEMO = new File("/demo/textures");
	public static final File REQUEST_ENABLE = new File("/just/enable/textures");
	public static final File REQUEST_DISABLE = new File("/just/disable/textures");
	protected CompoundButton master = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.manage_textures);
		adapter = new TexturesAdapter(this);
		setListAdapter(adapter);
		getListView().setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				File f = adapter.getItem(position);
				setTexturepack(f);
				finish();
			}
		});
		((Button) findViewById(R.id.manage_textures_select))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent target = FileUtils.createGetContentIntent();
						target.setType("application/zip");
						target.setClass(ManageTexturepacksActivity.this, FileChooserActivity.class);
						startActivityForResult(target,
								MainMenuOptionsActivity.REQUEST_MANAGE_TEXTURES);
					}
				});
		((Button) findViewById(R.id.manage_textures_extract))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						new ExtractTextureTask().execute();
					}
				});
		setResult(RESULT_CANCELED);
		loadHistory();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if ((requestCode == MainMenuOptionsActivity.REQUEST_MANAGE_TEXTURES)
				&& (resultCode == RESULT_OK)) {
			final Uri uri = data.getData();
			File file = FileUtils.getFile(uri);
			adapter.add(file);
			adapter.notifyDataSetChanged();
			setTexturepack(file);
			finish();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshABToggle();
	}

	@Override
	protected void onPause() {
		super.onPause();
		saveHistory();
		refreshABToggle();
	}

	public void loadHistory() {
		adapter.clear();
		if (isEnabled()) {
			if (canAccessMCPE())
				adapter.add(REQUEST_DEMO);
			SharedPreferences sh = Utils.getPrefs(0);
			String data = sh.getString("textures_history", "");
			for (String s : data.split(";")) {
				File f = new File(s);
				if (!f.exists() || !f.canRead())
					continue;
				adapter.add(f);
			}
			adapter.notifyDataSetChanged();
		}
	}

	public void saveHistory() {
		if (isEnabled()) {
			String out = "";
			List<String> res = new ArrayList<String>();
			for (int l = 0; l < adapter.getCount(); l++) {
				File f = adapter.getItem(l);
				if (f.exists() && f.canRead())
					res.add(f.getAbsolutePath());
			}
			out = Utils.join(res, ";");
			SharedPreferences.Editor sh = Utils.getPrefs(0).edit();
			sh.putString("textures_history", out);
			sh.apply();
		}
	}

	protected boolean isEnabled() {
		SharedPreferences sh = Utils.getPrefs(0);
		return sh.getBoolean("zz_texture_pack_enable", false);
	}

	public static void setTexturepack(File f, ManageTexturepacksActivity activity) {
		SharedPreferences.Editor p1 = Utils.getPrefs(0).edit();
		SharedPreferences.Editor p2 = Utils.getPrefs(1).edit();
		if (f.getAbsolutePath().equalsIgnoreCase(REQUEST_DISABLE.getAbsolutePath())) {
			// Disable texturepacks
			p1.putBoolean("zz_texture_pack_enable", false);
			p1.putBoolean("zz_texture_pack_demo", false);
		} else if (f.getAbsolutePath().equalsIgnoreCase(REQUEST_ENABLE.getAbsolutePath())) {
			// Enable texturepacks and set to demo
			p1.putBoolean("zz_texture_pack_enable", true);
			if (Utils.getPrefs(1).getString("texturePack", "no_pack").equals("no_pack")) {
				p1.putBoolean("zz_texture_pack_demo", true);
			}
		} else if (f.getAbsolutePath().equalsIgnoreCase(REQUEST_DEMO.getAbsolutePath())) {
			p1.putBoolean("zz_texture_pack_enable", true);
			// Demo
			p1.putBoolean("zz_texture_pack_demo", true);
			// Don't forget to reset the opposite value!
			p2.putString("texturePack", null);
		} else {
			p1.putBoolean("zz_texture_pack_enable", true);
			// Usual
			p2.putString("texturePack", f.getAbsolutePath());
			// Don't forget to reset the opposite value!
			p1.putBoolean("zz_texture_pack_demo", false);
		}
		p1.apply();
		p2.apply();
		if (activity != null) {
			activity.refreshABToggle();
			activity.setResult(RESULT_OK);
		}
	}

	protected void setTexturepack(File f) {
		setTexturepack(f, this);
	}

	CompoundButton.OnCheckedChangeListener ls = new CompoundButton.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (isChecked) {
				setTexturepack(REQUEST_ENABLE);
			} else {
				setTexturepack(REQUEST_DISABLE);
			}
			loadHistory();
		}
	};

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getMenuInflater().inflate(R.menu.ab_master, menu);
			master = (CompoundButton) menu.findItem(R.id.ab_switch_container).getActionView()
					.findViewById(R.id.ab_switch);
			if (master != null) {
				master.setOnCheckedChangeListener(ls);
				refreshABToggle();
			} else {
				System.err.println("WTF?");
			}
		}
		menu.add(getResources().getString(R.string.textures_clear_script_texture_overrides));
		return true;
	}

	protected void refreshABToggle() {
		if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) && (master != null)) {
			master.setOnCheckedChangeListener(null);
			master.setChecked(isEnabled());
			master.setOnCheckedChangeListener(ls);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		if (item.getTitle().equals(getResources().getString(R.string.textures_clear_script_texture_overrides))) {
			ScriptManager.clearTextureOverrides();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	protected boolean canAccessMCPE() {
		try {
			ApplicationInfo mcAppInfo = getPackageManager().getApplicationInfo(
					"com.mojang.minecraftpe", 0);
			return mcAppInfo.sourceDir.equalsIgnoreCase(mcAppInfo.publicSourceDir);
		} catch (PackageManager.NameNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	protected class TexturesAdapter extends ArrayAdapter<File> {
		private LayoutInflater inflater;

		public TexturesAdapter(Context context) {
			super(context, android.R.layout.simple_list_item_1, new ArrayList<File>());
			inflater = LayoutInflater.from(context);
		}

		@Override
		public View getView(int position, View v, ViewGroup parent) {
			if (v == null) {
				v = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
			}
			TextView text = (TextView) v.findViewById(android.R.id.text1);
			File f = getItem(position);
			if (f.getAbsolutePath().equalsIgnoreCase(REQUEST_DEMO.getAbsolutePath())) {
				text.setText(R.string.textures_demo);
			} else {
				text.setText(f.getName());
			}
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
			dialog = new ProgressDialog(ManageTexturepacksActivity.this);
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
				adapter.add(outFile);
				adapter.notifyDataSetChanged();
				saveHistory();
				setTexturepack(outFile);
				Toast.makeText(ManageTexturepacksActivity.this, R.string.extract_textures_success,
						Toast.LENGTH_SHORT).show();
			} else {
				new AlertDialog.Builder(ManageTexturepacksActivity.this)
						.setMessage(
								hasSu ? R.string.extract_textures_error
										: R.string.extract_textures_no_root)
						.setPositiveButton(android.R.string.ok, null).show();
			}
		}
	}

}
