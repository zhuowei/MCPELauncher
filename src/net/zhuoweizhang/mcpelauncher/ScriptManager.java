package net.zhuoweizhang.mcpelauncher;

import java.io.FileReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.PrintWriter;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import java.util.zip.*;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.view.KeyEvent;
import android.widget.PopupWindow;
import android.util.Log;
import org.mozilla.javascript.*;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSStaticFunction;

import org.json.*;

import com.mojang.minecraftpe.MainActivity;

import net.zhuoweizhang.mcpelauncher.api.modpe.*;
import net.zhuoweizhang.mcpelauncher.texture.AtlasProvider;
import net.zhuoweizhang.mcpelauncher.texture.ClientBlocksJsonProvider;
import net.zhuoweizhang.mcpelauncher.texture.ModPkgTexturePack;
import net.zhuoweizhang.mcpelauncher.patch.PatchUtils;

import static net.zhuoweizhang.mcpelauncher.PatchManager.join;
import static net.zhuoweizhang.mcpelauncher.PatchManager.blankArray;

public class ScriptManager {

	public static final String SCRIPTS_DIR = "modscripts";
	public static final int MAX_NUM_ERRORS = 5;

	public static List<ScriptState> scripts = new ArrayList<ScriptState>();

	public static android.content.Context androidContext;

	public static Set<String> enabledScripts = new HashSet<String>();

	public static String worldName;
	public static String worldDir;

	/** Is the currently loaded world a multiplayer world? */
	public static boolean isRemote = false;

	private static final int AXIS_X = 0;
	private static final int AXIS_Y = 1;
	private static final int AXIS_Z = 2;

	private static final int ITEMID = 0;
	private static final int DAMAGE = 1;
	private static final int AMOUNT = 2;

	private static String currentScript = "Unknown";

	private static boolean requestedGraphicsReset = false;

	public static boolean sensorEnabled = false;

	public static float newPlayerYaw = 0;
	public static float newPlayerPitch = 0;

	private static SelectLevelRequest requestSelectLevel = null;

	private static boolean requestLeaveGame = false;

	private static JoinServerRequest requestJoinServer = null;

	/** Is ModPE script currently enabled? Set to false by multiplayer world */

	private static boolean scriptingEnabled = true;

	private static boolean scriptingInitialized = false;

	public static String screenshotFileName = "";

	// public static Queue<Runnable> mainThreadRunnableQueue = new
	// ArrayDeque<Runnable>();

	private static final ModernWrapFactory modernWrapFactory = new ModernWrapFactory();

	private static boolean requestReloadAllScripts = false;

	private static List<Runnable> runOnMainThreadList = new ArrayList<Runnable>();
	
	/*About get all*/
	public static List<Long> allentities = new ArrayList<Long>();
	
	public static List<Long> allplayers = new ArrayList<Long>();
	/**/

	private static NativeArray entityList;

	private static String serverAddress = null;
	private static int serverPort = 0;
	private static Map<Long, String> entityUUIDMap = new HashMap<Long, String>();
	private static boolean nextTickCallsSetLevel = false;
	// initialized in MainActivity now
	public static AtlasProvider terrainMeta, itemsMeta;
	public static ClientBlocksJsonProvider blocksJson;
	public static boolean hasLevel = false;
	public static int requestLeaveGameCounter = 0;
	public static boolean requestScreenshot = false;
	public static boolean requestSelectLevelHasSetScreen = false;

	public static final int ARCH_ARM = 0;
	public static final int ARCH_I386 = 1;
	public static int ITEM_ID_COUNT = 512;
	private static android.app.Instrumentation instrumentation;
	private static ExecutorService instrumentationExecutor;
	public static ModPkgTexturePack modPkgTexturePack = new ModPkgTexturePack("resource_packs/vanilla/");
	private static WorldData worldData = null;
	private static int worldDataSaveCounter = 1;
	private static AndroidPrintStream scriptErrorStream = null;
	private static List<String> nextClientMessages = new ArrayList<String>();
	private static boolean needsReloadAfterHud = false;

	private static final String ENTITY_KEY_RENDERTYPE = "zhuowei.bl.rt";
	private static final String ENTITY_KEY_SKIN = "zhuowei.bl.s";
	private static final String ENTITY_KEY_IMMOBILE = "zhuowei.bl.im";
	private static List<Object[]> deferredLoads = new ArrayList<Object[]>();
	private static Object vmRuntime_singleton = null;
	private static Method vmRuntime_setTargetSdkVersion = null;

	public static void loadScript(Reader in, String sourceName) throws IOException {
		if (!scriptingInitialized)
			return;
		if (requestReloadAllScripts) {
			Log.i("BlockLauncher", "Deferring load of " + sourceName);
			return;
		}
		if (!scriptingEnabled)
			throw new RuntimeException("Not available in multiplayer");
		// Rhino needs lots of recursion depth to parse nested else ifs
		// dalvik vm/Thread.h specifies 256K as maximum stack size
		// default thread depth is 16K (8K on old devices, 1K on super-low-end
		// devices)
		ParseThread parseRunner = new ParseThread(in, sourceName);
		Thread t = new Thread(Thread.currentThread().getThreadGroup(), parseRunner,
				"BlockLauncher parse thread", 256 * 1024);
		t.start();
		try {
			t.join(); // block on this thread
		} catch (InterruptedException ie) {
			// shouldn't happen
		}
		if (parseRunner.error != null) {
			RuntimeException back;
			if (parseRunner.error instanceof RuntimeException) {
				back = (RuntimeException) parseRunner.error;
			} else {
				back = new RuntimeException(parseRunner.error);
			}
			throw back; // Thursdays
		}
	}

	private static class ParseThread implements Runnable {
		private Reader in;
		private String sourceName;
		public Exception error = null;

		public ParseThread(Reader in, String sourceName) {
			this.in = in;
			this.sourceName = sourceName;
		}

		public void run() {
			try {
				Context ctx = Context.enter();
				setupContext(ctx);
				Script script = ctx.compileReader(in, sourceName, 0, null);
				initJustLoadedScript(ctx, script, sourceName);
				Context.exit();
			} catch (Exception e) {
				e.printStackTrace();
				error = e;
			}
		}
	}

	public static void loadScript(File file, boolean firstLoad) throws IOException {
		if (needsToDeferScriptLoadForItem()) {
			Log.i("BlockLauncher", "Deferring load of " + file + " until later.");
			for (int i = deferredLoads.size() - 1; i >= 0; i--) {
				Object[] deferredRequest = deferredLoads.get(i);
				if (deferredRequest[0].equals(file)) {
					deferredLoads.remove(i);
					break;
				}
			}
			deferredLoads.add(new Object[] {file, firstLoad});
			nativeRequestFrameCallback();
			return;
		}
		if (isClassGenMode()) {
			if (!scriptingInitialized)
				return;
			if (!scriptingEnabled)
				throw new RuntimeException("Not available in multiplayer");
			loadScriptFromInstance(ScriptTranslationCache.get(androidContext, file), file.getName());
			return;
		}
		if (isPackagedScript(file)) {
			loadPackagedScript(file, firstLoad);
			return;
		}
		Reader in = null;
		try {
			in = new FileReader(file);
			loadScript(in, file.getName());
		} finally {
			if (in != null)
				in.close();
		}
	}

	public static void loadScriptFromInstance(Script script, String sourceName) {
		Context ctx = Context.enter();
		setupContext(ctx);
		initJustLoadedScript(ctx, script, sourceName);
		Context.exit();
	}

	private static Class<?>[] constantsClasses = {
		ChatColor.class, ItemCategory.class, ParticleType.class, EntityType.class,
		EntityRenderType.class, ArmorType.class, MobEffect.class, DimensionId.class,
		BlockFace.class, UseAnimation.class, Enchantment.class, EnchantType.class,
		BlockRenderLayer.class
	};

	public static void initJustLoadedScript(Context ctx, Script script, String sourceName) {
		Scriptable scope = ctx.initStandardObjects(new BlockHostObject(), false);
		ScriptState state = new ScriptState(script, scope, sourceName);
		String[] names = getAllJsFunctions(BlockHostObject.class);
		((ScriptableObject) scope).defineFunctionProperties(names, BlockHostObject.class,
				ScriptableObject.DONTENUM);
		try {
			// NativeLevelApi levelApi = new NativeLevelApi();
			// levelApi.defineFunctionProperties(getAllJsFunctions(NativeLevelApi.class),
			// NativeLevelApi.class, ScriptableObject.DONTENUM);
			// ScriptableObject.defineProperty(scope, "Level", levelApi,
			// ScriptableObject.DONTENUM);
			ScriptableObject.defineClass(scope, NativePlayerApi.class);
			ScriptableObject.defineClass(scope, NativeLevelApi.class);
			ScriptableObject.defineClass(scope, NativeEntityApi.class);
			ScriptableObject.defineClass(scope, NativeModPEApi.class);
			ScriptableObject.defineClass(scope, NativeItemApi.class);
			ScriptableObject.defineClass(scope, NativeBlockApi.class);
			ScriptableObject.defineClass(scope, NativeServerApi.class);
			RendererManager.defineClasses(scope);
			for (Class<?> clazz: constantsClasses) {
				ScriptableObject.putProperty(scope, clazz.getSimpleName(),
						classConstantsToJSObject(clazz));
			}
		} catch (Exception e) {
			dumpScriptError(e);
			reportScriptError(state, e);
		}

		script.exec(ctx, scope);
		scripts.add(state);
	}

	public static void callScriptMethod(String functionName, Object... args) {
		if (!scriptingEnabled)
			return; // No script loading/callbacks when in a remote world
		Context ctx = Context.enter();
		setupContext(ctx);
		for (ScriptState state : scripts) {
			if (state.errors >= MAX_NUM_ERRORS)
				continue; // Too many errors, skip
			currentScript = state.name;
			Scriptable scope = state.scope;
			Object obj = scope.get(functionName, scope);
			if (obj != null && obj instanceof Function) {
				try {
					((Function) obj).call(ctx, scope, scope, args);
				} catch (Exception e) {
					dumpScriptError(e);
					reportScriptError(state, e);
				}
			}
		}
	}

	// same as above with a check on name
	public static void callScriptMethodOneScript(String scriptName, String functionName, Object... args) {
		if (!scriptingEnabled)
			return; // No script loading/callbacks when in a remote world
		Context ctx = Context.enter();
		setupContext(ctx);
		for (ScriptState state : scripts) {
			if (state.errors >= MAX_NUM_ERRORS)
				continue; // Too many errors, skip
			if (state.name == null || !state.name.equals(scriptName)) continue;
			currentScript = state.name;
			Scriptable scope = state.scope;
			Object obj = scope.get(functionName, scope);
			if (obj != null && obj instanceof Function) {
				try {
					((Function) obj).call(ctx, scope, scope, args);
				} catch (Exception e) {
					dumpScriptError(e);
					reportScriptError(state, e);
				}
			}
		}
	}

	@CallbackName(name="useItem", args={"x", "y", "z", "itemid", "blockid", "side", "itemDamage", "blockDamage"}, prevent=true)
	public static void useItemOnCallback(int x, int y, int z, int itemid, int blockid, int side,
			int itemDamage, int blockDamage) {
		callScriptMethod("useItem", x, y, z, itemid, blockid, side, itemDamage, blockDamage);
		if (itemid >= 0x100 && nativeItemIsExtendedBlock(itemid) && !nativeHasPreventedDefault()) {
			nativePreventDefault();
			doUseItemOurselves(x, y, z, itemid, blockid, side, itemDamage, blockDamage);
		}
	}

	@CallbackName(name="destroyBlock", args={"x", "y", "z", "side"}, prevent=true)
	public static void destroyBlockCallback(int x, int y, int z, int side) {
		int blockId = nativeGetTileWrap(x, y, z);
		callScriptMethod("destroyBlock", x, y, z, side);
		if (blockId >= 0x100 && !nativeHasPreventedDefault()) {
			nativePreventDefault();
			NativeLevelApi.destroyBlock(x, y, z, true);
		}
	}

	@CallbackName(name="startDestroyBlock", args={"x", "y", "z", "side"}, prevent=true)
	public static void startDestroyBlockCallback(int x, int y, int z, int side) {
		callScriptMethod("startDestroyBlock", x, y, z, side);
	}

	private static float lastDestroyProgress = -1f;
	private static int lastDestroyX = 0, lastDestroyY = -1, lastDestroyZ = 0, lastDestroySide = -1;

	@CallbackName(name="continueDestroyBlock", args={"x", "y", "z", "side", "progress"}, prevent=true)
	public static void continueDestroyBlockCallback(int x, int y, int z, int side, float progress) {
		boolean samePlace = x == lastDestroyX && y == lastDestroyY && z == lastDestroyZ && side == lastDestroySide;
		//if (progress == 0 && (progress != lastDestroyProgress || !samePlace)) {
		//	callScriptMethod("startDestroyBlock", x, y, z, side);
		//}
		lastDestroyProgress = progress;
		lastDestroyX = x;
		lastDestroyY = y;
		lastDestroyZ = z;
		lastDestroySide = side;
		callScriptMethod("continueDestroyBlock", x, y, z, side, progress);
	}

	public static void setLevelCallback(boolean hasLevel, boolean isRemoteAAAAAA) {
		//if (nativeGetArch() == ARCH_I386) nextTickCallsSetLevel = true;
	}

	@CallbackName(name="newLevel")
	public static void setLevelFakeCallback(boolean hasLevel, boolean isRemote) {
		isRemote = false;//nativeLevelIsRemote();
		nextTickCallsSetLevel = false;
		System.out.println("Level: " + hasLevel);
		//ScriptManager.isRemote = isRemote;
		if (!isRemote)
			ScriptManager.scriptingEnabled = true; // all local worlds get ModPE
													// support
		nativeSetGameSpeed(20.0f);
		allentities.clear();
		allplayers.clear();
		entityUUIDMap.clear();
		nativeClearCapes();
		ScriptManager.hasLevel = true;

		// I have no idea what to do now, so manually trigger the entity added listener for the local player
		entityAddedCallback(nativeGetPlayerEnt());

		nativeNewLevelCallbackStarted();

		callScriptMethod("newLevel", hasLevel);
		if (MainActivity.currentMainActivity != null) {
			MainActivity main = MainActivity.currentMainActivity.get();
			if (main != null) {
				if (!scriptingEnabled) modernWrapFactory.closePopups(main);
				main.setLevelCallback(!ScriptManager.scriptingEnabled);
			}
		}

		nativeNewLevelCallbackEnded();
		needsReloadAfterHud = true;
	}

	@CallbackName(name="selectLevelHook")
	private static void selectLevelCallback(String wName, String wDir) {
		System.out.println("World name: " + wName);
		System.out.println("World dir: " + wDir);

		worldName = wName;
		worldDir = wDir;
		scriptingEnabled = true;
		isRemote = false;

		if (worldData != null) {
			try {
				worldData.save();
			} catch (IOException ie) {
				ie.printStackTrace();
			}
			worldData = null;
		}
		try {
			File worldsDir = new File("/sdcard/games/com.mojang/minecraftWorlds");
			File theDir = new File(worldsDir, worldDir);
			worldData = new WorldData(theDir);
		} catch (IOException ie) {
			ie.printStackTrace();
		}

		callScriptMethod("selectLevelHook");
		nativeArmorAddQueuedTextures();
		nextTickCallsSetLevel = true;
	}

	@CallbackName(name="leaveGame")
	private static void leaveGameCallback(boolean thatboolean) {
		ScriptManager.isRemote = false;
		ScriptManager.scriptingEnabled = true;
		ScriptManager.hasLevel = false;
		// we check if script has been initialized as this may be called even without ModPE turned on
		if (scriptingInitialized) callScriptMethod("leaveGame");
		if (MainActivity.currentMainActivity != null) {
			MainActivity main = MainActivity.currentMainActivity.get();
			if (main != null) {
				main.leaveGameCallback();
			}
		}
		if (BuildConfig.DEBUG) System.out.println("world data: Leaving the game");
		if (worldData != null) {
			try {
				worldData.save();
			} catch (IOException ie) {
				ie.printStackTrace();
			}
			worldData = null;
		}
		serverAddress = null;
		serverPort = 0;
	}

	@CallbackName(name="attackHook", args={"attacker", "victim"}, prevent=true)
	public static void attackCallback(long attacker, long victim) {
		callScriptMethod("attackHook", attacker, victim);
	}

	@CallbackName(name="entityHurtHook", args={"attacker", "victim", "halfhearts"}, prevent=true)
	public static void entityHurtCallback(long attacker, long victim, int halfhearts) {
		callScriptMethod("entityHurtHook", attacker, victim, halfhearts);
	}

	@CallbackName(name="modTick")
	public static void tickCallback() {
		if (nextTickCallsSetLevel) {
			setLevelFakeCallback(true, nativeLevelIsRemote());
		}
		callScriptMethod("modTick");
		// do we have any requests for graphics reset?
		if (requestedGraphicsReset) {
			nativeOnGraphicsReset();
			requestedGraphicsReset = false;
		}
		// any takers for rotating the player?
		if (sensorEnabled)
			updatePlayerOrientation();
		if (requestLeaveGame && requestLeaveGameCounter-- <= 0) {
			nativeScreenChooserSetScreen(1);
			nativeLeaveGame(false);
			requestLeaveGame = false;
			if (MainActivity.currentMainActivity != null) {
				final MainActivity main = MainActivity.currentMainActivity.get();
				if (main != null) {
					main.runOnUiThread(new Runnable() {
						public void run() {
							main.dismissHiddenTextbox();
							main.hideKeyboardView();
							System.out.println("Closed keyboard, I hope");
						}
					});
				}
			}
			nativeRequestFrameCallback();
		}
		if (requestJoinServer != null && !requestLeaveGame) {
			nativeJoinServer(requestJoinServer.serverAddress, requestJoinServer.serverPort);
			requestJoinServer = null;
		}
		if (runOnMainThreadList.size() > 0) {
			synchronized (runOnMainThreadList) {
				for (Runnable r : runOnMainThreadList) {
					r.run();
				}
				runOnMainThreadList.clear();
			}
		}
		if (worldData != null && --worldDataSaveCounter <= 0) {
			try {
				worldData.save();
			} catch (IOException ie) {
				ie.printStackTrace();
			}
			worldDataSaveCounter = 20*10; // every 10 seconds
		}
		// runDownloadCallbacks();
	}

	private static void updatePlayerOrientation() {
		nativeSetRot(nativeGetPlayerEnt(), newPlayerYaw, newPlayerPitch);
	}

	@CallbackName(name="chatHook", args={"str"}, prevent=true)
	// fixme callback 2
	public static void chatCallback(String str) {
		if (str == null || str.length() < 1)
			return;
		callScriptMethod("chatHook", str);
		if (str.charAt(0) != '/')
			return;
		callScriptMethod("procCmd", str.substring(1));
		String[] splitted = str.substring(1).split(" ");
		boolean validNativeCommand = splitted.length > 0 && nativeIsValidCommand(splitted[0]);
		if (!isRemote && !validNativeCommand) {
			nativePreventDefault();
			if (MainActivity.currentMainActivity != null) {
				MainActivity main = MainActivity.currentMainActivity.get();
				if (main != null) {
					main.updateTextboxText("");
				}
			}
		}
		if (BuildConfig.DEBUG) {
			processDebugCommand(str.substring(1));
		}
	}

	// KsyMC's additions
	@CallbackName(name="deathHook", args={"attacker", "victim"}, prevent=true)
	public static void mobDieCallback(long attacker, long victim) {
		callScriptMethod("deathHook", attacker == -1 ? -1 : attacker, victim);
		if (worldData != null) worldData.clearEntityData(victim);
	}

