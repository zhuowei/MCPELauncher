package com.mojang.minecraftpe.store;

public class Store {
	private StoreListener listener;
	public Store(StoreListener listener) {
		this.listener = listener;
	}

	public String getStoreId() {
		System.out.println("Get store ID");
		return "Placeholder store ID"
	}
	public void queryProducts() {
		System.out.println("Query products");
	}

	public void purchase(String product) {
		System.out.println("Purchase " + product);
	}

	public void queryPurchases() {
		System.out.println("Query purchases");
	}

	public void destructor() {
		System.out.println("Destructor");
	}
}
