package com.mojang.minecraftpe.store;

public class StoreFactory {
	public static Store createStore(String something, StoreListener listener) {
		System.out.println("Store: factory: " + something + ":" + listener);
		return new Store(listener);
	}
}
