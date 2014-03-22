package net.zhuoweizhang.mcpelauncher;

import android.content.ComponentName;
import android.content.pm.*;

public class RedirectPackageManager extends WrappedPackageManager {

	protected String nativeLibraryDir;

	public RedirectPackageManager(PackageManager mgr, String nativeLibraryDir) {
		super(mgr);
		this.nativeLibraryDir = nativeLibraryDir;
	}

	@Override
	public ActivityInfo getActivityInfo(ComponentName className, int flags) throws NameNotFoundException {
		ActivityInfo retval = wrapped.getActivityInfo(className, flags);
		retval.applicationInfo.nativeLibraryDir = nativeLibraryDir;
		return retval;
	}
}
