package net.zhuoweizhang.mcpelauncher.ui;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.zhuoweizhang.mcpelauncher.R;
import android.content.res.Resources;

public class ContentListItem {
	public final File file;
	public final String displayName;
	public boolean enabled = true;
	public String extraData = null;

	public ContentListItem(File file, boolean enabled) {
		this.file = file;
		this.displayName = file.getName();
		this.enabled = enabled;
	}

	@Override
	public String toString() {
		// TODO: call our custom toString(Resources) withoutResources. Ideas?
		return displayName + (extraData != null? " " + extraData + " ": "") +
			(enabled ? "" : " ".concat("(disabled)"));
	}

	public String toString(Resources res) {
		return displayName + (extraData != null? " " + extraData + " ": "") +
			(enabled ? "" : " ".concat(res.getString(R.string.manage_patches_disabled)));
	}

	public static void sort(List<ContentListItem> list) {
		Collections.sort(list, new ContentListItem.ContentListComparator());
	}

	public static final class ContentListComparator implements Comparator<ContentListItem> {
		public int compare(ContentListItem a, ContentListItem b) {
			return a.displayName.toLowerCase().compareTo(b.displayName.toLowerCase());
		}

		public boolean equals(ContentListItem a, ContentListItem b) {
			return a.displayName.toLowerCase().equals(b.displayName.toLowerCase());
		}
	}
}
