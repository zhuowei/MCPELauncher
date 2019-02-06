package com.mojang.minecraftpe.store;

public class Store {
	public static boolean needsNativeListenerCallback = false;
	private StoreListener listener;
	public Store(StoreListener listener) {
		this.listener = listener;
		if (needsNativeListenerCallback) {
			NativeStoreListener nativeListener = (NativeStoreListener)listener;
			nativeListener.onStoreInitialized(nativeListener.callback, false);
		}
	}

	public String getStoreId() {
		System.out.println("Store: Get store ID");
		return "Placeholder store ID";
	}
	public void queryProducts(String[] products) {
		System.out.println("Store: Query products");
	}

	public void purchase(String product, boolean what, String what2) {
		System.out.println("Store: purchase " + product + ": " + what + ": " + what2);
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

	public void acknowledgePurchase(String a, String b) {
		System.out.println("Acknowledge purchase: " + a + ":" + b);
	}

	public String getProductSkuPrefix() {
		System.out.println("Get product SKU prefix");
		return "";
	}

	public String getRealmsSkuPrefix() {
		System.out.println("Get realms SKU prefix");
		return "";
	}
	public boolean hasVerifiedLicense() {
		return true;
	}

	public ExtraLicenseResponseData getExtraLicenseData() {
		return new ExtraLicenseResponseData();
	}

	public boolean receivedLicenseResponse() {
		return true;
	}
}
