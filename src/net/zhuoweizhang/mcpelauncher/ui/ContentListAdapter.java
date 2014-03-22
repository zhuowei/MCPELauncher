package net.zhuoweizhang.mcpelauncher.ui;

import java.util.*;
import android.content.*;
import android.content.res.*;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;

public class ContentListAdapter extends ArrayAdapter<ContentListItem> {

	private Resources mResources;

	public ContentListAdapter(Context context, int layout, List<ContentListItem> items) {
		super(context, layout, items);
		this.mResources = context.getResources();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView view = (TextView) super.getView(position, convertView, parent);
		ContentListItem item = getItem(position);
		view.setText(item.toString(mResources));
		view.setTypeface(null, item.enabled? Typeface.BOLD: 0);
		return view;
	}
}