	// Other nonstandard callbacks
	@CallbackName(name="entityRemovedHook", args={"entity"})
	public static void entityRemovedCallback(long entity) {
		if (NativePlayerApi.isPlayer(entity)) {
			playerRemovedHandler(entity);
		}
		int entityIndex = allentities.indexOf(entity);
		if (entityIndex >= 0) allentities.remove(entityIndex);
		callScriptMethod("entityRemovedHook", entity);
		// entityList.remove(Integer.valueOf(entity));
	}

	@CallbackName(name="entityAddedHook", args={"entity"})
	public static void entityAddedCallback(long entity) {
		System.out.println("Entity added: " + entity + " entity type: " + NativeEntityApi.getEntityTypeId(entity));
		String renderType = NativeEntityApi.getExtraData(entity, ENTITY_KEY_RENDERTYPE);
		if (renderType != null && renderType.length() != 0) {
			RendererManager.NativeRenderer renderer = RendererManager.NativeRendererApi.getByName(renderType);
			if (renderer != null) NativeEntityApi.setRenderTypeImpl(entity, renderer.getRenderType());
		}
		String customSkin = NativeEntityApi.getExtraData(entity, ENTITY_KEY_SKIN);
		if (customSkin != null && customSkin.length() != 0) {
			System.out.println("Custom skin: " + customSkin);
			NativeEntityApi.setMobSkinImpl(entity, customSkin, false);
		}
		String immobile = NativeEntityApi.getExtraData(entity, ENTITY_KEY_IMMOBILE);
		if (immobile != null && immobile.length() != 0) {
			System.out.println("Immobile: " + customSkin);
			NativeEntityApi.setImmobileImpl(entity, immobile.equals("1"));
		}
		// check if entity is player
		if (NativePlayerApi.isPlayer(entity)) {
			playerAddedHandler(entity);
		}
		allentities.add(entity);
		callScriptMethod("entityAddedHook", entity);
		// entityList.put(entityList.getLength(), entityList, entity);
	}

	public static void levelEventCallback(int player, int eventType, int x, int y, int z, int data) {
		callScriptMethod("levelEventHook", player, eventType, x, y, z, data);
	}

	public static void blockEventCallback(int x, int y, int z, int type, int data) {
		callScriptMethod("blockEventHook", x, y, z, type, data);
	}

	private static void rakNetConnectCallback(String hostname, int port) {
		Log.i("BlockLauncher", "Connecting to " + hostname + ":" + port);
		ScriptManager.scriptingEnabled = ScriptManager.isLocalAddress(hostname);
		Log.i("BlockLauncher", "Scripting is now " + (scriptingEnabled? "enabled" : "disabled"));
		serverAddress = hostname;
		serverPort = port;
		ScriptManager.isRemote = true;
		if (MainActivity.currentMainActivity != null) {
			MainActivity main = MainActivity.currentMainActivity.get();
			if (main != null) {
				if (!scriptingEnabled) modernWrapFactory.closePopups(main);
				main.setLevelCallback(!ScriptManager.scriptingEnabled);
			}
		}
	}

	public static void frameCallback() {
		if (requestReloadAllScripts) {
			if (!nativeIsValidItem(256)) {
				nativeRequestFrameCallback();
				return;
			}
			requestReloadAllScripts = false;
			System.out.println("BlockLauncher is loading scripts");
			try {
				if (!new File("/sdcard/mcpelauncher_do_not_create_placeholder_blocks").exists()) {
					nativeDefinePlaceholderBlocks();
				}
				MobEffect.initIds();
				loadEnabledScripts();
				// on 1.8/1.9 this only happens after selectLevelHook is hit
				callScriptMethod("selectLevelHook");
				//nativeArmorAddQueuedTextures();
			} catch (Exception e) {
				dumpScriptError(e);
				reportScriptError(null, e);
			}
		}

		tryDeferLoads();

		if (requestSelectLevel != null && !requestLeaveGame) {
			if (!requestSelectLevelHasSetScreen) {
				nativeShowProgressScreen();
				requestSelectLevelHasSetScreen = true;
				nativeRequestFrameCallback();
			} else {
				nativeSelectLevel(requestSelectLevel.dir, requestSelectLevel.name);
				requestSelectLevel = null;
				requestSelectLevelHasSetScreen = false;
			}
		}
		if (requestScreenshot) {
			ScreenshotHelper.takeScreenshot(screenshotFileName);
			requestScreenshot = false;
		}
		synchronized (nextClientMessages) {
			for (String a : nextClientMessages) {
				nativeClientMessage(a);
			}
			nextClientMessages.clear();
		}
	}

	@CallbackName(name="serverMessageReceiveHook", args={"str"}, prevent=true)
	public static void handleChatPacketCallback(String str) {
		if (str == null || str.length() < 1)
			return;
		callScriptMethod("serverMessageReceiveHook", str);
		if (BuildConfig.DEBUG) {
			System.out.println("message: " + str);
		}
	}

	@CallbackName(name="chatReceiveHook", args={"str", "sender"}, prevent=true)
	private static void handleMessagePacketCallback(String sender, String str) {
		if (str == null || str.length() < 1)
			return;
		if (sender.length() == 0 && str.equals("\u00a70BlockLauncher, enable scripts")) {
			scriptingEnabled = true;
			nativePreventDefault();
			if (MainActivity.currentMainActivity != null) {
				MainActivity main = MainActivity.currentMainActivity.get();
				if (main != null) {
					main.scriptPrintCallback("Scripts have been re-enabled", "");
				}
			}

		}
		callScriptMethod("chatReceiveHook", str, sender);
		if (BuildConfig.DEBUG) {
			System.out.println("chat: " + sender + ": " + str);
		}
	}

	@CallbackName(name="explodeHook", args={"entity", "x", "y", "z", "power", "onFire"}, prevent=true)
	public static void explodeCallback(long entity, float x, float y, float z, float power, boolean onFire) {
		callScriptMethod("explodeHook", entity, x, y, z, power, onFire);
	}

	@CallbackName(name="eatHook", args={"hearts", "saturationRatio"})
	public static void eatCallback(int hearts, float notHearts) {
		callScriptMethod("eatHook", hearts, notHearts);
	}

	@CallbackName(name="redstoneUpdateHook", args={"x", "y", "z", "newCurrent", "someBooleanIDontKnow", "blockId", "blockData"})
	public static void redstoneUpdateCallback(int x, int y, int z, int newCurrent, boolean something, int blockId, int blockData) {
		callScriptMethod("redstoneUpdateHook", x, y, z, newCurrent, something, blockId, blockData);
	}

	@CallbackName(name="projectileHitBlockHook", args={"projectile", "blockX", "blockY", "blockZ", "side"})
	public static void throwableHitCallback(long projectile, int type, int side,
		int x, int y, int z, float hX, float hY, float hZ, long targetEntity) {
		Integer customProjectileId = (nativeGetEntityTypeId(projectile) == EntityType.SNOWBALL)?
			NativeItemApi.itemIdToRendererId.get(nativeEntityGetRenderType(projectile)): null;
		if (type == 0) {
			if (customProjectileId != null) {
				callScriptMethod("customThrowableHitBlockHook", projectile, (int)customProjectileId, x, y, z, side);
			}
			callScriptMethod("projectileHitBlockHook", projectile, x, y, z, side);
		} else if (type == 1) {
			if (customProjectileId != null) {
				callScriptMethod("customThrowableHitEntityHook", projectile, (int)customProjectileId, targetEntity);
			}
			callScriptMethod("projectileHitEntityHook", projectile, targetEntity);
		}
	}
	@CallbackName(name="projectileHitEntityHook", args={"projectile", "targetEntity"})
	public static void dummyThrowableHitEntityCallback() {};

	@CallbackName(name="playerAddExpHook", args={"player", "experienceAdded"}, prevent=true)
	public static void playerAddExperienceCallback(long player, int experienceAdded) {
		callScriptMethod("playerAddExpHook", player, experienceAdded);
	}

	@CallbackName(name="playerExpLevelChangeHook", args={"player", "levelsAdded"}, prevent=true)
	public static void playerAddLevelsCallback(long player, int experienceAdded) {
		callScriptMethod("playerExpLevelChangeHook", player, experienceAdded);
	}

	private static String currentScreen;

	@CallbackName(name="screenChangeHook", args={"screenName"})
	public static void screenChangeCallback(String s1, String s2, String s3) {
		System.out.println("Screen change: " + s1 + ":" + s2 + ":" + s3);
		if ("options_screen".equals(s1) && "resource_packs_screen".equals(currentScreen)) {
			// reload scripts
			loadResourcePackScripts();
		}
		if ("store_screen".equals(s1)) {
			MainActivity activity = MainActivity.currentMainActivity.get();
			if (activity != null) {
				activity.showStoreNotWorkingDialog();
			}
		}

		if (needsReloadAfterHud && ("in_game_play_screen".equals(s1) || "hud_screen".equals(s1))) {
			NativeItemApi.redefineThrowableRenderersAfterReload();
			needsReloadAfterHud = false;
		}
		if ("in_game_play_screen".equals(s1)) {
			// DesnoGuns r18 needs this
			s1 = "hud_screen";
		}
		currentScreen = s1;
		callScriptMethod("screenChangeHook", s1);
	}

	private static void reregisterRecipesCallback() {
		NativeItemApi.reregisterRecipes();
	}

	public static InputStream getSoundInputStream(String name, long[] lengthout) {
		System.out.println("Get sound input stream");
		if (MainActivity.currentMainActivity != null) {
			MainActivity main = MainActivity.currentMainActivity.get();
			if (main != null) {
				return main.getInputStreamForAsset(name.substring("file:///android_asset/".length()), lengthout);
			}
		}
		return null;
	}

	public static byte[] getSoundBytes(String name) {
		if (name == null || !name.startsWith("file:///android_asset/")) {
			System.out.println("Invalid sound byte: " + name);
			return null;
		}
		if (MainActivity.currentMainActivity != null) {
			MainActivity main = MainActivity.currentMainActivity.get();
			if (main != null) {
				return main.getFileDataBytes(name.substring("file:///android_asset/".length()));
			}
		}
		return null;
	}

	public static void init(android.content.Context cxt) throws IOException {
		scriptingInitialized = true;
		MainActivity mainActivity = MainActivity.currentMainActivity.get();
		if (mainActivity != null && mainActivity.getMCPEVersion().startsWith("0.16")) {
			modPkgTexturePack = new ModPkgTexturePack("resourcepacks/vanilla/");
		}
		// set up hooks
		int versionCode = 0;
		try {
			versionCode = cxt.getPackageManager().getPackageInfo("com.mojang.minecraftpe", 0).versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			// impossible, as if the package isn't installed, the app would've
			// quit before loading scripts
		}
		if (MinecraftVersion.isAmazon()) {
			versionCode = 0xaaaa;
		}
		nativeSetupHooks(versionCode);
		ITEM_ID_COUNT = nativeGetItemIdCount();
		scripts.clear();
		entityList = new NativeArray(0);
		androidContext = cxt.getApplicationContext();
		// loadEnabledScripts(); Minecraft blocks wouldn't be initialized when
		// this is called
		// call it before the first frame renders

		ContextFactory.initGlobal(new BlockContextFactory());
		NativeJavaMethod.setMethodWatcher(new MyMethodWatcher());
		requestReloadAllScripts = true;
		nativeRequestFrameCallback();
		prepareEnabledScripts();
	}

	public static void destroy() {
		scriptingInitialized = false;
		androidContext = null;
		scripts.clear();
		runOnMainThreadList.clear();
	}

	public static void removeScript(String scriptId) {
		/*
		 * Don't clear data here - user can clear data by hand if needed
		 * SharedPreferences sPrefs =
		 * androidContext.getSharedPreferences("BlockLauncherModPEScript"
		 * +scriptId+"Data", 0); SharedPreferences.Editor prefsEditor =
		 * sPrefs.edit(); prefsEditor.clear(); prefsEditor.commit();
		 */
		// check if it's in the deferred loads
		for (int i = deferredLoads.size() - 1; i >= 0; i--) {
			Object[] deferredRequest = deferredLoads.get(i);
			File file = (File)deferredRequest[0];
			if (file.getName().equals(scriptId)) {
				deferredLoads.remove(i);
				break;
			}
		}

		for (int i = scripts.size() - 1; i >= 0; i--) {
			if (scripts.get(i).name.equals(scriptId)) {
				scripts.remove(i);
				break;
			}
		}
		if (isPackagedScript(scriptId)) {
			try {
				modPkgTexturePack.removePackage(scriptId);
			} catch (IOException ie) {
				ie.printStackTrace();
			}
		}
	}

	public static void reloadScript(File file) throws IOException {
		removeScript(file.getName());
		loadScript(file, false);
	}

	public static void reportScriptError(ScriptState state, Throwable t) {
		if (state != null)
			state.errors++;
		if (MainActivity.currentMainActivity != null) {
			MainActivity main = MainActivity.currentMainActivity.get();
			if (main != null) {
				main.scriptErrorCallback(state == null ? "Unknown script" : state.name, t);
				if (state != null && state.errors >= MAX_NUM_ERRORS) { // too
																		// many
																		// errors
					main.scriptTooManyErrorsCallback(state.name);
				}
			}
		}
	}

	private static void scriptPrint(String str) {
		System.out.println(str);
		if (MainActivity.currentMainActivity != null) {
			MainActivity main = MainActivity.currentMainActivity.get();
			if (main != null) {
				main.scriptPrintCallback(str, currentScript);
			}
		}
	}

	private static void scriptFakeTipMessage(String str) {
		if (MainActivity.currentMainActivity != null) {
			MainActivity main = MainActivity.currentMainActivity.get();
			if (main != null) {
				main.fakeTipMessageCallback(str);
			}
		}
	}

	public static void requestGraphicsReset() {
		requestedGraphicsReset = true;
	}

	// following taken from the patch manager
	public static Set<String> getEnabledScripts() {
		return enabledScripts;
	}

	private static void setEnabled(String name, boolean state) throws IOException {
		if (state) {
			reloadScript(getScriptFile(name));
			enabledScripts.add(name);
		} else {
			enabledScripts.remove(name);
			removeScript(name);
		}
		saveEnabledScripts();
	}

	private static void setEnabledWithoutLoad(String name, boolean state) throws IOException {
		if (state) {
			enabledScripts.add(name);
		} else {
			enabledScripts.remove(name);
		}
		saveEnabledScripts();
	}

	public static void setEnabled(File[] files, boolean state) throws IOException {
		for (File file : files) {
			String name = file.getAbsolutePath();
			if (name == null || name.length() <= 0)
				continue;
			if (state) {
				reloadScript(getScriptFile(name));
				enabledScripts.add(name);
			} else {
				enabledScripts.remove(name);
				removeScript(name);
			}
		}
		saveEnabledScripts();
	}

	public static void setEnabled(File file, boolean state) throws IOException {
		setEnabled(file.getName(), state);
	}

	public static void setEnabledWithoutLoad(File file, boolean state) throws IOException {
		setEnabledWithoutLoad(file.getName(), state);
	}

	private static boolean isEnabled(String name) {
		return enabledScripts.contains(name);
	}

	public static boolean isEnabled(File file) {
		return isEnabled(file.getName());
	}

	public static void removeDeadEntries(Collection<String> allPossibleFiles) {
		enabledScripts.retainAll(allPossibleFiles);
		saveEnabledScripts();
	}

	public static void loadEnabledScriptsNames(android.content.Context androidContext) {
		enabledScripts = Utils.getEnabledScripts();
	}

	protected static void loadEnabledScripts() throws IOException {
		for (String name : enabledScripts) {
			// load all scripts into the script interpreter
			File file = getScriptFile(name);
			if (!file.exists() || !file.isFile()) {
				Log.i("BlockLauncher", "ModPE script " + file.toString() + " doesn't exist");
				continue;
			}
			try {
				loadScript(file, true);
			} catch (Exception e) {
				dumpScriptError(e);
				MainActivity.currentMainActivity.get().reportError(e);
			}
		}
		//loadAddonScripts();
		loadResourcePackScripts();
	}

	protected static void loadAddonScripts() {
		MainActivity mainActivity = MainActivity.currentMainActivity.get();
		if (mainActivity == null || mainActivity.addonOverrideTexturePackInstance == null) return; // what
		Map<String, ZipFile> addonFiles = mainActivity.addonOverrideTexturePackInstance.getZipsByPackage();
		for (Map.Entry<String, ZipFile> s: addonFiles.entrySet()) {
			Reader theReader = null;
			try {
				ZipFile zipFile = s.getValue();
				ZipEntry entry = zipFile.getEntry("assets/script/main.js");
				if (entry == null) continue;
				InputStream is = zipFile.getInputStream(entry);
				theReader = new InputStreamReader(is);
				loadScript(theReader, "Addon " + s.getKey() + ":main.js");
			} catch (Exception e) {
				dumpScriptError(e);
				mainActivity.reportError(e);
			} finally {
				if (theReader != null) {
					try {
						theReader.close();
					} catch (IOException ie) {
						ie.printStackTrace();
					}
				}
			}
		}
	}

	protected static void loadResourcePackScripts() {
		MainActivity mainActivity = MainActivity.currentMainActivity.get();
		if (mainActivity == null) return;
		try {
			List<ResourcePack> resourcePacks = ResourcePack.getAllResourcePacks();
			System.out.println(resourcePacks);
			// disable existing resource pack scripts
			for (int i = scripts.size() - 1; i >= 0; i--) {
				if (scripts.get(i).name.startsWith("__bl_ResourcePack_")) {
					scripts.remove(i);
					break;
				}
			}
			for (ResourcePack pack: resourcePacks) {
				Reader theReader = null;
				try {
					InputStream is = pack.getInputStream("main.js");
					if (is == null) continue;
					theReader = new InputStreamReader(is);
					loadScript(theReader, "__bl_ResourcePack_" + pack.getName() + "_main.js");
				} catch (Exception e) {
					dumpScriptError(e);
					mainActivity.reportError(e);
				} finally {
					if (theReader != null) {
						try {
							theReader.close();
						} catch (IOException ie) {
							ie.printStackTrace();
						}
					}
				}
			}
		} catch (IOException ie) {
			ie.printStackTrace();
		}
	}

	protected static void prepareEnabledScripts() throws IOException {
		loadEnabledScriptsNames(androidContext);
		final boolean reimportEnabled = Utils.getPrefs(0).getBoolean("zz_reimport_scripts", false);
		final StringBuilder reimportedString = new StringBuilder();
		for (String name : enabledScripts) {
			// load all scripts into the script interpreter
			File file = getScriptFile(name);
			if (!file.exists() || !file.isFile()) {
				Log.i("BlockLauncher", "ModPE script " + file.toString() + " doesn't exist");
				continue;
			}
			try {
				if (reimportEnabled) {
					if (reimportIfPossible(file)) reimportedString.append(file.getName()).append(' ');
				}
				prepareScript(file);
			} catch (Exception e) {
				dumpScriptError(e);
				MainActivity.currentMainActivity.get().reportError(e);
			}
		}
		if (reimportedString.length() != 0) {
			MainActivity.currentMainActivity.get().reportReimported(reimportedString.toString());
		}
	}

	private static void prepareScript(File file) throws Exception {
		if (!isPackagedScript(file)) return;
		modPkgTexturePack.addPackage(file);
	}

	protected static void saveEnabledScripts() {
		SharedPreferences sharedPrefs = Utils.getPrefs(1);
		SharedPreferences.Editor edit = sharedPrefs.edit();
		edit.putString("enabledScripts", join(enabledScripts.toArray(blankArray), ";"));
		edit.putInt("scriptManagerVersion", 1);
		edit.apply();
	}

	public static File getScriptFile(String scriptId) {
		File scriptsFolder = androidContext.getDir(SCRIPTS_DIR, 0);
		return new File(scriptsFolder, scriptId);
	}

