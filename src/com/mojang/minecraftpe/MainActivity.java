package com.mojang.minecraftpe;

import java.io.*;

import java.lang.ref.WeakReference;

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
import android.content.pm.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.view.*;
import android.view.KeyCharacterMap;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.*;

import android.preference.*;

import net.zhuoweizhang.mcpelauncher.*;

import net.zhuoweizhang.pokerface.PokerFace;



public class MainActivity extends NativeActivity
{

	public static final String TAG = "BlockLauncher/MainActivity";

	public static final int INPUT_STATUS_IN_PROGRESS = -1;

	public static final int INPUT_STATUS_OK = 1;

	public static final int INPUT_STATUS_CANCELLED = 0;

	public static final int DIALOG_CREATE_WORLD = 1;

	public static final int DIALOG_SETTINGS = 3;

	public static final int DIALOG_COPY_WORLD = 4;

	/* private dialogs start here */
	public static final int DIALOG_CRASH_SAFE_MODE = 0x1000;
	public static final int DIALOG_RUNTIME_OPTIONS = 0x1001;
	public static final int DIALOG_INVALID_PATCHES = 0x1002;
	public static final int DIALOG_FIRST_LAUNCH = 0x1003;
	public static final int DIALOG_VERSION_MISMATCH_SAFE_MODE = 0x1004;
	public static final int DIALOG_NOT_SUPPORTED = 0x1005;
	public static final int DIALOG_UPDATE_TEXTURE_PACK = 0x1006;

	protected DisplayMetrics displayMetrics;

	protected TexturePack texturePack;

	protected Context minecraftApkContext;

	protected boolean fakePackage = false;

	public static final String[] GAME_MODES = {"creative", "survival"};

	private static String MC_NATIVE_LIBRARY_DIR = "/data/data/com.mojang.minecraftpe/lib/";
	private static String MC_NATIVE_LIBRARY_LOCATION = "/data/data/com.mojang.minecraftpe/lib/libminecraftpe.so";

	public static final String PT_PATCHES_DIR = "ptpatches";

	protected int inputStatus = INPUT_STATUS_IN_PROGRESS;

	protected String[] userInputStrings = null;

	public static ByteBuffer minecraftLibBuffer;

	public static boolean hasPrePatched = false;

	public static boolean libLoaded = false;

	public boolean forceFallback = false;

	public boolean requiresGuiBlocksPatch = false;

	private HoverCar hoverCar = null;

	public static WeakReference<MainActivity> currentMainActivity = null;

	public static Set<String> loadedAddons = new HashSet<String>();

	public ApplicationInfo mcAppInfo;

	public static List<String> failedPatches = new ArrayList<String>();

	public List<TexturePack> textureOverrides = new ArrayList<TexturePack>();

	public boolean minecraftApkForwardLocked = false;

	public boolean tempSafeMode = false;

	public String session = "";
	public String refreshToken = "";

