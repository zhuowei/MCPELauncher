package com.mojang.minecraftpe.store;

public class Store {
	private StoreListener listener;
	public Store(StoreListener listener) {
		this.listener = listener;
	}

	public String getStoreId() {
		System.out.println("Store: Get store ID");
		return "Placeholder store ID";
	}
	public void queryProducts() {
		System.out.println("Store: Query products");
	}

	public void purchase(String product) {
		System.out.println("Store: Purchase " + product);
	}

	public void queryPurchases() {
		System.out.println("Store: Query purchases");
	}

	public void destructor() {
		System.out.println("Store: Destructor");
	}
}
