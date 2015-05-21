package com.mojang.minecraftpe.store;

public class StoreFactory {
	public static Store createGooglePlayStore(String something, StoreListener listener) {
		System.out.println("Store: factory: " + something + ":" + listener);
		return new Store(listener);
	}

	public static Store createAmazonAppStore(StoreListener listener) {
		System.out.println("Amazon Store: " + listener);
		return new Store(listener);
	}
}