	private PackageInfo mcPkgInfo;

	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		System.out.println("oncreate");

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
			mcPkgInfo = getPackageManager().getPackageInfo("com.mojang.minecraftpe", 0);
			mcAppInfo = mcPkgInfo.applicationInfo;/*getPackageManager().getApplicationInfo("com.mojang.minecraftpe", 0);*/
			MC_NATIVE_LIBRARY_DIR = mcAppInfo.nativeLibraryDir;
			MC_NATIVE_LIBRARY_LOCATION = MC_NATIVE_LIBRARY_DIR + "/libminecraftpe.so";
			System.out.println("libminecraftpe.so is at " + MC_NATIVE_LIBRARY_LOCATION);
			minecraftApkForwardLocked = !mcAppInfo.sourceDir.equals(mcAppInfo.publicSourceDir);
			int minecraftVersionCode = mcPkgInfo.versionCode;
			if (minecraftVersionCode != MinecraftConstants.MINECRAFT_VERSION_CODE) {
				tempSafeMode = true;
				showDialog(DIALOG_VERSION_MISMATCH_SAFE_MODE);
			}
			SharedPreferences myprefs = getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0);
			int prepatchedVersionCode = myprefs.getInt("prepatch_version", -1);

			if (prepatchedVersionCode != minecraftVersionCode) {
				System.out.println("Version updated; forcing prepatch");
				myprefs.edit().putBoolean("force_prepatch", true).apply();
				disableAllPatches();
				if (myprefs.getString("texturePack", "").indexOf("minecraft.apk") >= 0) {
					showDialog(DIALOG_UPDATE_TEXTURE_PACK);
				}
			}

				
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
			finish();
			startActivity(new Intent(this, NoMinecraftActivity.class));
			try {
				Thread.sleep(100);
				android.os.Process.killProcess(android.os.Process.myPid());
			} catch (Throwable t) {
			}
			return;
		}

		
		try {
			if (this.getPackageName().equals("com.mojang.minecraftpe")) {
				minecraftApkContext = this;
			} else {
				minecraftApkContext = createPackageContext("com.mojang.minecraftpe", Context.CONTEXT_IGNORE_SECURITY);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, "Can't create package context for the original APK", Toast.LENGTH_LONG).show();
			finish();
		}
			

		forceFallback = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("zz_texture_pack_demo", false);

		loadTexturePack();

		textureOverrides.add(new SkinTextureOverride(this));

		requiresGuiBlocksPatch = doesRequireGuiBlocksPatch();

		migrateToPatchManager();

		try {
			if (!isSafeMode()) {
				prePatch();
			}
		} catch (Exception e) {
			e.printStackTrace();
			//showDialog(DIALOG_UNABLE_TO_PATCH);
		}

		try {
			System.load(MC_NATIVE_LIBRARY_LOCATION);
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, "Can't load libminecraftpe.so from the original APK", Toast.LENGTH_LONG).show();
			finish();
		}

		libLoaded = true;

		nativeRegisterThis();

		displayMetrics = new DisplayMetrics();

		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

		setFakePackage(true);

		super.onCreate(savedInstanceState);

		setFakePackage(false);

		try {
			if (!isSafeMode()) {
				initPatching();
				loadNativeAddons();
				//applyPatches();
				applyBuiltinPatches();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		enableSoftMenuKey();

		System.gc();

		currentMainActivity = new WeakReference<MainActivity>(this);

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
		if (true || PreferenceManager.getDefaultSharedPreferences(this).getBoolean("zz_enable_hovercar", false)) {
			if (hoverCar == null) {
				getWindow().getDecorView().post(new Runnable() {
					public void run() {
						try {
							setupHoverCar();
						} catch (Exception e) {
							e.printStackTrace();
						} //don't force close on hover car fail
					}
				});
			}
		} else {
			if (hoverCar != null) {
				hoverCar.dismiss();
				hoverCar = null;
			}
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

	private void prePatch() throws Exception {
		File patched = getDir("patched", 0);
		File originalLibminecraft = new File(mcAppInfo.nativeLibraryDir + "/libminecraftpe.so");
		File newMinecraft = new File(patched, "libminecraftpe.so");
		File patchesDir = this.getDir(PT_PATCHES_DIR, 0);
		boolean forcePrePatch = getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0).getBoolean("force_prepatch", true);
		if (!hasPrePatched && (!newMinecraft.exists() || forcePrePatch)) {

			System.out.println("Forcing new prepatch");

			byte[] libBytes = new byte[(int) originalLibminecraft.length()];

			InputStream is = new FileInputStream(originalLibminecraft);
			is.read(libBytes);
			is.close();

			int patchedCount = 0;
			int maxPatchNum = getMaxNumPatches();
			PatchManager patchMgr = PatchManager.getPatchManager(this);

			Set<String> patchLocs = patchMgr.getEnabledPatches();

			for (String patchLoc: patchLocs) {
				if (maxPatchNum >= 0 && patchedCount >= maxPatchNum) break;
				File patchFile = new File(patchLoc);
				if (!patchFile.exists()) continue;
				com.joshuahuelsman.patchtool.PTPatch patch = new com.joshuahuelsman.patchtool.PTPatch();
				patch.loadPatch(patchFile);
				if (!patch.checkMagic()) {
					failedPatches.add(patchFile.getName());
					continue;
				}
				patch.applyPatch(libBytes);
				patchedCount++;
			}

			/*patchedCount = prePatchDir(libBytes, patchesDir, patchMgr, patchedCount, maxPatchNum);
			patchedCount = prePatchDir(libBytes, new File(Environment.getExternalStorageDirectory(), 
				"Android/data/com.snowbound.pockettool.free/Patches"), patchMgr, patchedCount, maxPatchNum);
			patchedCount = prePatchDir(libBytes, new File(Environment.getExternalStorageDirectory(), 
				"Android/data/com.snowbound.pockettool.free/Patches"), patchMgr, patchedCount, maxPatchNum);*/

			/* patching specific built-in patches */
			if (requiresGuiBlocksPatch) {
				System.out.println("Patching guiblocks");
				com.joshuahuelsman.patchtool.PTPatch patch = new com.joshuahuelsman.patchtool.PTPatch();
				//patch.loadPatch(MinecraftConstants.GUI_BLOCKS_PATCH);
				patch.applyPatch(libBytes);
			}

			OutputStream os = new FileOutputStream(newMinecraft);
			os.write(libBytes);
			os.close();
			hasPrePatched = true;
			getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0).edit().putBoolean("force_prepatch", false).
				putInt("prepatch_version", mcPkgInfo.versionCode).apply();
			if (failedPatches.size() > 0) {
				showDialog(DIALOG_INVALID_PATCHES);
			}
				
		}

		MC_NATIVE_LIBRARY_DIR = patched.getCanonicalPath();
		MC_NATIVE_LIBRARY_LOCATION = newMinecraft.getCanonicalPath();
	}

	private int prePatchDir(byte[] libBytes, File patchesDir, PatchManager patchMgr, int patchedCount, int maxPatchNum) throws Exception {
		if (!patchesDir.exists()) return patchedCount;
		File[] patches = patchesDir.listFiles();

		for (File f: patches) {
			if (maxPatchNum >= 0 && patchedCount >= maxPatchNum) break;
			if (!patchMgr.isEnabled(f)) continue;
			com.joshuahuelsman.patchtool.PTPatch patch = new com.joshuahuelsman.patchtool.PTPatch();
			patch.loadPatch(f);
			patch.applyPatch(libBytes);
			patchedCount++;
		}
		return patchedCount;
	}

	public native void nativeRegisterThis();
	public native void nativeUnregisterThis();

	//added in 0.7.0:
	public native void nativeLoginData(String session, String param2, String refreshToken);
	public native void nativeStopThis();
	public native void nativeWebRequestCompleted(int requestId, long param2, int param3, String param4);

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
				inputStatus = INPUT_STATUS_IN_PROGRESS;
				startActivityForResult(intent, 1234);
				break;
			case DIALOG_COPY_WORLD:
				System.out.println("Copy world");
				inputStatus = INPUT_STATUS_IN_PROGRESS;
				runOnUiThread(new Runnable() {
					public void run() {
						showDialog(DIALOG_COPY_WORLD);
					}
				});
				break;
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == 1234) {
			inputStatus = INPUT_STATUS_OK;
			System.out.println("Settings OK");
			loadTexturePack();
			if (!isSafeMode()) {
				applyBuiltinPatches();
			}
		}
	}

	public Dialog onCreateDialog(int dialogId) {
		switch (dialogId) {
			case DIALOG_CREATE_WORLD:
				return createCreateWorldDialog();
			case DIALOG_COPY_WORLD:
				return createCopyWorldDialog();
			case DIALOG_CRASH_SAFE_MODE:
				return createSafeModeDialog(R.string.manage_patches_crash_safe_mode);
			case DIALOG_RUNTIME_OPTIONS:
				return createRuntimeOptionsDialog();
			case DIALOG_INVALID_PATCHES:
				return createInvalidPatchesDialog();
			case DIALOG_FIRST_LAUNCH:
				return createFirstLaunchDialog();
			case DIALOG_VERSION_MISMATCH_SAFE_MODE:
				return createSafeModeDialog(R.string.version_mismatch_message);
			case DIALOG_NOT_SUPPORTED:
				return createNotSupportedDialog();
			case DIALOG_UPDATE_TEXTURE_PACK:
				return createUpdateTexturePackDialog();
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

	protected Dialog createSafeModeDialog(int messageRes) {
		return new AlertDialog.Builder(this)
			.setMessage(messageRes)
			.setPositiveButton(android.R.string.ok, null)
			.create();
	}

	protected Dialog createRuntimeOptionsDialog() {
		CharSequence livePatch = getResources().getString(R.string.hovercar_live_patch);
		CharSequence optionMenu = getResources().getString(R.string.hovercar_options);
		CharSequence[] options = new CharSequence[] {livePatch, optionMenu};
		return new AlertDialog.Builder(this).setTitle(R.string.hovercar_title).
			setItems(options, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialogI, int button) {
					if (button == 0) {
						Intent intent = new Intent(MainActivity.this, ManagePatchesActivity.class);
						intent.putExtra("prePatchConfigure", false);
						startActivity(intent);	
					} else if (button == 1) {
						Intent intent = new Intent(MainActivity.this, MainMenuOptionsActivity.class);
						startActivity(intent);
					}
				}
			}).create();
	}

	protected Dialog createInvalidPatchesDialog() {
		return new AlertDialog.Builder(this)
			.setMessage(getResources().getString(R.string.manage_patches_invalid_patches) + "\n" + 
				PatchManager.join(failedPatches.toArray(PatchManager.blankArray), "\n"))
			.setPositiveButton(android.R.string.ok, null)
			.create();
	}

	protected Dialog createFirstLaunchDialog() {
		StringBuilder dialogMsg = new StringBuilder();
		dialogMsg.append(getResources().getString(R.string.firstlaunch_generic_intro)).append("\n\n");
		if (minecraftApkForwardLocked) {
			dialogMsg.append(getResources().getString(R.string.firstlaunch_jelly_bean)).append("\n\n");
		}
		dialogMsg.append(getResources().getString(R.string.firstlaunch_see_options)).append("\n\n");
		return new AlertDialog.Builder(this)
			.setTitle(R.string.firstlaunch_title)
			.setMessage(dialogMsg.toString())
			.setPositiveButton(android.R.string.ok, null)
			.setNeutralButton(R.string.firstlaunch_help, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialogI, int button) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(AboutAppActivity.FORUMS_PAGE_URL));
					startActivity(intent);
				}
			})
			.create();
	}

	protected Dialog createCopyWorldDialog() {
		final View textEntryView = getLayoutInflater().inflate(R.layout.copy_world_dialog, null);
		return new AlertDialog.Builder(this)
			.setTitle(R.string.copy_world_title)
			.setView(textEntryView)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialogI, int button) {
					AlertDialog dialog = (AlertDialog) dialogI;
					String worldName = ((TextView) dialog.findViewById(R.id.world_name_entry)).getText().toString();
					userInputStrings = new String[] {worldName};
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

	protected Dialog createNotSupportedDialog() {
		return new AlertDialog.Builder(this)
			.setMessage(R.string.feature_not_supported)
			.setPositiveButton(android.R.string.ok, null)
			.create();
	}

	protected Dialog createUpdateTexturePackDialog() {
		return new AlertDialog.Builder(this)
			.setMessage(R.string.extract_textures_need_update)
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
			for (int i = 0; i < textureOverrides.size(); i++) {
				try {
					is = textureOverrides.get(i).getInputStream(name);
					if (is != null) return is;
				} catch (IOException e) {
				}
			}
			if (texturePack == null) {
				return getLocalInputStreamForAsset(name);
			} else {
				System.out.println("Trying to load  " +name + "from tp");
				is = texturePack.getInputStream(name);
				if (is == null) {
					System.out.println("Can't load " + name + " from tp");
					return getLocalInputStreamForAsset(name);
				}
			}
			return is;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	protected InputStream getLocalInputStreamForAsset(String name) {
		InputStream is = null;
		try {
			if (forceFallback) {
				return getAssets().open(name);
			}
			try {
				is = minecraftApkContext.getAssets().open(name);
			} catch (Exception e) {
				//e.printStackTrace();
				System.out.println("Attempting to load fallback");
				is = getAssets().open(name);
			}
			if (is == null) {
				System.out.println("Can't find it in the APK - attempting to load fallback");
				is = getAssets().open(name);
			}
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
			bmp.recycle();

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
		List<String> retval = new ArrayList<String>();
		for (Map.Entry e: prefsSet) {
			String key = (String) e.getKey();
			if (key.indexOf("zz_") == 0) continue;
			retval.add(key);
			if (key.equals("ctrl_sensitivity")) {
				retval.add(Double.toString(Integer.parseInt(e.getValue().toString()) / 100.0));
			} else {
				retval.add(e.getValue().toString());
			}
		}
		retval.add("game_difficulty");
		if (sharedPref.getBoolean("game_difficultypeaceful", false)) {
			retval.add("0");
		} else {
			retval.add("2");
		}
		System.out.println(retval.toString());
		return retval.toArray(new String[0]);
	}

	public float getPixelsPerMillimeter() {
		System.out.println("Pixels per mm");
		float val = ((float) displayMetrics.densityDpi) / 25.4f;
		String custom = PreferenceManager.getDefaultSharedPreferences(this).getString("zz_custom_dpi", null);
		if (custom != null && custom.length() > 0) {
			try {
				val = Float.parseFloat(custom) / 25.4f;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return val;
		
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

	//added in 0.7.0
	public int abortWebRequest(int requestId) {
		Log.i(TAG, "Abort web request: " + requestId);
		return 0;
	}

	public String getRefreshToken() {
		Log.i(TAG, "Refresh token");
		return refreshToken;
	}

	public String getSession() {
		Log.i(TAG, "Session");
		return session;
	}

	public String getWebRequestContent(int requestId) {
		Log.i(TAG, "Get web request content: " + requestId);
		return "ThisIsSparta";
	}

	public int getWebRequestStatus(int requestId) {
		Log.i(TAG, "Get web request status: " + requestId);
		return 0;
	}

	public void openLoginWindow() {
		Log.i(TAG, "Open login window");
		this.runOnUiThread(new Runnable() {
			public void run() {
				showDialog(DIALOG_NOT_SUPPORTED);
			}
		});
		//nativeLoginData("Spartan", "Warrior", "Peacock");
	}

	public void setRefreshToken(String token) {
		Log.i(TAG, "set refresh token: " + token);
		this.refreshToken = token;
	}

	public void setSession(String session) {
		Log.i(TAG, "Session: " + session);
		this.session = session;
	}

	public boolean supportsNonTouchscreen() {
		Log.i(TAG, "Supports non touchscreen");
		return true;
	}

	public void webRequest(int requestId, long timestamp, String url, String method, String cookies) {
		Log.i(TAG, "Web request: " + requestId + ": " + timestamp + " :" + url + ":" + method + ":"+ cookies);
		nativeWebRequestCompleted(requestId, timestamp, 200, "SPARTA");
	}

	public boolean isSafeMode() {
		return tempSafeMode || PreferenceManager.getDefaultSharedPreferences(this).getBoolean("zz_safe_mode", false);
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
		return this.getResources().getInteger(R.integer.max_num_patches);
	}

	public boolean doesRequireGuiBlocksPatch() {
		if (forceFallback) return true;
		if (texturePack != null) {
			try {
				InputStream instr = texturePack.getInputStream("gui/gui_blocks.png");
				if (instr != null) instr.close();
				return instr == null;
			} catch (Exception e) {	
				e.printStackTrace();
				return false;
			}
		} else {
			try {
				InputStream instr = minecraftApkContext.getAssets().open("gui/gui_blocks.png");
				instr.close();
				return false;
			} catch (Exception e) {
				e.printStackTrace();
				return true;
			}
		}
	}

	protected void setupHoverCar() {
		hoverCar = new HoverCar(this);
		hoverCar.show(getWindow().getDecorView());
		hoverCar.mainButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(DIALOG_RUNTIME_OPTIONS);
			}
		});
	}

	protected void loadNativeAddons() {
		if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("zz_load_native_addons", false)) return;
		List<ApplicationInfo> apps = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
		for (ApplicationInfo app: apps) {
			if (app.metaData == null) continue;
			String nativeLibName = app.metaData.getString("net.zhuoweizhang.mcpelauncher.api.nativelibname");
			if (nativeLibName != null) {
				try {
					System.load(app.nativeLibraryDir + "/lib" + nativeLibName + ".so");
					loadedAddons.add(app.packageName);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	protected void migrateToPatchManager() {
		try {
			boolean enabledPatchMgr = getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0).getInt("patchManagerVersion", -1) > 0;
			if (enabledPatchMgr) return;
			showDialog(DIALOG_FIRST_LAUNCH);
			File patchesDir = this.getDir(PT_PATCHES_DIR, 0);
			PatchManager.getPatchManager(this).setEnabled(patchesDir.listFiles(), true);
			System.out.println(getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0).getString("enabledPatches", "LOL"));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void applyBuiltinPatches() {
		try {
			//Apply guiBlocks
			boolean patchGuiBlocks = doesRequireGuiBlocksPatch();
			System.out.println("Patching guiblocks: " + patchGuiBlocks);
			com.joshuahuelsman.patchtool.PTPatch patch = new com.joshuahuelsman.patchtool.PTPatch();
			//patch.loadPatch(patchGuiBlocks? MinecraftConstants.GUI_BLOCKS_PATCH : MinecraftConstants.GUI_BLOCKS_UNPATCH);
			patch.applyPatch(minecraftLibBuffer);
		} catch (IOException ie) {
			ie.printStackTrace();
		}

	}

	protected void loadTexturePack() {
		try {
			boolean loadTexturePack = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("zz_texture_pack_enable", false);
			String filePath = getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0).getString("texturePack", null);
			if (loadTexturePack && filePath != null) {
				File file = new File(filePath);
				System.out.println("File!! " + file);
				if (!file.exists()) {
					texturePack = null;
				} else {
					texturePack = new ZipTexturePack(file);
				}
			} else {
				texturePack = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.texture_pack_unable_to_load, Toast.LENGTH_LONG).show();
		}
	}

	/** enables the on-screen menu key on devices without a dedicated menu key, needed because target SDK is v15 */
	private void enableSoftMenuKey() {
		getWindow().addFlags(0x08000000); //FLAG_NEEDS_MENU_KEY
	}

	private void disableAllPatches() {
		Log.i(TAG, "Disabling all patches");
		PatchManager.getPatchManager(this).disableAllPatches();
	}
		

}
