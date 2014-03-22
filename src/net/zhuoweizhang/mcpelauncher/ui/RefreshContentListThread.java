package net.zhuoweizhang.mcpelauncher.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;

public class RefreshContentListThread implements Runnable {
	protected final Activity mContext;
	protected final OnRefreshContentList mListener;

	public RefreshContentListThread(Activity ctx, OnRefreshContentList listener) {
		this.mContext = ctx;
		this.mListener = listener;
	}

	public void run() {
		final List<ContentListItem> items = new ArrayList<ContentListItem>();

		for (File folder : mListener.getFolders()) {
			combOneFolder(folder, items);
		}

		ContentListItem.sort(items);

		mListener.onRefreshComplete(items);
	}

	private void combOneFolder(File patchesFolder, List<ContentListItem> patches) {
		if (!patchesFolder.exists()) {
			System.err.println("no storage folder");
		} else {
			for (File patchFile : patchesFolder.listFiles()) {
				boolean patchEnabled = mListener.isEnabled(patchFile);
				patches.add(new ContentListItem(patchFile, patchEnabled));
			}
		}
	}

	public static interface OnRefreshContentList {
		public void onRefreshComplete(List<ContentListItem> items);

		public List<File> getFolders();

		public boolean isEnabled(File f);
	}
}
