package net.zhuoweizhang.mcpelauncher.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.ipaulpro.afilechooser.FileChooserActivity;
import com.ipaulpro.afilechooser.utils.FileUtils;

import net.zhuoweizhang.mcpelauncher.R;
import net.zhuoweizhang.mcpelauncher.Utils;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

public class ManageSkinsActivity extends ListActivity {
	protected SkinsAdapter adapter;
	public static final File REQUEST_ENABLE = new File("/just/enable/skins");
	public static final File REQUEST_DISABLE = new File("/just/disable/skins");
	protected CompoundButton master = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.manage_skins);
		adapter = new SkinsAdapter(this);
		setListAdapter(adapter);
		getListView().setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				File f = adapter.getItem(position);
				setSkin(f);
				finish();
			}
		});
		((Button) findViewById(R.id.manage_skins_select)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent target = FileUtils.createGetContentIntent();
				target.setType("image/png");
				target.setClass(ManageSkinsActivity.this, FileChooserActivity.class);
				startActivityForResult(target, MainMenuOptionsActivity.REQUEST_MANAGE_SKINS);
			}
		});
		((Button) findViewById(R.id.manage_skins_players))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						AlertDialog.Builder b = new AlertDialog.Builder(ManageSkinsActivity.this);
						b.setTitle(R.string.pref_zz_skin_download_source);
						String[] items = new String[] {
								getString(R.string.skin_download_do_not_download),
								getString(R.string.skin_download_download_pc) };
						b.setSingleChoiceItems(items, getCurrentMode(),
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										SharedPreferences.Editor prefs = Utils.getPrefs(0).edit();
										switch (which) {
										case 1:
											prefs.putString("zz_skin_download_source", "mojang_pc");
											break;
										case 0:
										default:
											prefs.putString("zz_skin_download_source", "none");
											break;
										}
										prefs.apply();
									}
								});
						b.show();
					}

					protected int getCurrentMode() {
						String mode = Utils.getPrefs(0)
								.getString("zz_skin_download_source", "none");
						if (mode.equals("none"))
							return 0;
						if (mode.equals("mojang_pc"))
							return 1;
						return 0;
					}
				});
		setResult(RESULT_CANCELED);
		loadHistory();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if ((requestCode == MainMenuOptionsActivity.REQUEST_MANAGE_SKINS)
				&& (resultCode == RESULT_OK)) {
			final Uri uri = data.getData();
			File file = FileUtils.getFile(uri);
			adapter.add(file);
			adapter.notifyDataSetChanged();
			setSkin(file);
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
			SharedPreferences sh = Utils.getPrefs(1);
			String data = sh.getString("skins_history", "");
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
			SharedPreferences.Editor sh = Utils.getPrefs(1).edit();
			sh.putString("skins_history", out);
			sh.apply();
		}
	}

	protected boolean isEnabled() {
		SharedPreferences sh = Utils.getPrefs(0);
		return sh.getBoolean("zz_skin_enable", false);
	}

	public static void setSkin(File f, ManageSkinsActivity activity) {
		SharedPreferences.Editor p1 = Utils.getPrefs(0).edit();
		SharedPreferences.Editor p2 = Utils.getPrefs(1).edit();
		if (f.getAbsolutePath().equalsIgnoreCase(REQUEST_DISABLE.getAbsolutePath())) {
			p1.putBoolean("zz_skin_enable", false);
		} else if (f.getAbsolutePath().equalsIgnoreCase(REQUEST_ENABLE.getAbsolutePath())) {
			p1.putBoolean("zz_skin_enable", true);
		} else {
			// Usual
			p1.putBoolean("zz_skin_enable", true);
			p2.putString("player_skin", f.getAbsolutePath());
		}
		p1.apply();
		p2.apply();
		if (activity != null) {
			activity.refreshABToggle();
			activity.setResult(RESULT_OK);
		}
	}

	protected void setSkin(File f) {
		setSkin(f, this);
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getMenuInflater().inflate(R.menu.ab_master, menu);
			master = (CompoundButton) menu.findItem(R.id.ab_switch_container).getActionView()
					.findViewById(R.id.ab_switch);
			if (master != null) {
				master.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						if (isChecked) {
							setSkin(REQUEST_ENABLE);
						} else {
							setSkin(REQUEST_DISABLE);
						}
						loadHistory();
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
			master.setChecked(isEnabled());
		}
	}

	protected class SkinsAdapter extends ArrayAdapter<File> {
		private LayoutInflater inflater;

		public SkinsAdapter(Context context) {
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
			text.setText(f.getName());
			return v;
		}
	}

}
