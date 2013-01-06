package com.mojang.minecraftpe;

import android.app.Activity;
import android.app.NativeActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.util.DisplayMetrics;

public class MainActivity extends NativeActivity
{

	public static final int INPUT_STATUS_IN_PROGRESs = 0;

	public static final int INPUT_STATUS_OK = 1;

	public static final int INPUT_STATUS_CANCELLED = 2;

	protected DisplayMetrics displayMetrics;

	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		System.out.println("oncreate");
		nativeRegisterThis();
		super.onCreate(savedInstanceState);

		/*View contentView = this.findViewById(android.R.id.content);

		displayMetrics = new DisplayMetrics();

		contentView.getDisplay().getMetrics(displayMetrics);*/
		
		//setContentView(R.layout.main);
	}

	public void onStart() {
		super.onStart();
	}

	public native void nativeRegisterThis();
	public native void nativeUnregisterThis();

	public void buyGame() {
	}

	public int checkLicense() {
		System.err.println("CheckLicense");
		return 0;
	}

	public void displayDialog(int dialogId) {
		System.out.println("displayDialog: " + dialogId);
		switch (dialogId) {
			case 0:
				System.out.println("World creation");
				break;
			case 3:
				System.out.println("Settings");
				Intent intent = new Intent(this, MainMenuOptionsActivity.class);
				startActivityForResult(intent, 1234);
				break;
		}
	}

	public String getDateString(int a) {
		System.out.println("getDateString: " + a);
		return Integer.toString(a);
	}

	public byte[] getFileDataBytes(String name) {
		System.out.println("Get file data: " + name);
		return null;
	}

	public int[] getImageData(String name) {
		System.out.println("Get image data: " + name);
		return null;
	}

	public String[] getOptionStrings() {
		System.err.println("OptionStrings");
		return new String[] {"zebra"};
	}

	public float getPixelsPerMillimeter() {
		System.out.println("Pixels per mm");
		return 1.0f;
	}

	public String getPlatformStringVar(int a) {
		System.out.println("getPlatformStrinVar: " +a);
		return "";
	}

	public int getScreenHeight() {
		System.out.println("height");
		return 100;
	}

	public int getScreenWidth() {
		System.out.println("width");
		return 100;
	}

	public int getUserInputStatus() {
		System.out.println("User input status");
		return 2;
	}

	public String[] getUserInputString() {
		System.out.println("User input string");
		/* for the seed input: name, world type, seed */
		return new String[] {"elephant", "potato", "strawberry"};
	}

	public boolean hasBuyButtonWhenInvalidLicense() {
		return false;
	}

	public void initiateUserInput(int a) {
		System.out.println("initiateUserInput: " + a);
	}

	public boolean isNetworkEnabled(boolean a) {
		System.out.println("Network?:" + a);
		return true;
	}


	public boolean isTouchscreen() {
		System.err.println("Touchscreen");
		return true;
	}

	public void postScreenshotToFacebook(String name, int firstInt, int secondInt, int[] thatArray) {
	}

	public void quit() {
	}

	public void setIsPowerVR(boolean powerVR) {
		System.out.println("PowerVR: " + powerVR);
	}

	public void tick() {
	}

	public void vibrate(int duration) {
	}	

	public static void saveScreenshot(String name, int firstInt, int secondInt, int[] thatArray) {
	}

	static {
		try {
			System.loadLibrary("minecraftpe");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
