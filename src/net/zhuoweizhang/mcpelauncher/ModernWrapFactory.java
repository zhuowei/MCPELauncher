package net.zhuoweizhang.mcpelauncher;

import org.mozilla.javascript.*;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.util.Log;
import android.widget.PopupWindow;

/**
Because they are not classic
Disables wrapping of Java class objects
*/

@SuppressWarnings("rawtypes")
public class ModernWrapFactory extends WrapFactory {

	public static final String TAG = "BlockLauncher/ModernWrapFactory";
	public List<WeakReference<PopupWindow>> popups = new ArrayList<WeakReference<PopupWindow>>();

	/*
	@Override
	public Scriptable wrapJavaClass(Context cx, Scriptable scope, Class javaClass) {
		Log.i(TAG, "Wrapping " + javaClass.toString());
		return super.wrapJavaClass(cx, scope, javaClass);
	}
	*/

	@Override
	public Scriptable wrapAsJavaObject(Context cx, Scriptable scope, Object javaObject, Class<?> staticType) {
		Log.i(TAG, "Wrapping " + javaObject);
		if (javaObject instanceof PopupWindow) {
			synchronized(popups) {
				popups.add(new WeakReference<PopupWindow>((PopupWindow) javaObject));
			}
		}
		return super.wrapAsJavaObject(cx, scope, javaObject, staticType);
	}

	protected void closePopups(Activity activity) {
		System.out.println("close popups");
		activity.runOnUiThread(new Runnable() {
			public void run() {
				synchronized(popups) {
					System.out.println("close popups start");
					for (WeakReference<PopupWindow> ref: popups) {
						PopupWindow window = ref.get();
						if (window == null) continue;
						window.dismiss();
					}
					popups.clear();
				}
			}
		});
	}
}
