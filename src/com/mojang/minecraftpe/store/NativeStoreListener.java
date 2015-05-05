package com.mojang.minecraftpe.store;

public class NativeStoreListener implements StoreListener {
	private long callback;

	public NativeStoreListener(long callback) {
		this.callback = callback;
	}
}
