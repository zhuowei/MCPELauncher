package net.zhuoweizhang.mcpelauncher;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import android.content.res.Resources;

public class ContentListItem {
	public final File file;
	public final String displayName;
	public boolean enabled = true;

	public ContentListItem(File file, boolean enabled) {
		this.file = file;
		this.displayName = file.getName();
		this.enabled = enabled;
	}

	@Override
	public String toString() {
		// TODO: call our custom toString(Resources) withoutResources. Ideas?
		return displayName + (enabled ? "" : " ".concat("(disabled)"));
	}

	public String toString(Resources res) {
		return displayName + (enabled ? "" : " ".concat(res.getString(R.string.manage_patches_disabled)));
	}

	public static void sort(List<ContentListItem> list) {
		Arrays.sort(list.toArray(new ContentListItem[0]), new ContentListItem.ContentListComparator());
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
