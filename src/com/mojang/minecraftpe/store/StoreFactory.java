package com.mojang.minecraftpe.store;

public class StoreFactory {
	public static Store createStore(StoreListener listener) {
		return new Store(listener);
	}
}