	public static void setOriginalLocation(File source, File target) throws IOException {
		SharedPreferences sharedPrefs = Utils.getPrefs(1);
		SharedPreferences.Editor edit = sharedPrefs.edit();
		JSONObject originalLocations = getOriginalLocations();
		try {
			originalLocations.put(target.getName(), source.getAbsolutePath());
		} catch (JSONException jsonException) {
			throw new RuntimeException("Setting original location failed", jsonException);
		}
		edit.putString("scriptOriginalLocations", originalLocations.toString());
		edit.apply();
	}

	public static JSONObject getOriginalLocations() {
		try {
			SharedPreferences sharedPrefs = Utils.getPrefs(1);
			return new JSONObject(sharedPrefs.getString("scriptOriginalLocations", "{}"));
		} catch (JSONException ex) {
			return new JSONObject();
		}
	}

	public static File getOriginalFile(File curFile) {
		String originalLoc = getOriginalLocations().optString(curFile.getName(), null);
		if (originalLoc == null) return null;
		File originalFile = new File(originalLoc);
		if (!originalFile.exists()) return null;
		return originalFile;
	}

	public static boolean reimportIfPossible(File curFile) throws IOException {
		File originalFile = getOriginalFile(curFile);
		if (originalFile == null) return false;
		if (originalFile.lastModified() <= curFile.lastModified()) return false;
		PatchUtils.copy(originalFile, curFile);
		return true;
	}

	// end script manager controls

	private static String[] getAllJsFunctions(Class<? extends ScriptableObject> clazz) {
		List<String> allList = new ArrayList<String>();
		for (Method met : clazz.getMethods()) {
			if (met.getAnnotation(JSFunction.class) != null) {
				allList.add(met.getName());
			}
		}
		return allList.toArray(blankArray);
	}

	private static boolean invalidTexName(String tex) {
		return tex == null || tex.equals("undefined") || tex.equals("null");
	}

	private static boolean isValidStringParameter(String tex) {
		return !invalidTexName(tex);
	}

	private static void clientMessageImpl(String msg) {
		synchronized(nextClientMessages) {
			nextClientMessages.add(msg);
			nativeRequestFrameCallback();
		}
	}

	private static void wordWrapClientMessage(String msg) {
		String[] portions = msg.split("\n");
		for (int i = 0; i < portions.length; i++) {
			String line = portions[i];

			if (msg.indexOf(ChatColor.BEGIN) >= 0) {
				// TODO: properly word wrap colour codes
				clientMessageImpl(line);
				continue;
			}

			while (line.length() > 40) {
				String newStr = line.substring(0, 40);// colorCodeSubstring(line,
														// 0, 40);
				clientMessageImpl(newStr);
				line = line.substring(newStr.length());
			}
			if (line.length() > 0) {
				clientMessageImpl(line);
			}
		}
	}

	/*
	 * private static String colorCodeSubstring(String line, int begin, int end)
	 * { int charsCounted = 0; int i; for (i = begin; i < line.length(); i++) {
	 * char myChar = line.charAt(i); if (myChar == ChatColor.BEGIN) { i++;
	 * //skip the next character as well } else { charsCounted++; if
	 * (charsCounted == requiredChars) { return line.substring(begin, i + 1); }
	 * } } }
	 */

	/** Returns a description of ALL the methods this ModPE runtime supports. */
	public static String getAllApiMethodsDescriptions() {
		StringBuilder builder = new StringBuilder();
		appendApiMethods(builder, BlockHostObject.class, null);
		appendApiMethods(builder, NativeModPEApi.class, "ModPE");
		appendApiMethods(builder, NativeLevelApi.class, "Level");
		appendApiMethods(builder, NativePlayerApi.class, "Player");
		appendApiMethods(builder, NativeEntityApi.class, "Entity");
		appendApiMethods(builder, NativeItemApi.class, "Item");
		appendApiMethods(builder, NativeBlockApi.class, "Block");
		appendApiMethods(builder, NativeServerApi.class, "Server");
		appendCallbacks(builder);
		for (Class<?> clazz: constantsClasses) {
			appendApiClassConstants(builder, clazz);
		}
		return builder.toString();

	}

	private static void appendApiMethods(StringBuilder builder, Class<?> clazz, String namespace) {
		for (Method met : clazz.getMethods()) {
			if (met.getAnnotation(JSFunction.class) != null
					|| met.getAnnotation(JSStaticFunction.class) != null) {
				appendApiMethodDescription(builder, met, namespace);
			}
		}
		builder.append("\n");
	}

	private static void appendApiMethodDescription(StringBuilder builder, Method met,
			String namespace) {
		if (namespace != null) {
			builder.append(namespace);
			builder.append('.');
		}
		builder.append(met.getName());
		builder.append('(');
		Class<?>[] params = met.getParameterTypes();
		for (int i = 0; i < params.length; i++) {
			builder.append("par");
			builder.append(i + 1);
			builder.append(params[i].getSimpleName().replaceAll("Native", ""));
			if (i < params.length - 1) {
				builder.append(", ");
			}
		}
		builder.append(");\n");
	}

	private static void appendApiClassConstants(StringBuilder builder, Class<?> clazz) {
		String className = clazz.getSimpleName();
		for (Field field : clazz.getFields()) {
			int fieldModifiers = field.getModifiers();
			if (!Modifier.isStatic(fieldModifiers) || !Modifier.isPublic(fieldModifiers))
				continue;
			builder.append(className).append(".").append(field.getName()).append(";\n");
		}
		builder.append("\n");
	}

	private static void appendCallbacks(StringBuilder builder) {
		for (Method met: ScriptManager.class.getMethods()) {
			CallbackName name = met.getAnnotation(CallbackName.class);
			if (name == null) continue;
			if (name.prevent()) builder.append("// can use preventDefault()\n");
			builder.append("function ").append(name.name()).append("(").
				append(Utils.joinArray(name.args(), ", ")).append(")\n");
		}
		builder.append("\n");
	}

	// end method dumping code

	private static boolean isLocalAddress(String str) {
		return true;
/*
		// Use Java's built-in support for this
		try {
			InetAddress address = InetAddress.getByName(str);
			Log.i("BlockLauncher", str);
			boolean retval = address.isLoopbackAddress() || address.isLinkLocalAddress()
					|| address.isSiteLocalAddress();
			return retval;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		}
*/
	}

	public static void takeScreenshot(String fileName) {
		screenshotFileName = fileName.replace("/", "").replace("\\", "");
		requestScreenshot = true;
		nativeRequestFrameCallback();
	}

	/*
	 * private static void runDownloadCallbacks() { Runnable message; while
	 * ((message = mainThreadRunnableQueue.poll()) != null) { message.run(); } }
	 */

