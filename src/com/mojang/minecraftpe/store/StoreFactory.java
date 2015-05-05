package com.mojang.minecraftpe.store;

public class StoreFactory {
	public static Store createStore(StoreListener listener) {
		System.out.println("Store: factory: " + listener);
		return new Store(listener);
	}
}
