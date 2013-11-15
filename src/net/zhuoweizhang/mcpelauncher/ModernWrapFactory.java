package net.zhuoweizhang.mcpelauncher;

import org.mozilla.javascript.*;
import android.util.Log;

/**
Because they are not classic
Disables wrapping of Java class objects
*/

public class ModernWrapFactory extends WrapFactory {

	public static final String TAG = "BlockLauncher/ModernWrapFactory";

	@Override
	public Scriptable wrapJavaClass(Context cx, Scriptable scope, Class javaClass) {
		Log.i(TAG, "Wrapping " + javaClass.toString());
		throw new UnsupportedOperationException("Java bridge disabled");
	}

	@Override
	public Scriptable wrapAsJavaObject(Context cx, Scriptable scope, Object javaObject, Class<?> staticType) {
		Log.i(TAG, "Wrapping " + javaObject.toString());
		throw new UnsupportedOperationException("Java bridge disabled");
	}
}
