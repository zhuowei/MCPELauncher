package net.zhuoweizhang.mcpelauncher;

import java.io.FileReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
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

import java.util.zip.*;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;
import org.mozilla.javascript.*;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSStaticFunction;

import org.json.*;

import com.mojang.minecraftpe.MainActivity;

import net.zhuoweizhang.mcpelauncher.api.modpe.*;
import net.zhuoweizhang.mcpelauncher.texture.AtlasMeta;

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

	private static ModernWrapFactory modernWrapFactory = new ModernWrapFactory();

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
	public static AtlasMeta terrainMeta, itemsMeta;
	public static boolean hasLevel = false;
	public static int requestLeaveGameCounter = 0;
	public static boolean requestScreenshot = false;
	public static boolean requestSelectLevelHasSetScreen = false;

	public static final int ARCH_ARM = 0;
	public static final int ARCH_I386 = 1;
	public static int ITEM_ID_COUNT = 512;

	public static void loadScript(Reader in, String sourceName) throws IOException {
		if (!scriptingInitialized)
			return;
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

	public static void loadScript(File file) throws IOException {
		if (isClassGenMode()) {
			if (!scriptingInitialized)
				return;
			if (!scriptingEnabled)
				throw new RuntimeException("Not available in multiplayer");
			loadScriptFromInstance(ScriptTranslationCache.get(androidContext, file), file.getName());
			return;
		}
		if (isPackagedScript(file)) {
			loadPackagedScript(file);
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
			ScriptableObject.putProperty(scope, "ChatColor",
					classConstantsToJSObject(ChatColor.class));
			ScriptableObject.putProperty(scope, "ItemCategory",
					classConstantsToJSObject(ItemCategory.class));
			ScriptableObject.defineClass(scope, NativeBlockApi.class);
			ScriptableObject.defineClass(scope, NativeServerApi.class);
			RendererManager.defineClasses(scope);
			ScriptableObject.putProperty(scope, "ParticleType",
					classConstantsToJSObject(ParticleType.class));
			ScriptableObject.putProperty(scope, "EntityType",
					classConstantsToJSObject(EntityType.class));
			ScriptableObject.putProperty(scope, "EntityRenderType",
					classConstantsToJSObject(EntityRenderType.class));
			ScriptableObject.putProperty(scope, "ArmorType",
					classConstantsToJSObject(ArmorType.class));
			ScriptableObject.putProperty(scope, "MobEffect",
					classConstantsToJSObject(MobEffect.class));
		} catch (Exception e) {
			e.printStackTrace();
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
					e.printStackTrace();
					reportScriptError(state, e);
				}
			}
		}
	}

	public static void useItemOnCallback(int x, int y, int z, int itemid, int blockid, int side,
			int itemDamage, int blockDamage) {
		callScriptMethod("useItem", x, y, z, itemid, blockid, side, itemDamage, blockDamage);
	}

	public static void destroyBlockCallback(int x, int y, int z, int side) {
		callScriptMethod("destroyBlock", x, y, z, side);
	}

	public static void startDestroyBlockCallback(int x, int y, int z, int side) {
		callScriptMethod("startDestroyBlock", x, y, z, side);
	}

	public static void setLevelCallback(boolean hasLevel, boolean isRemoteAAAAAA) {
		if (nativeGetArch() == ARCH_I386) nextTickCallsSetLevel = true;
	}
	public static void setLevelFakeCallback(boolean hasLevel, boolean isRemote) {
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

		callScriptMethod("newLevel", hasLevel);
		if (MainActivity.currentMainActivity != null) {
			MainActivity main = MainActivity.currentMainActivity.get();
			if (main != null) {
				main.setLevelCallback(!ScriptManager.scriptingEnabled);
			}
		}
	}

	public static void selectLevelCallback(String wName, String wDir) {
		System.out.println("World name: " + wName);
		System.out.println("World dir: " + wDir);

		worldName = wName;
		worldDir = wDir;
		scriptingEnabled = true;
		isRemote = false;
		callScriptMethod("selectLevelHook");
		if (nativeGetArch() != ARCH_I386) nextTickCallsSetLevel = true;
	}

	public static void leaveGameCallback(boolean thatboolean) {
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
		serverAddress = null;
		serverPort = 0;
	}

	public static void attackCallback(long attacker, long victim) {
		callScriptMethod("attackHook", attacker, victim);
	}

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
			//nativeScreenChooserSetScreen(3);
			nativeLeaveGame(false);
			//nativeScreenChooserSetScreen(1);
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
		// runDownloadCallbacks();
	}

	private static void updatePlayerOrientation() {
		nativeSetRot(nativeGetPlayerEnt(), newPlayerYaw, newPlayerPitch);
	}

	public static void chatCallback(String str) {
		if (isRemote)
			nameAndShame(str);
		if (str == null || str.length() < 1)
			return;
		callScriptMethod("chatHook", str);
		if (str.charAt(0) != '/')
			return;
		callScriptMethod("procCmd", str.substring(1));
		if (!isRemote) {
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
	public static void mobDieCallback(long attacker, long victim) {
		callScriptMethod("deathHook", attacker == -1 ? -1 : attacker, victim);
	}

	// Other nonstandard callbacks
	public static void entityRemovedCallback(long entity) {
		if (NativePlayerApi.isPlayer(entity)) {
			playerRemovedHandler(entity);
		}
		int entityIndex = allentities.indexOf(entity);
		if (entityIndex >= 0) allentities.remove(entityIndex);
		callScriptMethod("entityRemovedHook", entity);
		// entityList.remove(Integer.valueOf(entity));
	}

	public static void entityAddedCallback(long entity) {
		System.out.println("Entity added: " + entity + " entity type: " + NativeEntityApi.getEntityTypeId(entity));
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

	public static void rakNetConnectCallback(String hostname, int port) {
		Log.i("BlockLauncher", "Connecting to " + hostname + ":" + port);
		ScriptManager.scriptingEnabled = ScriptManager.isLocalAddress(hostname);
		Log.i("BlockLauncher", "Scripting is now " + (scriptingEnabled? "enabled" : "disabled"));
		serverAddress = hostname;
		serverPort = port;
		ScriptManager.isRemote = true;
		if (MainActivity.currentMainActivity != null) {
			MainActivity main = MainActivity.currentMainActivity.get();
			if (main != null) {
				main.setLevelCallback(!ScriptManager.scriptingEnabled);
			}
		}
	}

	public static void frameCallback() {
		if (requestReloadAllScripts) {
			requestReloadAllScripts = false;
			try {
				if (!new File("/sdcard/mcpelauncher_do_not_create_placeholder_blocks").exists()) {
					nativeDefinePlaceholderBlocks();
				}
				MobEffect.initIds();
				loadEnabledScripts();
			} catch (Exception e) {
				e.printStackTrace();
				reportScriptError(null, e);
			}
		}
		if (requestSelectLevel != null && !requestLeaveGame) {
			if (!requestSelectLevelHasSetScreen) {
				nativeShowProgressScreen();
				requestSelectLevelHasSetScreen = true;
				nativeRequestFrameCallback();
			} else {
				nativeSelectLevel(requestSelectLevel.dir);
				requestSelectLevel = null;
				requestSelectLevelHasSetScreen = false;
			}
		}
		if (requestScreenshot) {
			ScreenshotHelper.takeScreenshot(screenshotFileName);
			requestScreenshot = false;
		}
	}

	public static void handleChatPacketCallback(String str) {
		if (str == null || str.length() < 1)
			return;
		callScriptMethod("serverMessageReceiveHook", str);
		if (BuildConfig.DEBUG) {
			System.out.println("message: " + str);
		}
	}

	public static void handleMessagePacketCallback(String sender, String str) {
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

	public static void explodeCallback(long entity, float x, float y, float z, float power, boolean onFire) {
		callScriptMethod("explodeHook", entity, x, y, z, power, onFire);
	}

	public static void init(android.content.Context cxt) throws IOException {
		scriptingInitialized = true;
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
		requestReloadAllScripts = true;
		nativeRequestFrameCallback();
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

		for (int i = scripts.size() - 1; i >= 0; i--) {
			if (scripts.get(i).name.equals(scriptId)) {
				scripts.remove(i);
				break;
			}
		}
	}

	public static void reloadScript(File file) throws IOException {
		removeScript(file.getName());
		loadScript(file);
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
		loadEnabledScriptsNames(androidContext);
		for (String name : enabledScripts) {
			// load all scripts into the script interpreter
			File file = getScriptFile(name);
			if (!file.exists() || !file.isFile()) {
				Log.i("BlockLauncher", "ModPE script " + file.toString() + " doesn't exist");
				continue;
			}
			loadScript(file);
		}
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

	private static void wordWrapClientMessage(String msg) {
		String[] portions = msg.split("\n");
		for (int i = 0; i < portions.length; i++) {
			String line = portions[i];

			if (msg.indexOf(ChatColor.BEGIN) >= 0) {
				// TODO: properly word wrap colour codes
				nativeClientMessage(line);
				continue;
			}

			while (line.length() > 40) {
				String newStr = line.substring(0, 40);// colorCodeSubstring(line,
														// 0, 40);
				nativeClientMessage(newStr);
				line = line.substring(newStr.length());
			}
			if (line.length() > 0) {
				nativeClientMessage(line);
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

	// end method dumping code

	private static boolean isLocalAddress(String str) {
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

	public static File getTextureOverrideFile(String textureName) {
		if (androidContext == null) return null;
		File stagingTextures = new File(androidContext.getExternalFilesDir(null), "textures");
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
		for (int i = 0; i < ITEM_ID_COUNT; i++) {
			String itemName = nativeGetItemName(i, 0, true);
			if (itemName == null)
				continue;
			boolean success = nativeGetTextureCoordinatesForItem(i, 0, textureUVbuf);
			String itemIcon = Arrays.toString(textureUVbuf).replace("[", "").replace("]", "")
					.replace(",", "|");
			out.println(i + "," + itemName + "," + itemIcon);
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
		String uuid = entityUUIDMap.get(entityId);
		if (uuid != null) return uuid;
		long[] uuidParts = nativeEntityGetUUID(entityId);
		if (uuidParts == null) return null;
		long lsb = Long.reverseBytes(uuidParts[0]);
		long msb = Long.reverseBytes(uuidParts[1]);
		UUID uuidObj = new UUID(lsb, msb);
		System.out.println(uuidObj);
		if (uuidObj.version() != 4) throw new RuntimeException("Invalid entity UUID");
		uuid = uuidObj.toString();
		entityUUIDMap.put(entityId, uuid);
		return uuid;
	}

	private static boolean isPackagedScript(File file) {
		return file.getName().toLowerCase().endsWith(".modpkg");
	}

	private static void loadPackagedScript(File file) throws IOException {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);

			MpepInfo info = null;
			boolean scrambled = false;
			try {
				info = MpepInfo.fromZip(zipFile);
				scrambled = info.scrambleCode.length() > 0;
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
		} finally {
			if (zipFile != null) zipFile.close();
		}
	}

	private static void verifyBlockTextures(TextureRequests requests) {
		if (terrainMeta == null) return;
		for (int i = 0; i < requests.names.length; i++) {
			if (!terrainMeta.hasIcon(requests.names[i], requests.coords[i])) {
				throw new IllegalArgumentException("The requested block texture " +
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

	public static native float nativeGetPlayerLoc(int axis);

	public static native long nativeGetPlayerEnt();

	public static native long nativeGetLevel();

	public static native void nativeSetPosition(long entityId, float x, float y, float z);

	public static native void nativeSetVel(long entityId, float vel, int axis);

	public static native void nativeExplode(float x, float y, float z, float radius);

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

	public static native void nativeDefineFoodItem(int itemId, String iconName, int iconId,
			int hearts, String name, int maxStackSize);

	// nonstandard
	public static native void nativeSetFov(float degrees, boolean override);

	public static native void nativeSetMobSkin(long ent, String str);

	public static native float nativeGetEntityLoc(long entity, int axis);

	public static native void nativeRemoveEntity(long entityId);

	public static native int nativeGetEntityTypeId(long entityId);

	public static native void nativeSetAnimalAge(long entityId, int age);

	public static native int nativeGetAnimalAge(long entityId);

	public static native void nativeSelectLevel(String levelName);

	public static native void nativeLeaveGame(boolean saveMultiplayerWorld);

	public static native void nativeJoinServer(String serverAddress, int serverPort);

	public static native void nativeSetGameSpeed(float ticksPerSecond);

	public static native void nativeGetAllEntities();

	public static native int nativeGetSelectedSlotId();

	public static native int nativeGetMobHealth(long entityId);

	public static native void nativeSetMobHealth(long entityId, int halfhearts);

	public static native void nativeSetEntityRenderType(long entityId, int renderType);

	public static native void nativeRequestFrameCallback();

	public static native String nativeGetSignText(int x, int y, int z, int line);

	public static native void nativeSetSignText(int x, int y, int z, int line, String text);

	public static native void nativeSetSneaking(long entityId, boolean doIt);
	public static native boolean nativeIsSneaking(long entityId);

	public static native String nativeGetPlayerName(long entityId);

	public static native String nativeGetItemName(int itemId, int itemDamage, boolean raw);

	public static native boolean nativeGetTextureCoordinatesForItem(int itemId, int itemDamage,
			float[] output);

	public static native void nativeDefineBlock(int blockId, String name, String[] textureNames,
			int[] textureCoords, int materialSourceId, boolean opaque, int renderType);

	public static native void nativeBlockSetDestroyTime(int blockId, float amount);

	public static native void nativeBlockSetExplosionResistance(int blockId, float amount);

	public static native void nativeBlockSetStepSound(int blockId, int sourceBlockId);

	public static native void nativeBlockSetLightLevel(int blockId, int level);
	public static native void nativeBlockSetLightOpacity(int blockId, int level);

	public static native void nativeBlockSetColor(int blockId, int[] colors);

	public static native void nativeBlockSetShape(int blockId, float v1, float v2, float v3,
			float v4, float v5, float v6);

	public static native void nativeBlockSetRenderLayer(int blockId, int renderLayer);

	public static native void nativeSetInventorySlot(int slot, int id, int count, int damage);

	public static native float nativeGetEntityVel(long entity, int axis);

	public static native void nativeSetI18NString(String key, String value);

	public static native void nativeAddShapedRecipe(int id, int count, int damage, String[] shape,
			int[] ingredients);

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
	public static native void nativeLevelAddParticle(int type, float x, float y, float z, float xVel, float yVel, float zVel, int data);

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

	public static native int nativeGetSlotArmor(int slot, int type);

	public static native void nativeSetArmorSlot(int slot, int id, int damage);

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

	public static native void nativeDefinePlaceholderBlocks();

	public static native long nativePlayerGetPointedEntity();

	public static native int nativePlayerGetPointedBlock(int type);

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

	// setup
	public static native void nativeSetupHooks(int versionCode);

	public static native void nativeRemoveItemBackground();

	public static native void nativeSetTextParseColorCodes(boolean doIt);

	public static native void nativePrePatch(boolean signalhandler, MainActivity activity);

	public static native boolean nativeLevelIsRemote();

	public static native void nativeSetIsRecording(boolean yep);

	public static native void nativeForceCrash();
	public static native int nativeGetArch();
	public static native void nativeSetUseController(boolean controller);
	public static native void nativeDumpVtable(String name, int size);
	public static native int nativeGetItemIdCount();
	public static native void nativeSetExitEnabled(boolean enabled);

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
		public void explode(double x, double y, double z, double radius) {
			nativeExplode((float) x, (float) y, (float) z, (float) radius);
		}

		@JSFunction
		public void addItemInventory(int id, int amount, int damage) {
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
			long entityId = nativeSpawnEntity((float) x, (float) y, (float) z, 10, tex);
			return entityId;
		}

		@JSFunction
		public long spawnCow(double x, double y, double z, String tex) {
			if (invalidTexName(tex)) {
				tex = "mob/cow.png";
			}
			long entityId = nativeSpawnEntity((float) x, (float) y, (float) z, 11, tex);
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
			nativeSetTile(x, y, z, id, damage);
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
			return nativeGetTile(x, y, z);
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
				tex = "mob/pigzombie.png";
			}
			long entityId = nativeSpawnEntity((float) x, (float) y, (float) z, 36, tex);
			nativeSetCarriedItem(entityId, item, 1, 0);
			return entityId;
		}

		// nonstandard methods

		@JSFunction
		public long bl_spawnMob(double x, double y, double z, int typeId, String tex) {
			if (invalidTexName(tex)) {
				tex = null;
			}
			long entityId = nativeSpawnEntity((float) x, (float) y, (float) z, typeId, tex);
			return entityId;
		}

		@JSFunction
		public void bl_setMobSkin(Object entityId, String tex) {
			nativeSetMobSkin(getEntityId(entityId), tex);
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
			return nativeGetTile(x, y, z);
		}

		@JSStaticFunction
		public static void explode(double x, double y, double z, double radius) {
			nativeExplode((float) x, (float) y, (float) z, (float) radius);
		}

		@JSStaticFunction
		public static void setTile(int x, int y, int z, int id, int damage) {
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
				tex = "mob/chicken.png";
			}
			long entityId = nativeSpawnEntity((float) x, (float) y, (float) z, 10, tex);
			return entityId;
		}

		@JSStaticFunction
		public static long spawnCow(double x, double y, double z, String tex) {
			if (invalidTexName(tex)) {
				tex = "mob/cow.png";
			}
			long entityId = nativeSpawnEntity((float) x, (float) y, (float) z, 11, tex);
			return entityId;
		}

		// nonstandard methods

		@JSStaticFunction
		public static long spawnMob(double x, double y, double z, int typeId, String tex) {
			if (invalidTexName(tex)) {
				tex = null;
			}
			long entityId = nativeSpawnEntity((float) x, (float) y, (float) z, typeId, tex);
			return entityId;
		}

		@JSStaticFunction
		public static String getSignText(int x, int y, int z, int line) {
			if (line < 0 || line >= 4)
				throw new RuntimeException("Invalid line for sign: must be in the range of 0 to 3");
			return nativeGetSignText(x, y, z, line);
		}

		@JSStaticFunction
		public static void setSignText(int x, int y, int z, int line, String newText) {
			if (line < 0 || line >= 4)
				throw new RuntimeException("Invalid line for sign: must be in the range of 0 to 3");
			nativeSetSignText(x, y, z, line, newText);
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
			return nativeDropItem((float) x, (float) y, (float) z, (float) range, id, count, damage);
		}

		@JSStaticFunction
		public static void setGameMode(int type) {
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
			int itmId = getTile(x, y, z);
			int itmDmg = getData(x, y, z);

			nativeDestroyBlock(x, y, z);
			if (shouldDrop)
				dropItem(((double) x) + 0.5, y, ((double) z) + 0.5, 1, itmId, 1, itmDmg);
		}

		@JSStaticFunction
		public static void setChestSlot(int x, int y, int z, int slot, int id, int damage,
				int amount) {
			nativeAddItemChest(x, y, z, slot, id, damage, amount);
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

		// KsyMC's additions
		@JSStaticFunction
		public static void playSound(double x, double y, double z, String sound, double volume,
				double pitch) {
			nativePlaySound((float) x, (float) y, (float) z, sound, (float) volume, (float) pitch);
		}

		@JSStaticFunction
		public static void playSoundEnt(Object ent, String sound, double volume, double pitch) {
			float x = nativeGetEntityLoc(getEntityId(ent), AXIS_X);
			float y = nativeGetEntityLoc(getEntityId(ent), AXIS_Y);
			float z = nativeGetEntityLoc(getEntityId(ent), AXIS_Z);

			nativePlaySound(x, y, z, sound, (float) volume, (float) pitch);
		}

		// Byteandahalf's additions
		@JSStaticFunction
		public static int getBrightness(int x, int y, int z) {
			return nativeGetBrightness(x, y, z);
		}

		@JSStaticFunction
		public static void setFurnaceSlot(int x, int y, int z, int slot, int id, int damage,
				int amount) {
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
		public static void addParticle(int type, double x, double y, double z, double xVel, double yVel, double zVel, int size) {
			if (type < 0 || type > 25) throw new RuntimeException("Invalid particle type " + type + ": should be between 0 and 25");
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
			nativeAddItemInventory(id, amount, damage);
		}

		// nonstandard
		@JSStaticFunction
		public static void setHealth(int value) {
			nativeHurtTo(value);
		}

		@JSStaticFunction
		public static int getSelectedSlotId() {
			return nativeGetSelectedSlotId();
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
			nativeAddItemCreativeInv(id, count, damage);
		}

		// InusualZ's additions

		@JSStaticFunction
		public static int getArmorSlot(int slot) {
			return nativeGetSlotArmor(slot, ITEMID);
		}

		@JSStaticFunction
		public static int getArmorSlotDamage(int slot) {
			return nativeGetSlotArmor(slot, DAMAGE);
		}

		@JSStaticFunction
		public static void setArmorSlot(int slot, int id, int damage) {
			nativeSetArmorSlot(slot, id, damage);
		}

		@JSStaticFunction
		public static String getName(Object ent) {
			if (!isPlayer(ent))
				return "Not a player";
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

		/*
		 * @JSStaticFunction public static void setInventorySlot(int slot, int
		 * itemId, int count, int damage) { nativeSetInventorySlot(slot, itemId,
		 * count, damage); }
		 */

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

		@Override
		public String getClassName() {
			return "Player";
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
			long entityId = nativeSpawnEntity((float) x, (float) y, (float) z, typeId, tex);
			return entityId;
		}

		@JSStaticFunction
		public static void setAnimalAge(Object animal, int age) {
			nativeSetAnimalAge(getEntityId(animal), age);
		}

		@JSStaticFunction
		public static int getAnimalAge(Object animal) {
			return nativeGetAnimalAge(getEntityId(animal));
		}

		@JSStaticFunction
		public static void setMobSkin(Object entity, String tex) {
			nativeSetMobSkin(getEntityId(entity), tex);
		}

		@JSStaticFunction
		public static void remove(Object ent) {
			nativeRemoveEntity(getEntityId(ent));
		}

		@JSStaticFunction
		public static int getHealth(Object ent) {
			return nativeGetMobHealth(getEntityId(ent));
		}

		@JSStaticFunction
		public static void setHealth(Object ent, int halfhearts) {
			nativeSetMobHealth(getEntityId(ent), halfhearts);
		}

		@JSStaticFunction
		public static void setRenderType(Object ent, int renderType) {
			nativeSetEntityRenderType(getEntityId(ent), renderType);
		}

		@JSStaticFunction
		public static void setSneaking(Object ent, boolean doIt) {
			nativeSetSneaking(getEntityId(ent), doIt);
		}

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
			return nativeEntityGetMobSkin(getEntityId(entity));
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
			if (id < 0 || id >= ITEM_ID_COUNT) {
				throw new IllegalArgumentException("Item IDs must be >= 0 and < ITEM_ID_COUNT");
			}
			if (itemsMeta != null && !itemsMeta.hasIcon(iconName, iconSubindex)) {
				throw new IllegalArgumentException("The item icon " + iconName + ":" + iconSubindex + " does not exist");
			}
			nativeDefineItem(id, iconName, iconSubindex, name, maxStackSize);
		}

		@JSStaticFunction
		public static void setFoodItem(int id, String iconName, int iconSubindex, int halfhearts,
				String name, int maxStackSize) {
			try {
				Integer.parseInt(iconName);
				throw new IllegalArgumentException("The item icon for " + name.trim()
					+ " is not updated for 0.8.0. Please ask the script author to update");
			} catch (NumberFormatException e) {
			}
			if (id < 0 || id >= ITEM_ID_COUNT) {
				throw new IllegalArgumentException("Item IDs must be >= 0 and < " + ITEM_ID_COUNT);
			}
			if (itemsMeta != null && !itemsMeta.hasIcon(iconName, iconSubindex)) {
				throw new IllegalArgumentException("The item icon " + iconName + ":" + iconSubindex + " does not exist");
			}
			nativeDefineFoodItem(id, iconName, iconSubindex, halfhearts, name, maxStackSize);
		}

		// nonstandard

		@JSStaticFunction
		public static void selectLevel(String levelDir) {
			throw new RuntimeException("FIXME 0.11");
			/*
			if (levelDir.equals(ScriptManager.worldDir)) {
				System.err.println("Attempted to load level that is already loaded - ignore");
				return;
			}
			setRequestLeaveGame();
			// nativeSelectLevel(levelDir);
			requestSelectLevel = new SelectLevelRequest();
			requestSelectLevel.dir = levelDir;
			*/
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
			screenshotFileName = fileName.replace("/", "").replace("\\", "");
			nativeRequestFrameCallback();
		}

		@JSStaticFunction
		public static void langEdit(String key, String value) {
			nativeSetI18NString(key, value);
		}

		@JSStaticFunction
		public static void showTipMessage(String msg) {
			nativeShowTipMessage(msg);
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

		@JSStaticFunction
		public static byte[] getBytesFromTexturePack(String name) {
			if (MainActivity.currentMainActivity != null) {
				MainActivity main = MainActivity.currentMainActivity.get();
				if (main != null) {
					return main.getFileDataBytes(name);
				}
			}
			return null;
		}

		@JSStaticFunction
		public static InputStream openInputStreamFromTexturePack(String name) {
			if (MainActivity.currentMainActivity != null) {
				MainActivity main = MainActivity.currentMainActivity.get();
				if (main != null) {
					return main.getInputStreamForAsset(name);
				}
			}
			return null;
		}

		@JSStaticFunction
		public static void dumpVtable(String className, int size) {
			nativeDumpVtable("_ZTV" + className.length() + className, size);
		}

		@Override
		public String getClassName() {
			return "ModPE";
		}
	}

	private static class NativeItemApi extends ScriptableObject {
		public NativeItemApi() {
		}
		
		@JSStaticFunction
		public static String getName(int id, int damage, boolean raw) {
			return nativeGetItemName(id, damage, raw);
		}

		@JSStaticFunction
		public static void addCraftRecipe(int id, int count, int damage, Scriptable ingredientsScriptable) {
			int[] expanded = expandShapelessRecipe(ingredientsScriptable);
			StringBuilder temprow = new StringBuilder();
			char nextchar = 'a';
			int[] ingredients = new int[expanded.length];
			for (int i = 0; i < expanded.length; i+=3) {
				int inputid = expanded[i];
				int inputcount = expanded[i + 1];
				int inputdamage = expanded[i + 2];
				char mychar = nextchar++;
				for (int a = 0; a < inputcount; a++) {
					temprow.append(mychar);
				}
				ingredients[i] = mychar;
				ingredients[i + 1] = inputid;
				ingredients[i + 2] = inputdamage;
			}
			int temprowLength = temprow.length();
			if (temprowLength > 9) {
				scriptPrint("Too many ingredients in shapeless recipe: max of 9 slots" +
					", the extra items have been ignored");
				temprow.delete(9, temprow.length());
				temprowLength = temprow.length();
			}
			// if the temp row is <= 4, make a 2x2 recipe, otherwise, make a 3x3 recipe
			int width = (temprowLength <= 4? 2: 3);
			String[] shape = new String[temprowLength / width + (temprowLength % width != 0? 1: 0)];
			for (int i = 0; i < shape.length; i++) {
				int begin = i * width;
				int end = begin + width;
				if (end > temprowLength) end = temprowLength;
				shape[i] = temprow.substring(begin, end);
			}
			verifyAndAddShapedRecipe(id, count, damage, shape, ingredients);
		}

		@JSStaticFunction
		public static void addFurnaceRecipe(int inputId, int outputId, int outputDamage) {
			// Do I need a count? If not, should I just fill it with null, or
			// skip it completely?
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
			if (id < 0 || id >= ITEM_ID_COUNT) {
				throw new RuntimeException("Invalid result in recipe: " + id + ": must be between 0 and " + ITEM_ID_COUNT);
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
			nativeAddShapedRecipe(id, count, damage, shape, ingredients);
		}

		@JSStaticFunction
		public static void setMaxDamage(int id, int maxDamage) {
			nativeSetItemMaxDamage(id, maxDamage);
		}

		/* This doesn't actually work, and has never worked, but people wanted it to be re-added. I dunno why. */
		@JSStaticFunction
		public static void setCategory(int id, int category, int whatever) {
			nativeSetItemCategory(id, category, whatever);
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
			if (!(armorType >= 0 && armorType <= 3)) {
				throw new RuntimeException("Invalid armor type: use ArmorType.helmet, ArmorType.chestplate," +
					"ArmorType.leggings, or ArmorType.boots");
			}
			if (id < 0 || id >= ITEM_ID_COUNT) {
				throw new IllegalArgumentException("Item IDs must be >= 0 and < " + ITEM_ID_COUNT);
			}
			if (itemsMeta != null && !itemsMeta.hasIcon(iconName, iconIndex)) {
				throw new IllegalArgumentException("The item icon " + iconName + ":" + iconIndex + " does not exist");
			}
			nativeDefineArmor(id, iconName, iconIndex, name,
				texture, damageReduceAmount, maxDamage, armorType);
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
			if (blockId < 0 || blockId >= 256) {
				throw new IllegalArgumentException("Block IDs must be >= 0 and < 256");
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
			TextureRequests finalTextures = expandTexturesArray(textures);
			verifyBlockTextures(finalTextures);
			nativeDefineBlock(blockId, name, finalTextures.names, finalTextures.coords,
					materialSourceId, opaque, renderType);
		}

		@JSStaticFunction
		public static void setDestroyTime(int blockId, double time) {
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
				double v5, double v6) {
			nativeBlockSetShape(blockId, (float) v1, (float) v2, (float) v3, (float) v4,
					(float) v5, (float) v6);
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
			nativeBlockSetRenderLayer(blockId, layer);
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
			if (!isRemote)
				return;
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
			long[] players = new long[allplayers.size()];
			for(int n=0;players.length>n;n++){
				players[n]=allplayers.get(n);
			}
			return players;
		}
		
		@JSStaticFunction
		public static String[] getAllPlayerNames() {
			String[] players = new String[allplayers.size()];
			for(int n=0;players.length>n;n++){
				players[n]=nativeGetPlayerName(allplayers.get(n));
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
