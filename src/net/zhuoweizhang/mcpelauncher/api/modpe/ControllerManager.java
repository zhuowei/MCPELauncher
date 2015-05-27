package net.zhuoweizhang.mcpelauncher.api.modpe;

public class ControllerManager {

	public static native void init();
	public static native void feed(int axis, int type, float x, float y);
	public static native void feedTrigger(int axis, float value);

	static {
		System.loadLibrary("mcpelauncher");
	}

}
