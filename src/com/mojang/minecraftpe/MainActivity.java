package com.mojang.minecraftpe;

import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

import java.nio.ByteBuffer;

import java.text.DateFormat;

import java.util.*;

import android.app.Activity;
import android.app.NativeActivity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.view.View;
import android.view.KeyCharacterMap;
import android.util.DisplayMetrics;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import android.preference.*;

import net.zhuoweizhang.mcpelauncher.*;

import net.zhuoweizhang.pokerface.PokerFace;



public class MainActivity extends NativeActivity
{

	public static final int INPUT_STATUS_IN_PROGRESS = -1;

	public static final int INPUT_STATUS_OK = 1;

	public static final int INPUT_STATUS_CANCELLED = 0;

	public static final int DIALOG_CREATE_WORLD = 1;

	public static final int DIALOG_SETTINGS = 3;

	/* private dialogs start here */
	public static final int DIALOG_CRASH_SAFE_MODE = 0x1000;

	protected DisplayMetrics displayMetrics;

	protected TexturePack texturePack;

	protected Context minecraftApkContext;

	protected boolean fakePackage = false;

	public static final String[] GAME_MODES = {"creative", "survival"};

	private static final String MC_NATIVE_LIBRARY_DIR = "/data/data/com.mojang.minecraftpe/lib/";
	private static final String MC_NATIVE_LIBRARY_LOCATION = "/data/data/com.mojang.minecraftpe/lib/libminecraftpe.so";

	protected int inputStatus = INPUT_STATUS_IN_PROGRESS;

	protected String[] userInputStrings = null;

