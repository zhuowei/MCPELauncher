package com.mojang.minecraftpe.store;

public class NativeStoreListener implements StoreListener {
	public long callback;

	public NativeStoreListener(long callback) {
		this.callback = callback;
	}
	public native void onStoreInitialized(long callback, boolean success);
}
