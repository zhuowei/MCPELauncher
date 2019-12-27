package com.mojang.minecraftpe;

import java.io.*;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.net.*;
import java.util.*;

import javax.net.ssl.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NativeActivity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface;
import android.content.pm.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaScannerConnection;
import android.media.AudioManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextWatcher;
import android.view.*;
import android.view.inputmethod.*;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.webkit.*;
import android.widget.*;

import dalvik.system.BaseDexClassLoader;

import org.mozilla.javascript.RhinoException;

import net.zhuoweizhang.mcpelauncher.*;
import static net.zhuoweizhang.mcpelauncher.Utils.isSafeMode;
import net.zhuoweizhang.mcpelauncher.patch.PatchUtils;
import net.zhuoweizhang.mcpelauncher.ui.AboutAppActivity;
import net.zhuoweizhang.mcpelauncher.ui.GetSubstrateActivity;
import net.zhuoweizhang.mcpelauncher.ui.HoverCar;
import net.zhuoweizhang.mcpelauncher.ui.MainMenuOptionsActivity;
import net.zhuoweizhang.mcpelauncher.ui.ManagePatchesActivity;
import net.zhuoweizhang.mcpelauncher.ui.ManageScriptsActivity;
import net.zhuoweizhang.mcpelauncher.ui.NerdyStuffActivity;
import net.zhuoweizhang.mcpelauncher.ui.NoMinecraftActivity;
import net.zhuoweizhang.mcpelauncher.ui.MinecraftNotSupportedActivity;
import net.zhuoweizhang.mcpelauncher.ui.TexturePacksActivity;
import net.zhuoweizhang.mcpelauncher.texture.*;
import net.zhuoweizhang.pokerface.PokerFace;

@SuppressLint("SdCardPath")
@SuppressWarnings("deprecation")
public class MainActivity extends NativeActivity {

	public static final String TAG = "BlockLauncher/Main";
	public static final String SCRIPT_SUPPORT_VERSION = "1.14.1.5";
	public static final String HALF_SUPPORT_VERSION = "1.15";
	private static final boolean HALF_VERSION_HAS_SCRIPTS = false;
	public static final String LITE_SUPPORT_VERSION = "~~~";
	private static final String SUPPORTED_VERSION_READABLE = "1.14.1";

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

	public static final String HEY_CAN_YOU_STOP_STEALING_BLOCKLAUNCHER_CODE = "please?";

	protected DisplayMetrics displayMetrics;

	protected TexturePack texturePack;

	protected Context minecraftApkContext;

	protected boolean fakePackage = false;

	public static final String[] GAME_MODES = { "creative", "survival" };

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

	public static boolean tempSafeMode = false;

	public String session = "";
	public String refreshToken = "";

	private PackageInfo mcPkgInfo;

	private WebView loginWebView;

	private Dialog loginDialog;

	private SparseArray<HurlRunner> requestMap = new SparseArray<HurlRunner>();

	protected MinecraftVersion minecraftVersion;

	private boolean overlyZealousSELinuxSafeMode = false;

	private PopupWindow hiddenTextWindow;
	private TextView hiddenTextView;
	private boolean hiddenTextDismissAfterOneLine = false;
	private PopupWindow commandHistoryWindow;
	private View commandHistoryView;
	private List<String> commandHistoryList = new ArrayList<String>();
	private Button prevButton, nextButton;
	private int commandHistoryIndex = 0;

	protected boolean hasRecorder = false;
	protected boolean isRecording = false;

	private boolean hasResetSafeModeCounter = false;

	private final static int MAX_FAILS = 2;

	private static final int REQUEST_PICK_IMAGE = 415;
	private static final int REQUEST_MANAGE_TEXTURES = 416;
	private static final int REQUEST_MANAGE_SCRIPTS = 417;
	private static final int REQUEST_STORAGE_PERMISSION = 418;

	private long pickImageCallbackAddress = 0;
	private Intent pickImageResult;

	private boolean controllerInit = false;

	private final Handler mainHandler = new Handler() {
		@Override
		public void dispatchMessage(Message msg) {
			toggleRecording();
		}
	};
	private int mcpeArch = ScriptManager.ARCH_ARM;
	public AddonOverrideTexturePack addonOverrideTexturePackInstance = null;
	private boolean textureVerbose = false;
	private PrintStream fileDataLog;

	private int storedSdkTarget = -1;
	private Class classVMRuntime;
	private Method methodVMRuntimeGetRuntime;
	private Method methodVMRuntimeGetTargetSdkVersion;
	private Method methodVMRuntimeSetTargetSdkVersion;
	private final boolean NATIVE_DEBUGGER_ENABLED = false;
	public static boolean forceLaunchedIntoDifferentAbi = false;
	private boolean extractNativeLibs = false;
	private File extractNativeLibsDir = null;

	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (needsRelaunchIntoCorrectAbi()) {
			relaunchIntoCorrectAbi();
		}
		if (BuildConfig.DEBUG && NATIVE_DEBUGGER_ENABLED && new File("/sdcard/bl_native_debugger.txt").exists()) {
			startNativeDebugger();
		}
		currentMainActivity = new WeakReference<MainActivity>(this);

		int safeModeCounter = Utils.getPrefs(2).getInt("safe_mode_counter", 0);
		System.out.println("Current fails: " + safeModeCounter);
		if (safeModeCounter == MAX_FAILS && !new File("/sdcard/bl_nosafemode.txt").exists()) {
			Utils.getPrefs(0).edit().putBoolean("zz_safe_mode", true).apply();
			safeModeCounter = 0;
		}
		safeModeCounter++;
		Utils.getPrefs(2).edit().putInt("safe_mode_counter", safeModeCounter).commit();
		textureVerbose = new File("/sdcard/bl_textureVerbose.txt").exists();

		MinecraftVersion.context = this.getApplicationContext();
		boolean needsToClearOverrides = false;