	private static void overrideTexture(String urlString, String textureName) {
		if (androidContext == null) return;
		if (textureName.contains("terrain-atlas.tga") || textureName.contains("items-opaque.png")) {
			scriptPrint("cannot override " + textureName);
			return;
		}

		// download from URL
		// saves it to ext storage's texture folder
		// then, schedule callback
		if (urlString == "") {
			// clear this texture override
			clearTextureOverride(textureName);
			return;
		}
		try {
			URL url = new URL(urlString);
			new Thread(new ScriptTextureDownloader(url, getTextureOverrideFile(textureName)))
					.start();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static File externalFilesDir = null;
	public static File getTextureOverrideFile(String textureName) {
		if (androidContext == null) return null;
		if (externalFilesDir == null) externalFilesDir = androidContext.getExternalFilesDir(null);
		File stagingTextures = new File(externalFilesDir, "textures");
		return new File(stagingTextures, textureName.replace("..", ""));
	}

	public static void clearTextureOverrides() {
		if (androidContext == null) return;
		File stagingTextures = new File(androidContext.getExternalFilesDir(null), "textures");
		Utils.clearDirectory(stagingTextures);
		requestedGraphicsReset = true;
	}

	private static void clearTextureOverride(String texture) {
		File file = getTextureOverrideFile(texture);
		if (file != null && file.exists()) {
			file.delete();
		}
		requestedGraphicsReset = true;
	}

	public static ScriptableObject classConstantsToJSObject(Class<?> clazz) {
		ScriptableObject obj = new NativeObject();
		for (Field field : clazz.getFields()) {
			int fieldModifiers = field.getModifiers();
			if (!Modifier.isStatic(fieldModifiers) || !Modifier.isPublic(fieldModifiers))
				continue;
			try {
				obj.putConst(field.getName(), obj, field.get(null));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return obj;
	}

	public static void setupContext(Context ctx) {
		ctx.setOptimizationLevel(-1); // No dynamic translation; we interpret
										// and/or precompile
		ctx.setLanguageVersion(Context.VERSION_ES6);
		/*
		 * if (android.preference.PreferenceManager.getDefaultSharedPreferences(
		 * androidContext).getBoolean("zz_script_paranoid_mode", false)) {
		 * ctx.setWrapFactory(modernWrapFactory); }
		 */
	}

	public static TextureRequests expandTexturesArray(Object inArrayObj) {
		int[] endArray = new int[16 * 6];
		String[] stringArray = new String[16 * 6];
		TextureRequests retval = new TextureRequests();
		retval.coords = endArray;
		retval.names = stringArray;

		if (inArrayObj instanceof String) {
			String fillVal = ((String) inArrayObj);
			Arrays.fill(stringArray, fillVal);
			return retval;
		}
		Scriptable inArrayScriptable = (Scriptable) inArrayObj;
		// if the in array count is a multiple of 6,
		// copy 6 at a time until we run out, then copy 6 from the first
		// element.
		int inArrayLength = ((Number) ScriptableObject.getProperty(inArrayScriptable, "length"))
				.intValue();
		int wrap = inArrayLength % 6 == 0 ? 6 : 1;
		Object firstObj = ScriptableObject.getProperty(inArrayScriptable, 0);
		if ((inArrayLength == 1 || inArrayLength == 2) && firstObj instanceof String) {
			// all blocks have same tex
			String fillVal = ((String) firstObj);
			Arrays.fill(stringArray, fillVal);
			if (inArrayLength == 2) {
				int fillVal2 = ((Number) ScriptableObject.getProperty(inArrayScriptable, 1))
						.intValue();
				Arrays.fill(endArray, fillVal2);
			}
			return retval;
		}
		for (int i = 0; i < endArray.length; i++) {
			Object myObj;
			if (i < inArrayLength) {
				myObj = ScriptableObject.getProperty(inArrayScriptable, i);
			} else {
				myObj = ScriptableObject.getProperty(inArrayScriptable, i % wrap);
			}
			Scriptable myScriptable = (Scriptable) myObj;
			String texName = ((String) ScriptableObject.getProperty(myScriptable, 0));
			int texCoord = 0;
			int subarrayLength = ((Number) ScriptableObject.getProperty(myScriptable, "length"))
				.intValue();
			if (subarrayLength > 1) texCoord = ((Number) ScriptableObject.getProperty(myScriptable, 1)).intValue();
			endArray[i] = texCoord;
			stringArray[i] = texName;
		}
		return retval;
	}

	public static TextureRequests mapTextureNames(TextureRequests requests) {
		for (int i = 0; i < requests.coords.length; i++) {
			String name = requests.names[i];
			if (name.equals("stonecutter")) {
				String[] stonecutterNames = new String[] {"stonecutter_side", "stonecutter_other_side", "stonecutter_top", "stonecutter_bottom"};
				requests.names[i] = stonecutterNames[requests.coords[i] % 4];
				requests.coords[i] = 0;
			} else if (name.equals("piston_inner")) {
				requests.names[i] = "piston_top";
			}
		}
		return requests;
	}

	/*
	 * public static int expandTextureCoordinate(Object myObj) { if (myObj
	 * instanceof Number) { return ((Number) myObj).intValue(); } else if (myObj
	 * instanceof Scriptable) { Scriptable myScriptable = (Scriptable) myObj;
	 * int texRow = ((Number) ScriptableObject.getProperty(myScriptable,
	 * 0)).intValue(); int texCol = ((Number)
	 * ScriptableObject.getProperty(myScriptable, 1)).intValue(); return (texRow
	 * * 16) + texCol; } throw new
	 * IllegalArgumentException("Invalid texture coordinate input: " + myObj); }
	 */

	public static int[] expandColorsArray(Scriptable inArrayScriptable) {
		int inArrayLength = ((Number) ScriptableObject.getProperty(inArrayScriptable, "length"))
				.intValue();
		int[] endArray = new int[16];
		for (int i = 0; i < endArray.length; i++) {
			if (i < inArrayLength) {
				endArray[i] = (int) ((Number) ScriptableObject.getProperty(inArrayScriptable, i))
						.longValue();
			} else {
				endArray[i] = (int) ((Number) ScriptableObject.getProperty(inArrayScriptable, 0))
						.longValue();
			}
		}
		Log.i("BlockLauncher", Arrays.toString(endArray));
		return endArray;
	}

	public static void processDebugCommand(String cmd) {
		try {
			if (cmd.equals("dumpitems")) {
				debugDumpItems();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void debugDumpItems() throws IOException {
		PrintWriter out = new PrintWriter(new File(Environment.getExternalStorageDirectory(),
				"/items.csv"));
		float[] textureUVbuf = new float[6];
		int[][] bonuses = {{1,1,6},{12,1,1},{38,0,8},{159,0,15},{171,0,15},{175,0,5},{349,1,3},{350,1,1},{383,10,63}};
		for (int i = 0; i < ITEM_ID_COUNT; i++) {
			String itemName = nativeGetItemName(i, 0, true);
			if (itemName == null)
				continue;
			boolean success = nativeGetTextureCoordinatesForItem(i, 0, textureUVbuf);
			String itemIcon = Arrays.toString(textureUVbuf).replace("[", "").replace("]", "")
					.replace(",", "|");
			out.println(i + "," + itemName + "," + itemIcon);
		}
		for (int[] bonus: bonuses) {
			int id = bonus[0];
			for (int dmg = bonus[1]; dmg <= bonus[2]; dmg++) {
				String itemName = nativeGetItemName(id, dmg, true);
				if (itemName == null)
					continue;
				boolean success = nativeGetTextureCoordinatesForItem(id, dmg, textureUVbuf);
				String itemIcon = Arrays.toString(textureUVbuf).replace("[", "").replace("]", "")
						.replace(",", "|");
				out.println(id+":"+dmg + "," + itemName + "," + itemIcon);
			}
		}
		out.close();
	}

	private static void playerAddedHandler(long entityId) {
		allplayers.add(entityId);
		if (!shouldLoadSkin())
			return;
		runOnMainThread(new SkinLoader(entityId));
	}
	private static class SkinLoader implements Runnable {
		private long entityId;
		public SkinLoader(long entityId) {
			this.entityId = entityId;
		}
		public void run() {
			try {
				// load skin for player
				String playerName = nativeGetPlayerName(entityId);
				System.out.println("Player name: " + playerName + " entity ID: " + entityId);
				if (playerName == null) return;

				if (isSkinNameNormalized()) playerName = playerName.toLowerCase();
				if (playerName.length() <= 0) return;
				String skinName = "mob/" + playerName + ".png";
				File skinFile = getTextureOverrideFile("images/" + skinName);
				if (skinFile == null) return;
				String urlString = getSkinURL(playerName);

				/*String capeName = "cape/" + playerName + ".png";
				File capeFile = getTextureOverrideFile("images/" + capeName);
				if (capeFile == null) return;
				String capeUrlString = getCapeURL(playerName);*/

				URL url = new URL(urlString);
				new Thread(new ScriptTextureDownloader(url, skinFile, new AfterSkinDownloadAction(
						entityId, skinName), false)).start();
				/*URL capeUrl = new URL(capeUrlString);
				new Thread(new ScriptTextureDownloader(capeUrl, capeFile, new AfterCapeDownloadAction(
						entityId, capeName), false)).start();*/
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static String getSkinURL(String name) {
		//if (Utils.getPrefs(0).getBoolean("zz_skin_load_pc", false)) {
		//	return "http://s3.amazonaws.com/MinecraftSkins/" + name + ".png";
		//}
		return "http://blskins.ablecuboid.com/blskins/" + name + ".png";
	}

	private static String getCapeURL(String name) {
		return "http://blskins.ablecuboid.com/blskins/capes/" + name + ".png";
	}

	private static boolean isSkinNameNormalized() {
		return true;
	}

	private static void playerRemovedHandler(long entityId) {
		int entityIndex = allplayers.indexOf(entityId);
		if (entityIndex >= 0) allplayers.remove(entityIndex);
	}

	public static void runOnMainThread(Runnable run) {
		synchronized (runOnMainThreadList) {
			runOnMainThreadList.add(run);
		}
	}

	private static boolean shouldLoadSkin() {
		return false;//Utils.getPrefs(0).getString("zz_skin_download_source", "mojang_pc")
			//	.equals("mojang_pc");
	}

	private static boolean isClassGenMode() {
		return false;
	}

	private static int[] expandShapelessRecipe(Scriptable inArrayScriptable) {
		int inArrayLength = ((Number) ScriptableObject.getProperty(inArrayScriptable, "length"))
				.intValue();
		Object firstObj = ScriptableObject.getProperty(inArrayScriptable, 0);
		int[] endArray = null;
		if (firstObj instanceof Number) {
			if (inArrayLength % 3 != 0) {
				throw new IllegalArgumentException(
						"Array length must be multiple of 3 (this was changed in 1.6.8): [itemid, itemCount, itemdamage, ...]");
			} else {
				endArray = new int[inArrayLength];
				for (int i = 0; i < endArray.length; i++) {
					endArray[i] = ((Number) ScriptableObject.getProperty(inArrayScriptable, i))
							.intValue();
				}
			}
		} else {
			throw new IllegalArgumentException(
					"Method takes in an array of [itemid, itemCount, itemdamage, ...]");
			// TODO: more types
		}
		return endArray;
	}

	private static void nameAndShame(String str) {
	}

	private static String getEntityUUID(long entityId) {
		return Long.toString(entityId);
	}

	private static boolean isPackagedScript(File file) {
		return isPackagedScript(file.getName());
	}
	private static boolean isPackagedScript(String str) {
		return str.toLowerCase().endsWith(".modpkg");
	}

	private static void loadPackagedScript(File file, boolean firstLoad) throws IOException {
		if (!firstLoad) modPkgTexturePack.addPackage(file);
		boolean success = false;
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);

			MpepInfo info = null;
			boolean scrambled = false;
			try {
				info = MpepInfo.fromZip(zipFile);
				scrambled = info != null && info.scrambleCode.length() > 0;
			} catch (JSONException json) {
				// ignore lol
			}

			ZipEntry entry;
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while(entries.hasMoreElements()) {
				entry = entries.nextElement();
				Reader reader = null;
				String name = entry.getName();
				if (!name.startsWith("script/") || !name.toLowerCase().endsWith(".js")) continue;
				try {
					if (scrambled) {
						InputStream is = zipFile.getInputStream(entry);
						byte[] scrambleBytes = new byte[(int) entry.getSize()];
						is.read(scrambleBytes);
						is.close();
						reader = Scrambler.scramble(scrambleBytes, info);
					} else {
						reader = new InputStreamReader(zipFile.getInputStream(entry));
					}
					loadScript(reader, file.getName());
				} finally {
					if (reader != null) reader.close();
				}
				break;
			}
			success = true;
		} finally {
			if (zipFile != null) zipFile.close();
			if (!firstLoad && !success) modPkgTexturePack.removePackage(file.getName());
		}
	}

	private static void verifyBlockTextures(TextureRequests requests) {
		if (terrainMeta == null) return;
		for (int i = 0; i < requests.names.length; i++) {
			if (!terrainMeta.hasIcon(requests.names[i], requests.coords[i])) {
				throw new MissingTextureException("The requested block texture " +
					requests.names[i] + ":" + requests.coords[i] + " does not exist");
			}
		}
	}

	private static void setRequestLeaveGame() {
		nativeCloseScreen();
		requestLeaveGame = true;
		requestLeaveGameCounter = 10;
	}

	private static long getEntityId(Object entityId) {
		if (entityId == null) return -1;
		if (entityId instanceof NativeJavaObject) {
			return (Long) ((NativeJavaObject) entityId).unwrap();
		}
		if (entityId instanceof Number) {
			return ((Number) entityId).longValue();
		}
		if (entityId instanceof Undefined) {
			return 0;
		}
		throw new RuntimeException("Not an entity: " + entityId + " (" + entityId.getClass().toString() + ")");
	}

	private static void injectKeyEvent(final int key, final int pressed) {
		if (instrumentation == null) {
			instrumentation = new android.app.Instrumentation();
			instrumentationExecutor = Executors.newSingleThreadExecutor();
		}
		instrumentationExecutor.execute(new Runnable() {
			public void run() {
				instrumentation.sendKeySync(new KeyEvent(pressed != 0? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP, key));
			}
		});
	}

	private static long spawnEntityImpl(float x, float y, float z, int entityType, String skinPath) {
		if (entityType <= 0) throw new RuntimeException("Invalid entity type: " + entityType);
		if (skinPath != null) {
			String newSkinPath = OldEntityTextureFilenameMapping.m.get(skinPath);
			if (newSkinPath != null) skinPath = newSkinPath;
			if (skinPath.endsWith(".png") || skinPath.endsWith(".tga")) {
				skinPath = skinPath.substring(0, skinPath.length() - 4);
			}
		}
		long retval = nativeSpawnEntity(x, y, z, entityType, skinPath);
		if (nativeEntityHasCustomSkin(retval)) {
			NativeEntityApi.setExtraData(retval, ENTITY_KEY_SKIN, skinPath);
		}
		return retval;
	}

	protected static ModernWrapFactory getWrapFactory() {
		return modernWrapFactory;
	}

	protected static boolean isScriptingEnabled() {
		return scriptingEnabled;
	}

	private static String getPlayerNameFromConfs() {
		try {
			File f = new File(Environment.getExternalStorageDirectory(), "games/com.mojang/minecraftpe/options.txt");
			if (!f.exists()) return "Steve";
			byte[] fileBytes = new byte[(int)f.length()];
			FileInputStream fis = new FileInputStream(f);
			fis.read(fileBytes);
			fis.close();
			String[] strs = new String(fileBytes, "UTF-8").split("\n");
			for (String s: strs) {
				if (s.startsWith("mp_username:")) {
					return s.substring("mp_username:".length());
				}
			}
		} catch (Exception ie) {
			ie.printStackTrace();
		}
		return "Steve"; // I DUNNO
	}

	private static void dumpScriptError(Throwable t) {
		if (scriptErrorStream == null) scriptErrorStream = new AndroidPrintStream(android.util.Log.ERROR, "ScriptError");
		t.printStackTrace(scriptErrorStream);
	}

	private static final class MyMethodWatcher implements NativeJavaMethod.MethodWatcher {
		private boolean testName(String name) {
			return name.equals("showAsDropDown") || name.equals("showAtLocation");
		}
		public boolean canCall(Method method, Object javaObject) {
			if (javaObject instanceof AccessibleObject && method.getName().equals("setAccessible")) {
				Class<?> cls = null;
				if (javaObject instanceof Member) cls = ((Member)javaObject).getDeclaringClass();
				if (cls == ScriptManager.class || cls == NativeJavaMethod.class ||
					cls == ContextFactory.class) return false;
			}
			if (ScriptManager.scriptingEnabled) return true;
			return !(javaObject instanceof PopupWindow && testName(method.getName()));
		}
	}

	// called from native AssetManager simulator
	// the path should already have the resourcepack/vanilla bit removed
	// paths returned don't have the resourcepack/vanilla bit
	private static boolean assetFileExists(String path) {
		MainActivity activity = MainActivity.currentMainActivity.get();
		if (activity == null) return false;
		boolean retval = activity.existsForPath(path);
		return retval;
	}
	private static final String assetsResPackPath = "resource_packs/vanilla";
	// path still has original res pack path;
	private static String[] assetListDir(String path) {
		//if (textureList == null) return null;
		//Set<String> texList = textureList.listDir(path.substring(assetsResPackPath.length()));
		//if (texList == null || texList.size() == 0) return null;
		MainActivity activity = MainActivity.currentMainActivity.get();
		if (activity == null) return null;
		/*String[] origDir = null;
		try {
			origDir = activity.getAssets().list(path);
		} catch (IOException ie) {
			origDir = new String[0];
		}
		// merge
		List<String> outList = new ArrayList<String>(texList);
		for (String s: origDir) {
			if (texList.contains(s)) continue;
			outList.add(s);
		}
		return outList.toArray(new String[0]);
		*/
		return activity.listDirForPath(path);
	}

	public static int nativeGetTileWrap(int x, int y, int z) {
		int tile = nativeGetTile(x, y, z);
		if (tile == 245) {
			int extraData = nativeLevelGetExtraData(x, y, z);
			if (extraData != 0) return extraData;
		}
		return tile;
	}

	private static final int[] useItemSideOffsets = {
		0, -1, 0,
		0, 1, 0,
		0, 0, -1,
		0, 0, 1,
		-1, 0, 0,
		1, 0, 0
	};

	public static void doUseItemOurselves(int x, int y, int z, int itemId, int blockId, int side, int itemDamage, int blockDamage) {
		// maybe implement in native instead?
		int blockX = x + useItemSideOffsets[side*3];
		int blockY = y + useItemSideOffsets[side*3 + 1];
		int blockZ = z + useItemSideOffsets[side*3 + 2];
		if (NativeLevelApi.getTile(blockX, blockY, blockZ) != 0) {
			return;
		}
		NativeLevelApi.setTile(blockX, blockY, blockZ, itemId, itemDamage);
	}

	public static void ensureEntityIsMob(long entityId) {
		int entityType = nativeGetEntityTypeId(entityId);
		if (!(entityType >= 10 && entityType < 64)) {
			throw new RuntimeException("entity is not a mob: entityType=" + entityType);
		}
	}

	private static void tryDeferLoads() {
		if (deferredLoads.size() == 0) return;
		if (!nativeIsValidItem(256)) {
			nativeRequestFrameCallback();
			return;
		}
		for (Object[] deferredRequest: deferredLoads) {
			try {
				File file = (File)deferredRequest[0];
				loadScript(file, (Boolean)deferredRequest[1]);
				// this is only hit when selectLevel anyways...
				callScriptMethodOneScript(file.getName(), "selectLevelHook");
			} catch (Exception e) {
				dumpScriptError(e);
				reportScriptError(null, e);
			}
		}
		deferredLoads.clear();
	}

	private static boolean needsToDeferScriptLoadForItem() {
		// only defer if scripting engine is initialized and first load has happened
		// but item has been removed.
		if (!scriptingInitialized)
			return false;
		if (requestReloadAllScripts) {
			return false;
		}
		if (!nativeIsValidItem(256)) {
			return true;
		}
		return false;
	}

	private static void setTargetSdkVersion(int version) {
		try {
			if (vmRuntime_setTargetSdkVersion == null) {
				Class vmRuntimeClass = Class.forName("dalvik.system.VMRuntime");
				vmRuntime_setTargetSdkVersion = nativeGrabMethod(vmRuntimeClass, "setTargetSdkVersion", "(I)V", false);
				//System.err.println("Method: " + vmRuntime_setTargetSdkVersion);
				vmRuntime_singleton = vmRuntimeClass.getMethod("getRuntime").invoke(null);
			}
			vmRuntime_setTargetSdkVersion.invoke(vmRuntime_singleton, version);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static native float nativeGetPlayerLoc(int axis);

	public static native long nativeGetPlayerEnt();

	public static native long nativeGetLevel();

	public static native void nativeSetPosition(long entityId, float x, float y, float z);

	public static native void nativeSetVel(long entityId, float vel, int axis);

	public static native void nativeExplode(float x, float y, float z, float radius, boolean onfire, boolean smoke, float override);

	public static native void nativeAddItemInventory(int id, int amount, int damage);

	public static native void nativeRideAnimal(long rider, long mount);

	public static native int nativeGetCarriedItem(int type);

	public static native void nativePreventDefault();

	public static native void nativeSetTile(int x, int y, int z, int id, int damage);

	public static native long nativeSpawnEntity(float x, float y, float z, int entityType,
			String skinPath);

	public static native void nativeClientMessage(String msg);

	public static native void nativeSetNightMode(boolean isNight);

	public static native int nativeGetTile(int x, int y, int z);

	public static native void nativeSetPositionRelative(long entity, float x, float y, float z);

	public static native void nativeSetRot(long ent, float yaw, float pitch);

	public static native float nativeGetYaw(long ent);

	public static native float nativeGetPitch(long ent);

	public static native void nativeSetCarriedItem(long ent, int id, int count, int damage);

	public static native void nativeOnGraphicsReset();

	public static native void nativeDefineItem(int itemId, String iconName, int iconId,
			String name, int maxStackSize);

	// nonstandard
	public static native void nativeSetFov(float degrees, boolean override);

	public static native void nativeSetMobSkin(long ent, String str);

	public static native float nativeGetEntityLoc(long entity, int axis);

	public static native void nativeRemoveEntity(long entityId);

	public static native int nativeGetEntityTypeId(long entityId);

	public static native void nativeSetAnimalAge(long entityId, int age);

	public static native int nativeGetAnimalAge(long entityId);

	public static native void nativeSelectLevel(String levelName, String levelRealName);

	public static native void nativeLeaveGame(boolean saveMultiplayerWorld);

	public static native void nativeJoinServer(String serverAddress, int serverPort);

	public static native void nativeSetGameSpeed(float ticksPerSecond);

	public static native void nativeGetAllEntities();

	public static native int nativeGetSelectedSlotId();

	public static native void nativeSetSelectedSlotId(int slot);

	public static native int nativeGetMobHealth(long entityId);

	public static native void nativeSetMobHealth(long entityId, int halfhearts);
	public static native void nativeSetMobMaxHealth(long entityId, int halfhearts);

	public static native boolean nativeSetEntityRenderType(long entityId, int renderType);

	public static native void nativeRequestFrameCallback();

	public static native String nativeGetSignText(int x, int y, int z);

	public static native void nativeSetSignText(int x, int y, int z, String text);

	public static native void nativeSetSneaking(long entityId, boolean doIt);
	public static native boolean nativeIsSneaking(long entityId);

	public static native String nativeGetPlayerName(long entityId);

	public static native String nativeGetItemName(int itemId, int itemDamage, boolean raw);

	public static native boolean nativeGetTextureCoordinatesForItem(int itemId, int itemDamage,
			float[] output);
	public static native boolean nativeGetTextureCoordinatesForBlock(int itemId, int itemDamage, int side,
			float[] output);

	public static native void nativeDefineBlock(int blockId, String name, String[] textureNames,
			int[] textureCoords, int materialSourceId, boolean opaque, int renderType,
			int customBlockType);

	public static native void nativeBlockSetDestroyTime(int blockId, float amount);

	public static native void nativeBlockSetExplosionResistance(int blockId, float amount);

	public static native void nativeBlockSetStepSound(int blockId, int sourceBlockId);

	public static native void nativeBlockSetLightLevel(int blockId, int level);
	public static native void nativeBlockSetLightOpacity(int blockId, int level);

	public static native void nativeBlockSetColor(int blockId, int[] colors);

	public static native void nativeBlockSetShape(int blockId, float v1, float v2, float v3,
			float v4, float v5, float v6, int damage);

	public static native void nativeBlockSetRenderLayer(int blockId, int renderLayer);

	public static native int nativeBlockGetRenderLayer(int blockId);

	public static native void nativeSetInventorySlot(int slot, int id, int count, int damage);

	public static native float nativeGetEntityVel(long entity, int axis);

	public static native void nativeSetI18NString(String key, String value);

	public static native void nativeAddShapedRecipe(int id, int count, int damage, String[] shape,
			int[] ingredients);

	public static native void nativeAddShapelessRecipe(int id, int count, int damage, int[] ingredients);

	public static native void nativeShowTipMessage(String msg);

	public static native void nativeEntitySetNameTag(long id, String msg);

	public static native void nativeSetStonecutterItem(int id, int status);

	public static native void nativeSetItemCategory(int id, int category, int status);

	public static native void nativeSendChat(String message);

	public static native String nativeEntityGetNameTag(long entityId);

	public static native int nativeEntityGetRiding(long entityId);

	public static native int nativeEntityGetRider(long entityId);

	public static native String nativeEntityGetMobSkin(long entityId);

	public static native int nativeEntityGetRenderType(long entityId);
	public static native void nativeSetCameraEntity(long entityId);
	public static native long[] nativeEntityGetUUID(long entityId);
	public static native void nativeLevelAddParticle(String type, float x, float y, float z, float xVel, float yVel, float zVel, int data);

	// MrARM's additions
	public static native int nativeGetData(int x, int y, int z);

	public static native void nativeHurtTo(int to);

	public static native void nativeDestroyBlock(int x, int y, int z);

	public static native long nativeGetTime();

	public static native void nativeSetTime(long time);

	public static native int nativeGetGameType();

	public static native void nativeSetGameType(int type);

	public static native void nativeSetOnFire(long entity, int howLong);

	public static native void nativeSetSpawn(int x, int y, int z);

	public static native void nativeAddItemChest(int x, int y, int z, int slot, int id, int damage,
			int amount);

	public static native int nativeGetItemChest(int x, int y, int z, int slot);

	public static native int nativeGetItemDataChest(int x, int y, int z, int slot);

	public static native int nativeGetItemCountChest(int x, int y, int z, int slot);
	public static native String nativeGetItemNameChest(int x, int y, int z, int slot);
	public static native void nativeSetItemNameChest(int x, int y, int z, int slot, String name);

	public static native long nativeDropItem(float x, float y, float z, float range, int id,
			int count, int damage);

	// KsyMC's additions
	public static native void nativePlaySound(float x, float y, float z, String sound,
			float volume, float pitch);

	public static native void nativeClearSlotInventory(int slot);

	public static native int nativeGetSlotInventory(int slot, int type);

	public static native void nativeAddItemCreativeInv(int id, int count, int damage);

	// InusualZ's additions
	public static native void nativeExtinguishFire(int x, int y, int z, int side);

	public static native int nativeMobGetArmor(long entityId, int slot, int type);

	public static native void nativeMobSetArmor(long entityId, int slot, int id, int damage);

	// Byteandahalf's additions
	public static native int nativeGetBrightness(int x, int y, int z);

	public static native void nativeAddFurnaceRecipe(int inputId, int outputId, int outputDamage);

	public static native void nativeAddItemFurnace(int x, int y, int z, int slot, int id,
			int damage, int amount);

	public static native int nativeGetItemFurnace(int x, int y, int z, int slot);

	public static native int nativeGetItemDataFurnace(int x, int y, int z, int slot);

	public static native int nativeGetItemCountFurnace(int x, int y, int z, int slot);

 	public static native void nativeSetItemMaxDamage(int id, int maxDamage);
	
	public static native int nativeGetBlockRenderShape(int blockId);
	
	public static native void nativeSetBlockRenderShape(int blockId, int renderType);

	public static native boolean nativeIsBlockTextureAtlasLoaded();
	public static native void nativeDefinePlaceholderBlocks();

	public static native long nativePlayerGetPointedEntity();

	public static native int nativePlayerGetPointedBlock(int type);
	public static native float nativePlayerGetPointedVec(int axis);

	public static native int nativeLevelGetBiome(int x, int z);
	public static native String nativeLevelGetBiomeName(int x, int z);
	public static native String nativeBiomeIdToName(int id);
	public static native void nativeLevelSetBiome(int x, int z, int id);

	public static native int nativeLevelGetGrassColor(int x, int z);
	public static native void nativeLevelSetGrassColor(int x, int z, int val);

	public static native boolean nativePlayerIsFlying();
	public static native void nativePlayerSetFlying(boolean val);

	public static native boolean nativePlayerCanFly();
	public static native void nativePlayerSetCanFly(boolean val);
	public static native void nativeBlockSetCollisionEnabled(int id, boolean enable);
	public static native void nativeEntitySetSize(long entity, float a, float b);
	public static native void nativeSetCape(long ent, String str);
	public static native void nativeClearCapes();
	public static native void nativeSetHandEquipped(int id, boolean handEquipped);
	public static native void nativeSpawnerSetEntityType(int x, int y, int z, int type);
	public static native int nativeSpawnerGetEntityType(int x, int y, int z);
	public static native void nativeDefineArmor(int id, String iconName, int iconIndex, String name,
			String texture, int damageReduceAmount, int maxDamage, int armorType);
	public static native void nativeScreenChooserSetScreen(int id);
	public static native void nativeCloseScreen();
	public static native void nativeShowProgressScreen();
	public static native void nativeMobAddEffect(long entity, int id, int duration, int amplifier,
		boolean ambient, boolean showParticles);
	public static native void nativeMobRemoveEffect(long entity, int id);
	public static native void nativeMobRemoveAllEffects(long entity);
	public static native int nativeGetItemEntityItem(long entity, int type);
	public static native boolean nativeIsValidItem(int id);
	public static native boolean nativeIsValidCommand(String name);
	public static native int nativeBlockGetSecondPart(int x, int y, int z, int axis);
	public static native int nativePlayerGetDimension();

	public static native float nativeLevelGetLightningLevel();
	public static native void nativeLevelSetLightningLevel(float a);
	public static native float nativeLevelGetRainLevel();
	public static native void nativeLevelSetRainLevel(float a);

	public static native float nativePlayerGetHunger(long entity);
	public static native void nativePlayerSetHunger(long entity, float value);
	public static native float nativePlayerGetExhaustion();
	public static native void nativePlayerSetExhaustion(float value);
	public static native float nativePlayerGetSaturation();
	public static native void nativePlayerSetSaturation(float value);

	public static native float nativePlayerGetExperience();
	public static native void nativePlayerSetExperience(float value);
	public static native int nativePlayerGetLevel();
	public static native void nativePlayerSetLevel(int value);
	public static native void nativePlayerAddExperience(int value);

	public static native int nativeGetMobMaxHealth(long entity);
	public static native float nativeBlockGetDestroyTime(int id, int data);
	public static native float nativeBlockGetFriction(int id);
	public static native void nativeBlockSetFriction(int id, float friction);
	public static native void nativeBlockSetRedstoneConsumer(int id, boolean yep);
	public static native boolean nativeLevelCanSeeSky(int x, int y, int z);
	public static native boolean nativeItemSetProperties(int id, String json);
	public static native String nativeGetI18NString(String key);
	public static native String nativeGetLanguageName();
	public static native int nativeItemGetUseAnimation(int id);
	public static native void nativeItemSetUseAnimation(int id, int anim);
	public static native void nativeItemSetStackedByData(int id, boolean yep);
	public static native boolean nativePlayerEnchant(int slot, int enchantment, int level);
	public static native int[] nativePlayerGetEnchantments(int slot);
	public static native String nativePlayerGetItemCustomName(int slot);
	public static native void nativePlayerSetItemCustomName(int slot, String name);
	public static native void nativeSetAllowEnchantments(int id, int flag, int value);
	public static native int nativeLevelGetDifficulty();
	public static native void nativeLevelSetDifficulty(int difficulty);
	public static native void nativeArmorAddQueuedTextures();
	public static native boolean nativeEntityHasCustomSkin(long entity);
	public static native void nativeEntitySetImmobile(long id, boolean immobile);
	public static native void nativeModPESetRenderDebug(boolean debug);
	public static native long nativeEntityGetTarget(long id);
	public static native void nativeEntitySetTarget(long id, long target);
	public static native int nativePlayerGetScore();
	public static native String nativeMobGetArmorCustomName(long entity, int slot);
	public static native void nativeMobSetArmorCustomName(long entity, int slot, String name);
	public static native int nativeGetItemMaxDamage(int id);
	// not added to JS yet
	public static native int nativeEntityGetCarriedItem(long id, int type);
	public static native int nativeItemGetMaxStackSize(int id);
	public static native void nativeDefineSnowballItem(int itemId, String iconName, int iconId,
			String name, int maxStackSize);
	public static native void nativeLevelSetExtraData(int x, int y, int z, int data);
	public static native int nativeLevelGetExtraData(int x, int y, int z);
	public static native boolean nativeItemIsExtendedBlock(int itemId);
	public static native String nativeLevelExecuteCommand(String cmd, boolean silent);
	public static native long[] nativeServerGetPlayers();
	public static native int nativeEntityGetOffhandSlot(long id, int type);
	public static native void nativeEntitySetOffhandSlot(long ent, int id, int count, int damage);

	// setup
	public static native void nativeSetupHooks(int versionCode);

	public static native void nativeRemoveItemBackground();

	public static native void nativeSetTextParseColorCodes(boolean doIt);

	public static native void nativePrePatch(boolean signalhandler, MainActivity activity, boolean limited, int versionCode);

	public static native boolean nativeLevelIsRemote();

	public static native void nativeSetIsRecording(boolean yep);

	public static native void nativeForceCrash();
	public static native int nativeGetArch();
	public static native void nativeSetUseController(boolean controller);
	public static native void nativeDumpVtable(String name, int size);
	public static native int nativeGetItemIdCount();
	public static native void nativeSetExitEnabled(boolean enabled);
	public static native void nativeRecipeSetAnyAuxValue(int id, boolean anyAux);
	public static native void nativeModPESetDesktopGui(boolean desktop);
	public static native void nativeNewLevelCallbackStarted();
	public static native void nativeNewLevelCallbackEnded();
	public static native boolean nativeHasPreventedDefault();
	public static native void nativeItemSetAllowOffhand(int id, boolean yep);
	public static native Method nativeGrabMethod(Class javaClass, String methodName, String methodSignature, boolean isStatic);

	public static class ScriptState {
		public Script script;
		public Scriptable scope;
		public String name;
		public int errors = 0;

		protected ScriptState(Script script, Scriptable scope, String name) {
			this.script = script;
			this.scope = scope;
			this.name = name;
		}
	}

	// To contributors: if you are adding a BlockLauncher-specific method,
	// please add it to one of the namespaces (Entity, Level, ModPE, Player)
	// instead of the top-level namespace. thanks.
	// e.g. Entity.fireLaz0rs = good, fireLaz0rs = bad, bl_fireLaz0rs = bad

	private static class BlockHostObject extends ImporterTopLevel {
		private long playerEnt = 0;

		@Override
		public String getClassName() {
			return "BlockHostObject";
		}

		@JSFunction
		public void print(String str) {
			scriptPrint(str);
		}

		@JSFunction
		public double getPlayerX() {
			return nativeGetPlayerLoc(AXIS_X);
		}

		@JSFunction
		public double getPlayerY() {
			return nativeGetPlayerLoc(AXIS_Y);
		}

		@JSFunction
		public double getPlayerZ() {
			return nativeGetPlayerLoc(AXIS_Z);
		}

		@JSFunction
		public long getPlayerEnt() {
			playerEnt = nativeGetPlayerEnt();
			return playerEnt;
		}

		@JSFunction
		public NativePointer getLevel() {
			return new NativePointer(nativeGetLevel());
			// TODO: WTF does this do?
		}

		@JSFunction
		public void setPosition(Object ent, double x, double y, double z) {
			nativeSetPosition(getEntityId(ent), (float) x, (float) y, (float) z);
		}

		@JSFunction
		public void setVelX(Object ent, double amount) {
			nativeSetVel(getEntityId(ent), (float) amount, AXIS_X);
		}

		@JSFunction
		public void setVelY(Object ent, double amount) {
			nativeSetVel(getEntityId(ent), (float) amount, AXIS_Y);
		}

		@JSFunction
		public void setVelZ(Object ent, double amount) {
			nativeSetVel(getEntityId(ent), (float) amount, AXIS_Z);
		}

		@JSFunction
		public void explode(double x, double y, double z, double radius, boolean onfire) {
			nativeExplode((float) x, (float) y, (float) z, (float) radius, onfire, true, Float.POSITIVE_INFINITY);
		}

		@JSFunction
		public void addItemInventory(int id, int amount, int damage) {
			if (!nativeIsValidItem(id)) throw new RuntimeException("invalid item id " + id);
			nativeAddItemInventory(id, amount, damage);
		}

		@JSFunction
		public void rideAnimal(Object /* Flynn */rider, Object mount) {
			nativeRideAnimal(getEntityId(rider), getEntityId(mount));
		}

		@JSFunction
		public long spawnChicken(double x, double y, double z, String tex) {
			if (invalidTexName(tex)) {
				tex = "mob/chicken.png";
			}
			long entityId = spawnEntityImpl((float) x, (float) y, (float) z, 10, tex);
			return entityId;
		}

		@JSFunction
		public long spawnCow(double x, double y, double z, String tex) {
			if (invalidTexName(tex)) {
				tex = "mob/cow.png";
			}
			long entityId = spawnEntityImpl((float) x, (float) y, (float) z, 11, tex);
			return entityId;
		}

		@JSFunction
		public int getCarriedItem() {
			return nativeGetCarriedItem(ITEMID);
		}

		@JSFunction
		public void preventDefault() {
			nativePreventDefault();
		}

		@JSFunction
		public void setTile(int x, int y, int z, int id, int damage) {
			NativeLevelApi.setTile(x, y, z, id, damage);
		}

		@JSFunction
		public void clientMessage(String text) {
			wordWrapClientMessage(text);
		}

		@JSFunction
		public void setNightMode(boolean isNight) {
			nativeSetNightMode(isNight);
		}

		@JSFunction
		public int getTile(int x, int y, int z) {
			return nativeGetTileWrap(x, y, z);
		}

		@JSFunction
		public void setPositionRelative(Object ent, double x, double y, double z) {
			nativeSetPositionRelative(getEntityId(ent), (float) x, (float) y, (float) z);
		}

		@JSFunction
		public void setRot(Object ent, double yaw, double pitch) {
			nativeSetRot(getEntityId(ent), (float) yaw, (float) pitch);
		}

		@JSFunction
		public double getPitch(Object entObj) {
			long ent;
			if (entObj == null || !(entObj instanceof Number)) {
				ent = getPlayerEnt();
			} else {
				ent = ((Number) entObj).longValue();
			}
			return nativeGetPitch(ent);
		}

		@JSFunction
		public double getYaw(Object entObj) {
			long ent;
			if (entObj == null || !(entObj instanceof Number)) {
				ent = getPlayerEnt();
			} else {
				ent = ((Number) entObj).longValue();
			}
			return nativeGetYaw(ent);
		}

		@JSFunction
		public long spawnPigZombie(double x, double y, double z, int item, String tex) {
			if (invalidTexName(tex)) {
				tex = null;
			}
			long entityId = spawnEntityImpl((float) x, (float) y, (float) z, 36, tex);
			if (item == 0 || !nativeIsValidItem(item)) item = 283; // gold sword
			nativeSetCarriedItem(entityId, item, 1, 0);
			return entityId;
		}

		// nonstandard methods

		@JSFunction
		public long bl_spawnMob(double x, double y, double z, int typeId, String tex) {
			if (invalidTexName(tex)) {
				tex = null;
			}
			long entityId = spawnEntityImpl((float) x, (float) y, (float) z, typeId, tex);
			return entityId;
		}

		@JSFunction
		public void bl_setMobSkin(Object entityId, String tex) {
			NativeEntityApi.setMobSkin(getEntityId(entityId), tex);
		}

	}

	private static class NativePointer extends ScriptableObject {
		public long value;

		public NativePointer(long value) {
			this.value = value;
		}

		@Override
		public String getClassName() {
			return "NativePointer";
		}
	}

	private static class NativeLevelApi extends ScriptableObject {
		public NativeLevelApi() {
			super();
		}

		@JSStaticFunction
		public static void setNightMode(boolean isNight) {
			nativeSetNightMode(isNight);
		}

		@JSStaticFunction
		public static int getTile(int x, int y, int z) {
			return nativeGetTileWrap(x, y, z);
		}

		@JSStaticFunction
		public static void explode(double x, double y, double z, double radius, boolean onfire, boolean smoke, double somethingelse) {
			nativeExplode((float) x, (float) y, (float) z, (float) radius, onfire, smoke, (float)somethingelse);
		}

		@JSStaticFunction
		public static void setTile(int x, int y, int z, int id, int damage) {
			if (id >= 0x100) {
				// extended block ID.
				nativeSetTile(x, y, z, 0, 0);
				nativeSetTile(x, y, z, 245, damage);
				nativeLevelSetExtraData(x, y, z, id);
				return;
			}
			nativeSetTile(x, y, z, id, damage);
		}

		@JSStaticFunction
		public static NativePointer getAddress() {
			// TODO: I still don't know WTF this does.
			return new NativePointer(nativeGetLevel());
		}

		@JSStaticFunction
		public static long spawnChicken(double x, double y, double z, String tex) {
			if (invalidTexName(tex)) {
				tex = null;
			}
			long entityId = spawnEntityImpl((float) x, (float) y, (float) z, 10, tex);
			return entityId;
		}

		@JSStaticFunction
		public static long spawnCow(double x, double y, double z, String tex) {
			if (invalidTexName(tex)) {
				tex = null;
			}
			long entityId = spawnEntityImpl((float) x, (float) y, (float) z, 11, tex);
			return entityId;
		}

		// nonstandard methods

		@JSStaticFunction
		public static long spawnMob(double x, double y, double z, int typeId, String tex) {
			if (invalidTexName(tex)) {
				tex = null;
			}
			long entityId = spawnEntityImpl((float) x, (float) y, (float) z, typeId, tex);
			return entityId;
		}

		@JSStaticFunction
		public static String getSignText(int x, int y, int z, int line) {
			if (line < 0 || line >= 4)
				throw new RuntimeException("Invalid line for sign: must be in the range of 0 to 3");
			String text = nativeGetSignText(x, y, z);
			if (text == null) return "";
			String[] textArr = text.split("\n");
			if (line >= textArr.length) return "";
			return textArr[line];
		}

		@JSStaticFunction
		public static void setSignText(int x, int y, int z, int line, String newText) {
			if (line < 0 || line >= 4)
				throw new RuntimeException("Invalid line for sign: must be in the range of 0 to 3");
			String text = nativeGetSignText(x, y, z);
			String newTextJoined = null;
			if (text == null) {
				StringBuilder out = new StringBuilder();
				for (int i = 0; i < line; i++) {
					out.append("\n");
				}
				newTextJoined = out.append(newText).toString();
			} else {
				String[] outArr = text.split("\n");
				if (line >= outArr.length) {
					String[] newArr = new String[line + 1];
					for (int i = 0; i < outArr.length; i++) {
						newArr[i] = outArr[i];
					}
					outArr = newArr;
				}
				outArr[line] = newText;
				newTextJoined = Utils.joinArray(outArr, "\n");
			}
			nativeSetSignText(x, y, z, newTextJoined);
		}

		// thanks to MrARM

		@JSStaticFunction
		public static int getData(int x, int y, int z) {
			return nativeGetData(x, y, z);
		}

		@JSStaticFunction
		public static String getWorldName() {
			return worldName;
		}

		@JSStaticFunction
		public static String getWorldDir() {
			return worldDir;
		}

		@JSStaticFunction
		public static long dropItem(double x, double y, double z, double range, int id, int count,
				int damage) {
			if (!nativeIsValidItem(id)) throw new RuntimeException("invalid item id " + id);
			return nativeDropItem((float) x, (float) y, (float) z, (float) range, id, count, damage);
		}

		@JSStaticFunction
		public static void setGameMode(int type) {
			if (!scriptingEnabled) return;
			nativeSetGameType(type);
		}

		@JSStaticFunction
		public static int getGameMode() {
			return nativeGetGameType();
		}

		@JSStaticFunction
		public static int getTime() {
			return (int) nativeGetTime();
		}

		@JSStaticFunction
		public static void setTime(int time) {
			nativeSetTime((long) time);
		}

		@JSStaticFunction
		public static void setSpawn(int x, int y, int z) {
			nativeSetSpawn(x, y, z);
		}

		@JSStaticFunction
		public static void destroyBlock(int x, int y, int z, boolean shouldDrop) {
			if (!scriptingEnabled) return;
			int itmId = getTile(x, y, z);
			int itmDmg = getData(x, y, z);

			nativeDestroyBlock(x, y, z);
			if (shouldDrop)
				dropItem(((double) x) + 0.5, y, ((double) z) + 0.5, 1, itmId, 1, itmDmg);
		}

		@JSStaticFunction
		public static void setChestSlot(int x, int y, int z, int slot, int id, int damage,
				int amount) {
			if (!nativeIsValidItem(id)) throw new RuntimeException("invalid item id " + id);
			nativeAddItemChest(x, y, z, slot, id, damage, amount);
		}

		@JSStaticFunction
		public static void setChestSlotCustomName(int x, int y, int z, int slot, String name) {
			nativeSetItemNameChest(x, y, z, slot, name);
		}

		@JSStaticFunction
		public static int getChestSlot(int x, int y, int z, int slot) {
			return nativeGetItemChest(x, y, z, slot);
		}

		@JSStaticFunction
		public static int getChestSlotData(int x, int y, int z, int slot) {
			return nativeGetItemDataChest(x, y, z, slot);
		}

		@JSStaticFunction
		public static int getChestSlotCount(int x, int y, int z, int slot) {
			return nativeGetItemCountChest(x, y, z, slot);
		}

		@JSStaticFunction
		public static String getChestSlotCustomName(int x, int y, int z, int slot) {
			return nativeGetItemNameChest(x, y, z, slot);
		}

		// KsyMC's additions
		@JSStaticFunction
		public static void playSound(double x, double y, double z, String sound, double volume,
				double pitch) {
			playSoundImpl((float) x, (float) y, (float) z, sound,
				(volume <= 0 || volume != volume)? 1.0f: (float) volume, (pitch <= 0 || pitch != pitch)? 1.0f: (float) pitch);
		}

		@JSStaticFunction
		public static void playSoundEnt(Object ent, String sound, double volume, double pitch) {
			float x = nativeGetEntityLoc(getEntityId(ent), AXIS_X);
			float y = nativeGetEntityLoc(getEntityId(ent), AXIS_Y);
			float z = nativeGetEntityLoc(getEntityId(ent), AXIS_Z);

			playSoundImpl(x, y, z, sound,
				(volume <= 0 || volume != volume)? 1.0f: (float) volume, (pitch <= 0 || pitch != pitch)? 1.0f: (float) pitch);
		}

		private static void playSoundImpl(double x, double y, double z, String sound, float volume, float pitch) {
			String cmd = new StringBuilder().append("/playsound ").append(sound).append(' ').
				append("@a ").append(Math.round(x)).append(' ').append(Math.round(y)).append(' ').
				append(Math.round(z)).append(' ').append(volume).append(' ').append(pitch).toString();
			String retval = executeCommand(cmd, true);
			//if (retval != null) System.err.println("Failed to play sound " + sound + ": " + retval);
		}

		// Byteandahalf's additions
		@JSStaticFunction
		public static int getBrightness(int x, int y, int z) {
			return nativeGetBrightness(x, y, z);
		}

		@JSStaticFunction
		public static void setFurnaceSlot(int x, int y, int z, int slot, int id, int damage,
				int amount) {
			if (!nativeIsValidItem(id)) throw new RuntimeException("invalid item id " + id);
			nativeAddItemFurnace(x, y, z, slot, id, damage, amount);
		}

		@JSStaticFunction
		public static int getFurnaceSlot(int x, int y, int z, int slot) {
			return nativeGetItemFurnace(x, y, z, slot);
		}

		@JSStaticFunction
		public static int getFurnaceSlotData(int x, int y, int z, int slot) {
			return nativeGetItemDataFurnace(x, y, z, slot);
		}

		@JSStaticFunction
		public static int getFurnaceSlotCount(int x, int y, int z, int slot) {
			return nativeGetItemCountFurnace(x, y, z, slot);
		}

		@JSStaticFunction
		public static void addParticle(Object typeRaw, double x, double y, double z, double xVel, double yVel, double zVel, int size) {
			String type = ParticleType.getTypeFromRaw(typeRaw);
			if (!ParticleType.checkValid(type, size)) return;
			nativeLevelAddParticle(type, (float) x, (float) y, (float) z, (float) xVel, (float) yVel, (float) zVel, size);
		}

		@JSStaticFunction
		public static int getBiome(int x, int z) {
			return nativeLevelGetBiome(x, z);
		}

		@JSStaticFunction
		public static String getBiomeName(int x, int z) {
			return nativeLevelGetBiomeName(x, z);
		}

		@JSStaticFunction
		public static String biomeIdToName(int id) {
			return nativeBiomeIdToName(id);
		}

		/*@JSStaticFunction
		public static void setBiome(int x, int z, int id) {
			nativeLevelSetBiome(x, z, id);
		}*/

		@JSStaticFunction
		public static int getGrassColor(int x, int z) {
			return nativeLevelGetGrassColor(x, z);
		}

		@JSStaticFunction
		public static void setGrassColor(int x, int z, int color) {
			nativeLevelSetGrassColor(x, z, color);
		}

		@JSStaticFunction
		public static void setSpawnerEntityType(int x, int y, int z, int type) {
			if (getTile(x, y, z) != 52) {
				throw new RuntimeException("Block at " + x + ":" + y + ":" + z + " is not a mob spawner!");
			}
			nativeSpawnerSetEntityType(x, y, z, type);
		}

		@JSStaticFunction
		public static int getSpawnerEntityType(int x, int y, int z) {
			if (getTile(x, y, z) != 52) {
				throw new RuntimeException("Block at " + x + ":" + y + ":" + z + " is not a mob spawner!");
			}
			return nativeSpawnerGetEntityType(x, y, z);
		}

		@JSStaticFunction
		public static double getLightningLevel() {
			return nativeLevelGetLightningLevel();
		}

		@JSStaticFunction
		public static void setLightningLevel(double val) {
			nativeLevelSetLightningLevel((float) val);
		}

		@JSStaticFunction
		public static double getRainLevel() {
			return nativeLevelGetRainLevel();
		}

		@JSStaticFunction
		public static void setRainLevel(double val) {
			nativeLevelSetRainLevel((float) val);
		}

		@JSStaticFunction
		public static boolean canSeeSky(int x, int y, int z) {
			return nativeLevelCanSeeSky(x, y, z);
		}

		@JSStaticFunction
		public static int getDifficulty() {
			return nativeLevelGetDifficulty();
		}

		@JSStaticFunction
		public static void setDifficulty(int difficulty) {
			nativeLevelSetDifficulty(difficulty);
		}

		@JSStaticFunction
		public static void setBlockExtraData(int x, int y, int z, int data) {
			nativeLevelSetExtraData(x, y, z, data);
		}

		@JSStaticFunction
		public static String executeCommand(final String command, final boolean silent) {
			// fixme 1.2
			runOnMainThread(new Runnable() {
				public void run() {
					nativeLevelExecuteCommand(command, silent);
				}
			});
/*
			String result = nativeLevelExecuteCommand(command, silent);
			if ("<no result>".equals(result)) return "";
			return result;
*/
			return "";
		}

		@JSStaticFunction
		public static boolean isRemote() {
			return nativeLevelIsRemote();
		}

		@Override
		public String getClassName() {
			return "Level";
		}
	}

	private static class NativePlayerApi extends ScriptableObject {
		private static long playerEnt = 0;

		public NativePlayerApi() {
			super();
		}

		@JSStaticFunction
		public static double getX() {
			return nativeGetPlayerLoc(AXIS_X);
		}

		@JSStaticFunction
		public static double getY() {
			return nativeGetPlayerLoc(AXIS_Y);
		}

		@JSStaticFunction
		public static double getZ() {
			return nativeGetPlayerLoc(AXIS_Z);
		}

		@JSStaticFunction
		public static long getEntity() {
			playerEnt = nativeGetPlayerEnt();
			return playerEnt;
		}

		@JSStaticFunction
		public static int getCarriedItem() {
			return nativeGetCarriedItem(ITEMID);
		}

		@JSStaticFunction
		public static void addItemInventory(int id, int amount, int damage) {
			if (!nativeIsValidItem(id)) throw new RuntimeException("invalid item id " + id);
			nativeAddItemInventory(id, amount, damage);
		}

		// nonstandard
		@JSStaticFunction
		public static void setHealth(int value) {
			//nativeHurtTo(value);
			nativeSetMobHealth(nativeGetPlayerEnt(), value);
		}

		@JSStaticFunction
		public static int getSelectedSlotId() {
			return nativeGetSelectedSlotId();
		}
		@JSStaticFunction
		public static void setSelectedSlotId(int slot) {
			nativeSetSelectedSlotId(slot);
		}

		// KsyMC's additions
		@JSStaticFunction
		public static void clearInventorySlot(int slot) {
			nativeClearSlotInventory(slot);
		}

		@JSStaticFunction
		public static int getInventorySlot(int slot) {
			return nativeGetSlotInventory(slot, ITEMID);
		}

		@JSStaticFunction
		public static int getInventorySlotData(int slot) {
			return nativeGetSlotInventory(slot, DAMAGE);
		}

		@JSStaticFunction
		public static int getInventorySlotCount(int slot) {
			return nativeGetSlotInventory(slot, AMOUNT);
		}

		@JSStaticFunction
		public static int getCarriedItemData() {
			return nativeGetCarriedItem(DAMAGE);
		}

		@JSStaticFunction
		public static int getCarriedItemCount() {
			return nativeGetCarriedItem(AMOUNT);
		}

		@JSStaticFunction
		public static void addItemCreativeInv(int id, int count, int damage) {
			if (!nativeIsValidItem(id)) {
				throw new RuntimeException("You must make an item with id " + id +
					" before you can add it to the creative inventory.");
			}
			nativeAddItemCreativeInv(id, count, damage);
		}

		// InusualZ's additions

		@JSStaticFunction
		public static int getArmorSlot(int slot) {
			return NativeEntityApi.getArmor(getEntity(), slot);
		}

		@JSStaticFunction
		public static int getArmorSlotDamage(int slot) {
			return NativeEntityApi.getArmorDamage(getEntity(), slot);
		}

		@JSStaticFunction
		public static void setArmorSlot(int slot, int id, int damage) {
			if (!nativeIsValidItem(id)) throw new RuntimeException("invalid item id " + id);
			NativeEntityApi.setArmor(getEntity(), slot, id, damage);
		}

		@JSStaticFunction
		public static String getName(Object ent) {
			if (!isPlayer(ent)) {
				if (ent == null || getEntityId(ent) == getEntity()) {
					return getPlayerNameFromConfs();
				}
				return "Not a player";
			}
			return nativeGetPlayerName(getEntityId(ent));
		}

		@JSStaticFunction
		public static boolean isPlayer(Object ent) {
			return NativeEntityApi.getEntityTypeId(getEntityId(ent)) == EntityType.PLAYER;
		}

		@JSStaticFunction
		public static long getPointedEntity() {
			return nativePlayerGetPointedEntity();
		}

		@JSStaticFunction
		public static int getPointedBlockX() {
			return nativePlayerGetPointedBlock(AXIS_X);
		}

		@JSStaticFunction
		public static int getPointedBlockY() {
			return nativePlayerGetPointedBlock(AXIS_Y);
		}

		@JSStaticFunction
		public static int getPointedBlockZ() {
			return nativePlayerGetPointedBlock(AXIS_Z);
		}

		@JSStaticFunction
		public static int getPointedBlockId() {
			return nativePlayerGetPointedBlock(0x10 + 0);
		}

		@JSStaticFunction
		public static int getPointedBlockData() {
			return nativePlayerGetPointedBlock(0x10 + 1);
		}

		@JSStaticFunction
		public static int getPointedBlockSide() {
			return nativePlayerGetPointedBlock(0x10 + 2);
		}

		@JSStaticFunction
		public static double getPointedVecX() {
			return nativePlayerGetPointedVec(AXIS_X);
		}

		@JSStaticFunction
		public static double getPointedVecY() {
			return nativePlayerGetPointedVec(AXIS_Y);
		}

		@JSStaticFunction
		public static double getPointedVecZ() {
			return nativePlayerGetPointedVec(AXIS_Z);
		}

		@JSStaticFunction
		public static void setInventorySlot(int slot, int itemId, int count, int damage) {
			nativeSetInventorySlot(slot, itemId, count, damage);
		}

		@JSStaticFunction
		public static boolean isFlying() {
			return nativePlayerIsFlying();
		}

		@JSStaticFunction
		public static void setFlying(boolean val) {
			nativePlayerSetFlying(val);
		}

		@JSStaticFunction
		public static boolean canFly() {
			return nativePlayerCanFly();
		}

		@JSStaticFunction
		public static void setCanFly(boolean val) {
			nativePlayerSetCanFly(val);
		}

		@JSStaticFunction
		public static int getDimension() {
			return nativePlayerGetDimension();
		}

		@JSStaticFunction
		public static double getHunger() {
			return nativePlayerGetHunger(getEntity());
		}

		@JSStaticFunction
		public static void setHunger(double value) {
			nativePlayerSetHunger(getEntity(), (float) value);
		}

		@JSStaticFunction
		public static double getExhaustion() {
			return nativePlayerGetExhaustion();
		}

		@JSStaticFunction
		public static void setExhaustion(double value) {
			nativePlayerSetExhaustion((float) value);
		}

		@JSStaticFunction
		public static double getSaturation() {
			return nativePlayerGetSaturation();
		}

		@JSStaticFunction
		public static void setSaturation(double value) {
			nativePlayerSetSaturation((float) value);
		}

		@JSStaticFunction
		public static int getLevel() {
			return nativePlayerGetLevel();
		}

		@JSStaticFunction
		public static void setLevel(int value) {
			nativePlayerSetLevel(value);
		}

		@JSStaticFunction
		public static void addExp(int value) {
			nativePlayerAddExperience(value);
		}

		@JSStaticFunction
		public static double getExp() {
			return nativePlayerGetExperience();
		}

		@JSStaticFunction
		public static void setExp(double value) {
			nativePlayerSetExperience((float) value);
		}

		@JSStaticFunction
		public static boolean enchant(int slot, int enchantment, int level) {
			if (enchantment < Enchantment.PROTECTION || enchantment > Enchantment.LURE) {
				throw new RuntimeException("Invalid enchantment: " + enchantment);
			}
			return nativePlayerEnchant(slot, enchantment, level);
		}

		@JSStaticFunction
		public static EnchantmentInstance[] getEnchantments(int slot) {
			int[] ret = nativePlayerGetEnchantments(slot);
			if (ret == null) return null;
			EnchantmentInstance[] en = new EnchantmentInstance[ret.length / 2];
			for (int i = 0; i < en.length; i++) {
				en[i] = new EnchantmentInstance(ret[i*2], ret[i*2 + 1]);
			}
			return en;
		}

		@JSStaticFunction
		public static String getItemCustomName(int slot) {
			return nativePlayerGetItemCustomName(slot);
		}

		@JSStaticFunction
		public static void setItemCustomName(int slot, String name) {
			nativePlayerSetItemCustomName(slot, name);
		}

		@JSStaticFunction
		public static int getScore() {
			return nativePlayerGetScore();
		}

		@Override
		public String getClassName() {
			return "Player";
		}
	}

	private static class EnchantmentInstance {
		public final int type;
		public final int level;
		public EnchantmentInstance(int type, int level) {
			this.type = type;
			this.level = level;
		}
		@Override
		public String toString() {
			return "EnchantmentInstance[type=" + type + ",level=" + level + "]";
		}
	}

	private static class NativeEntityApi extends ScriptableObject {
		public NativeEntityApi() {
			super();
		}

		@JSStaticFunction
		public static void setVelX(Object ent, double amount) {
			nativeSetVel(getEntityId(ent), (float) amount, AXIS_X);
		}

		@JSStaticFunction
		public static void setVelY(Object ent, double amount) {
			nativeSetVel(getEntityId(ent), (float) amount, AXIS_Y);
		}

		@JSStaticFunction
		public static void setVelZ(Object ent, double amount) {
			nativeSetVel(getEntityId(ent), (float) amount, AXIS_Z);
		}

		@JSStaticFunction
		public static void setRot(Object ent, double yaw, double pitch) {
			nativeSetRot(getEntityId(ent), (float) yaw, (float) pitch);
		}

		@JSStaticFunction
		public static void rideAnimal(Object /* insert funny reference */rider, Object mount) {
			nativeRideAnimal(getEntityId(rider), getEntityId(mount));
		}

		@JSStaticFunction
		public static void setPosition(Object ent, double x, double y, double z) {
			nativeSetPosition(getEntityId(ent), (float) x, (float) y, (float) z);
		}

		@JSStaticFunction
		public static void setPositionRelative(Object ent, double x, double y, double z) {
			nativeSetPositionRelative(getEntityId(ent), (float) x, (float) y, (float) z);
		}

		@JSStaticFunction
		public static double getPitch(Object ent) {
			return nativeGetPitch(getEntityId(ent));
		}

		@JSStaticFunction
		public static double getYaw(Object ent) {
			return nativeGetYaw(getEntityId(ent));
		}

		// nonstandard

		@JSStaticFunction
		public static void setFireTicks(Object ent, int howLong) {
			nativeSetOnFire(getEntityId(ent), howLong);
		}

		@JSStaticFunction
		public static double getX(Object ent) {
			return nativeGetEntityLoc(getEntityId(ent), AXIS_X);
		}

		@JSStaticFunction
		public static double getY(Object ent) {
			return nativeGetEntityLoc(getEntityId(ent), AXIS_Y);
		}

		@JSStaticFunction
		public static double getZ(Object ent) {
			return nativeGetEntityLoc(getEntityId(ent), AXIS_Z);
		}

		@JSStaticFunction
		public static void setCarriedItem(Object ent, int id, int count, int damage) {
			if (!nativeIsValidItem(id)) {
				throw new RuntimeException("The item ID " + id + " is invalid.");
			}
			nativeSetCarriedItem(getEntityId(ent), id, count, damage);
		}

		@JSStaticFunction
		public static int getEntityTypeId(Object ent) {
			return nativeGetEntityTypeId(getEntityId(ent));
		}

		@JSStaticFunction
		public static long spawnMob(double x, double y, double z, int typeId, String tex) {
			if (invalidTexName(tex)) {
				tex = null;
			}
			long entityId = spawnEntityImpl((float) x, (float) y, (float) z, typeId, tex);
			return entityId;
		}

		@JSStaticFunction
		public static void setAnimalAge(Object animal, int age) {
			int type = getEntityTypeId(animal);
			nativeSetAnimalAge(getEntityId(animal), age);
		}

		@JSStaticFunction
		public static int getAnimalAge(Object animal) {
			int type = getEntityTypeId(animal);
			return nativeGetAnimalAge(getEntityId(animal));
		}

		@JSStaticFunction
		public static void setMobSkin(Object entity, String tex) {
			setMobSkinImpl(entity, tex, true);
		}

		public static void setMobSkinImpl(Object entity, String text, boolean persist) {
			String newSkinPath = OldEntityTextureFilenameMapping.m.get(text);
			if (newSkinPath != null) text = newSkinPath;
			if (text.endsWith(".png") || text.endsWith(".tga")) {
				text = text.substring(0, text.length() - 4);
			}
			nativeSetMobSkin(getEntityId(entity), text);
			if (persist) {
				setExtraData(entity, ENTITY_KEY_SKIN, text);
			}
		}

		@JSStaticFunction
		public static void remove(Object ent) {
			nativeRemoveEntity(getEntityId(ent));
		}

		@JSStaticFunction
		public static int getHealth(Object ent) {
			int entityType = getEntityTypeId(ent);
			if (!(entityType >= 10 && entityType < 64)) {
				return 0; // not a mob
			}
			return nativeGetMobHealth(getEntityId(ent));
		}

		@JSStaticFunction
		public static void setHealth(Object ent, int halfhearts) {
			int entityType = getEntityTypeId(ent);
			if (!(entityType >= 10 && entityType < 64)) {
				return;
				// Pokecube 4.0 crashes with this
				// throw new RuntimeException("setHealth called on non-mob: entityType=" + entityType);
			}
			nativeSetMobHealth(getEntityId(ent), halfhearts);
		}

		@JSStaticFunction
		public static void setMaxHealth(Object ent, int halfhearts) {
			int entityType = getEntityTypeId(ent);
			if (!(entityType >= 10 && entityType < 64)) {
				throw new RuntimeException("setMaxHealth called on non-mob: entityType=" + entityType);
			}
			nativeSetMobMaxHealth(getEntityId(ent), halfhearts);
		}

		@JSStaticFunction
		public static void setRenderType(Object ent, Object renderType) {
			/*
			RendererManager.NativeRenderer theRenderer = null;
			if (renderType instanceof NativeJavaObject) {
				renderType = ((NativeJavaObject)renderType).unwrap();
			}
			boolean alreadySet = false;
			if (renderType instanceof Number) {
				int rendererId = ((Number)renderType).intValue();
				setRenderTypeImpl(ent, rendererId);
				alreadySet = true;
				theRenderer = RendererManager.NativeRendererApi.getById(rendererId);
				if (theRenderer == null) return; // not a named/persistent renderer
			} else if (renderType instanceof RendererManager.NativeRenderer) {
				theRenderer = (RendererManager.NativeRenderer) renderType;
			} else {
				String theName = renderType.toString();
				theRenderer = RendererManager.NativeRendererApi.getByName(theName);
			}
			if (!alreadySet) setRenderTypeImpl(ent, theRenderer.getRenderType());
			setExtraData(ent, ENTITY_KEY_RENDERTYPE, theRenderer.getName());
			*/
		}

		public static void setRenderTypeImpl(Object ent, int renderType) {
			if (renderType < 0x1000 && !EntityRenderType.isValidRenderType(renderType)) {
				throw new RuntimeException("Render type " + renderType + " does not exist");
			}
			if (renderType == EntityRenderType.villager && getEntityTypeId(ent) != EntityType.VILLAGER) {
				throw new RuntimeException("Villager render type can only be used on villagers");
			}
			boolean ret = nativeSetEntityRenderType(getEntityId(ent), renderType);
			if (!ret) {
				throw new RuntimeException("Custom render type " + renderType + " does not exist");
			}
		}

		@JSStaticFunction
		public static void setSneaking(Object ent, boolean doIt) {
			nativeSetSneaking(getEntityId(ent), doIt);
		}

		@JSStaticFunction
		public static boolean isSneaking(Object ent) {
			return nativeIsSneaking(getEntityId(ent));
		}

		@JSStaticFunction
		public static double getVelX(Object ent) {
			return nativeGetEntityVel(getEntityId(ent), AXIS_X);
		}

		@JSStaticFunction
		public static double getVelY(Object ent) {
			return nativeGetEntityVel(getEntityId(ent), AXIS_Y);
		}

		@JSStaticFunction
		public static double getVelZ(Object ent) {
			return nativeGetEntityVel(getEntityId(ent), AXIS_Z);
		}

		@JSStaticFunction
		public static void setNameTag(Object entity, String name) {
			int entityType = nativeGetEntityTypeId(getEntityId(entity));
			if (entityType >= 64)
				throw new IllegalArgumentException("setNameTag only works on mobs");
			nativeEntitySetNameTag(getEntityId(entity), name);
		}
		
		
		// KMCPE's additions
		@JSStaticFunction
		public static long[] getAll() {
			long[] entities = new long[allentities.size()];
			for(int n=0;entities.length>n;n++){
				entities[n]=allentities.get(n);
			}
			return entities;
		}

		@JSStaticFunction
		public static String getNameTag(Object entity) {
			return nativeEntityGetNameTag(getEntityId(entity));
		}

		@JSStaticFunction
		public static int getRiding(Object entity) {
			return nativeEntityGetRiding(getEntityId(entity));
		}

		@JSStaticFunction
		public static int getRider(Object entity) {
			return nativeEntityGetRider(getEntityId(entity));
		}

		@JSStaticFunction
		public static String getMobSkin(Object entity) {
			long entityId = getEntityId(entity);
			int entityType = getEntityTypeId(entityId);
			if (!(entityType > 0 && entityType < 64)) return "";
			return nativeEntityGetMobSkin(entityId);
		}

		@JSStaticFunction
		public static int getRenderType(Object entity) {
			return nativeEntityGetRenderType(getEntityId(entity));
		}

		@JSStaticFunction
		public static String getUniqueId(Object entity) {
			return getEntityUUID(getEntityId(entity));
		}

		@JSStaticFunction
		public static void setCollisionSize(Object entity, double a, double b) {
			nativeEntitySetSize(getEntityId(entity), (float) a, (float) b);
		}

		@JSStaticFunction
		public static void setCape(Object entity, String location) {
			int typeId = nativeGetEntityTypeId(getEntityId(entity));
			if (!(typeId >= 32 && typeId < 64)) {
				throw new RuntimeException("Set cape only works for humanoid mobs");
			}
			nativeSetCape(getEntityId(entity), location);
		}

		@JSStaticFunction
		public static void addEffect(Object entity, int potionId, int duration, int amplifier, boolean isAmbient,
			boolean showParticles) {
			long entityId = getEntityId(entity);
			int typeId = nativeGetEntityTypeId(entityId);
			if (typeId <= 0) return; // ignore remove effect on no mob.
			if (!(typeId > 0 && typeId < 64)) {
				throw new RuntimeException("addEffect only works for mobs");
			}
			
			if (MobEffect.effectIds.get(potionId) == null) {
				throw new RuntimeException("Invalid MobEffect id: " + potionId);
			}
			nativeMobAddEffect(entityId, potionId, duration, amplifier, isAmbient, showParticles);
		}

		@JSStaticFunction
		public static void removeEffect(Object entity, int potionId) {
			long entityId = getEntityId(entity);
			int typeId = nativeGetEntityTypeId(entityId);
			if (typeId <= 0) return; // ignore remove effect on no mob.
			if (!(typeId > 0 && typeId < 64)) {
				throw new RuntimeException("removeEffect only works for mobs");
			}
			
			if (MobEffect.effectIds.get(potionId) == null) {
				throw new RuntimeException("Invalid MobEffect id: " + potionId);
			}
			nativeMobRemoveEffect(entityId, potionId);
		}

		@JSStaticFunction
		public static void removeAllEffects(Object entity) {
			long entityId = getEntityId(entity);
			int typeId = nativeGetEntityTypeId(entityId);
			if (typeId <= 0) return; // ignore remove effect on no mob.
			if (!(typeId > 0 && typeId < 64)) {
				throw new RuntimeException("removeAllEffects only works for mobs");
			}
			
			nativeMobRemoveAllEffects(entityId);
		}

		@JSStaticFunction
		public static int getItemEntityId(Object entity) {
			long entityId = getEntityId(entity);
			int typeId = nativeGetEntityTypeId(entityId);
			if (typeId != EntityType.ITEM) {
				throw new RuntimeException("getItemEntity only works on item entities: got " + typeId);
			}
			return nativeGetItemEntityItem(entityId, ITEMID);
		}

		@JSStaticFunction
		public static int getItemEntityData(Object entity) {
			long entityId = getEntityId(entity);
			int typeId = nativeGetEntityTypeId(entityId);
			if (typeId != EntityType.ITEM) {
				throw new RuntimeException("getItemEntity only works on item entities");
			}
			return nativeGetItemEntityItem(entityId, DAMAGE);
		}

		@JSStaticFunction
		public static int getItemEntityCount(Object entity) {
			long entityId = getEntityId(entity);
			int typeId = nativeGetEntityTypeId(entityId);
			if (typeId != EntityType.ITEM) {
				throw new RuntimeException("getItemEntity only works on item entities");
			}
			return nativeGetItemEntityItem(entityId, AMOUNT);
		}

		@JSStaticFunction
		public static int getArmor(Object entity, int slot) {
			if (!(slot >= 0 && slot < 4)) {
				throw new RuntimeException("slot " + slot + " is not a valid armor slot");
			}
			long entityId = getEntityId(entity);
			int typeId = nativeGetEntityTypeId(entityId);
// fixme 1.1
/*
			if (!(typeId > 0 && typeId < 64)) {
				throw new RuntimeException("getArmor only works for mobs");
			}
*/
			return nativeMobGetArmor(entityId, slot, ITEMID);
		}
		@JSStaticFunction
		public static int getArmorDamage(Object entity, int slot) {
			if (!(slot >= 0 && slot < 4)) {
				throw new RuntimeException("slot " + slot + " is not a valid armor slot");
			}
			long entityId = getEntityId(entity);
			int typeId = nativeGetEntityTypeId(entityId);
// fixme 1.1
/*
			if (!(typeId > 0 && typeId < 64)) {
				throw new RuntimeException("getArmorDamage only works for mobs");
			}
*/
			return nativeMobGetArmor(entityId, slot, DAMAGE);
		}

		@JSStaticFunction
		public static void setArmor(Object entity, int slot, int id, int damage) {
			if (!(slot >= 0 && slot < 4)) {
				throw new RuntimeException("slot " + slot + " is not a valid armor slot");
			}
			long entityId = getEntityId(entity);
			int typeId = nativeGetEntityTypeId(entityId);
			if (!(typeId > 0 && typeId < 64)) {
				throw new RuntimeException("setArmor only works for mobs");
			}
			nativeMobSetArmor(entityId, slot, id, damage);
		}

		@JSStaticFunction
		public static String getArmorCustomName(Object entity, int slot) {
			if (!(slot >= 0 && slot < 4)) {
				throw new RuntimeException("slot " + slot + " is not a valid armor slot");
			}
			long entityId = getEntityId(entity);
			int typeId = nativeGetEntityTypeId(entityId);
			if (!(typeId > 0 && typeId < 64)) {
				throw new RuntimeException("setArmor only works for mobs");
			}
			return nativeMobGetArmorCustomName(entityId, slot);
		}

		@JSStaticFunction
		public static void setArmorCustomName(Object entity, int slot, String name) {
			if (!(slot >= 0 && slot < 4)) {
				throw new RuntimeException("slot " + slot + " is not a valid armor slot");
			}
			long entityId = getEntityId(entity);
			int typeId = nativeGetEntityTypeId(entityId);
			if (!(typeId > 0 && typeId < 64)) {
				throw new RuntimeException("setArmor only works for mobs");
			}
			nativeMobSetArmorCustomName(entityId, slot, name);
		}

		@JSStaticFunction
		public static int getMaxHealth(Object entity) {
			return nativeGetMobMaxHealth(getEntityId(entity));
		}

		@JSStaticFunction
		public static String getExtraData(Object entity, String key) {
			if (worldData == null) return null;
			return worldData.getEntityData(getEntityId(entity), key);
		}

		@JSStaticFunction
		public static boolean setExtraData(Object entity, String key, String value) {
			if (worldData == null) return false;
			worldData.setEntityData(getEntityId(entity), key, value);
			return true;
		}

		@JSStaticFunction
		public static void setImmobile(Object entity, boolean immobile) {
			setImmobileImpl(entity, immobile);
			setExtraData(entity, ENTITY_KEY_IMMOBILE, immobile? "1": "0");
		}

		public static void setImmobileImpl(Object entity, boolean immobile) {
			nativeEntitySetImmobile(getEntityId(entity), immobile);
		}

		@JSStaticFunction
		public static long getTarget(Object entity) {
			long entityId = getEntityId(entity);
			int typeId = nativeGetEntityTypeId(entityId);
			if (!(typeId > 0 && typeId < 64)) {
				throw new RuntimeException("getTarget only works on mobs");
			}
			return nativeEntityGetTarget(entityId);
		}

		@JSStaticFunction
		public static void setTarget(Object entity, Object target) {
			long entityId = getEntityId(entity);
			int typeId = nativeGetEntityTypeId(entityId);
			if (!(typeId > 0 && typeId < 64)) {
				throw new RuntimeException("setTarget only works on mob entities");
			}
			long targetId = target == null? -1: getEntityId(target);
			if (targetId != -1) {
				int targetTypeId = nativeGetEntityTypeId(targetId);
				if (!(targetTypeId > 0 && targetTypeId < 64)) {
					throw new RuntimeException("setTarget only works on mob targets");
				}
			}
			nativeEntitySetTarget(entityId, targetId);
		}

		@JSStaticFunction
		public static int getCarriedItem(Object entity) {
			long entityId = getEntityId(entity);
			return nativeEntityGetCarriedItem(entityId, ITEMID);
		}

		@JSStaticFunction
		public static int getCarriedItemData(Object entity) {
			long entityId = getEntityId(entity);
			return nativeEntityGetCarriedItem(entityId, DAMAGE);
		}

		@JSStaticFunction
		public static int getCarriedItemCount(Object entity) {
			long entityId = getEntityId(entity);
			return nativeEntityGetCarriedItem(entityId, AMOUNT);
		}

		@JSStaticFunction
		public static int getOffhandSlot(Object entity) {
			long entityId = getEntityId(entity);
			ensureEntityIsMob(entityId);
			return nativeEntityGetOffhandSlot(entityId, ITEMID);
		}

		@JSStaticFunction
		public static int getOffhandSlotData(Object entity) {
			long entityId = getEntityId(entity);
			ensureEntityIsMob(entityId);
			return nativeEntityGetOffhandSlot(entityId, DAMAGE);
		}

		@JSStaticFunction
		public static int getOffhandSlotCount(Object entity) {
			long entityId = getEntityId(entity);
			ensureEntityIsMob(entityId);
			return nativeEntityGetOffhandSlot(entityId, AMOUNT);
		}

		@JSStaticFunction
		public static void setOffhandSlot(Object ent, int id, int count, int damage) {
			if (!nativeIsValidItem(id)) {
				throw new RuntimeException("The item ID " + id + " is invalid.");
			}
			long entityId = getEntityId(ent);
			ensureEntityIsMob(entityId);
			nativeEntitySetOffhandSlot(entityId, id, count, damage);
		}

		@Override
		public String getClassName() {
			return "Entity";
		}
	}

	private static class NativeModPEApi extends ScriptableObject {
		public NativeModPEApi() {
			super();
		}

		@JSStaticFunction
		public static void log(String str) {
			Log.i("MCPELauncherLog", str);
		}

		@JSStaticFunction
		public static void setTerrain(String url) {
			overrideTexture("images/terrain-atlas.tga", url);
		}

		@JSStaticFunction
		public static void setItems(String url) {
			overrideTexture("images/items-opaque.png", url);
		}

		@JSStaticFunction
		public static void setGuiBlocks(String url) {
			overrideTexture("gui/gui_blocks.png", url);
		}

		@JSStaticFunction
		public static void overrideTexture(String theOverridden, String url) {
			ScriptManager.overrideTexture(url, theOverridden);
		}

		@JSStaticFunction
		public static void resetImages() {
			ScriptManager.clearTextureOverrides();
		}

		@JSStaticFunction
		public static void setItem(int id, String iconName, int iconSubindex, String name,
				int maxStackSize) {
			try {
				Integer.parseInt(iconName);
				throw new IllegalArgumentException("The item icon for " + name.trim()
					+ " is not updated for 0.8.0. Please ask the script author to update");
			} catch (NumberFormatException e) {
			}
			if (id <= 0 || id >= ITEM_ID_COUNT) {
				throw new IllegalArgumentException("Item IDs must be > 0 and < ITEM_ID_COUNT");
			}
			if (itemsMeta != null && !itemsMeta.hasIcon(iconName, iconSubindex)) {
				throw new MissingTextureException("The item icon " + iconName + ":" + iconSubindex + " does not exist");
			}
			nativeDefineItem(id, iconName, iconSubindex, name, maxStackSize);
		}

		@JSStaticFunction
		public static void setFoodItem(int id, String iconName, int iconSubindex, int halfhearts,
				String name, int maxStackSize) {
			setItem(id, iconName, iconSubindex, name, maxStackSize);
			NativeItemApi.setProperties(id, "{\"use_animation\":\"eat\",\"use_duration\": 32," +
				"\"food\":{\"nutrition\":" + halfhearts + ",\"saturation_modifier\": \"normal\"," +
				"\"is_meat\": false}}");
		}

		// nonstandard
		private static String getLevelName(File worldDir) throws IOException {
			File nameFile = new File(worldDir, "levelname.txt");
			if (!nameFile.exists()) return null;
			FileInputStream fis = new FileInputStream(nameFile);
			byte[] buf = new byte[(int) nameFile.length()];
			fis.read(buf);
			fis.close();
			String worldName = new String(buf, "UTF-8");
			return worldName;
		}

		@JSStaticFunction
		public static void selectLevel(String levelDir) {
			String levelDirName = levelDir;
			File worldsDir = new File("/sdcard/games/com.mojang/minecraftWorlds");
			File theDir = new File(worldsDir, levelDirName);
			if (!theDir.exists()) {
				for (File worldDir: worldsDir.listFiles()) {
					try {
						String worldName = getLevelName(worldDir);
						if (worldName != null && worldName.equals(levelDir)) {
							levelDirName = worldDir.getName();
							theDir = worldDir;
							break;
						}
					} catch (IOException ie) {
						ie.printStackTrace();
					}
				}
			}
			if (!theDir.exists()) {
				throw new RuntimeException("The selected world " + levelDir + " does not exist.");
			}
			if (levelDirName.equals(ScriptManager.worldDir)) {
				System.err.println("Attempted to load level that is already loaded - ignore");
				return;
			}
			String levelFullName = null;
			try {
				levelFullName = getLevelName(theDir);
			} catch (IOException ie) {
				ie.printStackTrace();
			}

			setRequestLeaveGame();
			// nativeSelectLevel(levelDir);
			requestSelectLevel = new SelectLevelRequest();
			requestSelectLevel.dir = levelDirName;
			requestSelectLevel.name = levelFullName == null? levelDirName : levelFullName;
		}

		@JSStaticFunction
		public static String readData(String prefName) {
			SharedPreferences sPrefs = androidContext.getSharedPreferences(
					"BlockLauncherModPEScript" + currentScript, 0);
			return sPrefs.getString(prefName, "");
		}

		@JSStaticFunction
		public static void saveData(String prefName, String prefValue) {
			SharedPreferences sPrefs = androidContext.getSharedPreferences(
					"BlockLauncherModPEScript" + currentScript, 0);
			SharedPreferences.Editor prefsEditor = sPrefs.edit();
			prefsEditor.putString(prefName, prefValue);
			prefsEditor.commit();
		}

		@JSStaticFunction
		public static void removeData(String prefName) {
			SharedPreferences sPrefs = androidContext.getSharedPreferences(
					"BlockLauncherModPEScript" + currentScript, 0);
			SharedPreferences.Editor prefsEditor = sPrefs.edit();
			prefsEditor.remove(prefName);
			prefsEditor.commit();
		}

		@JSStaticFunction
		public static void leaveGame() {
			setRequestLeaveGame();
		}

		@JSStaticFunction
		public static void setGameSpeed(double ticksPerSecond) {
			nativeSetGameSpeed((float) ticksPerSecond);
		}

		@JSStaticFunction
		public static void takeScreenshot(String fileName) {
			ScriptManager.takeScreenshot(fileName);
		}

		@JSStaticFunction
		public static void langEdit(String key, String value) {
			nativeSetI18NString(key, value);
		}

		@JSStaticFunction
		public static void showTipMessage(String msg) {
			nativeShowTipMessage(msg);
			// showTipMessage doesn't work anymore?
			//scriptFakeTipMessage(msg);
		}

		@JSStaticFunction
		public static void setCamera(Object entityId) {
			nativeSetCameraEntity(getEntityId(entityId));
		}

		@JSStaticFunction
		public static void setFov(double fov) {
			nativeSetFov((float) fov, true);
		}

		@JSStaticFunction
		public static void resetFov() {
			nativeSetFov(0.0f, false);
		}

		@JSStaticFunction
		public static String getMinecraftVersion() {
			try {
				return androidContext.getPackageManager().
					getPackageInfo("com.mojang.minecraftpe", 0).
					versionName;
			} catch (Exception e) {
				e.printStackTrace();
				return "Unknown";
			}
		}

		/*@JSStaticFunction
		public static void crash() {
			if (MainActivity.currentMainActivity != null) {
				MainActivity main = MainActivity.currentMainActivity.get();
				if (main != null) {
					main.runOnUiThread(new Runnable() {
						public void run() {
							throw new RuntimeException("Intentional crash");
						}
					});
				}
			}
		}

		@JSStaticFunction
		public static void crashNative() {
			nativeForceCrash();
		}*/

		private static String trimTexturePackName(String name) {
			// DesnoGuns calls getBytesFromTexturePack with a leading "/"; strip this
			if (name.length() > 1 && name.charAt(0) == '/') {
				return name.substring(1);
			}
			return name;
		}

		@JSStaticFunction
		public static byte[] getBytesFromTexturePack(String name) {
			name = trimTexturePackName(name);
			if (MainActivity.currentMainActivity != null) {
				MainActivity main = MainActivity.currentMainActivity.get();
				if (main != null) {
					byte[] bytes = main.getFileDataBytes(name);
					if (bytes != null) return bytes;
					String startingPath = main.getMCPEVersion().startsWith("0.16")?
						"resourcepacks/vanilla/client/": "resource_packs/vanilla/";
					return main.getFileDataBytes(startingPath + (name.startsWith("images/")? "textures/" + name.substring("images/".length()) : name));
				}
			}
			return null;
		}

		@JSStaticFunction
		public static InputStream openInputStreamFromTexturePack(String name) {
			name = trimTexturePackName(name);
			if (MainActivity.currentMainActivity != null) {
				MainActivity main = MainActivity.currentMainActivity.get();
				if (main != null) {
					InputStream is = main.getInputStreamForAsset(name);
					if (is != null) return is;
					String startingPath = main.getMCPEVersion().startsWith("0.16")?
						"resourcepacks/vanilla/client/": "resource_packs/vanilla/";
					return main.getInputStreamForAsset(startingPath + (name.startsWith("images/")? "textures/" + name.substring("images/".length()) : name));
				}
			}
			return null;
		}

		@JSStaticFunction
		public static void dumpVtable(String className, int size) {
			nativeDumpVtable("_ZTV" + className.length() + className, size);
		}

		@JSStaticFunction
		public static String getI18n(String key) {
			return nativeGetI18NString(key);
		}

		@JSStaticFunction
		public static String getLanguage() {
			return nativeGetLanguageName();
		}

		@JSStaticFunction
		public static void setUiRenderDebug(boolean render) {
			nativeModPESetRenderDebug(render);
		}

		@JSStaticFunction
		public static String getOS() {
			return "Android";
		}

		@Override
		public String getClassName() {
			return "ModPE";
		}
	}

	private static class NativeItemApi extends ScriptableObject {
		private static List<Object[]> activeRecipes = new ArrayList<Object[]>();
		private static List<Object[]> activeShapelessRecipes = new ArrayList<Object[]>();
		private static List<int[]> activeFurnaceRecipes = new ArrayList<int[]>();
		private static Map<Integer, Integer> itemIdToRendererId = new HashMap<Integer, Integer>();
		private static Map<Integer, Integer> rendererToItemId = new HashMap<Integer, Integer>();
		public NativeItemApi() {
		}
		
		@JSStaticFunction
		public static String getName(int id, int damage, boolean raw) {
			if (!nativeIsValidItem(id)) {
				throw new RuntimeException("getName called with invalid item ID: " + id);
			}
			if (id == 358) return "Map"; // Maps need an NBT, otherwise crash
			return nativeGetItemName(id, damage, raw);
		}

		@JSStaticFunction
		public static int internalNameToId(String name) {
			return nameToIdImpl(name, true);
		}

		@JSStaticFunction
		public static int translatedNameToId(String name) {
			return nameToIdImpl(name, false);
		}

		private static int nameToIdImpl(String name, boolean internal) {
			if (name == null) return -1;
			name = name.replace(" ", "_").toLowerCase();
			for (int i = 0x100; i < 0x1000; i++) {
				if (idHasName(name, i, internal)) return i;
			}
			for (int i = 1; i < 0x100; i++) {
				if (idHasName(name, i, internal)) return i;
			}
			try {
				return Integer.parseInt(name);
			} catch (Exception e) {
				return -1;
			}
		}

		private static boolean idHasName(String targetname, int id, boolean internal) {
			if (id == 358) return false;
			String name = nativeGetItemName(id, 0, internal);
			if (name == null) return false;
			if (internal) {
				int endSub = name.endsWith(".name")? name.length() - 5: name.length() /* .name */;
				int startSub = name.startsWith("tile.") || name.startsWith("item.")? 5: 0;
				name = name.substring(startSub, endSub);
			}
			name = name.replace(" ", "_").toLowerCase();
			return targetname.equals(name);
		}

		@JSStaticFunction
		public static void addCraftRecipe(int id, int count, int damage, Scriptable ingredientsScriptable) {
			int[] expanded = expandShapelessRecipe(ingredientsScriptable);
			if (!nativeIsValidItem(id)) {
				throw new RuntimeException("Invalid input in shapeless recipe: " + id + " is not a valid item. " +
					"You must create the item before you can add it to a recipe.");
			}
			for (int i = 0; i < expanded.length; i += 3) {
				if (!nativeIsValidItem(expanded[i])) {
					throw new RuntimeException("Invalid output in shapeless recipe: " + expanded[i] +
					" is not a valid item. You must create the item before you can add it to a" +
					" recipe.");
				}
			}
			for (Object[] r: activeShapelessRecipes) {
				if (
					((Integer) r[0]) == id &&
					((Integer) r[1]) == count &&
					((Integer) r[2]) == damage &&
					Arrays.equals((int[]) r[3], expanded)
				) {
					System.out.println("Recipe already exists.");
					return;
				}
			}
			activeShapelessRecipes.add(new Object[]{id, count, damage, expanded});
			nativeAddShapelessRecipe(id, count, damage, expanded);
		}

		@JSStaticFunction
		public static void addFurnaceRecipe(int inputId, int outputId, int outputDamage) {
			// Do I need a count? If not, should I just fill it with null, or
			// skip it completely?
			if (!nativeIsValidItem(inputId)) {
				throw new RuntimeException("Invalid input in furnace recipe: " + inputId + " is not a valid item. " +
					"You must create the item before you can add it to a recipe.");
			}
			if (!nativeIsValidItem(outputId)) {
				throw new RuntimeException("Invalid output in furnace recipe: " + outputId + " is not a valid item. " +
					"You must create the item before you can add it to a recipe.");
			}
			for (int[] recipe: activeFurnaceRecipes) {
				if (recipe[0] == inputId && recipe[1] == outputId && recipe[2] == outputDamage) {
					System.out.println("Furnace recipe already exists.");
					return;
				}
			}
			activeFurnaceRecipes.add(new int[]{inputId, outputId, outputDamage});
			nativeAddFurnaceRecipe(inputId, outputId, outputDamage);
		}

		@JSStaticFunction
		public static void addShapedRecipe(int id, int count, int damage, Scriptable shape, Scriptable ingredients) {
			int shapeArrayLength = ((Number) ScriptableObject.getProperty(shape, "length"))
				.intValue();
			String[] shapeArray = new String[shapeArrayLength];
			for (int i = 0; i < shapeArrayLength; i++) {
				shapeArray[i] = ScriptableObject.getProperty(shape, i).toString();
			}
			int ingredientsArrayLength = ((Number) ScriptableObject.getProperty(ingredients, "length"))
				.intValue();
			if (ingredientsArrayLength % 3 != 0) {
				throw new RuntimeException("Ingredients array must be [\"?\", id, damage, ...]");
			}
			int[] ingredientsArray = new int[ingredientsArrayLength];
			for (int i = 0; i < ingredientsArrayLength; i++) {
				Object str = ScriptableObject.getProperty(ingredients, i);
				if (i % 3 == 0) {
					ingredientsArray[i] = str.toString().charAt(0);
				} else {
					ingredientsArray[i] = ((Number) str).intValue();
				}
			}
			//System.out.println(Arrays.toString(shapeArray));
			//System.out.println(Arrays.toString(ingredientsArray));
			verifyAndAddShapedRecipe(id, count, damage, shapeArray, ingredientsArray);
		}

		private static void verifyAndAddShapedRecipe(int id, int count, int damage, String[] shape,
			int[] ingredients) {
			if (id < -512 || id >= ITEM_ID_COUNT) {
				throw new RuntimeException("Invalid result in recipe: " + id + ": must be between -512 and " + ITEM_ID_COUNT);
			}
			if (!nativeIsValidItem(id)) {
				throw new RuntimeException("Invalid result in recipe: " + id + " is not a valid item. " +
					"You must create the item before you can add it to a recipe.");
			}
			for (int i = 0; i < ingredients.length; i += 3) {
				if (!nativeIsValidItem(ingredients[i + 1])) {
					throw new RuntimeException("Invalid input in recipe: " + id + " is not a valid item. " +
						"You must create the item before you can add it to a recipe.");
				}
			}
			for (Object[] r: activeRecipes) {
				if (
					((Integer) r[0]) == id &&
					((Integer) r[1]) == count &&
					((Integer) r[2]) == damage &&
					Arrays.equals((String[]) r[3], shape) &&
					Arrays.equals((int[]) r[4], ingredients)
				) {
					System.out.println("Recipe already exists.");
					return;
				}
			}
			activeRecipes.add(new Object[]{id, count, damage, shape, ingredients});
			nativeAddShapedRecipe(id, count, damage, shape, ingredients);
		}

		public static void reregisterRecipes() {
			Log.i("BlockLauncher", "Reregister recipes");
			for (Object[] r: activeRecipes) {
				nativeAddShapedRecipe((Integer) r[0], (Integer) r[1], (Integer) r[2], (String[]) r[3], (int[]) r[4]);
			}
			for (Object[] r: activeShapelessRecipes) {
				nativeAddShapelessRecipe((Integer) r[0], (Integer) r[1], (Integer) r[2], (int[]) r[3]);
			}
		}

		@JSStaticFunction
		public static void setMaxDamage(int id, int maxDamage) {
			nativeSetItemMaxDamage(id, maxDamage);
		}

		@JSStaticFunction
		public static int getMaxDamage(int id) {
			return nativeGetItemMaxDamage(id);
		}

		@JSStaticFunction
		public static void setCategory(int id, int category) {
			if (category < 0 || category > 4)
				throw new RuntimeException("Invalid category " + category +
					": should be one of ItemCategory.MATERIAL, ItemCategory.DECORATION, " + 
					"ItemCategory.TOOL, or ItemCategory.FOOD");
			nativeSetItemCategory(id, category, 0);
		}

		/*@JSStaticFunction
		public static void addIcon(String url, String name, int index) {
			ScriptManager.addIcon("items", url, name, index);
		}*/

		@JSStaticFunction
		public static void setHandEquipped(int id, boolean yep) {
			nativeSetHandEquipped(id, yep);
		}

		@JSStaticFunction
		public static void defineArmor(int id, String iconName, int iconIndex, String name,
			String texture, int damageReduceAmount, int maxDamage, int armorType) {
			NativeModPEApi.setItem(id, iconName, iconIndex, name, 1);
		}
		public static void defineArmor2(int id, String iconName, int iconIndex, String name,
			String texture, int damageReduceAmount, int maxDamage, int armorType) {
			String newSkinPath = OldEntityTextureFilenameMapping.m.get(texture);
			if (newSkinPath != null) texture = newSkinPath;
			if (texture != null && texture.toLowerCase().endsWith(".png")) {
				texture = texture.substring(0, texture.length() - 4);
			}
			if (!(armorType >= 0 && armorType <= 3)) {
				throw new RuntimeException("Invalid armor type: use ArmorType.helmet, ArmorType.chestplate," +
					"ArmorType.leggings, or ArmorType.boots");
			}
			if (id <= 0 || id >= ITEM_ID_COUNT) {
				throw new IllegalArgumentException("Item IDs must be > 0 and < " + ITEM_ID_COUNT);
			}
			if (itemsMeta != null && !itemsMeta.hasIcon(iconName, iconIndex)) {
				throw new MissingTextureException("The item icon " + iconName + ":" + iconIndex + " does not exist");
			}
			nativeDefineArmor(id, iconName, iconIndex, name,
				texture, damageReduceAmount, maxDamage, armorType);
		}

		@JSStaticFunction
		public static boolean isValidItem(int id) {
			return nativeIsValidItem(id);
		}

		@JSStaticFunction
		public static void setProperties(int id, Object props) {
			if (!isValidItem(id)) throw new RuntimeException(id + " is not a valid item");
			String theJson;
			if (props instanceof CharSequence || ScriptRuntime.typeof(props).equals("string")) {
				theJson = props.toString();
			} else if (props instanceof Scriptable) {
				Scriptable s = (Scriptable) props;
				theJson = NativeJSON.stringify(Context.getCurrentContext(), s.getParentScope(), s, null, "").toString();
			} else {
				// What is this
				throw new RuntimeException("Invalid input to setProperties: " + props + " cannot be converted to JSON");
			}
			boolean ret = nativeItemSetProperties(id, theJson);
			if (!ret) throw new RuntimeException("Failed to set properties for item " + id);
		}

		@JSStaticFunction
		public static int getUseAnimation(int id) {
			return nativeItemGetUseAnimation(id);
		}

		@JSStaticFunction
		public static void setUseAnimation(int id, int animation) {
			nativeItemSetUseAnimation(id, animation);
		}

		@JSStaticFunction
		public static void setStackedByData(int id, boolean stacked) {
			nativeItemSetStackedByData(id, stacked);
		}

		@JSStaticFunction
		public static void setAllowOffhand(int id, boolean allowOffhand) {
			nativeItemSetAllowOffhand(id, allowOffhand);
		}

		/*
		@JSStaticFunction
		public static void setRecipeIgnoreData(int id, boolean ignore) {
			nativeRecipeSetAnyAuxValue(id, ignore);
		}
		*/

		@JSStaticFunction
		public static void setEnchantType(int id, int flag, int value) {
			nativeSetAllowEnchantments(id, flag, value);
		}

		@JSStaticFunction
		public static int[] getTextureCoords(int id, int damage) {
			float[] retval = new float[6];
			boolean success = nativeGetTextureCoordinatesForItem(id, damage, retval);
			if (!success) throw new RuntimeException("Can't get texture for item " + id + ":" + damage);
			int[] newretval = new int[] {(int) (retval[0] * retval[4] + 0.5), (int) (retval[1] * retval[5] + 0.5),
				(int) (retval[2] * retval[4] + 0.5), (int) (retval[3] * retval[5] + 0.5),
				(int) (retval[4] + 0.5), (int) (retval[5] + 0.5)};
			return newretval;
		}

		@JSStaticFunction
		public static int getMaxStackSize(int id) {
			return nativeItemGetMaxStackSize(id);
		}


		@JSStaticFunction
		public static void defineThrowable(int id, String iconName, int iconSubindex, String name, int maxStackSize) {
			if (id <= 0 || id >= ITEM_ID_COUNT) {
				throw new IllegalArgumentException("Item IDs must be > 0 and < ITEM_ID_COUNT");
			}
			if (itemsMeta != null && !itemsMeta.hasIcon(iconName, iconSubindex)) {
				throw new MissingTextureException("The item icon " + iconName + ":" + iconSubindex + " does not exist");
			}
			nativeDefineSnowballItem(id, iconName, iconSubindex, name, maxStackSize);
			int renderer = RendererManager.nativeCreateItemSpriteRenderer(id);
			itemIdToRendererId.put(renderer, id);
			rendererToItemId.put(id, renderer);
		}

		@JSStaticFunction
		public static int getCustomThrowableRenderType(int itemId) {
			Integer i = rendererToItemId.get(itemId);
			if (i == null) throw new RuntimeException("Not a custom throwable item ID: " + itemId);
			return i;
		}

		protected static void redefineThrowableRenderersAfterReload() {
			/* Not working yet. FIXME 1.6
			Log.i("BlockLauncher", "Redefining throwables");
			for (Map.Entry<Integer, Integer> entry : rendererToItemId.entrySet()) {
				int id = entry.getKey();
				int renderer = RendererManager.nativeCreateItemSpriteRenderer(id);
				itemIdToRendererId.put(renderer, id);
				entry.setValue(renderer);
			}
			*/
		}

		@Override
		public String getClassName() {
			return "Item";
		}
	}

	private static class NativeBlockApi extends ScriptableObject {
		public NativeBlockApi() {
		}

		@JSStaticFunction
		public static void defineBlock(int blockId, String name, Object textures,
				Object materialSourceIdSrc, Object opaqueSrc, Object renderTypeSrc) {
			defineBlockImpl(blockId, name, textures, materialSourceIdSrc, opaqueSrc, renderTypeSrc, 0);
		}

		@JSStaticFunction
		public static int defineLiquidBlock(int blockId, String name, Object textures,
				Object materialSourceIdSrc) {
			defineBlockImpl(blockId, name, textures, materialSourceIdSrc, 8, 8, 1 /* flowing */);
			defineBlockImpl(blockId + 1, "Still " + name , textures, materialSourceIdSrc, 8, 8, 2 /* still */);
			return blockId + 1;
		}

		private static void defineBlockImpl(int blockId, String name, Object textures,
				Object materialSourceIdSrc, Object opaqueSrc, Object renderTypeSrc, int customBlockType) {
			if (blockId < -256 || blockId >= ITEM_ID_COUNT) {
				throw new IllegalArgumentException("Block IDs must be >= -256 and < " + ITEM_ID_COUNT);
			}
			int materialSourceId = 17; // wood
			boolean opaque = true;
			int renderType = 0;
			if (materialSourceIdSrc != null && materialSourceIdSrc instanceof Number) {
				materialSourceId = ((Number) materialSourceIdSrc).intValue();
				Log.i("BlockLauncher", "setting material source to " + materialSourceId);
			}
			if (opaqueSrc != null && opaqueSrc instanceof Boolean) {
				opaque = (Boolean) opaqueSrc;
				Log.i("BlockLauncher", "setting opaque to " + opaque);
			}
			if (renderTypeSrc != null && renderTypeSrc instanceof Number) {
				renderType = ((Number) renderTypeSrc).intValue();
				Log.i("BlockLauncher", "setting renderType to " + renderType);
			}
			TextureRequests finalTextures = mapTextureNames(expandTexturesArray(textures));
			verifyBlockTextures(finalTextures);
			try {
				blocksJson.setBlockTextures(name, blockId, finalTextures.names, finalTextures.coords);
			} catch (JSONException je) {
				throw new RuntimeException(je);
			}
			//nativeDefineBlock(blockId, name, finalTextures.names, finalTextures.coords,
			//		materialSourceId, opaque, renderType, customBlockType);
		}

		@JSStaticFunction
		public static void setDestroyTime(int blockId, double time) {
			if (!scriptingEnabled) return;
			nativeBlockSetDestroyTime(blockId, (float) time);
		}

		@JSStaticFunction
		public static int getRenderType(int blockId) {
			return nativeGetBlockRenderShape(blockId);
		}

		@JSStaticFunction
		public static void setRenderType(int blockId, int renderType) {
			nativeSetBlockRenderShape(blockId, renderType);
		}
		
		@JSStaticFunction
		public static void setExplosionResistance(int blockId, double resist) {
			nativeBlockSetExplosionResistance(blockId, (float) resist);
		}

		@JSStaticFunction
		public static void setShape(int blockId, double v1, double v2, double v3, double v4,
				double v5, double v6, int damage) {
			nativeBlockSetShape(blockId, (float) v1, (float) v2, (float) v3, (float) v4,
					(float) v5, (float) v6, damage);
		}

		/*
		 * @JSStaticFunction public static void setStepSound(int blockId, int
		 * sourceId) { nativeBlockSetStepSound(blockId, sourceId); }
		 */
		@JSStaticFunction
		public static void setLightLevel(int blockId, int lightLevel) {
			nativeBlockSetLightLevel(blockId, lightLevel);
		}

		@JSStaticFunction
		public static void setColor(int blockId, Scriptable colorArray) {
			int[] finalColors = expandColorsArray(colorArray);
			nativeBlockSetColor(blockId, finalColors);
		}

		@JSStaticFunction
		public static void setRenderLayer(int blockId, int layer) {
			// workaround for hardcoded 3 value
			if (layer == 3) {
				layer = BlockRenderLayer.alpha_single_side;
			} else if (layer == BlockRenderLayer.alpha) {
				layer = 3;
			}
			if (layer == 4) layer = 3; // layer 4 is dead
			nativeBlockSetRenderLayer(blockId, layer);
		}

		@JSStaticFunction
		public static int getRenderLayer(int blockId) {
			return nativeBlockGetRenderLayer(blockId);
		}

		/*@JSStaticFunction
		public static void addIcon(String url, String name, int index) {
			ScriptManager.addIcon("terrain", url, name, index);
		}*/

		@JSStaticFunction
		public static void setLightOpacity(int blockId, int lightLevel) {
			nativeBlockSetLightOpacity(blockId, lightLevel);
		}

		/*@JSStaticFunction
		public static void setCollisionEnabled(int blockId, boolean yep) {
			nativeBlockSetCollisionEnabled(blockId, yep);;
		}*/

		/*

		@JSStaticFunction
		public static boolean isDouble(int x, int y, int z) {
			return nativeBlockGetSecondPart(x, y, z, AXIS_Y) != -1;
		}

		@JSStaticFunction
		public static int getSecondPartX(int x, int y, int z) {
			return nativeBlockGetSecondPart(x, y, z, AXIS_X);
		}

		@JSStaticFunction
		public static int getSecondPartY(int x, int y, int z) {
			return nativeBlockGetSecondPart(x, y, z, AXIS_Y);
		}

		@JSStaticFunction
		public static int getSecondPartZ(int x, int y, int z) {
			return nativeBlockGetSecondPart(x, y, z, AXIS_Z);
		}

		*/

		@JSStaticFunction
		public static int[] getAllBlockIds() {
			boolean[] validIds = new boolean[0x100];
			int theCount = 0;
			for (int i = 0; i < 0x100; i++) {
				if (nativeIsValidItem(i)) {
					validIds[i] = true;
					theCount++;
				}
			}
			int[] retval = new int[theCount];
			int b = 0;
			for (int i = 0; i < 0x100; i++) {
				if (validIds[i]) retval[b++] = i;
			}
			return retval;
		}

		@JSStaticFunction
		public static double getDestroyTime(int id, int damage) {
			return nativeBlockGetDestroyTime(id, damage);
		}

		@JSStaticFunction
		public static double getFriction(int id, int damage) {
			return nativeBlockGetFriction(id);
		}

		@JSStaticFunction
		public static void setFriction(int id, double friction) {
			if (friction < 0.1) friction = 0.1; // avoid crash - thanks @VeroXCode
			nativeBlockSetFriction(id, (float)friction);
		}

		@JSStaticFunction
		public static void setRedstoneConsumer(int id, boolean enabled) {
			nativeBlockSetRedstoneConsumer(id, enabled);
		}

		@JSStaticFunction
		public static int[] getTextureCoords(int id, int damage, int side) {
			float[] retval = new float[6];
			boolean success = nativeGetTextureCoordinatesForBlock(id, damage, side, retval);
			if (!success) throw new RuntimeException("Can't get texture for block " + id + ":" + damage);
			int[] newretval = new int[] {(int) (retval[0] * retval[4] + 0.5), (int) (retval[1] * retval[5] + 0.5),
				(int) (retval[2] * retval[4] + 0.5), (int) (retval[3] * retval[5] + 0.5),
				(int) (retval[4] + 0.5), (int) (retval[5] + 0.5)};
			return newretval;
		}

		@Override
		public String getClassName() {
			return "Block";
		}
	}

	private static class NativeServerApi extends ScriptableObject {
		public NativeServerApi() {
		}

		@JSStaticFunction
		public static void joinServer(String serverAddress, int port) {
			throw new RuntimeException("FIXME 0.11");
			/*
			setRequestLeaveGame();
			requestJoinServer = new JoinServerRequest();
			String resolvedAddress;
			try {
				InetAddress address = InetAddress.getByName(serverAddress);
				resolvedAddress = address.getHostAddress();
			} catch (UnknownHostException e) {
				throw new RuntimeException(e);
			}
			requestJoinServer.serverAddress = resolvedAddress;
			requestJoinServer.serverPort = port;
			*/
		}

		@JSStaticFunction
		public static void sendChat(String message) {
			nativeSendChat(message);
		}

		@JSStaticFunction
		public static String getAddress() {
			return serverAddress;
		}

		@JSStaticFunction
		public static int getPort() {
			return serverPort;
		}
		
		// KMCPE's additions
		@JSStaticFunction
		public static long[] getAllPlayers() {
			return nativeServerGetPlayers();
			/*
			long[] players = new long[allplayers.size()];
			for(int n=0;players.length>n;n++){
				players[n]=allplayers.get(n);
			}
			return players;
			*/
		}
		
		@JSStaticFunction
		public static String[] getAllPlayerNames() {
			long[] playerIds = getAllPlayers();
			String[] players = new String[playerIds.length];
			for(int n = 0; n < players.length; n++){
				players[n] = nativeGetPlayerName(playerIds[n]);
			}
			return players;
		}
		
		@Override
		public String getClassName() {
			return "Server";
		}
	}

	private static class NativeGuiApi extends ScriptableObject {

		public NativeGuiApi() {
		}

		@JSStaticFunction
		public static int getScreenWidth() {
			return 0;
		}

		@JSStaticFunction
		public static int getScreenHeight() {
			return 0;
		}

		@Override
		public String getClassName() {
			return "Gui";
		}
	}

	private static class SelectLevelRequest {
		public String dir;
		public String name, seed;
		public int gameMode = 0;
	}

	private static class JoinServerRequest {
		public String serverAddress;
		public int serverPort;
	}

	private static class AfterSkinDownloadAction implements Runnable {
		private long entityId;
		private String skinPath;

		public AfterSkinDownloadAction(long entityId, String skinPath) {
			this.entityId = entityId;
			this.skinPath = skinPath;
		}

		public void run() {
			File skinFile = getTextureOverrideFile("images/" + skinPath);
			if (skinFile == null || !skinFile.exists())
				return;
			NativeEntityApi.setMobSkin(entityId, skinPath);
		}
	}

	private static class AfterCapeDownloadAction implements Runnable {
		private long entityId;
		private String skinPath;

		public AfterCapeDownloadAction(long entityId, String skinPath) {
			this.entityId = entityId;
			this.skinPath = skinPath;
		}

		public void run() {
			try {
				File skinFile = getTextureOverrideFile("images/" + skinPath);
				if (skinFile == null || !skinFile.exists()) return;
				NativeEntityApi.setCape(entityId, skinPath);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static class TextureRequests {
		public String[] names;
		public int[] coords;
	}
}
