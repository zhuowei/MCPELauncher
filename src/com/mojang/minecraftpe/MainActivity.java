package com.mojang.minecraftpe;

import java.io.*;

import java.lang.ref.WeakReference;

import java.nio.ByteBuffer;

import java.text.DateFormat;

import java.net.*;

import java.util.*;

import javax.net.ssl.*;
import java.security.*;
import java.security.cert.*;

import java.lang.reflect.Field;

import android.app.Activity;
import android.app.NativeActivity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.pm.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextWatcher;
import android.view.*;
import android.view.KeyCharacterMap;
import android.view.inputmethod.*;
import android.util.DisplayMetrics;
import android.util.Log;
import android.webkit.*;
import android.widget.*;

import android.preference.*;

import dalvik.system.PathClassLoader;

import org.mozilla.javascript.RhinoException;

import net.zhuoweizhang.mcpelauncher.*;

import net.zhuoweizhang.mcpelauncher.patch.PatchUtils;

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

	public static final String MOJANG_ACCOUNT_LOGIN_URL = "https://account.mojang.com/m/login?app=mcpe";

	/* private dialogs start here */
	public static final int DIALOG_CRASH_SAFE_MODE = 0x1000;
	public static final int DIALOG_RUNTIME_OPTIONS = 0x1001;
	public static final int DIALOG_INVALID_PATCHES = 0x1002;
	public static final int DIALOG_FIRST_LAUNCH = 0x1003;
	public static final int DIALOG_VERSION_MISMATCH_SAFE_MODE = 0x1004;
	public static final int DIALOG_NOT_SUPPORTED = 0x1005;
	public static final int DIALOG_UPDATE_TEXTURE_PACK = 0x1006;
	public static final int DIALOG_INSERT_TEXT = 0x1007;
	public static final int DIALOG_MULTIPLAYER_DISABLE_SCRIPTS = 0x1008;
	public static final int DIALOG_RUNTIME_OPTIONS_WITH_INSERT_TEXT = 0x1009;
	public static final int DIALOG_SELINUX_BROKE_EVERYTHING = 0x1000 + 10;

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

	private WebView loginWebView;

	private Dialog loginDialog;

	private Map<Integer, HurlRunner> requestMap = new HashMap<Integer, HurlRunner>();

	protected MinecraftVersion minecraftVersion;

	private boolean overlyZealousSELinuxSafeMode = false;

	private PopupWindow hiddenTextWindow;
	private TextView hiddenTextView;

	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		System.out.println("oncreate");

		File lockFile = new File(getFilesDir(), "running.lock");
		if (lockFile.exists()) {
			try {
				PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("zz_safe_mode", true).apply();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		try {
			lockFile.createNewFile();
		} catch (Exception e) {
			e.printStackTrace();
		}

		MinecraftVersion.context = this.getApplicationContext();

		try {
			mcPkgInfo = getPackageManager().getPackageInfo("com.mojang.minecraftpe", 0);
			mcAppInfo = mcPkgInfo.applicationInfo;/*getPackageManager().getApplicationInfo("com.mojang.minecraftpe", 0);*/
			MC_NATIVE_LIBRARY_DIR = mcAppInfo.nativeLibraryDir;
			MC_NATIVE_LIBRARY_LOCATION = MC_NATIVE_LIBRARY_DIR + "/libminecraftpe.so";
			System.out.println("libminecraftpe.so is at " + MC_NATIVE_LIBRARY_LOCATION);
			minecraftApkForwardLocked = !mcAppInfo.sourceDir.equals(mcAppInfo.publicSourceDir);
			int minecraftVersionCode = mcPkgInfo.versionCode;
			minecraftVersion = MinecraftVersion.getRaw(minecraftVersionCode);
			if (minecraftVersion == null) {
				tempSafeMode = true;
				showDialog(DIALOG_VERSION_MISMATCH_SAFE_MODE);
				minecraftVersion = MinecraftVersion.getDefault();
			}
			if (minecraftVersion.needsWarning) {
				Log.w(TAG, "OMG hipster version code found - breaking mod compat before it's cool");
			}
			net.zhuoweizhang.mcpelauncher.patch.PatchUtils.minecraftVersion = minecraftVersion;

			migrateToPatchManager();

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

		Utils.setLanguageOverride(this);

		forceFallback = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("zz_texture_pack_demo", false);

		loadTexturePack();

		textureOverrides.clear();
		textureOverrides.add(new SkinTextureOverride(this));
		if (allowScriptOverrideTextures()) {
			textureOverrides.add(new ScriptOverrideTexturePack(this));
		}

		ScriptTextureDownloader.attachCache(this);

		requiresGuiBlocksPatch = doesRequireGuiBlocksPatch();

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

		addLibraryDirToPath(MC_NATIVE_LIBRARY_DIR);

		setFakePackage(true);

		super.onCreate(savedInstanceState);

		setFakePackage(false);

		try {
			if (!isSafeMode()) {
				initPatching();
			}
			if (!isSafeMode() && minecraftLibBuffer != null) {
				loadNativeAddons();
				//applyPatches();
				applyBuiltinPatches();
				boolean shouldLoadScripts = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("zz_script_enable", true);
				if (shouldLoadScripts) ScriptManager.init(this);
			}

		} catch (Exception e) {
			e.printStackTrace();
			reportError(e);
		}

		enableSoftMenuKey();

		java.net.CookieManager cookieManager = new java.net.CookieManager();
		java.net.CookieHandler.setDefault(cookieManager);

		if (isSafeMode()) {
			if (overlyZealousSELinuxSafeMode) {
				showDialog(DIALOG_SELINUX_BROKE_EVERYTHING);
			} else {
				showDialog(DIALOG_CRASH_SAFE_MODE);
			}
		}

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
			} else {
				hoverCar.setVisible(!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("zz_hovercar_hide", false));
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
		hideKeyboardView();
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
		if (hoverCar != null) {
			hoverCar.dismiss();
			hoverCar = null;
		}
	}

	public void onStop() {
		super.onStop();
		ScriptTextureDownloader.flushCache();
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
			ByteBuffer libBuffer = ByteBuffer.wrap(libBytes);

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
				try {
					com.joshuahuelsman.patchtool.PTPatch patch = new com.joshuahuelsman.patchtool.PTPatch();
					patch.loadPatch(patchFile);
					if (!patch.checkMagic()) {
						failedPatches.add(patchFile.getName());
						continue;
					}
					//patch.applyPatch(libBytes);
					PatchUtils.patch(libBuffer, patch);
					patchedCount++;
				} catch (Exception e) {
					e.printStackTrace();
					failedPatches.add(patchFile.getName());
				}
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
				if (minecraftVersion.guiBlocksPatch != null) {
					patch.loadPatch(minecraftVersion.guiBlocksPatch);
					//patch.applyPatch(libBytes);
					PatchUtils.patch(libBuffer, patch);
				} //TODO: load patches from assets
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

	/*private int prePatchDir(byte[] libBytes, File patchesDir, PatchManager patchMgr, int patchedCount, int maxPatchNum) throws Exception {
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
	}*/

	public native void nativeRegisterThis();
	public native void nativeUnregisterThis();

	//added in 0.7.0:
	//sig changed in 0.7.3. :(
	public native void nativeLoginData(String session, String param2, String refreshToken, String user);
	public native void nativeStopThis();
	public native void nativeWebRequestCompleted(int requestId, long param2, int param3, String param4);

	//added in 0.7.2
	public native void nativeTypeCharacter(String character);

	//added in 0.8.0
	public native void nativeSuspend();
	public native void nativeSetTextboxText(String text);
	public native void nativeBackPressed();
	public native void nativeBackSpacePressed();
	public native void nativeReturnKeyPressed();

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
				inputStatus = INPUT_STATUS_IN_PROGRESS;
				Intent intent = getOptionsActivityIntent();
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
				return createRuntimeOptionsDialog(false);
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
			case DIALOG_INSERT_TEXT:
				return createInsertTextDialog();
			case DIALOG_MULTIPLAYER_DISABLE_SCRIPTS:
				return createMultiplayerDisableScriptsDialog();
			case DIALOG_RUNTIME_OPTIONS_WITH_INSERT_TEXT:
				return createRuntimeOptionsDialog(true);
			case DIALOG_SELINUX_BROKE_EVERYTHING:
				return createSELinuxBrokeEverythingDialog();
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
			.setPositiveButton(R.string.safe_mode_exit, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialogI, int button) {
					turnOffSafeMode();
				}
			})
			.setNegativeButton(R.string.safe_mode_continue, null)
			.create();
	}

	protected Dialog createRuntimeOptionsDialog(final boolean hasInsertText) {
		CharSequence livePatch = getResources().getString(R.string.hovercar_live_patch);
		CharSequence optionMenu = getResources().getString(R.string.hovercar_options);
		CharSequence insertText = getResources().getString(R.string.hovercar_insert_text);
		CharSequence manageModPEScripts = getResources().getString(R.string.pref_zz_manage_scripts);
		CharSequence takeScreenshot = getResources().getString(R.string.take_screenshot);
		CharSequence[] options = null;
		if (hasInsertText) {
			options = new CharSequence[] {livePatch, manageModPEScripts, takeScreenshot, optionMenu, insertText};
		} else {
			options = new CharSequence[] {livePatch, manageModPEScripts, takeScreenshot, optionMenu};
		}
		return new AlertDialog.Builder(this).setTitle(R.string.hovercar_title).
			setItems(options, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialogI, int button) {
					if (button == 0) {
						if (minecraftLibBuffer == null) {
							startActivity(getOptionsActivityIntent());
							return;
						}
						Intent intent = new Intent(MainActivity.this, ManagePatchesActivity.class);
						intent.putExtra("prePatchConfigure", false);
						startActivity(intent);
					} else if (button == 1) {
						Intent intent = new Intent(MainActivity.this, ManageScriptsActivity.class);
						startActivity(intent);
					} else if (button == 2) {
						boolean hasLoadedScripts = 
							PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean(
								"zz_script_enable", true) && !isSafeMode();
						if (hasLoadedScripts) {
							ScriptManager.takeScreenshot("screenshot");
						} else {
							new AlertDialog.Builder(MainActivity.this).
								setMessage(R.string.take_screenshot_requires_modpe_script).
								setPositiveButton(android.R.string.ok, null).
								show();
						}
					} else if (button == 3) {
						startActivity(getOptionsActivityIntent());
					} else if (button == 4 && hasInsertText) {
						showDialog(DIALOG_INSERT_TEXT);
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

	protected Dialog createBackupsNotSupportedDialog() {
		return new AlertDialog.Builder(this)
			.setMessage("Backed up versions of BlockLauncher are not supported, as" +
				" BlockLauncher depends on updates from the application store. " +
				" Please reinstall BlockLauncher. If you believe you received this message in error, contact zhuowei_applications@yahoo.com")
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialogI, int button) {
					finish();
				}
			})
			.setCancelable(false)
			.create();
	}

	protected Dialog createInsertTextDialog() {
		final EditText editText = new EditText(this);
		editText.setSingleLine(true);
		return new AlertDialog.Builder(this)
			.setTitle(R.string.hovercar_insert_text)
			.setView(editText)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialogI, int button) {
					try {
						nativeTypeCharacter(editText.getText().toString());
						editText.setText("");
					} catch (UnsatisfiedLinkError e) {
						showDialog(DIALOG_NOT_SUPPORTED);
					}
				}
			})
			.setNegativeButton(android.R.string.cancel, null)
			.create();
	}

	protected Dialog createMultiplayerDisableScriptsDialog() {
		return new AlertDialog.Builder(this)
			.setMessage(R.string.script_disabled_in_multiplayer)
			.setPositiveButton(android.R.string.ok, null)
			.create();
	}

	protected Dialog createSELinuxBrokeEverythingDialog() {
		return new AlertDialog.Builder(this)
			.setMessage(R.string.selinux_broke_everything)
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
		if (BuildConfig.DEBUG) Log.i(TAG, "getKey: " + keyCode + ":" + metaState + ":" + deviceId);
		KeyCharacterMap characterMap = KeyCharacterMap.load(deviceId);
		return characterMap.get(keyCode, metaState);
	}

	public static void saveScreenshot(String name, int firstInt, int secondInt, int[] thatArray) {
	}

	//added in 0.7.0
	public int abortWebRequest(int requestId) {
		Log.i(TAG, "Abort web request: " + requestId);
		HurlRunner runner = requestMap.get(requestId);
		if (runner != null) runner.isValid = false;
		return 0;
	}

	public String getRefreshToken() {
		Log.i(TAG, "Get Refresh token");
		return PreferenceManager.getDefaultSharedPreferences(this).getString("refreshToken", "");
	}

	public String getSession() {
		Log.i(TAG, "Get Session");
		return PreferenceManager.getDefaultSharedPreferences(this).getString("sessionId", "");
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
				loginWebView = new WebView(MainActivity.this);
				loginWebView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
				loginWebView.setWebViewClient(new LoginWebViewClient());
				WebSettings settings = loginWebView.getSettings();
				settings.setJavaScriptEnabled(true); //at least on Firefox, the webview tries to do some Ajax stuff
				/*loginPopup = new PopupWindow(loginWebView, ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
				loginPopup.showAtLocation(getWindow().getDecorView(), Gravity.CENTER, 0, 0);*/

				loginDialog = new Dialog(MainActivity.this);
				loginDialog.setCancelable(true);
				loginDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				loginDialog.setContentView(loginWebView);
				loginDialog.getWindow().setLayout(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
				loginDialog.show();

				loginWebView.loadUrl(getRealmsRedirectInfo().loginUrl);
			}
		});
		//nativeLoginData("Spartan", "Warrior", "Peacock");
	}

	public void setRefreshToken(String token) {
		Log.i(TAG, "Set refresh token");
		PreferenceManager.getDefaultSharedPreferences(this).edit().putString("refreshToken", token).apply();
	}

	public void setSession(String session) {
		Log.i(TAG, "Set Session");
		PreferenceManager.getDefaultSharedPreferences(this).edit().putString("sessionId", session).apply();
	}

	public boolean supportsNonTouchscreen() {
		Log.i(TAG, "Supports non touchscreen");
		return true;
	}

	public void webRequest(int requestId, long timestamp, String url, String method, String cookies) {
		this.webRequest(requestId, timestamp, url, method, cookies, "");
	}

	//signature change in 0.7.3
	public void webRequest(int requestId, long timestamp, String url, String method, String cookies, String extraParam) {
		if (BuildConfig.DEBUG) Log.i(TAG, "Web request: " + requestId + ": " + timestamp + " :" + url + ":" + method + ":"+ cookies + ":" + extraParam);
		//nativeWebRequestCompleted(requestId, timestamp, 200, "SPARTA");
		url = filterUrl(url);
		if (BuildConfig.DEBUG) Log.i(TAG, url);
		new Thread(new HurlRunner(requestId, timestamp, url, method, cookies)).start();
	}

	protected String filterUrl(String url) {
		return url;
		/*String peoapiRedirect = PreferenceManager.getDefaultSharedPreferences(this).getString("zz_redirect_mco_address", "NONE");
		if (peoapiRedirect.equals("NONE")) return url;
		RealmsRedirectInfo info = getRealmsRedirectInfo();
		if (info.accountUrl != null) {
			url.replace("account.mojang.com", info.accountUrl); //TODO: better system
		}
		return url.replace("peoapi.minecraft.net", peoapiRedirect);*/
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (BuildConfig.DEBUG) Log.i(TAG, event.toString());
		if (event.getAction() == KeyEvent.ACTION_MULTIPLE && event.getKeyCode() == KeyEvent.KEYCODE_UNKNOWN) {
			try {
				nativeTypeCharacter(event.getCharacters());
				return true;
			} catch (UnsatisfiedLinkError e) {
				//Do nothing
			}
		}
		return super.dispatchKeyEvent(event);
	}

	//added in 0.7.2

	public void showKeyboardView() {
		Log.i(TAG, "Show keyboard view");
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(getWindow().getDecorView(), InputMethodManager.SHOW_FORCED);
	}

	//added in 0.7.3
	public String getAccessToken() {
		Log.i(TAG, "Get access token");
		return PreferenceManager.getDefaultSharedPreferences(this).getString("accessToken", "");
	}

	public String getClientId() {
		Log.i(TAG, "Get client ID");
		return PreferenceManager.getDefaultSharedPreferences(this).getString("clientId", "");
	}

	public String getProfileId() {
		Log.i(TAG, "Get profile ID");
		return PreferenceManager.getDefaultSharedPreferences(this).getString("profileUuid", "");
	}

	public String getProfileName() {
		Log.i(TAG, "Get profile name");
		return PreferenceManager.getDefaultSharedPreferences(this).getString("profileName", "");
	}

	public void statsTrackEvent(String firstEvent, String secondEvent) {
		Log.i(TAG, "Stats track: " + firstEvent + ":" + secondEvent);
	}

	public void statsUpdateUserData(String firstEvent, String secondEvent) {
		Log.i(TAG, "Stats update user data: " + firstEvent + ":" + secondEvent);
	}

	public boolean isDemo() {
		Log.i(TAG, "Is demo");
		return false;
	}

	public void setLoginInformation(String accessToken, String clientId, String profileUuid, String profileName) {
		if (BuildConfig.DEBUG) Log.i(TAG, "Login info: " + accessToken + ":" + clientId + ":" + profileUuid + ":" + profileName);
		PreferenceManager.getDefaultSharedPreferences(this).edit().putString("accessToken", accessToken).
			putString("clientId", clientId).
			putString("profileUuid", profileUuid).
			putString("profileName", profileName).
			apply();
	}

	public void clearLoginInformation() {
		Log.i(TAG, "Clear login info");
		PreferenceManager.getDefaultSharedPreferences(this).edit().putString("accessToken", "").
			putString("clientId", "").
			putString("profileUuid", "").
			putString("profileName", "").
			apply();
	}

	//added in 0.8.0
	public void showKeyboard(final String mystr, final int maxLength, final boolean mybool) {
		Log.i(TAG, "Show keyboard: " + mystr + ":" + maxLength + ":" + mybool);
		this.runOnUiThread(new Runnable() {
			public void run() {
				showHiddenTextbox(mystr, maxLength, mybool);
			}
		});
		//showKeyboardView();
	}

	public void hideKeyboard() {
		Log.i(TAG, "Hide keyboard");
		this.runOnUiThread(new Runnable() {
			public void run() {
				dismissHiddenTextbox();
			}
		});
		//hideKeyboardView();
	}

	public void updateTextboxText(String text) {
		Log.i(TAG, "Update text to " + text);
		if (hiddenTextView == null) return;
		hiddenTextView.setText(text);
	}

	public void showHiddenTextbox(String text, int maxLength, boolean myBool) {
		if (hiddenTextWindow == null) {
			hiddenTextView = new EditText(this);
			hiddenTextView.addTextChangedListener(new PopupTextWatcher());
			hiddenTextView.setSingleLine(true);
			hiddenTextWindow = new PopupWindow(hiddenTextView);
			hiddenTextWindow.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			hiddenTextWindow.setFocusable(true);
			hiddenTextWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
		}

		hiddenTextView.setText(text);
		Selection.setSelection((Spannable) hiddenTextView.getText(), text.length());
		
		hiddenTextWindow.showAtLocation(this.getWindow().getDecorView(), Gravity.LEFT | Gravity.TOP, 0, 0);
		hiddenTextView.requestFocus();
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(hiddenTextView, InputMethodManager.SHOW_FORCED);
	}

	public void dismissHiddenTextbox() {
		if (hiddenTextWindow == null) return;
		hiddenTextWindow.dismiss();
		hideKeyboardView();
	}

	public boolean isSafeMode() {
		return tempSafeMode || PreferenceManager.getDefaultSharedPreferences(this).getBoolean("zz_safe_mode", false);
	}

	public void initPatching() throws Exception {
		/*if (BuildConfig.DEBUG) {
			System.err.println("Skipping this to simulate Samsung");
			System.err.println(minecraftLibBuffer);
			overlyZealousSELinuxSafeMode = true;
			tempSafeMode = true;
			return;
		}*/
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
			tempSafeMode = true;
			overlyZealousSELinuxSafeMode = true;
			return;
		}
		ByteBuffer buffer = PokerFace.createDirectByteBuffer(minecraftLibLocation, minecraftLibLength);
		//findMinecraftLibLocation();
		System.out.println("Has the byte buffer: " + buffer);
		minecraftLibBuffer = buffer;
	}

	public void applyPatches() throws Exception {
		ByteBuffer buffer = minecraftLibBuffer;
		/*buffer.position(0x1b6d50);//"v0.6.1" offset
		byte[] testBuffer = new byte[6];
		buffer.get(testBuffer);
		System.out.println("Before: " + Arrays.toString(testBuffer));
		buffer.position(0x1b6d50);//"v0.6.1" offset
		buffer.put(">9000!".getBytes());
		buffer.position(0x1b6d50);//"v0.6.1" offset
		buffer.get(testBuffer);
		System.out.println("After " + Arrays.toString(testBuffer));*/
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
		hoverCar.setVisible(!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("zz_hovercar_hide", false));
		hoverCar.mainButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				boolean showInsertText = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).
					getBoolean("zz_show_insert_text", false);
				showDialog(showInsertText? DIALOG_RUNTIME_OPTIONS_WITH_INSERT_TEXT: DIALOG_RUNTIME_OPTIONS);
				resetOrientation(); //for sensor controls. TODO: better place to do this?
			}
		});
	}

	protected void loadNativeAddons() {
		if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("zz_load_native_addons", false)) return;
		PackageManager pm = getPackageManager();
		List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
		for (ApplicationInfo app: apps) {
			if (app.metaData == null) continue;
			String nativeLibName = app.metaData.getString("net.zhuoweizhang.mcpelauncher.api.nativelibname");
			if (nativeLibName != null && pm.checkPermission("net.zhuoweizhang.mcpelauncher.ADDON", app.packageName) ==
				PackageManager.PERMISSION_GRANTED) {
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
			byte[] patchData = patchGuiBlocks? minecraftVersion.guiBlocksPatch : minecraftVersion.guiBlocksUnpatch;
			if (patchData != null) {
				patch.loadPatch(patchData);
				PatchUtils.patch(minecraftLibBuffer, patch);
			} //TODO: load from assets
		} catch (Exception ie) {
			ie.printStackTrace();
		}

		//Patch out the red item missing icon
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("zz_script_enable", true)) {
			ScriptManager.nativeRemoveItemBackground();
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
		int flag = Build.VERSION.SDK_INT >= 19? 0x40000000: 0x08000000; //FLAG_NEEDS_MENU_KEY
		getWindow().addFlags(flag); //KitKat reused old show menu key flag for transparent navbars
	}

	private void disableAllPatches() {
		Log.i(TAG, "Disabling all patches");
		PatchManager.getPatchManager(this).disableAllPatches();
	}

	protected void loginLaunchCallback(Uri launchUri) {
		loginDialog.dismiss();
		String session = launchUri.getQueryParameter("sessionId");
		if (session == null) return;
		String profileName = launchUri.getQueryParameter("profileName");
		String refreshToken = launchUri.getQueryParameter("identity");
		String accessToken = launchUri.getQueryParameter("accessToken");
		String clientToken = launchUri.getQueryParameter("clientToken");
		String profileUuid = launchUri.getQueryParameter("profileUuid");
		nativeLoginData(accessToken, clientToken, profileUuid, profileName);
	}

	protected Intent getOptionsActivityIntent() {
		return new Intent(this, MainMenuOptionsActivity.class);
	}

	public boolean isRedirectingRealms() {
		//String peoapiRedirect = PreferenceManager.getDefaultSharedPreferences(this).getString("zz_redirect_mco_address", "NONE");
		//return !peoapiRedirect.equals("NONE");
		return false;
	}

	public RealmsRedirectInfo getRealmsRedirectInfo() {
		//String peoapiRedirect = PreferenceManager.getDefaultSharedPreferences(this).getString("zz_redirect_mco_address", "NONE");
		return RealmsRedirectInfo.targets.get("NONE");
	}

	private void turnOffSafeMode() {
		PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("zz_safe_mode", false).commit();
		getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0).edit().putBoolean("force_prepatch", true).commit();
		finish();
		NerdyStuffActivity.forceRestart(this);
	}

	protected void hideKeyboardView() {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(this.getWindow().getDecorView().getWindowToken(), 0);
	}
	/**
	 * Called by the ScriptManager when a new level is loaded.
	 * This is for subclasses to do cleanup/disable menu items that cannot be used ingame/show ads, etc
	 */
	public void setLevelCallback(boolean isRemote) {
		if (isRemote && ScriptManager.scripts.size() > 0) {
			this.runOnUiThread(new Runnable() {
				public void run() {
					showDialog(DIALOG_MULTIPLAYER_DISABLE_SCRIPTS);
				}
			});
		}
	}

	/**
	 * Called by the ScriptManager when exiting to the main menu.
	 * This is for subclasses to do cleanup/disable menu items that cannot be used ingame/show ads, etc
	 */
	public void leaveGameCallback() {
	}

	public void scriptPrintCallback(final String message, final String scriptName) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(MainActivity.this, "Script " + scriptName + ": " + message, Toast.LENGTH_SHORT).show();
			}
		});
	}

	public void scriptOverrideTexture(final String theOverridden, final String url) {
		forceTextureReload();
	}

	public void scriptResetImages() {
		forceTextureReload();
	}

	public void forceTextureReload() {
		ScriptManager.nativeOnGraphicsReset();
	}

	private static String stringFromInputStream(InputStream in, int startingLength) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream(startingLength);
		try {
			byte[] buffer = new byte[1024];
			int count;
			while ((count = in.read(buffer)) != -1) {
				bytes.write(buffer, 0, count);
			}
			return bytes.toString("UTF-8");
		} finally {
			bytes.close();
		}
	}

	private void reportError(final Throwable t) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				final StringWriter strWriter = new StringWriter();
				PrintWriter pWriter = new PrintWriter(strWriter);
				t.printStackTrace(pWriter);
				new AlertDialog.Builder(MainActivity.this).setTitle("Oh nose everything broke").setMessage(strWriter.toString()).
					setPositiveButton(android.R.string.ok, null).
					setNeutralButton(android.R.string.copy, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface aDialog, int button) {
							ClipboardManager mgr = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
							mgr.setText(strWriter.toString());
						}
					}).
							
					show();
			}
		});
	}

	public void scriptErrorCallback(final String scriptName, final Throwable t) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				final StringWriter strWriter = new StringWriter();
				PrintWriter pWriter = new PrintWriter(strWriter);
				pWriter.println("Error occurred in script: " + scriptName);
				if (t instanceof RhinoException) {
					String lineSource = ((RhinoException) t).lineSource();
					if (lineSource != null) pWriter.println(lineSource);
				}
				t.printStackTrace(pWriter);
				new AlertDialog.Builder(MainActivity.this).setTitle(R.string.script_execution_error).setMessage(strWriter.toString()).
					setPositiveButton(android.R.string.ok, null).
					setNeutralButton(android.R.string.copy, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface aDialog, int button) {
							ClipboardManager mgr = (ClipboardManager) MainActivity.this.getSystemService(CLIPBOARD_SERVICE);
							mgr.setText(strWriter.toString());
						}
					}).
					show();
			}
		});
	}

	protected void resetOrientation() {
		//for sensor controls
	}

	public void scriptTooManyErrorsCallback(final String scriptName) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				new AlertDialog.Builder(MainActivity.this).setTitle(R.string.script_execution_error).
					setMessage(scriptName + " " + getResources().getString(R.string.script_too_many_errors)).
					setPositiveButton(android.R.string.ok, null).
					show();
			}
		});
	}

	public void screenshotCallback(final File file) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(MainActivity.this, getResources().getString(R.string.screenshot_saved_as) + " " +
					file.getAbsolutePath(), Toast.LENGTH_LONG).show();
				MediaScannerConnection.scanFile(MainActivity.this, 
					new String[] {file.getAbsolutePath()}, new String[] {"image/png"}, null);
			}
		});
	}

	protected boolean allowScriptOverrideTextures() {
		return false;
	}


	private void addLibraryDirToPath(String path) {
		try {
			ClassLoader classLoader = getClassLoader();
			Class<? extends ClassLoader> clazz = classLoader.getClass();
			Field field = Utils.getDeclaredFieldRecursive(clazz, "pathList");
			field.setAccessible(true);
			Object pathListObj = field.get(classLoader);
			Class<? extends Object> pathListClass = pathListObj.getClass();
			Field natfield = Utils.getDeclaredFieldRecursive(pathListClass, "nativeLibraryDirectories");
			natfield.setAccessible(true);
			File[] fileList = (File[]) natfield.get(pathListObj);
			File[] newList = addToFileList(fileList, new File(path));
			if (fileList != newList) natfield.set(pathListObj, newList);
			//check
			//System.out.println("Class loader shenanigans: " + ((PathClassLoader) getClassLoader()).findLibrary("minecraftpe"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private File[] addToFileList(File[] files, File toAdd) {
		boolean needsAdding = true;
		for (File f: files) {
			if (f.equals(toAdd)) {
				//System.out.println("Already added path to list");
				return files;
			}
		}
		File[] retval = new File[files.length + 1];
		System.arraycopy(files, 0, retval, 1, files.length);
		retval[0] = toAdd;
		return retval;
	}

	private class PopupTextWatcher implements TextWatcher {
		public void afterTextChanged(Editable e) {
			if (BuildConfig.DEBUG) Log.i(TAG, "Text changed: " + e.toString());
			nativeSetTextboxText(e.toString());
			hiddenTextWindow.dismiss();
			hiddenTextWindow.showAtLocation(MainActivity.this.getWindow().getDecorView(), Gravity.LEFT | Gravity.TOP, 0, 0);
			
		}
		public void beforeTextChanged(CharSequence c, int start, int count, int after) {
		}
		public void onTextChanged(CharSequence c, int start, int count, int after) {
		}
	}

	private class LoginWebViewClient extends WebViewClient {
		boolean hasFiredLaunchEvent = false;
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			Uri tempUri = Uri.parse(url);
			if (BuildConfig.DEBUG) Log.i(TAG, tempUri.toString());
			String endHost = getRealmsRedirectInfo().accountUrl;
			if (endHost == null) endHost = "account.mojang.com";
			if (tempUri.getHost().equals(endHost)) {
				if (tempUri.getPath().equals("/m/launch")) {
					loginLaunchCallback(tempUri);
					hasFiredLaunchEvent = true;
				} else {
					view.loadUrl(url);
				}
				return true;
			} else {
				return false;
			}

		}
		@Override
		public void onReceivedSslError (WebView view, SslErrorHandler handler, SslError error) {
			if (isRedirectingRealms()) {
				handler.proceed();
				return;
			}
			super.onReceivedSslError(view, handler, error);
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			Uri tempUri = Uri.parse(url);
			if (BuildConfig.DEBUG) Log.i(TAG, "PageStarted: " + tempUri.toString());
			String endHost = getRealmsRedirectInfo().accountUrl;
			if (endHost == null) endHost = "account.mojang.com";
			if (tempUri.getHost().equals(endHost)) {
				if (tempUri.getPath().equals("/m/launch") && !hasFiredLaunchEvent) {
					loginLaunchCallback(tempUri);
					hasFiredLaunchEvent = true;
				}
			}
		}
	}

	private class HurlRunner implements Runnable {
		private URL url;
		private String method, cookies, strurl;
		private int requestId;
		private long timestamp;
		private boolean isValid = true;

		private HttpURLConnection conn;

		public HurlRunner(int requestId, long timestamp, String url, String method, String cookies) {
			this.requestId = requestId;
			this.timestamp = timestamp;
			this.strurl = url;
			this.method = method;
			this.cookies = cookies;
			synchronized(requestMap) {
				requestMap.put(requestId, this);
			}
		}

		public void run() {
			InputStream is = null;
			String content = null;
			int response = 0;

			try {
				url = new URL(strurl);
				conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod(method);
				conn.setRequestProperty("Cookie", cookies);
				conn.setRequestProperty("User-Agent", "MCPE/Curl");
				conn.setUseCaches(false);
				conn.setDoInput(true);
				if (conn instanceof HttpsURLConnection && isRedirectingRealms()) {
					TrustModifier.relaxHostChecking(conn);
				}
				conn.connect();
				try {
					response = conn.getResponseCode();
					is = conn.getInputStream();
				} catch (Exception e) {
					is = conn.getErrorStream();
				}

				if (is != null) {
					content = stringFromInputStream(is, conn.getContentLength() < 0? 1024: conn.getContentLength());
				}

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (Exception e) {
					}
				}
			}

			if (content != null && BuildConfig.DEBUG) Log.i(TAG, url + ":" + response + ":" + content);
			
			if (isValid) {
				nativeWebRequestCompleted(requestId, timestamp, response, content);
			}
			synchronized(requestMap) {
				requestMap.remove(this);
			}
		}
		
	}	

}