	public static ByteBuffer minecraftLibBuffer;

	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		System.out.println("oncreate");
		try {
			System.load(MC_NATIVE_LIBRARY_LOCATION);
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, "Can't load libminecraftpe.so from the original APK", Toast.LENGTH_LONG).show();
			finish();
		}

		nativeRegisterThis();

		displayMetrics = new DisplayMetrics();

		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

		setFakePackage(true);

		super.onCreate(savedInstanceState);

		setFakePackage(false);

		try {
			String filePath = getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0).getString("texturePack", null);
			if (filePath != null) {
				File file = new File(filePath);
				System.out.println("File!! " + file);
				if (!file.exists()) {
					texturePack = null;
				} else {
					texturePack = new ZipTexturePack(file);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.texture_pack_unable_to_load, Toast.LENGTH_LONG).show();
		}
		
		try {
			if (this.getPackageName().equals("com.mojang.minecraftpe")) {
				minecraftApkContext = this;
			} else {
				minecraftApkContext = createPackageContext("com.mojang.minecraftpe", 0);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, "Can't create package context for the original APK", Toast.LENGTH_LONG).show();
			finish();
		}


		File lockFile = new File(getFilesDir(), "running.lock");
		if (lockFile.exists()) {
			try {
				PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("zz_safe_mode", true).apply();
				showDialog(DIALOG_CRASH_SAFE_MODE);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		try {
			if (!isSafeMode()) {
				initPatching();
				applyPatches();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		//setContentView(R.layout.main);
	}

	@Override
	protected void onResume() {
		super.onResume();
		File lockFile = new File(getFilesDir(), "running.lock");
		try {
			lockFile.createNewFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		File lockFile = new File(getFilesDir(), "running.lock");
		try {
			lockFile.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onDestroy() {
		super.onDestroy();
		nativeUnregisterThis();
		File lockFile = new File(getFilesDir(), "running.lock");
		try {
			lockFile.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setFakePackage(boolean enable) {
		fakePackage = enable;
	}

	@Override
	public PackageManager getPackageManager() {
		if (fakePackage) {
			return new RedirectPackageManager(super.getPackageManager(), MC_NATIVE_LIBRARY_DIR);
		}
		return super.getPackageManager();
	}
		

	public native void nativeRegisterThis();
	public native void nativeUnregisterThis();

	public void buyGame() {
	}

	public int checkLicense() {
		System.err.println("CheckLicense");
		return 0;
	}

	/** displays a dialog. Not called from UI thread. */
	public void displayDialog(int dialogId) {
		System.out.println("displayDialog: " + dialogId);
		inputStatus = INPUT_STATUS_CANCELLED;
		switch (dialogId) {
			case DIALOG_CREATE_WORLD:
				System.out.println("World creation");
				inputStatus = INPUT_STATUS_IN_PROGRESS;
				runOnUiThread(new Runnable() {
					public void run() {
						showDialog(DIALOG_CREATE_WORLD);
					}
				});
				break;
			case DIALOG_SETTINGS:
				System.out.println("Settings");
				Intent intent = new Intent(this, MainMenuOptionsActivity.class);
				inputStatus = INPUT_STATUS_OK;
				intent.putExtra("maxNumPatches", getMaxNumPatches());
				startActivityForResult(intent, 1234);
				break;
		}
	}

	/*protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == DIALOG_SETTINGS) {
			inputStatus = INPUT_STATUS_OK;
		}
	}*/

	public Dialog onCreateDialog(int dialogId) {
		switch (dialogId) {
			case DIALOG_CREATE_WORLD:
				return createCreateWorldDialog();
			case DIALOG_CRASH_SAFE_MODE:
				return createCrashSafeModeDialog();
			default:
				return super.onCreateDialog(dialogId);
		}
	}

	protected Dialog createCreateWorldDialog() {
		final View textEntryView = getLayoutInflater().inflate(R.layout.create_world_dialog, null);
		return new AlertDialog.Builder(this)
			.setTitle(R.string.world_create_title)
			.setView(textEntryView)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialogI, int button) {
					AlertDialog dialog = (AlertDialog) dialogI;
					String worldName = ((TextView) dialog.findViewById(R.id.world_name_entry)).getText().toString();
					String worldSeed = ((TextView) dialog.findViewById(R.id.world_seed_entry)).getText().toString();
					String worldGameMode = GAME_MODES[
						((Spinner) dialog.findViewById(R.id.world_gamemode_spinner)).getSelectedItemPosition()];
					userInputStrings = new String[] {worldName, worldSeed, worldGameMode};
					inputStatus = INPUT_STATUS_OK;
					
				}
			})
			.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialogI, int button) {
					inputStatus = INPUT_STATUS_CANCELLED;
				}
			})
			.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialogI) {
					inputStatus = INPUT_STATUS_CANCELLED;
				}
			})
			.create();
	}

	protected Dialog createCrashSafeModeDialog() {
		return new AlertDialog.Builder(this)
			.setMessage(R.string.manage_patches_crash_safe_mode)
			.setPositiveButton(android.R.string.ok, null)
			.create();
	}

	/**
	 * @param time Unix timestamp
	 * @returns a formatted time value
	 */

	public String getDateString(int time) {
		System.out.println("getDateString: " + time);
		return DateFormat.getDateInstance(DateFormat.SHORT, Locale.US).format(new Date(((long) time) * 1000));
	}

	public byte[] getFileDataBytes(String name) {
		System.out.println("Get file data: " + name);
		try {
			InputStream is = getInputStreamForAsset(name);
			if (is == null) return null;
			// can't always find length - use the method from 
			// http://www.velocityreviews.com/forums/t136788-store-whole-inputstream-in-a-string.html
			// instead
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			while(true) {
				int len = is.read(buffer);
				if(len < 0) {
					break;
				}
				bout.write(buffer, 0, len);
			}
			byte[] retval = bout.toByteArray();

			return retval;
		} catch (Exception e) {
			return null;
		}
	}

	protected InputStream getInputStreamForAsset(String name) {
		InputStream is = null;
		try {
			System.out.println("Trying to load " + name);
			is = minecraftApkContext.getAssets().open(name);
			return is;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public int[] getImageData(String name) {
		System.out.println("Get image data: " + name);
		try {
			InputStream is = getInputStreamForAsset(name);
			if (is == null) return null;
			Bitmap bmp = BitmapFactory.decodeStream(is);
			int[] retval = new int[(bmp.getWidth() * bmp.getHeight()) + 2];
			retval[0] = bmp.getWidth();
			retval[1] = bmp.getHeight();
			bmp.getPixels(retval, 2, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());
			is.close();

			return retval;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		/* format: width, height, each integer a pixel */
		/* 0 = white, full transparent */
	}

	public String[] getOptionStrings() {
		System.err.println("OptionStrings");
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		Map prefsMap = sharedPref.getAll();
		Set<Map.Entry> prefsSet = prefsMap.entrySet();
		String[] retval = new String[prefsSet.size() * 2];
		int i = 0;
		for (Map.Entry e: prefsSet) {
			retval[i] = (String) e.getKey();
			retval[i + 1] = e.getValue().toString();
			i+= 2;
		}
		System.out.println(Arrays.toString(retval));
		return retval;
	}

	public float getPixelsPerMillimeter() {
		System.out.println("Pixels per mm");
		return ((float) displayMetrics.densityDpi) / 25.4f ;
	}

	public String getPlatformStringVar(int a) {
		System.out.println("getPlatformStringVar: " +a);
		return "";
	}

	public int getScreenHeight() {
		System.out.println("height");
		return displayMetrics.heightPixels;
	}

	public int getScreenWidth() {
		System.out.println("width");
		return displayMetrics.widthPixels;
	}

	public int getUserInputStatus() {
		System.out.println("User input status: " + inputStatus);
		return inputStatus;
	}

	public String[] getUserInputString() {
		System.out.println("User input string");
		/* for the seed input: name, world type, seed */
		return userInputStrings;
	}

	public boolean hasBuyButtonWhenInvalidLicense() {
		return false;
	}

	/** Seems to be called whenever displayDialog is called. Not on UI thread. */
	public void initiateUserInput(int a) {
		System.out.println("initiateUserInput: " + a);
	}

	public boolean isNetworkEnabled(boolean a) {
		System.out.println("Network?:" + a);
		return true;
	}


	public boolean isTouchscreen() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean("ctrl_usetouchscreen", true);
	}

	public void postScreenshotToFacebook(String name, int firstInt, int secondInt, int[] thatArray) {
	}

	public void quit() {
		finish();
	}

	public void setIsPowerVR(boolean powerVR) {
		System.out.println("PowerVR: " + powerVR);
	}

	public void tick() {
	}

	public void vibrate(int duration) {
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("zz_longvibration", false)) {
			duration *= 5;
		}
		((Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(duration);
	}

	public int getKeyFromKeyCode(int keyCode, int metaState, int deviceId) {
		KeyCharacterMap characterMap = KeyCharacterMap.load(deviceId);
		return characterMap.get(keyCode, metaState);
	}

	public static void saveScreenshot(String name, int firstInt, int secondInt, int[] thatArray) {
	}

	public boolean isSafeMode() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean("zz_safe_mode", false);
	}

	public void initPatching() throws Exception {
		long pageSize = PokerFace.sysconf(PokerFace._SC_PAGESIZE);
		System.out.println(Long.toString(pageSize, 16));
		long minecraftLibLocation = findMinecraftLibLocation();
		long minecraftLibLength = findMinecraftLibLength();
		long mapPageLength = ((minecraftLibLength / pageSize) + 1) * pageSize;
		System.out.println("Calling mprotect with " + minecraftLibLocation + " and " + mapPageLength);
		int returnStatus = PokerFace.mprotect(minecraftLibLocation, mapPageLength, PokerFace.PROT_WRITE | PokerFace.PROT_READ | PokerFace.PROT_EXEC);
		System.out.println("mprotect result is " + returnStatus);
		if (returnStatus < 0) {
			System.out.println("Well, that sucks!");
			return;
		}
		ByteBuffer buffer = PokerFace.createDirectByteBuffer(minecraftLibLocation, minecraftLibLength);
		//findMinecraftLibLocation();
		System.out.println("Has the byte buffer: " + buffer);
		minecraftLibBuffer = buffer;
	}

	public void applyPatches() throws Exception {
		ByteBuffer buffer = minecraftLibBuffer;
		buffer.position(0x1b6d50);//"v0.6.1" offset
		byte[] testBuffer = new byte[6];
		buffer.get(testBuffer);
		System.out.println("Before: " + Arrays.toString(testBuffer));
		buffer.position(0x1b6d50);//"v0.6.1" offset
		buffer.put(">9000!".getBytes());
		buffer.position(0x1b6d50);//"v0.6.1" offset
		buffer.get(testBuffer);
		System.out.println("After " + Arrays.toString(testBuffer));
	}

	public static long findMinecraftLibLocation() throws Exception {
		Scanner scan = new Scanner(new File("/proc/self/maps"));
		long minecraftLocation = -1;
		while (scan.hasNextLine()) {
			String line = scan.nextLine();
			//System.out.println(line);
			String[] parts = line.split(" ");
			if (parts[parts.length - 1].indexOf("libminecraftpe.so") >= 0 && parts[1].indexOf("x") >= 0) {
				System.out.println("Found minecraft location");
				minecraftLocation = Long.parseLong(parts[0].substring(0, parts[0].indexOf("-")), 16);
				break;
			}
		}
		scan.close();
		return minecraftLocation;
	}

	public static long findMinecraftLibLength() throws Exception {
		return new File(MC_NATIVE_LIBRARY_LOCATION).length(); //TODO: don't hardcode the 0x1000 page for relocation .data.rel.ro.local
	}

	public int getMaxNumPatches() {
		return 3; //3 patches
	}

}
