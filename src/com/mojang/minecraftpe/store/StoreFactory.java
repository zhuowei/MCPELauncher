package com.mojang.minecraftpe.store;

import net.zhuoweizhang.mcpelauncher.BuildConfig;

public class StoreFactory {
	public static Store createGooglePlayStore(String something, StoreListener listener) {
		if (BuildConfig.DEBUG) System.out.println("Store: factory: " + something + ":" + listener);
		return new Store(listener);
	}

	public static Store createAmazonAppStore(StoreListener listener) {
		if (BuildConfig.DEBUG) System.out.println("Amazon Store: " + listener);
		return new Store(listener);
	}

	public static Store createAmazonAppStore(StoreListener listener, boolean a) {
		if (BuildConfig.DEBUG) System.out.println("Amazon Store: " + listener + ":" + a);
		return new Store(listener);
	}

	public static Store createSamsungAppStore(StoreListener listener) {
		if (BuildConfig.DEBUG) System.out.println("Amazon Store: " + listener);
		return new Store(listener);
	}
}