		try {
			mcPkgInfo = getPackageManager().getPackageInfo("com.mojang.minecraftpe", 0);
			mcAppInfo = mcPkgInfo.applicationInfo;
			MC_NATIVE_LIBRARY_DIR = mcAppInfo.nativeLibraryDir;
			MC_NATIVE_LIBRARY_LOCATION = MC_NATIVE_LIBRARY_DIR + "/libminecraftpe.so";
			System.out.println("libminecraftpe.so is at " + MC_NATIVE_LIBRARY_LOCATION);
			checkArch();
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
			com.mojang.minecraftpe.store.Store.needsNativeListenerCallback =
				mcpeGreaterEqualThan(mcPkgInfo.versionName, 1, 10, 0);

			boolean isTooOldMinorVersion = !mcpeGreaterEqualThan(mcPkgInfo.versionName, 1, 11, 1);
			boolean isSupportedVersion = (mcPkgInfo.versionName.startsWith(SCRIPT_SUPPORT_VERSION) &&
				!isTooOldMinorVersion) ||
				mcPkgInfo.versionName.startsWith(HALF_SUPPORT_VERSION) ||
				mcPkgInfo.versionName.startsWith(LITE_SUPPORT_VERSION);
			// && !mcPkgInfo.versionName.startsWith("0.11.0");

			if (!isSupportedVersion) {
				Intent intent = new Intent(this, MinecraftNotSupportedActivity.class);
				intent.putExtra("minecraftVersion", mcPkgInfo.versionName);
				intent.putExtra("supportedVersion", SUPPORTED_VERSION_READABLE);
				startActivity(intent);
				finish();
				try {
					Thread.sleep(1000);
					android.os.Process.killProcess(android.os.Process.myPid());
				} catch (Throwable t) {
				}
			}

			checkForSubstrate();

			fixMyEpicFail();

			migrateToPatchManager();

			SharedPreferences myprefs = Utils.getPrefs(1);
			int prepatchedVersionCode = myprefs.getInt("prepatch_version", -1);

			if (prepatchedVersionCode != minecraftVersionCode) {
				System.out.println("Version updated; forcing prepatch");
				myprefs.edit().putBoolean("force_prepatch", true).apply();
				disableAllPatches();
				needsToClearOverrides = true;
			}

			int lastVersionCode = myprefs.getInt("last_version", -1);

			if (lastVersionCode != minecraftVersionCode) {
				// don't depend on prepatch
				Utils.getPrefs(0).edit().putBoolean("zz_texture_pack_enable", false).apply();
				myprefs.edit().putInt("last_version", minecraftVersionCode).apply();
				if (myprefs.getString("texture_packs", "").indexOf("minecraft.apk") >= 0) {
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
				minecraftApkContext = createPackageContext("com.mojang.minecraftpe",
						Context.CONTEXT_IGNORE_SECURITY);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, "Can't create package context for the original APK",
					Toast.LENGTH_LONG).show();
			finish();
		}

		Utils.setLanguageOverride();

		forceFallback = new File("/sdcard/bl_forcefallback.txt").exists();

		//loadTexturePack();

		textureOverrides.clear();

		loadTexturePack();

		if (allowScriptOverrideTextures()) {
			textureOverrides.add(new ScriptOverrideTexturePack(this));
		}

		ScriptTextureDownloader.attachCache(this);

		requiresGuiBlocksPatch = doesRequireGuiBlocksPatch();

		extractNativeLibs = false;

		try {
			loadLibrary("mcpelauncher_early");
		} catch (UnsatisfiedLinkError e) {
			System.err.println(e);
			forceLaunchedIntoDifferentAbi = true;
			// for now, always re-extract the libs on every load.
			extractNativeLibs = true;
		}

		if (extractNativeLibs) {
			try {
				extractNativeLibsIfNeeded();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			loadLibrary("mcpelauncher_early");
		}

		try {
			if ((!isSafeMode() && Utils.getPrefs(0).getBoolean("zz_manage_patches", true)) ||
				getMCPEVersion().startsWith(HALF_SUPPORT_VERSION) ||
				getMCPEVersion().startsWith(LITE_SUPPORT_VERSION)) {
				prePatch();
			}
		} catch (Exception e) {
			e.printStackTrace();
			// showDialog(DIALOG_UNABLE_TO_PATCH);
		}

		//org.fmod.FMOD.assetManager = getAssets();
		boolean shouldInitPatching = !isSafeMode() || requiresPatchingInSafeMode();

		// between here and end init patching, we temporarily drop to Lollipop target SDK
		// this is a danger zone: run as little code as possible in here.
		lowerSdkTarget();

		try {
			//System.load(mcAppInfo.nativeLibraryDir + "/libgnustl_shared.so");
			System.load(mcAppInfo.nativeLibraryDir + "/libfmod.so");
			System.load(MC_NATIVE_LIBRARY_LOCATION);
		} catch (Exception e) {
			throw new RuntimeException(e);
			//e.printStackTrace();
			//Toast.makeText(this, "Can't load libminecraftpe.so from the original APK",
			//		Toast.LENGTH_LONG).show();
			//finish();
		}

		try {
			if (shouldInitPatching) {
				initPatching();
			}
		} catch (Exception e) {
			e.printStackTrace();
			reportError(e);
		}

		restoreSdkTarget();
		// we should be back to regular target SDK.

		org.fmod.FMOD.init(this);

		libLoaded = true;

		try {
			if (shouldInitPatching) {
				//initPatching();
				if (minecraftLibBuffer != null) {
					boolean signalHandler = Utils.getPrefs(0).getBoolean("zz_signal_handler", false);
					ScriptManager.nativePrePatch(signalHandler, this, /* limited? */ !hasScriptSupport(),
						mcPkgInfo.versionCode);
					if (hasScriptSupport() && Utils.getPrefs(0).getBoolean("zz_desktop_gui", false)) {
						ScriptManager.nativeModPESetDesktopGui(true);
					}
					if (!isSafeMode()) loadNativeAddons();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			reportError(e);
		}

		try {
			boolean shouldLoadScripts = hasScriptSupport();
			if (!isSafeMode() && minecraftLibBuffer != null) {
				applyBuiltinPatches();
				if (shouldLoadScripts && Utils.getPrefs(0).getBoolean("zz_script_enable", true)) {
					ScriptManager.init(this);
					textureOverrides.add(ScriptManager.modPkgTexturePack);
				}
			}
			if (isSafeMode() || !shouldLoadScripts) {
				ScriptManager.loadEnabledScriptsNames(this);
				// in safe mode, script names, but not the actual scripts,
				// should be loaded
			}

		} catch (Exception e) {
			e.printStackTrace();
			reportError(e);
		}

		if (needsToClearOverrides) ScriptManager.clearTextureOverrides();

		initAtlasMeta();

		if (BuildConfig.DEBUG && new File("/sdcard/bl_enablelog.txt").exists()) {
			try {
				fileDataLog = new PrintStream(new File("/sdcard/bl_filelog.txt"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		displayMetrics = new DisplayMetrics();

		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

		addLibraryDirToPath(MC_NATIVE_LIBRARY_DIR);

		setFakePackage(true);

		super.onCreate(savedInstanceState);

		nativeRegisterThis();

		setFakePackage(false);

		Utils.setupTheme(this, true);

		//enableSoftMenuKey();
		disableTransparentSystemBar();

		java.net.CookieManager cookieManager = new java.net.CookieManager();
		java.net.CookieHandler.setDefault(cookieManager);

		if (isSafeMode()) {
			if (overlyZealousSELinuxSafeMode) {
				showDialog(DIALOG_SELINUX_BROKE_EVERYTHING);
			} else {
				showDialog(DIALOG_CRASH_SAFE_MODE);
			}
		}

		// note that Kamcord works better with targetSdkVersion=19 than with 21
		initKamcord();

		grabPermissionsIfNeeded();

		System.gc();

	}

	@Override
	protected void onStart() {
		super.onStart();
		processIntentImport();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (hasResetSafeModeCounter) {
			int safeModeCounter = Utils.getPrefs(2).getInt("safe_mode_counter", 0);
			safeModeCounter++;
			Utils.getPrefs(2).edit().putInt("safe_mode_counter", safeModeCounter).commit();
		}
		if (hoverCar == null) {
			getWindow().getDecorView().post(new Runnable() {
				public void run() {
					try {
						setupHoverCar();
					} catch (Exception e) {
						e.printStackTrace();
					} // don't force close on hover car fail
				}
			});
		} else {
			hoverCar.setVisible(!Utils.getPrefs(0).getBoolean("zz_hovercar_hide", false));
		}
		setImmersiveMode(Utils.getPrefs(0).getBoolean("zz_immersive_mode", false));

	}

	@Override
	protected void onPause() {
		nativeSuspend();
		super.onPause();
		Utils.getPrefs(2).edit().putInt("safe_mode_counter", 0).commit();
		hasResetSafeModeCounter = true;
		hideKeyboardView();
	}

	public void onDestroy() {
		nativeUnregisterThis();
		super.onDestroy();
		File lockFile = new File(getFilesDir(), "running.lock");
		if (lockFile.exists())
			lockFile.delete();
		if (hoverCar != null) {
			hoverCar.dismiss();
			hoverCar = null;
		}
		ScriptManager.destroy();

		System.exit(0);
	}

	public void onStop() {
		nativeStopThis();
		super.onStop();
		ScriptTextureDownloader.flushCache();
		System.gc();
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
		boolean forcePrePatch = Utils.getPrefs(1).getBoolean("force_prepatch", true);
		if (!hasPrePatched && Utils.getEnabledPatches().size() == 0) {
			// no patches needed
			hasPrePatched = true;
			if (newMinecraft.exists()) newMinecraft.delete();
			if (forcePrePatch) Utils.getPrefs(1).edit().putBoolean("force_prepatch", false)
					.putInt("prepatch_version", mcPkgInfo.versionCode).apply();
			return;
		}
		if (!hasPrePatched && (!newMinecraft.exists() || forcePrePatch)) {

			System.out.println("Forcing new prepatch");

			byte[] libBytes = new byte[(int) originalLibminecraft.length()];
			ByteBuffer libBuffer = ByteBuffer.wrap(libBytes);

			InputStream is = new FileInputStream(originalLibminecraft);
			is.read(libBytes);
			is.close();

			int patchedCount = 0;
			int maxPatchNum = getMaxNumPatches();

			Set<String> patchLocs = Utils.getEnabledPatches();

			for (String patchLoc : patchLocs) {
				if (maxPatchNum >= 0 && patchedCount >= maxPatchNum)
					break;
				File patchFile = new File(patchLoc);
				if (!patchFile.exists())
					continue;
				try {
					com.joshuahuelsman.patchtool.PTPatch patch = new com.joshuahuelsman.patchtool.PTPatch();
					patch.loadPatch(patchFile);
					if (!patch.checkMagic()) {
						failedPatches.add(patchFile.getName());
						continue;
					}
					// patch.applyPatch(libBytes);
					PatchUtils.patch(libBuffer, patch);
					patchedCount++;
				} catch (Exception e) {
					e.printStackTrace();
					failedPatches.add(patchFile.getName());
				}
			}

			/*
			 * patchedCount = prePatchDir(libBytes, patchesDir, patchMgr,
			 * patchedCount, maxPatchNum); patchedCount = prePatchDir(libBytes,
			 * new File(Environment.getExternalStorageDirectory(),
			 * "Android/data/com.snowbound.pockettool.free/Patches"), patchMgr,
			 * patchedCount, maxPatchNum); patchedCount = prePatchDir(libBytes,
			 * new File(Environment.getExternalStorageDirectory(),
			 * "Android/data/com.snowbound.pockettool.free/Patches"), patchMgr,
			 * patchedCount, maxPatchNum);
			 */

			/* patching specific built-in patches */
			if (requiresGuiBlocksPatch) {
				System.out.println("Patching guiblocks");
				com.joshuahuelsman.patchtool.PTPatch patch = new com.joshuahuelsman.patchtool.PTPatch();
				if (minecraftVersion.guiBlocksPatch != null) {
					patch.loadPatch(minecraftVersion.guiBlocksPatch);
					// patch.applyPatch(libBytes);
					PatchUtils.patch(libBuffer, patch);
				} // TODO: load patches from assets
			}

			OutputStream os = new FileOutputStream(newMinecraft);
			os.write(libBytes);
			os.close();
			hasPrePatched = true;
			Utils.getPrefs(1).edit().putBoolean("force_prepatch", false)
					.putInt("prepatch_version", mcPkgInfo.versionCode).apply();
			if (failedPatches.size() > 0) {
				showDialog(DIALOG_INVALID_PATCHES);
			}

		}

		MC_NATIVE_LIBRARY_DIR = patched.getCanonicalPath();
		MC_NATIVE_LIBRARY_LOCATION = newMinecraft.getCanonicalPath();
	}

	/*
	 * private int prePatchDir(byte[] libBytes, File patchesDir, PatchManager
	 * patchMgr, int patchedCount, int maxPatchNum) throws Exception { if
	 * (!patchesDir.exists()) return patchedCount; File[] patches =
	 * patchesDir.listFiles();
	 * 
	 * for (File f: patches) { if (maxPatchNum >= 0 && patchedCount >=
	 * maxPatchNum) break; if (!patchMgr.isEnabled(f)) continue;
	 * com.joshuahuelsman.patchtool.PTPatch patch = new
	 * com.joshuahuelsman.patchtool.PTPatch(); patch.loadPatch(f);
	 * patch.applyPatch(libBytes); patchedCount++; } return patchedCount; }
	 */

	public native void nativeRegisterThis();

	public native void nativeUnregisterThis();

	// added in 0.7.0:
	// sig changed in 0.7.3. :(
	public native void nativeLoginData(String session, String param2, String refreshToken,
			String user);

	public native void nativeStopThis();

	public native void nativeWebRequestCompleted(int requestId, long param2, int param3,
			String param4);

	// added in 0.7.2
	public native void nativeTypeCharacter(String character);

	// added in 0.8.0
	public native void nativeSuspend();

	public native void nativeSetTextboxText(String text);

	public native void nativeBackPressed();

	public native void nativeBackSpacePressed();

	public native void nativeReturnKeyPressed();

	public native void nativeProcessIntentUriQuery(String a, String b);

	// added in 0.16.0
	public native void nativeKeyHandler(int keyCode, boolean keyDown);

	public void buyGame() {
	}

	public int checkLicense() {
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
			//loadTexturePack();
			if (!isSafeMode()) {
				applyBuiltinPatches();
			}
		} else if (requestCode == REQUEST_PICK_IMAGE) {
			if (resultCode == Activity.RESULT_OK) {
				pickImageResult = intent;
				File tempFile = copyContentStoreToTempFile(intent.getData());
				nativeOnPickImageSuccess(pickImageCallbackAddress, tempFile.getAbsolutePath());
			} else {
				nativeOnPickImageCanceled(pickImageCallbackAddress);
			}
		} else if (requestCode == REQUEST_MANAGE_TEXTURES || requestCode == REQUEST_MANAGE_SCRIPTS) {
			if (resultCode == RESULT_OK) {
				finish();
				NerdyStuffActivity.forceRestart(this);
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
						String worldName = ((TextView) dialog.findViewById(R.id.world_name_entry))
								.getText().toString();
						String worldSeed = ((TextView) dialog.findViewById(R.id.world_seed_entry))
								.getText().toString();
						String worldGameMode = GAME_MODES[((Spinner) dialog
								.findViewById(R.id.world_gamemode_spinner))
								.getSelectedItemPosition()];
						userInputStrings = new String[] { worldName, worldSeed, worldGameMode };
						inputStatus = INPUT_STATUS_OK;

					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialogI, int button) {
						inputStatus = INPUT_STATUS_CANCELLED;
					}
				}).setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialogI) {
						inputStatus = INPUT_STATUS_CANCELLED;
					}
				}).create();
	}

	protected Dialog createSafeModeDialog(int messageRes) {
		return new AlertDialog.Builder(this).setMessage(messageRes)
				.setPositiveButton(R.string.safe_mode_exit, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialogI, int button) {
						turnOffSafeMode();
					}
				}).setNegativeButton(R.string.safe_mode_continue, null).create();
	}

	protected Dialog createRuntimeOptionsDialog(final boolean hasInsertText) {
		CharSequence livePatch = getResources().getString(R.string.pref_texture_pack);
		final CharSequence optionMenu = getResources().getString(R.string.hovercar_options);
		final CharSequence insertText = getResources().getString(R.string.hovercar_insert_text);
		CharSequence manageModPEScripts = getResources().getString(R.string.pref_zz_manage_scripts);
		CharSequence takeScreenshot = getResources().getString(R.string.take_screenshot);
		final CharSequence startRecording = getResources().getString(R.string.hovercar_start_recording);
		final CharSequence stopRecording = getResources().getString(R.string.hovercar_stop_recording);
		final List<CharSequence> options = new ArrayList<CharSequence>(
			Arrays.asList(livePatch, manageModPEScripts, takeScreenshot));
		if (hasRecorder) {
			isRecording = isKamcordRecording();
			options.add(isRecording? stopRecording: startRecording);
		}
		if (hasInsertText) {
			options.add(insertText);
		}
		options.add(optionMenu);
		AlertDialog.Builder builder =
			new AlertDialog.Builder(this).setTitle(isSafeMode()? R.string.pref_zz_safe_mode: R.string.app_name)
				.setItems(options.toArray(new CharSequence[0]), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialogI, int button) {
						CharSequence buttonText = options.get(button);
						if (button == 0) {
							Intent intent = new Intent(MainActivity.this, TexturePacksActivity.class);
							startActivityForResult(intent, REQUEST_MANAGE_TEXTURES);
						} else if (button == 1) {
							if (hasScriptSupport()) {
								Intent intent = new Intent(MainActivity.this,
										ManageScriptsActivity.class);
								startActivityForResult(intent, REQUEST_MANAGE_SCRIPTS);
							} else {
								new AlertDialog.Builder(MainActivity.this)
										.setMessage("Scripts are not supported yet in Minecraft PE "
											+ mcPkgInfo.versionName)
										.setPositiveButton(android.R.string.ok, null).show();
							}
						} else if (button == 2) {
							boolean hasLoadedScripts = Utils.getPrefs(0).getBoolean(
									"zz_script_enable", true)
									&& !isSafeMode() && hasScriptSupport();
							if (hasLoadedScripts) {
								ScriptManager.takeScreenshot("screenshot");
							} else {
								new AlertDialog.Builder(MainActivity.this)
										.setMessage(R.string.take_screenshot_requires_modpe_script)
										.setPositiveButton(android.R.string.ok, null).show();
							}
						} else if (buttonText.equals(optionMenu)) {
							startActivity(getOptionsActivityIntent());
						} else if (buttonText.equals(insertText)) {
							showDialog(DIALOG_INSERT_TEXT);
						} else if (buttonText.equals(startRecording) || buttonText.equals(stopRecording)) {
							//toggleRecording();
							mainHandler.sendEmptyMessageDelayed(327, 1000); // 1 second delay
						}
					}
				});
		if (Build.VERSION.SDK_INT >= 19) { // KitKat, introduction of immersive mode
			builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
				public void onDismiss(DialogInterface dialog) {
					touchImmersiveMode();
				}
			});
		}
		return builder.create();
	}

	protected Dialog createInvalidPatchesDialog() {
		return new AlertDialog.Builder(this)
				.setMessage(
						getResources().getString(R.string.manage_patches_invalid_patches)
								+ "\n"
								+ PatchManager.join(failedPatches.toArray(PatchManager.blankArray),
										"\n")).setPositiveButton(android.R.string.ok, null)
				.create();
	}

	protected Dialog createFirstLaunchDialog() {
		StringBuilder dialogMsg = new StringBuilder();
		dialogMsg.append(getResources().getString(R.string.firstlaunch_generic_intro)).append(
				"\n\n");
		if (minecraftApkForwardLocked) {
			dialogMsg.append(getResources().getString(R.string.firstlaunch_jelly_bean)).append(
					"\n\n");
		}
		dialogMsg.append(getResources().getString(R.string.firstlaunch_see_options)).append("\n\n");
		return new AlertDialog.Builder(this).setTitle(R.string.firstlaunch_title)
				.setMessage(dialogMsg.toString()).setPositiveButton(android.R.string.ok, null)
				.setNeutralButton(R.string.firstlaunch_help, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialogI, int button) {
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(Uri.parse(AboutAppActivity.FORUMS_PAGE_URL));
						startActivity(intent);
					}
				}).create();
	}

	protected Dialog createCopyWorldDialog() {
		final View textEntryView = getLayoutInflater().inflate(R.layout.copy_world_dialog, null);
		return new AlertDialog.Builder(this)
				.setTitle(R.string.copy_world_title)
				.setView(textEntryView)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialogI, int button) {
						AlertDialog dialog = (AlertDialog) dialogI;
						String worldName = ((TextView) dialog.findViewById(R.id.world_name_entry))
								.getText().toString();
						userInputStrings = new String[] { worldName };
						inputStatus = INPUT_STATUS_OK;
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialogI, int button) {
						inputStatus = INPUT_STATUS_CANCELLED;
					}
				}).setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialogI) {
						inputStatus = INPUT_STATUS_CANCELLED;
					}
				}).create();
	}

	protected Dialog createNotSupportedDialog() {
		return new AlertDialog.Builder(this).setMessage(R.string.feature_not_supported)
				.setPositiveButton(android.R.string.ok, null).create();
	}

	protected Dialog createUpdateTexturePackDialog() {
		return new AlertDialog.Builder(this).setMessage(R.string.extract_textures_need_update)
				.setPositiveButton(android.R.string.ok, null).create();
	}

	protected Dialog createBackupsNotSupportedDialog() {
		return new AlertDialog.Builder(this)
				.setMessage(
						"Backed up versions of BlockLauncher are not supported, as"
								+ " BlockLauncher depends on updates from the application store. "
								+ " Please reinstall BlockLauncher. If you believe you received this message in error, contact zhuowei_applications@yahoo.com")
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialogI, int button) {
						finish();
					}
				}).setCancelable(false).create();
	}

	protected Dialog createInsertTextDialog() {
		final EditText editText = new EditText(this);
		editText.setSingleLine(false);
		final LinearLayout ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.HORIZONTAL);
		ll.addView(editText, ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		Button back = new Button(this);
		back.setText(R.string.hovercar_insert_text_backspace);
		back.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					//nativeTypeCharacter("" + ((char) 0x08)); // is this the
																// correct
																// method of
																// backspace?
				} catch (Exception e) {
					showDialog(DIALOG_NOT_SUPPORTED);
				}
			}
		});
		ll.addView(back, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		return new AlertDialog.Builder(this).setTitle(R.string.hovercar_insert_text).setView(ll)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialogI, int button) {
						/*
						try {
							String[] lines = editText.getText().toString().split("\n");
							for (int line = 0; line < lines.length; line++) {
								if (line != 0)
									nativeTypeCharacter("" + ((char) 0x0A));
								// I am not sure if keyboard-entered "enter" is
								// 0x0A
								nativeTypeCharacter(lines[line]);
							}
							editText.setText("");
						} catch (UnsatisfiedLinkError e) {
							showDialog(DIALOG_NOT_SUPPORTED);
						}
						*/
					}
				}).setNegativeButton(android.R.string.cancel, null).create();
	}

	protected Dialog createMultiplayerDisableScriptsDialog() {
		return new AlertDialog.Builder(this).setMessage(R.string.script_disabled_in_multiplayer)
				.setPositiveButton(android.R.string.ok, null).create();
	}

	protected Dialog createSELinuxBrokeEverythingDialog() {
		return new AlertDialog.Builder(this).setMessage(R.string.selinux_broke_everything)
				.setPositiveButton(android.R.string.ok, null).create();
	}

	/**
	 * @param time
	 *            Unix timestamp
	 * @returns a formatted time value
	 */

	public String getDateString(int time) {
		//System.out.println("getDateString: " + time);
		return DateFormat.getDateInstance(DateFormat.SHORT, Locale.US).format(
				new Date(((long) time) * 1000));
	}
	public byte[] getFileDataBytes(String name) {
		byte[] bytes = getFileDataBytes(name, false);
		if (name.endsWith(".meta")) { // hack for people trying to use 0.11 textures on 0.12.1
			String fileStr = new String(bytes, Charset.forName("UTF-8"));
			if (!fileStr.contains("portal") && !fileStr.contains("rabbit_foot")) {
				bytes = getFileDataBytes(name, true);
			}
		}
		return bytes;
	}

	public byte[] getFileDataBytes(String name, boolean forceInternal) {
		//if (BuildConfig.DEBUG) System.out.println("Get file data: " + name);
		if (BuildConfig.DEBUG && fileDataLog != null) {
			fileDataLog.println("Get file data: " + name);
		}
		try {
			InputStream is = null;
			if (name.charAt(0) == '/') {
				is = getRegularInputStream(name);
			} else if (name.equals("behavior_packs/vanilla/entities/villager.json") ||
				name.equals("resourcepacks/vanilla/server/entities/villager.json")) {
				// FIXME 0.17: villagers
				// kludge to make villagers rideable
				is = openFallbackAsset(name);
			} else {
				is = forceInternal? getLocalInputStreamForAsset(name): getInputStreamForAsset(name);
			}
			if (is == null || TAG.hashCode() != -1771687045)
				return null;
			// can't always find length - use the method from
			// http://www.velocityreviews.com/forums/t136788-store-whole-inputstream-in-a-string.html
			// instead
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			while (true) {
				int len = is.read(buffer);
				if (len < 0) {
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

	public InputStream getInputStreamForAsset(String name) {
		return getInputStreamForAsset(name, null);
	}

	public InputStream getInputStreamForAsset(String name, long[] lengthOut) {
		InputStream is = null;
		try {
			for (int i = 0; i < textureOverrides.size(); i++) {
				try {
					is = textureOverrides.get(i).getInputStream(name);
					if (is != null) {
						if (lengthOut != null) lengthOut[0] = textureOverrides.get(i).getSize(name);
						return is;
					}
				} catch (IOException e) {
				}
			}

			if (texturePack == null) {
				return getLocalInputStreamForAsset(name, lengthOut);
			} else {
				System.out.println("Trying to load  " + name + "from tp");
				is = texturePack.getInputStream(name);
				if (is == null) {
					System.out.println("Can't load " + name + " from tp");
					return getLocalInputStreamForAsset(name, lengthOut);
				}
			}
			return is;
		} catch (Exception e) {
			System.err.println(e);
			return null;
		}
	}

	protected InputStream getLocalInputStreamForAsset(String name) {
		return getLocalInputStreamForAsset(name, null);
	}

	private boolean mcpeGreaterEqualThan(int major, int minor, int rel) {
		return mcpeGreaterEqualThan(getMCPEVersion(), major, minor, rel);
	}
	private static boolean mcpeGreaterEqualThan(String baseVersion, int major, int minor, int rel) {
		String[] parts = baseVersion.split("\\.");
		if (parts.length == 0) return true;
		int theirMajor = Integer.parseInt(parts[0]);
		int theirMinor = parts.length < 2? 0: Integer.parseInt(parts[1]);
		int theirRel = parts.length < 3? 0: Integer.parseInt(parts[2]);
		if (theirMajor > major) return true;
		if (theirMajor < major) return false;
		// major is equal
		if (theirMinor > minor) return true;
		if (theirMinor < minor) return false;
		// minor is also equal
		return theirRel >= rel;
	}

	protected InputStream openFallbackAsset(String name) throws IOException {
		return getAssets().open(name);
	}

	protected InputStream getLocalInputStreamForAsset(String name, long[] lengthOut) {
		InputStream is = null;
		try {
			if (forceFallback) {
				return openFallbackAsset(name);
			}
			try {
				is = minecraftApkContext.getAssets().open(name);
			} catch (Exception e) {
				// e.printStackTrace();
				if (textureVerbose) System.out.println("Attempting to load fallback");
				is = openFallbackAsset(name);
			}
			if (is == null) {
				if (textureVerbose) System.out.println("Can't find it in the APK - attempting to load fallback");
				is = openFallbackAsset(name);
			}
			if (is != null && lengthOut != null) {
				lengthOut[0] = is.available();
			}
			return is;
		} catch (Exception e) {
			if (textureVerbose) System.err.println(e);
			return null;
		}
	}

	public String[] listDirForPath(String dirPath) {
		System.out.println("Listing dir for " + dirPath);
		String prefix = dirPath + "/";
		Set<String> outList = new HashSet<String>();
		//System.out.println("Start listing texture overrides");
		long start = System.currentTimeMillis();
		for (TexturePack pack: textureOverrides) {
			List<String> addedFiles = null;
			try {
				addedFiles = pack.listFiles();
			} catch (IOException ie) {
				continue;
			}
			for (String path: addedFiles) {
				if (!path.startsWith(prefix) || path.indexOf("/", prefix.length()) != -1) continue;
				outList.add(path.substring(path.lastIndexOf("/")));
			}
		}
		//System.out.println("End listing texture overrides " + (System.currentTimeMillis() - start));
		String[] origDir = null;
		String newPath = dirPath;

		try {
			origDir = this.getAssets().list(newPath);
		} catch (IOException ie) {
			origDir = new String[0];
		}
		//System.out.println("list 1 " + (System.currentTimeMillis() - start));
		// merge
		for (String s: origDir) {
			outList.add(s);
		}

		//System.out.println("End merge 1 " + (System.currentTimeMillis() - start));

		try {
			origDir = minecraftApkContext.getAssets().list(dirPath);
		} catch (IOException ie) {
			origDir = new String[0];
		}
		//System.out.println("list 2 " + (System.currentTimeMillis() - start));
		// merge
		for (String s: origDir) {
			outList.add(s);
		}
		//System.out.println("End merge 2 " + (System.currentTimeMillis() - start));
		String[] retval = outList.toArray(origDir);
		System.out.println(Arrays.toString(retval));
		return retval;
	}

	public boolean existsForPath(String path) {
		InputStream is = null;
		if ((path.startsWith("resource_packs/") && !path.startsWith("resource_packs/vanilla")) ||
			(path.startsWith("resourcepacks") && !path.startsWith("resourcepacks/vanilla"))) {
			is = getLocalInputStreamForAsset(path);
		} else {
			is = getInputStreamForAsset(path);
		}
		if (is != null) {
			try {
				is.close();
			} catch (IOException ie) {}
		}
		return is != null;
	}

	public int[] getImageData(String name) {
		return getImageData(name, true);
	}

	public int[] getImageData(String name, boolean fromAssets) {
		System.out.println("Get image data: " + name + " from assets? " + fromAssets);
		boolean externalData = (name.length() > 0 && name.charAt(0) == '/');
		try {
			InputStream is = externalData? getRegularInputStream(name) : getInputStreamForAsset(name);
			if (is == null)
				return getFakeImageData(name, fromAssets);
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

	public int[] getFakeImageData(String name, boolean fromAssets) {
		return new int[] {1, 1, 0};
	}

	public String[] getOptionStrings() {
		System.err.println("OptionStrings");
		SharedPreferences sharedPref = Utils.getPrefs(0);
		Map prefsMap = sharedPref.getAll();
		Set<Map.Entry> prefsSet = prefsMap.entrySet();
		List<String> retval = new ArrayList<String>();
		for (Map.Entry<String, ?> e : prefsSet) {
			String key = e.getKey();
			if (key.indexOf("zz_") == 0)
				continue;
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
		String custom = Utils.getPrefs(0).getString("zz_custom_dpi", null);
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
		System.out.println("getPlatformStringVar: " + a);
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
		//System.out.println("Network?:" + a); 0.11 beta 1 spams this on the title screen
		return true;
	}

	public boolean isTouchscreen() {
		return Utils.getPrefs(0).getBoolean("ctrl_usetouchscreen", true);
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
		if (Utils.getPrefs(0).getBoolean("zz_longvibration", false)) {
			duration *= 5;
		}
		((Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(duration);
	}

	public int getKeyFromKeyCode(int keyCode, int metaState, int deviceId) {
		KeyCharacterMap characterMap = KeyCharacterMap.load(deviceId);
		int retval = characterMap.get(keyCode, metaState);
		if (BuildConfig.DEBUG)
			Log.i(TAG, "getKey: " + keyCode + ":" + metaState + ":" + deviceId + ":" + retval);
		return retval;
	}

	public static void saveScreenshot(String name, int firstInt, int secondInt, int[] thatArray) {
	}

	// added in 0.7.0
	public int abortWebRequest(int requestId) {
		Log.i(TAG, "Abort web request: " + requestId);
		HurlRunner runner = requestMap.get(requestId);
		if (runner != null)
			runner.isValid = false;
		return 0;
	}

	public String getRefreshToken() {
		Log.i(TAG, "Get Refresh token");
		return Utils.getPrefs(0).getString("refreshToken", "");
	}

	public String getSession() {
		Log.i(TAG, "Get Session");
		return Utils.getPrefs(0).getString("sessionId", "");
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
			@SuppressLint("SetJavaScriptEnabled")
			public void run() {
				loginWebView = new WebView(MainActivity.this);
				loginWebView.setLayoutParams(new ViewGroup.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
				loginWebView.setWebViewClient(new LoginWebViewClient());
				WebSettings settings = loginWebView.getSettings();
				settings.setJavaScriptEnabled(true); // at least on Firefox, the
														// webview tries to do
														// some Ajax stuff
				/*
				 * loginPopup = new PopupWindow(loginWebView,
				 * ViewGroup.LayoutParams.FILL_PARENT,
				 * ViewGroup.LayoutParams.FILL_PARENT);
				 * loginPopup.showAtLocation(getWindow().getDecorView(),
				 * Gravity.CENTER, 0, 0);
				 */

				loginDialog = new Dialog(MainActivity.this);
				loginDialog.setCancelable(true);
				loginDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				loginDialog.setContentView(loginWebView);
				loginDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.MATCH_PARENT);
				loginDialog.show();

				loginWebView.loadUrl(getRealmsRedirectInfo().loginUrl);
			}
		});
		// nativeLoginData("Spartan", "Warrior", "Peacock");
	}

	public void setRefreshToken(String token) {
		Utils.getPrefs(0).edit().putString("refreshToken", token).apply();
	}

	public void setSession(String session) {
		Utils.getPrefs(0).edit().putString("sessionId", session).apply();
	}

	public boolean supportsNonTouchscreen() {
		if (isForcingController()) {
			if (!controllerInit && !isSafeMode()) {
				net.zhuoweizhang.mcpelauncher.api.modpe.ControllerManager.init();
				controllerInit = true;
			}
			return true;
		}
		boolean xperia = false;
		boolean play = false;
		String[] data = new String[3];
		data[0] = Build.MODEL.toLowerCase(Locale.ENGLISH);
		data[1] = Build.DEVICE.toLowerCase(Locale.ENGLISH);
		data[2] = Build.PRODUCT.toLowerCase(Locale.ENGLISH);
		for (String s : data) {
			if (s.indexOf("xperia") >= 0)
				xperia = true;
			if (s.indexOf("play") >= 0)
				play = true;
		}
		return xperia && play;
	}

	public void webRequest(int requestId, long timestamp, String url, String method, String cookies) {
		this.webRequest(requestId, timestamp, url, method, cookies, "");
	}

	// signature change in 0.7.3
	public void webRequest(int requestId, long timestamp, String url, String method,
			String cookies, String extraParam) {
		if (BuildConfig.DEBUG)
			Log.i(TAG, "Web request: " + requestId + ": " + timestamp + " :" + url + ":" + method
					+ ":" + cookies + ":" + extraParam);
		// nativeWebRequestCompleted(requestId, timestamp, 200, "SPARTA");
		url = filterUrl(url);
		if (BuildConfig.DEBUG)
			Log.i(TAG, url);
		new Thread(new HurlRunner(requestId, timestamp, url, method, cookies)).start();
	}

	protected String filterUrl(String url) {
		return url;
		// String peoapiRedirect =
		// Utils.getPrefs(0).getString("zz_redirect_mco_address", "NONE");
		// if (peoapiRedirect.equals("NONE"))
		// return url;
		// RealmsRedirectInfo info = getRealmsRedirectInfo();
		// if (info.accountUrl != null) {
		// url.replace("account.mojang.com", info.accountUrl);
		// // TODO: better system
		// }
		// return url.replace("peoapi.minecraft.net", peoapiRedirect);

	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (BuildConfig.DEBUG)
			Log.i(TAG, event.toString());
/*
		if (event.getAction() == KeyEvent.ACTION_MULTIPLE
				&& event.getKeyCode() == KeyEvent.KEYCODE_UNKNOWN) {
			try {
				nativeTypeCharacter(event.getCharacters());
				return true;
			} catch (UnsatisfiedLinkError e) {
				// Do nothing
			}
		}
*/
		return super.dispatchKeyEvent(event);
	}

	// added in 0.7.2

	public void showKeyboardView() {
		Log.i(TAG, "Show keyboard view");
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(getWindow().getDecorView(), InputMethodManager.SHOW_FORCED);
	}

	// added in 0.7.3
	public String getAccessToken() {
		Log.i(TAG, "Get access token");
		return Utils.getPrefs(0).getString("accessToken", "");
	}

	public String getClientId() {
		Log.i(TAG, "Get client ID");
		return Utils.getPrefs(0).getString("clientId", "");
	}

	public String getProfileId() {
		Log.i(TAG, "Get profile ID");
		return Utils.getPrefs(0).getString("profileUuid", "");
	}

	public String getProfileName() {
		Log.i(TAG, "Get profile name");
		return Utils.getPrefs(0).getString("profileName", "");
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

	public void setLoginInformation(String accessToken, String clientId, String profileUuid,
			String profileName) {
		if (BuildConfig.DEBUG)
			Log.i(TAG, "Login info: " + accessToken + ":" + clientId + ":" + profileUuid + ":"
					+ profileName);
		Utils.getPrefs(0).edit().putString("accessToken", accessToken)
				.putString("clientId", clientId).putString("profileUuid", profileUuid)
				.putString("profileName", profileName).apply();
	}

	public void clearLoginInformation() {
		Log.i(TAG, "Clear login info");
		Utils.getPrefs(0).edit().putString("accessToken", "").putString("clientId", "")
				.putString("profileUuid", "").putString("profileName", "").apply();
	}

	// added in 0.8.0
	// showKeyboard modified in 0.12.0b10 and 1.2.13
	public void showKeyboard(final String mystr, final int maxLength, final boolean mybool) {
		this.showKeyboard(mystr, maxLength, mybool, false, false);
	}
	public void showKeyboard(final String mystr, final int maxLength, final boolean mybool, final boolean mybool2,
		final boolean mybool3) {
		if (BuildConfig.DEBUG)
			Log.i(TAG, "Show keyboard: " + mystr + ":" + maxLength + ":" + mybool + ":" + mybool2 + ":" + mybool3);
		if (useLegacyKeyboardInput()) {
			showKeyboardView();
			return;
		}
		this.runOnUiThread(new Runnable() {
			public void run() {
				showHiddenTextbox(mystr, maxLength, mybool);
			}
		});
	}

	public void hideKeyboard() {
		if (BuildConfig.DEBUG)
			Log.i(TAG, "Hide keyboard");
		if (useLegacyKeyboardInput()) {
			hideKeyboardView();
			return;
		}
		this.runOnUiThread(new Runnable() {
			public void run() {
				dismissHiddenTextbox();
			}
		});
	}

	public void updateTextboxText(final String text) {
		if (BuildConfig.DEBUG)
			Log.i(TAG, "Update text to " + text);
		if (hiddenTextView == null)
			return;
		hiddenTextView.post(new Runnable() {
			public void run() {
				boolean commandHistory = isCommandHistoryEnabled();
				if (commandHistory) {
					if (commandHistoryList.size() >= 1 && commandHistoryList.get(commandHistoryList.size() - 1).
						length() <= 0) {
						commandHistoryList.set(commandHistoryList.size() - 1, text);
					} else {
						commandHistoryList.add(text);
					}
					setCommandHistoryIndex(commandHistoryList.size() - 1);
					if (BuildConfig.DEBUG) {
						Log.i(TAG, commandHistoryIndex + ":" + commandHistoryList);
					}
				}
				hiddenTextView.setText(text);
			}
		});
	}

	public void showHiddenTextbox(String text, int maxLength, boolean dismissAfterOneLine) {
		int IME_FLAG_NO_FULLSCREEN = 0x02000000;
		boolean commandHistory = isCommandHistoryEnabled();
		if (hiddenTextWindow == null) {
			if (commandHistory) {
				commandHistoryView = this.getLayoutInflater().inflate(R.layout.chat_history_popup,
					null);
				hiddenTextView = (TextView) commandHistoryView.findViewById(R.id.hidden_text_view);
				prevButton = (Button) commandHistoryView.findViewById(R.id.command_history_previous);
				nextButton = (Button) commandHistoryView.findViewById(R.id.command_history_next);
				View.OnClickListener listener = new View.OnClickListener() {
					public void onClick(View v) {
						if (v == prevButton) {
							navigateCommandHistory(-1);
						} else if (v == nextButton) {
							navigateCommandHistory(1);
						}
					}
				};
				prevButton.setOnClickListener(listener);
				nextButton.setOnClickListener(listener);
			} else {
				hiddenTextView = new EditText(this);
			}
			PopupTextWatcher whoWatchesTheWatcher = new PopupTextWatcher();
			hiddenTextView.addTextChangedListener(whoWatchesTheWatcher);
			hiddenTextView.setOnEditorActionListener(whoWatchesTheWatcher);
			hiddenTextView.setSingleLine(true);
			hiddenTextView.setImeOptions(EditorInfo.IME_ACTION_NEXT
					| EditorInfo.IME_FLAG_NO_EXTRACT_UI | IME_FLAG_NO_FULLSCREEN);
			hiddenTextView.setInputType(InputType.TYPE_CLASS_TEXT);
			if (commandHistory) {
				hiddenTextWindow = new PopupWindow(commandHistoryView);
			} else {
				LinearLayout linearLayout = new LinearLayout(this);
				linearLayout.addView(hiddenTextView);
				hiddenTextWindow = new PopupWindow(linearLayout);
			}
			hiddenTextWindow.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			hiddenTextWindow.setFocusable(true);
			hiddenTextWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
			hiddenTextWindow.setBackgroundDrawable(new ColorDrawable());
			// To get back button handling for free
			hiddenTextWindow.setClippingEnabled(false);
			hiddenTextWindow.setTouchable(commandHistory);
			hiddenTextWindow.setOutsideTouchable(true);
			// These flags were taken from a dumpsys window output of Mojang's
			// window
			/*
			hiddenTextWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
				public void onDismiss() {
					nativeBackPressed();
				}
			});
			*/
		}

		if (commandHistory) {
			// clear blank lines
			for (int i = commandHistoryList.size() - 1; i >= 0; i--) {
				if (commandHistoryList.get(i).equals("")) commandHistoryList.remove(i);
			}
			commandHistoryList.add(text);
			setCommandHistoryIndex(commandHistoryList.size() - 1);
		}

		hiddenTextView.setText(text);
		Selection.setSelection((Spannable) hiddenTextView.getText(), text.length());
		this.hiddenTextDismissAfterOneLine = dismissAfterOneLine;

		int xLoc = commandHistory? 0: -10000;

		hiddenTextWindow.showAtLocation(this.getWindow().getDecorView(),
				Gravity.LEFT | Gravity.TOP, xLoc, 0);
		hiddenTextView.requestFocus();
		showKeyboardView();
	}

	public void dismissHiddenTextbox() {
		if (hiddenTextWindow == null)
			return;
		hiddenTextWindow.dismiss();
		this.getWindow().getDecorView().postDelayed(new Runnable() {
			public void run() {
				hideKeyboardView();
			}
		}, 100);
	}

	// added in 0.9.0
	public String[] getBroadcastAddresses() {
		// TODO: can we use this for server patching?
		Log.i(TAG, "get broadcast addresses");
		return new String[] {"255.255.255.255"};
	}

	public long getTotalMemory() {
		try {
			long retval = Utils.parseMemInfo();
			Log.i(TAG, "Get total memory: " + retval);
			return retval;
		} catch (Exception e) {
			e.printStackTrace();
			return 0x400000000L; // 16GB
		}
	}

	// added in 0.10.0
	public String getDeviceModel() {
		return HardwareInformation.getDeviceModelName();
	}

	public int getAndroidVersion() {
		return Build.VERSION.SDK_INT;
	}

	private boolean useLegacyKeyboardInput() {
		return Utils.getPrefs(0).getBoolean("zz_legacy_keyboard_input", false);
	}

	public void initPatching() throws Exception {
		loadLibrary("gnustl_shared");
		loadLibrary("mcpelauncher_tinysubstrate");
		String mcpeVersion = getMCPEVersion();
		System.out.println("MCPE Version is " + mcpeVersion);
		if (mcpeVersion.startsWith(HALF_SUPPORT_VERSION)) {
			if (HALF_VERSION_HAS_SCRIPTS) {
				loadLibrary("mcpelauncher_new");
			} else {
				loadLibrary("mcpelauncher_lite");
			}
		} else if (mcpeVersion.startsWith(LITE_SUPPORT_VERSION)) {
			loadLibrary("mcpelauncher_lite");
		} else {
			loadLibrary("mcpelauncher");
		}

		long minecraftLibLength = findMinecraftLibLength();
		boolean success = MaraudersMap.initPatching(this, minecraftLibLength);
		if (!success) {
			System.out.println("Well, that sucks!");
			tempSafeMode = true;
			overlyZealousSELinuxSafeMode = true;
			return;
		}
	}

	public static long findMinecraftLibLength() throws Exception {
		return new File(MC_NATIVE_LIBRARY_LOCATION).length();
		// TODO: don't hardcode the 0x1000 page for relocation data.rel.ro.local
	}

	public int getMaxNumPatches() {
		return this.getResources().getInteger(R.integer.max_num_patches);
	}

	public boolean doesRequireGuiBlocksPatch() {
		return false;
	}

	protected void setupHoverCar() {
		hoverCar = new HoverCar(this, isSafeMode());
		hoverCar.show(getWindow().getDecorView());
		hoverCar.setVisible(!Utils.getPrefs(0).getBoolean("zz_hovercar_hide", false));
		hoverCar.mainButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				boolean showInsertText = Utils.getPrefs(0).getBoolean("zz_show_insert_text", false);
				showDialog(showInsertText ? DIALOG_RUNTIME_OPTIONS_WITH_INSERT_TEXT
						: DIALOG_RUNTIME_OPTIONS);
				resetOrientation();
				// for sensor controls. TODO: better place to do this?
			}
		});
	}

	protected void loadNativeAddons() {
		if (!Utils.getPrefs(0).getBoolean("zz_load_native_addons", true))
			return;
		PackageManager pm = getPackageManager();
		AddonManager addonManager = AddonManager.getAddonManager(this);
		List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
		StringBuilder archFail = new StringBuilder();
		for (ApplicationInfo app : apps) {
			if (app.metaData == null)
				continue;
			String nativeLibName = app.metaData
					.getString("net.zhuoweizhang.mcpelauncher.api.nativelibname");
			String targetMCPEVersion = app.metaData
					.getString("net.zhuoweizhang.mcpelauncher.api.targetmcpeversion");
			if (pm.checkPermission("net.zhuoweizhang.mcpelauncher.ADDON", app.packageName) ==
					PackageManager.PERMISSION_GRANTED
				&& addonManager.isEnabled(app.packageName)) {
				try {
					if (!isAddonCompat(targetMCPEVersion)) {
						throw new Exception("The addon \"" + pm.getApplicationLabel(app).toString() +
							"\" (" + app.packageName + ")" +
							" is not compatible with Minecraft PE " + mcPkgInfo.versionName + ".");
					}
					if (nativeLibName != null) {
						if (checkAddonArch(new File(app.nativeLibraryDir + "/lib" + nativeLibName + ".so"))) {
							System.load(app.nativeLibraryDir + "/lib" + nativeLibName + ".so");
							loadedAddons.add(app.packageName);
						} else {
							archFail.append("\"").append(pm.getApplicationLabel(app).toString()).
								append("\" (").append(app.packageName).append(") ");
						}
					} else {
						// no native code; just texture pack
						loadedAddons.add(app.packageName);
					}
				} catch (Throwable e) {
					reportError(e);
					e.printStackTrace();
				}
			}
		}
		if (archFail.length() != 0) {
			reportError(new Exception(
				this.getResources().getString(R.string.addons_wrong_arch).toString()
				.replaceAll("ARCH", Utils.getArchName(mcpeArch)).replaceAll("ADDONS", archFail.toString())
			));
		}
		addonOverrideTexturePackInstance = new AddonOverrideTexturePack(this, "resource_packs/vanilla/");
		textureOverrides.add(addonOverrideTexturePackInstance);
	}

	protected void migrateToPatchManager() {
		try {
			boolean enabledPatchMgr = Utils.getPrefs(1).getInt("patchManagerVersion", -1) > 0;
			if (enabledPatchMgr)
				return;
			showDialog(DIALOG_FIRST_LAUNCH);
			File patchesDir = this.getDir(PT_PATCHES_DIR, 0);
			PatchManager.getPatchManager(this).setEnabled(patchesDir.listFiles(), true);
			System.out.println(Utils.getPrefs(1).getString("enabledPatches", "LOL"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void applyBuiltinPatches() {
		//do nothing
	}

	protected void loadTexturePackOld() {
		String filePath = null;
		try {
			boolean loadTexturePack = Utils.getPrefs(0).getBoolean("zz_texture_pack_enable", false);
			filePath = Utils.getPrefs(1).getString("texturePack", null);
			if (loadTexturePack && (filePath != null)) {
				File file = new File(filePath);
				if (BuildConfig.DEBUG)
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
			reportError(e, R.string.texture_pack_unable_to_load, filePath + ": size is " + new File(filePath).length());
		}
	}

	protected void loadTexturePack() {
		try {
			boolean loadTexturePack = Utils.getPrefs(0).getBoolean("zz_texture_pack_enable", false);
			texturePack = null;
			if (loadTexturePack) {
				List<String> incompatible = new ArrayList<String>();
				List<TexturePack> packs = TexturePackLoader.loadTexturePacks(this, incompatible,
					getFileDataBytes("images/terrain.meta", true),
					getFileDataBytes("images/items.meta", true));
				if (incompatible.size() != 0) {
					new AlertDialog.Builder(this)
						.setMessage("Some of your texture packs are not compatible with Minecraft PE " +
							getMCPEVersion() + ". Please update " + Utils.join(incompatible, ", ") + ".")
						.setPositiveButton(android.R.string.ok, null)
						.show();
				}
				textureOverrides.addAll(packs);
			}
			//System.out.println(textureOverrides);
		} catch (Exception e) {
			e.printStackTrace();
			reportError(e, R.string.texture_pack_unable_to_load, null);
		}
	}

	/**
	 * enables the on-screen menu key on devices without a dedicated menu key,
	 * needed because target SDK is v15
	 */
	private void enableSoftMenuKey() {
		int flag = Build.VERSION.SDK_INT >= 19 ? 0x40000000 : 0x08000000; // FLAG_NEEDS_MENU_KEY
		getWindow().addFlags(flag);
		// KitKat reused old show menu key flag for transparent navbars
	}

	private void disableTransparentSystemBar() {
		if (Build.VERSION.SDK_INT < 21) return; // below Lollipop
		getWindow().clearFlags(0x80000000); // FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS 
	}

	private void disableAllPatches() {
		if (BuildConfig.DEBUG)
			Log.i(TAG, "Disabling all patches");
		PatchManager.getPatchManager(this).disableAllPatches();
		//Utils.getPrefs(0).edit().putBoolean("zz_load_native_addons", false).apply();
	}

	protected void loginLaunchCallback(Uri launchUri) {
		loginDialog.dismiss();
		String session = launchUri.getQueryParameter("sessionId");
		if (session == null)
			return;
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
		// String peoapiRedirect =
		// PreferenceManager.getDefaultSharedPreferences(this).getString("zz_redirect_mco_address",
		// "NONE");
		// return !peoapiRedirect.equals("NONE");
		return false;
	}

	public RealmsRedirectInfo getRealmsRedirectInfo() {
		// String peoapiRedirect =
		// PreferenceManager.getDefaultSharedPreferences(this).getString("zz_redirect_mco_address",
		// "NONE");
		return RealmsRedirectInfo.targets.get("NONE");
	}

	private void turnOffSafeMode() {
		Utils.getPrefs(0).edit().putBoolean("zz_safe_mode", false).commit();
		Utils.getPrefs(1).edit().putBoolean("force_prepatch", true).commit();
		finish();
		NerdyStuffActivity.forceRestart(this);
	}

	public void hideKeyboardView() {
		System.out.println("Hiding the keyboard view");
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(this.getWindow().getDecorView().getWindowToken(), 0);
		touchImmersiveMode();
	}

	// added in 0.11
	// for selecting skins
	public static native void nativeOnPickImageSuccess(long callbackAddress, String url);
	public static native void nativeOnPickImageCanceled(long callbackAddress);
	public void pickImage(long callbackAddress) {
		pickImageCallbackAddress = callbackAddress;

		Intent picker = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(picker, REQUEST_PICK_IMAGE);
	}

	// for the snooper or something
	public String getDeviceId() {
		String deviceId = Utils.getPrefs(0).getString("snooperId", null);
		if (deviceId == null) {
			deviceId = createUUID();
			Utils.getPrefs(0).edit().putString("snooperId", deviceId).apply();
		}
		System.out.println("Get device ID");
		return deviceId;
	}

	public String createUUID() {
		/* Generates a fresh UUID; used by snooper for session ID */
		System.out.println("Create UUID");
		return UUID.randomUUID().toString().replace("-", "");
	}

	// more snooper stuff
	public String getLocale() {
		Locale locale = getResources().getConfiguration().locale;
		return locale.getLanguage() + "_" + locale.getCountry();
	}

	public String getExternalStoragePath() {
		return Environment.getExternalStorageDirectory().getAbsolutePath();
	}

	public boolean isFirstSnooperStart() {
		System.out.println("Is first snooper start");
		return Utils.getPrefs(0).getString("snooperId", null) == null;
	}

	public boolean hasHardwareChanged() {
		return false;
	}

	public boolean isTablet() {
		// metric: >= sw600dp
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2) {
			return false; //getResources().getConfiguration().screenHeightDp >= 600;
		}
		return getResources().getConfiguration().smallestScreenWidthDp >= 600;
	}

	// end 0.11

	// 0.12, changed in 0.13
	public float getKeyboardHeight() {
		Rect r = new Rect();
		View rootview = this.getWindow().getDecorView();
		rootview.getWindowVisibleDisplayFrame(r);
		if (r.bottom == 0) return 0; // if the keyboard height goes fullscreen, ignore
		return displayMetrics.heightPixels - r.bottom;
	}
	// end 0.12

	// 0.14
	public void launchUri(String theUri) {
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(theUri));
		try {
			startActivity(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 0.15
	public void setFileDialogCallback(long pointer) {
		if (BuildConfig.DEBUG) System.out.println("Set file dialog callback: " + Long.toString(pointer, 16));
	}

	public void updateLocalization(String a, String b) {
		System.out.println("Update localization: " + a + ":" + b);
	}

	public String[] getIPAddresses() {
		System.out.println("get IP addresses?");
		return new String[] {"127.0.0.1"};
	}

	public Intent getLaunchIntent() {
		System.out.println("get launch intent");
		return getIntent();
	}

	public Intent createAndroidLaunchIntent() {
		System.out.println("create android launch intent");
		return getIntent();
	}
	private TextToSpeech tts;
	// 0.17
	public void startTextToSpeech(String a) {
		System.out.println("Text to speech: " + a);
		if (tts != null) tts.speak(a, TextToSpeech.QUEUE_ADD, null);
	}

	public void stopTextToSpeech() {
		System.out.println("Shutting up");
		if (tts != null) tts.stop();
	}

	public boolean isTextToSpeechInProgress() {
		return false;
	}

	public void setTextToSpeechEnabled(boolean enabled) {
		System.out.println("Text to speech?");
		if (enabled) {
			if (tts == null) tts = new TextToSpeech(this, null);
		} else {
			if (tts != null) {
				tts.shutdown();
				tts = null;
			}
		}
	}

	public void setClipboard(String text) {
		ClipboardManager mgr = (ClipboardManager) this.getSystemService(CLIPBOARD_SERVICE);
		mgr.setText(text);
	}

	public long calculateAvailableDiskFreeSpace(String disk) {
		System.out.println("Calculate disk free space: " + disk);
		return 0;
	}

	public int getCursorPosition() {
		if (hiddenTextView == null) return 0;
		return hiddenTextView.getSelectionStart();
	}

	// 1.0.5
	public long getAvailableMemory() {
		return 0x40000000L;
	}

	public void requestStoragePermission(int requestId) {
		System.out.println("Request storage: " + requestId);
		grabPermissionsIfNeeded();
	}

	public boolean hasWriteExternalStoragePermission() {
		return hasStoragePermissions();
	}

	public void trackPurchaseEvent(String a, String b, String c) {
		System.out.println("Track purchase event: " + a + ":" + b + ":" + c);
	}

	// 1.1
	public String getFormattedDateString(int date) {
		return getDateString(date);
	}

	public String getFileTimestamp(int date) {
		return getDateString(date);
	}

	public long getUsedMemory() {
		return 0;
	}

	public void trackPurchaseEvent(String a, String b, String c, String d, String e) {
		System.out.println("Track purchase event: " + a + ":" + b + ":" + c + ":" + d + ":" + e);
	}

	public String createDeviceID() {
		return getDeviceId();
	}

	// 1.2.0
	public boolean unpackMonoAssemblies() {
		// WHAT
		System.out.println("Unpack Mono assemblies");
		return false;
	}

	public void share(String shareText) {
		System.out.println("Share: " + shareText);
	}

	public void trackPurchaseEvent(String a, String b, String c, String d, String e, String f) {
		System.out.println("Track purchase event: " + a + ":" + b + ":" + c + ":" + d + ":" + e + ":" + f);
	}

	public String getLegacyDeviceID() {
		return getDeviceId();
	}

	private boolean isPackageInstalledByName(String pkgName) {
		try {
			return this.getPackageManager().getPackageInfo(pkgName, 0) != null;
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
	}
	public boolean isMixerCreateInstalled() {
		return isPackageInstalledByName("com.microsoft.beambroadcast") ||
			isPackageInstalledByName("com.microsoft.beambroadcast.beta");
	}
	public void navigateToPlaystoreForMixerCreate() {
		launchUri("market://details?id=com.microsoft.beambroadcast");
	}
	public boolean launchMixerCreateForBroadcast() {
		try {
			launchUri("beambroadcast://");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public int getAPIVersion(String theName) {
		System.out.println("Get API version: " + theName);
		try {
			Field field = Build.VERSION_CODES.class.getField(theName);
			if (field == null) return -1;
			return field.getInt(null);
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	public String getSecureStorageKey(String key) {
		System.out.println("Get secure storage key: " + key);
		SharedPreferences myprefs = Utils.getPrefs(1);
		return myprefs.getString("secure_storage_" + key, "");
	}

	public void setSecureStorageKey(String key, String value) {
		System.out.println("Set secure storage key: " + key);
		SharedPreferences myprefs = Utils.getPrefs(1);
		myprefs.edit().putString("secure_storage_" + key, value).apply();
	}

	// 1.2.10
	public long getMemoryLimit() {
		return 0x100000000L; // 4GB
	}
	public long getFreeMemory() {
		return 0x100000000L; // 4GB
	}

	// 1.2.20
	public void setCachedDeviceId(String deviceId) {
		if (BuildConfig.DEBUG) {
			System.out.println("Cached device ID: " + deviceId);
		}
	}

	public void trackPurchaseEvent(String arg1, String arg2, String arg3, String arg4,
		String arg5, String arg6, String arg7) {
		System.out.println("Track purchase event: " + arg1 + ":" + arg2 + ":" + arg3 + ":" + arg4 + ":" +
			arg5 + ":" + arg6 + ":" + arg7);
	}

	// 1.4.4
	public HardwareInformation getHardwareInfo() {
		return new HardwareInformation();
	}

	public void setLastDeviceSessionId(String sessionId) {
		if (BuildConfig.DEBUG) {
			System.out.println("Last device session ID: " + sessionId);
		}
		SharedPreferences myprefs = Utils.getPrefs(1);
		myprefs.edit().putString("last_device_session_id", sessionId).apply();
	}

	public String getLastDeviceSessionId() {
		if (BuildConfig.DEBUG) {
			System.out.println("Get last device session ID");
		}
		SharedPreferences myprefs = Utils.getPrefs(1);
		return myprefs.getString("last_device_session_id", "");
	}

	// 1.7.0
	public void share(String shareText, String shareText2, String shareText3) {
		System.out.println("Share: " + shareText + ":" + shareText2 + ":" + shareText3);
	}

	public void deviceIdCorrelationStart() {
		if (BuildConfig.DEBUG) {
			System.out.println("Device ID correlation start");
		}
	}

	// 1.9.0
	public void sendBrazeEvent(String event) {
		if (BuildConfig.DEBUG) {
			System.out.println("send braze event: " + event);
		}
	}

	public void sendBrazeEventWithProperty(String event, String property, int number) {
		if (BuildConfig.DEBUG) {
			System.out.println("send braze event with property: " + event + ": " + property + ": " + number);
		}
	}

	public void sendBrazeEventWithStringProperty(String event, String property, String value) {
		if (BuildConfig.DEBUG) {
			System.out.println("send braze event with string: " + event + ": " + property + ": " + value);
		}
	}

	public void sendBrazeToastClick() {
		if (BuildConfig.DEBUG) {
			System.out.println("send braze toast click");
		}
	}

	public void sendBrazeDialogButtonClick(int value) {
		if (BuildConfig.DEBUG) {
			System.out.println("send braze dialog button click " + value);
		}
	}

	// 1.11.0

	public boolean isTTSEnabled() {
		return false;
	}

	// 1.12.0
	public boolean hasHardwareKeyboard() {
		return getResources().getConfiguration().keyboard == android.content.res.Configuration.KEYBOARD_QWERTY;
	}

	public void trackPurchaseEvent(String arg1, String arg2, String arg3, String arg4, String arg5, String arg6, String arg7, String arg8) {
		if (BuildConfig.DEBUG) {
			System.out.println(arg1 + ":" + arg2 + ":" + arg3 + ":" + arg4 + ":" + arg5 + ":" + arg6 + ":" + arg7 + ":" + arg8);
		}
	}

	@Override
	public void onBackPressed() {
		nativeBackPressed();
	}

	private InputStream getRegularInputStream(String path) {
		try {
			return new BufferedInputStream(new FileInputStream(new File(path)));
		} catch (IOException ie) {
			ie.printStackTrace();
			return null;
		}
	}

	private File copyContentStoreToTempFile(Uri content) {
		return copyContentStoreToTempFile(content, "skintemp.png");
	}
	private File copyContentStoreToTempFile(Uri content, String targetName) {
		try {
			File tempFile = new File(this.getExternalFilesDir(null), targetName);
			tempFile.getParentFile().mkdirs();
			InputStream is = getContentResolver().openInputStream(content);
			OutputStream os = new FileOutputStream(tempFile);
			byte[] buffer = new byte[0x1000];
			int count;
			while ((count = is.read(buffer)) != -1) {
				os.write(buffer, 0, count);
			}
			is.close();
			os.close();
			return tempFile;
		} catch (IOException ie) {
			ie.printStackTrace();
			return new File("/sdcard/totally/fake");
		}
	}

	/**
	 * Called by the ScriptManager when a new level is loaded. This is for
	 * subclasses to do cleanup/disable menu items that cannot be used
	 * ingame/show ads, etc
	 */
	public void setLevelCallback(boolean isRemote) {
		System.out.println("Set level callback: " + isRemote);
		if (isRemote && ScriptManager.scripts.size() > 0) {
			this.runOnUiThread(new Runnable() {
				public void run() {
					showDialog(DIALOG_MULTIPLAYER_DISABLE_SCRIPTS);
				}
			});
		}
		if (hasRecorder) clearRuntimeOptionsDialog();
	}

	/**
	 * Called by the ScriptManager when exiting to the main menu. This is for
	 * subclasses to do cleanup/disable menu items that cannot be used
	 * ingame/show ads, etc
	 */
	public void leaveGameCallback() {
		System.out.println("Leave game");
		if (hasRecorder) clearRuntimeOptionsDialog();
		if (BuildConfig.DEBUG) {
			scriptPrintCallback("Leave game called", "bl");
		}
	}

	public void scriptPrintCallback(final String message, final String scriptName) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(MainActivity.this, "Script " + scriptName + ": " + message,
						Toast.LENGTH_SHORT).show();
			}
		});
	}

	private Typeface minecraftTypeface = null;
	private Toast lastToast = null;
	public void fakeTipMessageCallback(final String messageRaw) {
		if (minecraftTypeface == null) {
			minecraftTypeface = Typeface.createFromAsset(this.getAssets(), "fonts/minecraft.ttf");
		}
		// strip all colour characters
		final String message = messageRaw.replaceAll(ChatColor.BEGIN + ".", "");
		this.runOnUiThread(new Runnable() {
			public void run() {
				TextView toastText = new TextView(MainActivity.this);
				toastText.setText(message);
				toastText.setTypeface(minecraftTypeface);
				toastText.setTextColor(0xffffffff);
				toastText.setShadowLayer(0.1f, 8f, 8f, 0xff000000);
				toastText.setTextSize(16);
				if (lastToast != null) lastToast.cancel();
				Toast myToast = new Toast(MainActivity.this);
				myToast.setView(toastText);
				lastToast = myToast;
				myToast.show();
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

	private static String stringFromInputStream(InputStream in, int startingLength)
			throws IOException {
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

	public void reportError(final Throwable t) {
		reportError(t, R.string.report_error_title, null);
	}

	public void reportError(final Throwable t, final int messageId, final String extraData) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				final StringWriter strWriter = new StringWriter();
				PrintWriter pWriter = new PrintWriter(strWriter);
				t.printStackTrace(pWriter);
				final String msg;
				if (extraData != null) {
					msg = extraData + "\n" + strWriter.toString();
				} else {
					msg = strWriter.toString();
				}
				new AlertDialog.Builder(MainActivity.this)
						.setTitle(messageId)
						.setMessage(msg)
						.setPositiveButton(android.R.string.ok, null)
						.setNeutralButton(android.R.string.copy,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface aDialog, int button) {
										ClipboardManager mgr = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
										mgr.setText(msg);
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
					if (lineSource != null)
						pWriter.println(lineSource);
				}
				t.printStackTrace(pWriter);
				new AlertDialog.Builder(MainActivity.this)
						.setTitle(R.string.script_execution_error)
						.setMessage(strWriter.toString())
						.setPositiveButton(android.R.string.ok, null)
						.setNeutralButton(android.R.string.copy,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface aDialog, int button) {
										ClipboardManager mgr = (ClipboardManager) MainActivity.this
												.getSystemService(CLIPBOARD_SERVICE);
										mgr.setText(strWriter.toString());
									}
								}).show();
			}
		});
	}

	protected void resetOrientation() {
		// for sensor controls
	}

	public void scriptTooManyErrorsCallback(final String scriptName) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				new AlertDialog.Builder(MainActivity.this)
						.setTitle(R.string.script_execution_error)
						.setMessage(
								scriptName + " "
										+ getResources().getString(R.string.script_too_many_errors))
						.setPositiveButton(android.R.string.ok, null).show();
			}
		});
	}

	public void screenshotCallback(final File file) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(
						MainActivity.this,
						getResources().getString(R.string.screenshot_saved_as) + " "
								+ file.getAbsolutePath(), Toast.LENGTH_LONG).show();
				MediaScannerConnection.scanFile(MainActivity.this,
						new String[] { file.getAbsolutePath() }, new String[] { "image/png" }, null);
			}
		});
	}

	protected boolean allowScriptOverrideTextures() {
		return true; // Let's do this! Leeroy Jenkins!
	}

	private void addLibraryDirToPath(String path) {
		try {
			ClassLoader classLoader = getClassLoader();
			Class<? extends ClassLoader> clazz = classLoader.getClass();
			Field field = Utils.getDeclaredFieldRecursive(clazz, "pathList");
			field.setAccessible(true);
			Object pathListObj = field.get(classLoader);
			Class<? extends Object> pathListClass = pathListObj.getClass();
			Field natfield = Utils.getDeclaredFieldRecursive(pathListClass,
					"nativeLibraryDirectories");
			natfield.setAccessible(true);
			Object theFileList = natfield.get(pathListObj);
			if (theFileList instanceof File[]) {
				File[] fileList = (File[]) theFileList;
				File[] newList = addToFileList(fileList, new File(path));
				if (fileList != newList)
					natfield.set(pathListObj, newList);
			}
			Field natElemsField = Utils.getDeclaredFieldRecursive(pathListClass,
					"nativeLibraryPathElements");
			if (natElemsField != null && classLoader instanceof BaseDexClassLoader &&
				((BaseDexClassLoader) classLoader).findLibrary("minecraftpe") == null) {
				// Android 6.0 and above; needed on N
				natElemsField.setAccessible(true);
				Object[] theObjects = (Object[]) natElemsField.get(pathListObj);
				Class<? extends Object> elemClass = theObjects.getClass().getComponentType();
				Object newObject = null;
				try {
					Constructor<? extends Object> elemConstructor =
						elemClass.getConstructor(File.class, Boolean.TYPE, File.class, dalvik.system.DexFile.class);
					elemConstructor.setAccessible(true);
					newObject = elemConstructor.newInstance(new File(path), true, null, null);
				} catch (NoSuchMethodException nsm) {
					// Android 8.0 uses a different signature.
					Constructor<? extends Object> elemConstructor =
						elemClass.getConstructor(File.class);
					elemConstructor.setAccessible(true);
					newObject = elemConstructor.newInstance(new File(path));
				}
				Object[] newObjects = Arrays.copyOf(theObjects, theObjects.length + 1);
				newObjects[newObjects.length - 1] = newObject;
				System.out.println(newObjects);
				natElemsField.set(pathListObj, newObjects);
			}
			// check
			// System.out.println("Class loader shenanigans: " +
			// ((PathClassLoader) getClassLoader()).findLibrary("minecraftpe"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private File[] addToFileList(File[] files, File toAdd) {
		boolean needsAdding = true;
		for (File f : files) {
			if (f.equals(toAdd)) {
				// System.out.println("Already added path to list");
				return files;
			}
		}
		File[] retval = new File[files.length + 1];
		System.arraycopy(files, 0, retval, 1, files.length);
		retval[0] = toAdd;
		return retval;
	}

	private void navigateCommandHistory(int direction) {
		if (BuildConfig.DEBUG) {
			Log.i(TAG, commandHistoryIndex + ":" + commandHistoryList);
		}
		int newIndex = commandHistoryIndex + direction;
		if (newIndex < 0) newIndex = 0;
		if (newIndex >= commandHistoryList.size()) newIndex = commandHistoryList.size() - 1;
		setCommandHistoryIndex(newIndex);
		String newCommand = commandHistoryList.get(newIndex);
		hiddenTextView.setText(newCommand);
		Selection.setSelection((Spannable) hiddenTextView.getText(), newCommand.length());
	}

	private void setCommandHistoryIndex(int index) {
		commandHistoryIndex = index;
		prevButton.setEnabled(index != 0);
		nextButton.setEnabled(index != commandHistoryList.size() - 1);
	}

	private boolean isCommandHistoryEnabled() {
		return Utils.getPrefs(0).getBoolean("zz_command_history", true);
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	private void setImmersiveMode(boolean set) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return;
		int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
		int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		if (set) {
			uiOptions |= flags;
		} else {
			uiOptions &= ~flags;
		}
		getWindow().getDecorView().setSystemUiVisibility(uiOptions);
	}

	private void touchImmersiveMode() {
		final boolean immersive = Utils.getPrefs(0).getBoolean("zz_immersive_mode", false);
		if (!immersive) return;
		this.runOnUiThread(new Runnable() {
			public void run() {
				setImmersiveMode(immersive);
			}
		});
	};

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			touchImmersiveMode();
		}
	}

	protected void initKamcord() {
	}

	protected void toggleRecording() {
	}
	protected boolean isKamcordRecording() {
		return false;
	}

	private void clearRuntimeOptionsDialog() {
		this.runOnUiThread(new Runnable() {
			public void run() {
				if (BuildConfig.DEBUG) System.out.println("clearRuntimeOptionsDialog");
				removeDialog(DIALOG_RUNTIME_OPTIONS);
				removeDialog(DIALOG_RUNTIME_OPTIONS_WITH_INSERT_TEXT);
			}
		});
	}

	private void fixMyEpicFail() {
		SharedPreferences prefs = Utils.getPrefs(1);
		int lastVersion = prefs.getInt("last_bl_version", 0);
		int myVersion = 0;
		try {
			myVersion = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			// impossible
		}
		if (lastVersion < 69) {
			// force addons back on
			Utils.getPrefs(0).edit().putBoolean("zz_load_native_addons", true).apply();
		}
		if (lastVersion != myVersion) {
			prefs.edit().putInt("last_bl_version", myVersion).apply();
		}
	}

	private void checkForSubstrate() {
		if (!Build.CPU_ABI.equals("x86")) return; // we're not on x86
		PackageInfo substrateInfo = null;
		try {
			substrateInfo = this.getPackageManager().getPackageInfo("com.saurik.substrate", 0);
		} catch (PackageManager.NameNotFoundException e) {
		}
		if (substrateInfo == null) {
			finish();
			startActivity(new Intent(this, GetSubstrateActivity.class));
			try {
				Thread.sleep(100);
				android.os.Process.killProcess(android.os.Process.myPid());
			} catch (Throwable t) {
			}
			return;
		}
		File substrateLibFile = this.getFileStreamPath("libmcpelauncher_tinysubstrate.so");
		if (!substrateLibFile.exists()) {
			// copy the substrate lib file over
			File substrateSourceLibFile = new File(substrateInfo.applicationInfo.nativeLibraryDir, "libsubstrate.so");
			try {
				PatchUtils.copy(substrateSourceLibFile, substrateLibFile);
			} catch (IOException ie) {
				throw new RuntimeException(ie);
			}
		}
		System.load(substrateLibFile.getAbsolutePath());
	}

	private void checkArch() {
		try {
			File mcpeLib = new File(MC_NATIVE_LIBRARY_LOCATION);
			mcpeArch = Utils.getElfArch(mcpeLib);
			File ownLib = new File(this.getApplicationInfo().nativeLibraryDir + "/libmcpelauncher.so");
			int myArch = Utils.getElfArch(ownLib);
			if (mcpeArch != myArch) {
				Intent intent = new Intent(this, NoMinecraftActivity.class);
				String message = this.getResources().getString(R.string.minecraft_wrong_arch).toString().
					replaceAll("ARCH", Utils.getArchName(myArch));
				intent.putExtra("message", message);
				intent.putExtra("learnmore_uri", "https://github.com/zhuowei/MCPELauncher/issues/495");
				startActivity(intent);
				finish();
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.exit(0);
			}
		} catch (IOException e) {
		}
	}

	private boolean checkAddonArch(File file) {
		try {
			int addonArch = Utils.getElfArch(file);
			return addonArch == mcpeArch;
		} catch (IOException e) {
			return true;
		}
	}

	private boolean isAddonCompat(String version) {
		if (version == null) return false;
		//if (version.matches("0\\.11\\.0.*")) return true;
		if (version.equals(mcPkgInfo.versionName)) return true;
		if (mcPkgInfo.versionName.startsWith("1.0")) {
			if (version.startsWith("1.0.0")) return true;
			if (version.startsWith("1.0.2")) return true;
			if (version.startsWith("1.0.3")) return true;
			if (version.startsWith("1.0.4")) return true;
			if (version.startsWith("1.0.5")) return true;
			if (version.startsWith("1.0.6")) return true;
		}
		return false;
	}

	private void initAtlasMeta() {
		final boolean dumpAtlas = BuildConfig.DEBUG && hasStoragePermissions() && new File("/sdcard/bl_dump_atlas.txt").exists();
		if (isSafeMode()) return;
		try {
			AtlasProvider terrainProvider = new AtlasProvider("resource_packs/vanilla/textures/terrain_texture.json",
				"images/terrain-atlas/", "block.bl_modpkg.");
			AtlasProvider itemsProvider = new AtlasProvider("resource_packs/vanilla/textures/item_texture.json",
				"images/items-opaque/", "item.bl_modpkg.");
			PackContentsProvider packContentsProvider = new PackContentsProvider(
				"resource_packs/vanilla/textures/textures_list.json",
				"resource_packs/vanilla/contents.json");
			terrainProvider.initAtlas(this);
			itemsProvider.initAtlas(this);
			packContentsProvider.init(this);

			ClientBlocksJsonProvider blocksJsonProvider = new ClientBlocksJsonProvider("resource_packs/vanilla/blocks.json");
			blocksJsonProvider.init(this);
			if (dumpAtlas) {
				terrainProvider.dumpAtlas();
				itemsProvider.dumpAtlas();
				blocksJsonProvider.dumpAtlas();
				packContentsProvider.dumpAtlas();
			}
			textureOverrides.add(0, terrainProvider);
			textureOverrides.add(1, itemsProvider);
			textureOverrides.add(2, blocksJsonProvider);
			textureOverrides.add(3, packContentsProvider);
			ScriptManager.terrainMeta = terrainProvider;
			ScriptManager.itemsMeta = itemsProvider;
			ScriptManager.blocksJson = blocksJsonProvider;
		} catch (Exception e) {
			e.printStackTrace();
			reportError(e);
		}
/*
		FIXME 0.16
			ResourcePackManifestProvider resourcePackManifestProvider =
				new ResourcePackManifestProvider("resourcepacks/vanilla/resources.json");
			resourcePackManifestProvider.init(this);
			resourcePackManifestProvider.addTextures(terrainProvider.addedTextureNames);
			resourcePackManifestProvider.addTextures(itemsProvider.addedTextureNames);
*/
	}

	private boolean isForcingController() {
		return Build.VERSION.SDK_INT >= 12 &&
			Utils.hasExtrasPackage(this) && Utils.getPrefs(0).getBoolean("zz_use_controller", false);
	}

	protected boolean hasScriptSupport() {
		String mcpeVersion = mcPkgInfo.versionName;
		if (HALF_VERSION_HAS_SCRIPTS && mcpeVersion.startsWith(HALF_SUPPORT_VERSION)) return true;
		return mcpeVersion.startsWith(SCRIPT_SUPPORT_VERSION);
	}
	public String getMCPEVersion() {
		return mcPkgInfo.versionName;
	}
	private boolean requiresPatchingInSafeMode() {
		return true;// fixme 1.1 getMCPEVersion().startsWith(HALF_SUPPORT_VERSION);
	}

	public void reportReimported(final String scripts) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(MainActivity.this, MainActivity.this.getResources().
					getString(R.string.manage_scripts_reimported_toast) + " " + scripts,
					Toast.LENGTH_SHORT).show();
			}
		});
	}

	public void showStoreNotWorkingDialog() {
		this.runOnUiThread(new Runnable() {
			public void run() {
				new AlertDialog.Builder(MainActivity.this).setTitle(R.string.store_not_supported_title).
					setMessage(R.string.store_not_supported_message).
					setPositiveButton(android.R.string.ok, null).
					show();
			}
		});
	}

	private void grabPermissionsIfNeeded() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			grabPermissions();
		}
	}

	private boolean hasStoragePermissions() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			return hasStoragePermissionsM();
		}
		return true;
	}

	private boolean hasStoragePermissionsM() {
		return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
	}

	private boolean grabPermissions() {
		if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
			return false;
		}
		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		if (requestCode == REQUEST_STORAGE_PERMISSION) {
			if (permissions.length >= 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				// todo?
			} else {
			}
		}
	}

	private void startNativeDebugger() {
		System.out.println("gdbserver: " + android.os.Process.myPid());
		try {
			Runtime.getRuntime().exec(new String[] {
				new File(getFilesDir(), "gdbserver").getAbsolutePath(),
				"--attach",
				":3333",
				"" + android.os.Process.myPid()
			});
				Thread.sleep(5000);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void lowerSdkTarget() {
		try {
			// I'm so, so sorry.
			classVMRuntime = Class.forName("dalvik.system.VMRuntime");

			methodVMRuntimeGetRuntime = classVMRuntime.getMethod("getRuntime");
			Object runtime = methodVMRuntimeGetRuntime.invoke(null);
			methodVMRuntimeGetTargetSdkVersion = classVMRuntime.getMethod("getTargetSdkVersion");
			if (Build.VERSION.SDK_INT > 28) { // higher than Pie
				methodVMRuntimeSetTargetSdkVersion = ScriptManager.nativeGrabMethod(classVMRuntime,
					"setTargetSdkVersion", "(I)V", false);
			} else {
				methodVMRuntimeSetTargetSdkVersion = classVMRuntime.getMethod("setTargetSdkVersion", Integer.TYPE);
			}
			storedSdkTarget = (Integer)methodVMRuntimeGetTargetSdkVersion.invoke(runtime);
			methodVMRuntimeSetTargetSdkVersion.invoke(runtime, Build.VERSION_CODES.LOLLIPOP);
			int newSdkTarget = (Integer)methodVMRuntimeGetTargetSdkVersion.invoke(runtime);
			System.out.println("Temporarily setting SDK target to: " + newSdkTarget);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void restoreSdkTarget() {
		if (storedSdkTarget == -1) return;
		try {
			Object runtime = methodVMRuntimeGetRuntime.invoke(null);
			methodVMRuntimeSetTargetSdkVersion.invoke(runtime, storedSdkTarget);
			int newSdkTarget = (Integer)methodVMRuntimeGetTargetSdkVersion.invoke(runtime);
			System.out.println("Restored SDK target to: " + newSdkTarget);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void processIntentImport() {
		try {
			Intent intent = this.getIntent();
			Uri uri = intent.getData();
			System.out.println("Intent: " + intent.getAction() + ":" + uri);
			if (intent.getAction().equals(Intent.ACTION_VIEW)) {
				if (uri == null) return;
				String scheme = uri.getScheme();
				String outputPath = uri.getPath();
				if (outputPath.toLowerCase().endsWith(".mcworld")) {
					if (!scheme.equals("file")) {
						File tempFile = copyContentStoreToTempFile(intent.getData(), uri.getLastPathSegment());
						outputPath = tempFile.getAbsolutePath();
					}
					nativeProcessIntentUriQuery("fileIntent", outputPath + "&" + outputPath);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			reportError(e);
		}
	}

	private void loadLibrary(String libraryName) {
		if (!extractNativeLibs) {
			System.loadLibrary(libraryName);
			return;
		}
		System.load(new File(extractNativeLibsDir, "lib" + libraryName + ".so").getAbsolutePath());
	}

	private void extractNativeLibsIfNeeded() throws Exception {
		// figure out which arch we need in this VM
		String arch = System.getProperty("os.arch");
		String targetAbi = "armeabi-v7a";
		if (arch.startsWith("armv") || arch.startsWith("aarch")) {
			// already arm
		} else if (arch.equals("x86") || arch.equals("i686") || arch.equals("x86_64")) {
			targetAbi = "x86";
		}

		ApplicationInfo appInfo = getApplicationInfo();
		File apkFile = new File(appInfo.sourceDir);
		File outputDir = getDir("nativelibs", 0);

		Utils.extractFromZip(apkFile, outputDir, "lib/" + targetAbi + "/");

		extractNativeLibsDir = new File(outputDir, "lib/" + targetAbi);
	}

	private boolean needsRelaunchIntoCorrectAbi() {
		try {
			PackageInfo mcPkgInfo = getPackageManager().getPackageInfo("com.mojang.minecraftpe", 0);
			ApplicationInfo mcAppInfo = mcPkgInfo.applicationInfo;
			String mcNativeLibraryDir = mcAppInfo.nativeLibraryDir;
			String minecraftArch = new File(mcNativeLibraryDir).getName();
			boolean minecraftIs64 = minecraftArch.startsWith("arm64-") || minecraftArch.startsWith("x86_64");
			boolean thisProcessIs64 = thisProcessIs64();
			return minecraftIs64 != thisProcessIs64;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private static boolean thisProcessIs64() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return false;
		List<String> abi64s = Arrays.asList(Build.SUPPORTED_64_BIT_ABIS);
		return abi64s.contains(Build.CPU_ABI) || abi64s.contains(Build.CPU_ABI2);
	}
		

	private void relaunchIntoCorrectAbi() {
		Utils.forceRestartIntoDifferentAbi(this, getIntent());
	}

	private class PopupTextWatcher implements TextWatcher, TextView.OnEditorActionListener {
		public void afterTextChanged(Editable e) {
			if (BuildConfig.DEBUG)
				Log.i(TAG, "Text changed: " + e.toString());
			nativeSetTextboxText(e.toString());
			boolean commandHistory = isCommandHistoryEnabled();
			if (commandHistory) {
				if (BuildConfig.DEBUG) {
					Log.i(TAG, commandHistoryIndex + ":" + commandHistoryList);
				}
				if (commandHistoryIndex >= 0 && commandHistoryIndex < commandHistoryList.size()) {
					commandHistoryList.set(commandHistoryIndex, e.toString());
				}
			}
		}

		public void beforeTextChanged(CharSequence c, int start, int count, int after) {
		}

		public void onTextChanged(CharSequence c, int start, int count, int after) {
		}

		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			if (BuildConfig.DEBUG)
				Log.i(TAG, "Editor action: " + actionId);
			nativeReturnKeyPressed();
			//nativeTypeCharacter("\n");
			return true;
		}
	}

	private class LoginWebViewClient extends WebViewClient {
		boolean hasFiredLaunchEvent = false;

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			Uri tempUri = Uri.parse(url);
			if (BuildConfig.DEBUG)
				Log.i(TAG, tempUri.toString());
			String endHost = getRealmsRedirectInfo().accountUrl;
			if (endHost == null)
				endHost = "account.mojang.com";
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
		public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
			if (isRedirectingRealms()) {
				handler.proceed();
				return;
			}
			super.onReceivedSslError(view, handler, error);
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			Uri tempUri = Uri.parse(url);
			if (BuildConfig.DEBUG)
				Log.i(TAG, "PageStarted: " + tempUri.toString());
			String endHost = getRealmsRedirectInfo().accountUrl;
			if (endHost == null)
				endHost = "account.mojang.com";
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
			synchronized (requestMap) {
				requestMap.put(requestId, this);
			}
		}

		@SuppressWarnings("resource")
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

				conn.connect();
				try {
					response = conn.getResponseCode();
					is = conn.getInputStream();
				} catch (Exception e) {
					try {
						is = conn.getErrorStream();
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}

				if (is != null) {
					content = stringFromInputStream(is,
							conn.getContentLength() < 0 ? 1024 : conn.getContentLength());
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

			if (content != null && BuildConfig.DEBUG)
				Log.i(TAG, url + ":" + response + ":" + content);

			if (isValid) {
				nativeWebRequestCompleted(requestId, timestamp, response, content);
			}
			synchronized (requestMap) {
				requestMap.remove(requestMap.indexOfValue(this));
			}
		}

	}

}
