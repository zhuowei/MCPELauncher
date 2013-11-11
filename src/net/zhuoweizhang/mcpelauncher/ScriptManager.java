package net.zhuoweizhang.mcpelauncher;

import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;

import java.lang.reflect.Method;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

import org.mozilla.javascript.*;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSStaticFunction;

import com.mojang.minecraftpe.MainActivity;

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

	public static String screenshotFileName = "";

	//public static Queue<Runnable> mainThreadRunnableQueue = new ArrayDeque<Runnable>();

	public static void loadScript(Reader in, String sourceName) throws IOException {
		if (!scriptingEnabled) throw new RuntimeException("Not available in multiplayer");
		//Rhino needs lots of recursion depth to parse nested else ifs
		//dalvik vm/Thread.h specifies 256K as maximum stack size
		//default thread depth is 16K (8K on old devices, 1K on super-low-end devices)
		Thread t = new Thread(Thread.currentThread().getThreadGroup(), new ParseThread(in, sourceName), 
			"BlockLauncher parse thread", 256*1024);
		t.start();
		try {
			t.join(); //block on this thread
		} catch (InterruptedException ie) {
			//shouldn't happen
		}
	}

	private static class ParseThread implements Runnable {
		private Reader in;
		private String sourceName;
		public ParseThread(Reader in, String sourceName) {
			this.in = in;
			this.sourceName = sourceName;
		}
		public void run() {
			try {
				Context ctx = Context.enter();
				Script script = ctx.compileReader(in, sourceName, 0, null);
				initJustLoadedScript(ctx, script, sourceName);
				Context.exit();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void loadScript(File file) throws IOException {
		Reader in = null;
		try {
			in = new FileReader(file);
			loadScript(in, file.getName());
		} finally {
			if (in != null) in.close();
		}
	}

	public static void initJustLoadedScript(Context ctx, Script script, String sourceName) {
		Scriptable scope = ctx.initStandardObjects(new BlockHostObject(), false);
		ScriptState state = new ScriptState(script, scope, sourceName);
		String[] names = getAllJsFunctions(BlockHostObject.class);
		((ScriptableObject) scope).defineFunctionProperties(names, BlockHostObject.class, ScriptableObject.DONTENUM);
		try {
			//NativeLevelApi levelApi = new NativeLevelApi();
			//levelApi.defineFunctionProperties(getAllJsFunctions(NativeLevelApi.class), NativeLevelApi.class, ScriptableObject.DONTENUM);
			//ScriptableObject.defineProperty(scope, "Level", levelApi, ScriptableObject.DONTENUM);
			ScriptableObject.defineClass(scope, NativePlayerApi.class);
			ScriptableObject.defineClass(scope, NativeLevelApi.class);
			ScriptableObject.defineClass(scope, NativeEntityApi.class);
			ScriptableObject.defineClass(scope, NativeModPEApi.class);
			ScriptableObject.putProperty(scope, "ChatColor", new NativeJavaClass(scope, ChatColor.class));
		} catch (Exception e) {
			e.printStackTrace();
			reportScriptError(state, e);
		}

		script.exec(ctx, scope);
		scripts.add(state);
	}

	public static void callScriptMethod(String functionName, Object... args) {
		if (!scriptingEnabled) return; //No script loading/callbacks when in a remote world
		Context ctx = Context.enter();
		for (ScriptState state: scripts) {
			if (state.errors >= MAX_NUM_ERRORS) continue; //Too many errors, skip
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

	public static void useItemOnCallback(int x, int y, int z, int itemid, int blockid, int side, int itemDamage, int blockDamage) {
		callScriptMethod("useItem", x, y, z, itemid, blockid, side, itemDamage, blockDamage);
	}

	public static void destroyBlockCallback(int x, int y, int z, int side) {
		callScriptMethod("destroyBlock", x, y, z, side);
	}

	public static void setLevelCallback(boolean hasLevel, boolean isRemote) {
		System.out.println("Level: " + hasLevel);
		ScriptManager.isRemote = isRemote;
		if (!isRemote) ScriptManager.scriptingEnabled = true; //all local worlds get ModPE support
		nativeSetGameSpeed(20.0f);
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
	}

	public static void leaveGameCallback(boolean thatboolean) {
		ScriptManager.isRemote = false;
		ScriptManager.scriptingEnabled = true;
		callScriptMethod("leaveGame");
		if (MainActivity.currentMainActivity != null) {
			MainActivity main = MainActivity.currentMainActivity.get();
			if (main != null) {
				main.leaveGameCallback();
			}
		}
	}

	public static void attackCallback(int attacker, int victim) {
		callScriptMethod("attackHook", new NativeEntity(attacker), new NativeEntity(victim));
	}

	public static void tickCallback() {
		callScriptMethod("modTick");
		//do we have any requests for graphics reset?
		if (requestedGraphicsReset) {
			nativeOnGraphicsReset();
			requestedGraphicsReset = false;
		}
		//any takers for rotating the player?
		if (sensorEnabled) updatePlayerOrientation();
		if (requestLeaveGame) {
			nativeLeaveGame(false);
			requestLeaveGame = false;
		}
		if (requestSelectLevel != null) {
			nativeSelectLevel(requestSelectLevel.dir);
			requestSelectLevel = null;
		}
		if (requestJoinServer != null) {
			nativeJoinServer(requestJoinServer.serverAddress, requestJoinServer.serverPort);
			requestJoinServer = null;
		}
		//runDownloadCallbacks();
	}

	private static void updatePlayerOrientation() {
		nativeSetRot(nativeGetPlayerEnt(), newPlayerYaw, newPlayerPitch);
	}

	public static void chatCallback(String str) {
		if (str == null || str.length() < 1 || str.charAt(0) != '/') return;
		callScriptMethod("procCmd", str.substring(1));
		if (!isRemote) nativePreventDefault();
	}

	// KsyMC's additions
	public static void mobDieCallback(int attacker, int victim) {
		callScriptMethod("deathHook", attacker == -1? null: new NativeEntity(attacker), new NativeEntity(victim));
	}

	//Other nonstandard callbacks
	public static void entityRemovedCallback(int entity) {
		callScriptMethod("entityRemovedHook", new NativeEntity(entity));
	}

	public static void entityAddedCallback(int entity) {
		callScriptMethod("entityAddedHook", new NativeEntity(entity));
	}

	public static void levelEventCallback(int player, int eventType, int x, int y, int z, int data) {
		callScriptMethod("levelEventHook", new NativeEntity(player), eventType, x, y, z, data);
	}

	public static void blockEventCallback(int x, int y, int z, int type, int data) {
		callScriptMethod("blockEventHook", x, y, z, type, data);
	}

	public static void rakNetConnectCallback(String hostname, int port) {
		Log.i("BlockLauncher", "Connecting to " + hostname + ":" + port);
		ScriptManager.scriptingEnabled = ScriptManager.isLocalAddress(hostname);
		Log.i("BlockLauncher", "Scripting is now " + (scriptingEnabled? "enabled" : "disabled"));
		
	}

	public static void frameCallback() {
		ScreenshotHelper.takeScreenshot(screenshotFileName);
	}

	public static void init(android.content.Context cxt) throws IOException {
		//set up hooks
		int versionCode = 0;
		try {
			versionCode = cxt.getPackageManager().getPackageInfo("com.mojang.minecraftpe", 0).versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			//impossible, as if the package isn't installed, the app would've quit before loading scripts
		}
		if (MinecraftVersion.isAmazon()) {
			versionCode = 0xaaaa;
		}
		nativeSetupHooks(versionCode);
		scripts.clear();
		androidContext = cxt.getApplicationContext();
		loadEnabledScripts();
	}

	public static void removeScript(String scriptId) {
		/* Don't clear data here - user can clear data by hand if needed
		SharedPreferences sPrefs = androidContext.getSharedPreferences("BlockLauncherModPEScript"+scriptId+"Data", 0);
		SharedPreferences.Editor prefsEditor = sPrefs.edit();
		prefsEditor.clear();
		prefsEditor.commit();
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
		if (state != null) state.errors++;
		if (MainActivity.currentMainActivity != null) {
			MainActivity main = MainActivity.currentMainActivity.get();
			if (main != null) {
				main.scriptErrorCallback(state == null? "Unknown script" : state.name, t);
				if (state != null && state.errors >= MAX_NUM_ERRORS) { //too many errors
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


	//following taken from the patch manager
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
		for (File file: files) {
			String name = file.getAbsolutePath();
			if (name == null || name.length() <= 0) continue;
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

	protected static void loadEnabledScripts() throws IOException {
		SharedPreferences sharedPrefs = androidContext.getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0);
		String enabledScriptsStr = sharedPrefs.getString("enabledScripts", "");
		enabledScripts = new HashSet<String>(Arrays.asList(enabledScriptsStr.split(";")));
		for (String name: enabledScripts) {
			//load all scripts into the script interpreter
			loadScript(getScriptFile(name));
		}
	}

	protected static void saveEnabledScripts() {
		SharedPreferences sharedPrefs = androidContext.getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0);
		SharedPreferences.Editor edit = sharedPrefs.edit();
		edit.putString("enabledScripts", join(enabledScripts.toArray(blankArray), ";"));
		edit.putInt("scriptManagerVersion", 1);
		edit.apply();
	}

	public static File getScriptFile(String scriptId) {
		File scriptsFolder = androidContext.getDir(SCRIPTS_DIR, 0);
		return new File(scriptsFolder, scriptId);
	}
	//end script manager controls

	private static String[] getAllJsFunctions(Class clazz) {
		List<String> allList = new ArrayList<String>();
		for (Method met: clazz.getMethods()) {
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
		//TODO: properly word wrap colour codes
		if (msg.indexOf(ChatColor.BEGIN) >= 0) {
			nativeClientMessage(msg);
			return;
		}

		String[] portions = msg.split("\n");
		for(int i = 0; i < portions.length; i++) {
			String line = portions[i];
			while(line.length() > 40) {
				String newStr = line.substring(0, 40);//colorCodeSubstring(line, 0, 40);
				nativeClientMessage(newStr);
				line = line.substring(newStr.length());
			}
			if (line.length() > 0) {
				nativeClientMessage(line);
			}
		}
	}

	/*private static String colorCodeSubstring(String line, int begin, int end) {
		int charsCounted = 0;
		int i;
		for (i = begin; i < line.length(); i++) {
			char myChar = line.charAt(i);
			if (myChar == ChatColor.BEGIN) {
				i++; //skip the next character as well
			} else {
				charsCounted++;
				if (charsCounted == requiredChars) {
					return line.substring(begin, i + 1);
				}
			}
		}
	}*/

	/** Returns a description of ALL the methods this ModPE runtime supports. */
	public static String getAllApiMethodsDescriptions() {
		StringBuilder builder = new StringBuilder();
		appendApiMethods(builder, BlockHostObject.class, null);
		appendApiMethods(builder, NativeModPEApi.class, "ModPE");
		appendApiMethods(builder, NativeLevelApi.class, "Level");
		appendApiMethods(builder, NativePlayerApi.class, "Player");
		appendApiMethods(builder, NativeEntityApi.class, "Entity");
		return builder.toString();
		
	}
	private static void appendApiMethods(StringBuilder builder, Class<?> clazz, String namespace) {
		for (Method met: clazz.getMethods()) {
			if (met.getAnnotation(JSFunction.class) != null || met.getAnnotation(JSStaticFunction.class) != null) {
				appendApiMethodDescription(builder, met, namespace);
			}
		}
		builder.append("\n");
	}

	private static void appendApiMethodDescription(StringBuilder builder, Method met, String namespace) {
		if (namespace != null) {
			builder.append(namespace);
			builder.append('.');
		}
		builder.append(met.getName());
		builder.append('(');
		Class[] params = met.getParameterTypes();
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
	//end method dumping code

	private static boolean isLocalAddress(String str) {
		//Use Java's built-in support for this
		try {
			InetAddress address = InetAddress.getByName(str);
			Log.i("BlockLauncher", str);
			boolean retval = address.isLoopbackAddress() || address.isLinkLocalAddress() ||
				address.isSiteLocalAddress();
			return retval;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static void takeScreenshot(String fileName) {
		screenshotFileName = fileName.replace("/", "").replace("\\", "");
		nativeRequestFrameCallback();
	}

	/*private static void runDownloadCallbacks() {
		Runnable message;
		while ((message = mainThreadRunnableQueue.poll()) != null) {
			message.run();
		}
	}*/

	private static void overrideTexture(String urlString, String textureName) {
		//download from URL
		//saves it to ext storage's texture folder
		//then, schedule callback
		if (urlString == "") {
			//clear this texture override
			clearTextureOverride(textureName);
			return;
		}
		try {
			URL url = new URL(urlString);
			new Thread(new ScriptTextureDownloader(url, getTextureOverrideFile(textureName))).start();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static File getTextureOverrideFile(String textureName) {
		File stagingTextures = new File(androidContext.getExternalFilesDir(null), "textures");
		return new File(stagingTextures, textureName.replace("..", ""));
	}

	private static void clearTextureOverrides() {
		File stagingTextures = new File(androidContext.getExternalFilesDir(null), "textures");
		Utils.clearDirectory(stagingTextures);
		requestedGraphicsReset = true;
	}

	private static void clearTextureOverride(String texture) {
		File file = getTextureOverrideFile(texture);
		if (file.exists()) {
			file.delete();
		}
		requestedGraphicsReset = true;
	}

	public static native float nativeGetPlayerLoc(int axis);
	public static native int nativeGetPlayerEnt();
	public static native long nativeGetLevel();
	public static native void nativeSetPosition(int entity, float x, float y, float z);
	public static native void nativeSetVel(int ent, float vel, int axis);
	public static native void nativeExplode(float x, float y, float z, float radius);
	public static native void nativeAddItemInventory(int id, int amount, int damage);
	public static native void nativeRideAnimal(int rider, int mount);
	public static native int nativeGetCarriedItem(int type);
	public static native void nativePreventDefault();
	public static native void nativeSetTile(int x, int y, int z, int id, int damage);
	public static native int nativeSpawnEntity(float x, float y, float z, int entityType, String skinPath);
	public static native void nativeClientMessage(String msg);

	//0.2
	public static native void nativeSetNightMode(boolean isNight);
	public static native int nativeGetTile(int x, int y, int z);
	public static native void nativeSetPositionRelative(int entity, float x, float y, float z);
	public static native void nativeSetRot(int ent, float yaw, float pitch);
	//0.3
	public static native float nativeGetYaw(int ent);
	public static native float nativeGetPitch(int ent);

	//0.4
	public static native void nativeSetCarriedItem(int ent, int id, int count, int damage);

	//0.5
	public static native void nativeOnGraphicsReset();

	//0.6
	public static native void nativeDefineItem(int itemId, int iconId, String name);
	public static native void nativeDefineFoodItem(int itemId, int iconId, int hearts, String name);

	//nonstandard
	public static native void nativeSetFov(float degrees);
	public static native void nativeSetMobSkin(int ent, String str);
	public static native float nativeGetEntityLoc(int entity, int axis);
	public static native void nativeRemoveEntity(int entityId);
	public static native int nativeGetEntityTypeId(int entityId);
	public static native void nativeSetAnimalAge(int entityId, int age);
	public static native int nativeGetAnimalAge(int entityId);
	public static native void nativeSelectLevel(String levelName);
	public static native void nativeLeaveGame(boolean saveMultiplayerWorld);
	public static native void nativeJoinServer(String serverAddress, int serverPort);
	public static native void nativeSetGameSpeed(float ticksPerSecond);
	public static native void nativeGetAllEntities();
	public static native int nativeGetSelectedSlotId();
	public static native int nativeGetMobHealth(int entityId);
	public static native void nativeSetMobHealth(int entityId, int halfhearts);
	public static native void nativeSetEntityRenderType(int entityId, int renderType);
	public static native void nativeRequestFrameCallback();
	public static native String nativeGetSignText(int x, int y, int z, int line);
	public static native void nativeSetSignText(int x, int y, int z, int line, String text);

	// MrARM's additions
	public static native int nativeGetData(int x, int y, int z);
	public static native void nativeHurtTo(int to);
	public static native void nativeDestroyBlock(int x, int y, int z);
	public static native long nativeGetTime();
	public static native void nativeSetTime(long time);
	public static native int nativeGetGameType();
	public static native void nativeSetGameType(int type);
	public static native void nativeSetOnFire(int entity, int howLong);
	public static native void nativeSetSpawn(int x, int y, int z);
	public static native void nativeAddItemChest(int x, int y, int z, int slot, int id, int damage, int amount);
	public static native int nativeGetItemChest(int x, int y, int z, int slot);
	public static native int nativeGetItemDataChest(int x, int y, int z, int slot);
	public static native int nativeGetItemCountChest(int x, int y, int z, int slot);
	public static native void nativeDropItem(float x, float y, float z, float range, int id, int count, int damage);

	// KsyMC's additions
	public static native void nativePlaySound(float x, float y, float z, String sound, float volume, float pitch);
	public static native void nativeClearSlotInventory(int slot);
	public static native int nativeGetSlotInventory(int slot, int type);

	//InusualZ's additions
	public static native void nativeExtinguishFire(int x, int y, int z, int side);
	public static native int nativeGetSlotArmor(int slot, int type);
	public static native void nativeSetArmorSlot(int slot, int id, int damage);
	
	//setup
	public static native void nativeSetupHooks(int versionCode);
	public static native void nativeRemoveItemBackground();
	public static native void nativeSetTextParseColorCodes(boolean doIt);

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

	/*
	 To contributors: if you are adding a BlockLauncher-specific method, please
	 add it to one of the namespaces (Entity, Level, ModPE, Player)
	 instead of the top-level namespace.
	 thanks.
	 e.g. Entity.fireLaz0rs = good, fireLaz0rs = bad, bl_fireLaz0rs = bad
	*/

	private static class BlockHostObject extends ScriptableObject {
		private NativeEntity playerEnt = new NativeEntity(0);
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
		public NativeEntity getPlayerEnt() {
			playerEnt.entityId = nativeGetPlayerEnt();
			return playerEnt;
		}
		@JSFunction
		public NativePointer getLevel() {
			return new NativePointer(nativeGetLevel()); //TODO: WTF does this do?
		}

		@JSFunction
		public void setPosition(NativeEntity ent, double x, double y, double z) {
			nativeSetPosition(ent.entityId, (float) x, (float) y, (float) z);
		}

		@JSFunction
		public void setVelX(NativeEntity ent, double amount) {
			nativeSetVel(ent.entityId, (float) amount, AXIS_X);
		}
		@JSFunction
		public void setVelY(NativeEntity ent, double amount) {
			nativeSetVel(ent.entityId, (float) amount, AXIS_Y);
		}
		@JSFunction
		public void setVelZ(NativeEntity ent, double amount) {
			nativeSetVel(ent.entityId, (float) amount, AXIS_Z);
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
		public void rideAnimal(NativeEntity /*Flynn*/rider, NativeEntity mount) {
			nativeRideAnimal(rider.entityId, mount.entityId);
		}

		@JSFunction
		public NativeEntity spawnChicken(double x, double y, double z, String tex) { //Textures not supported
			if (invalidTexName(tex)) {
				tex = "mob/chicken.png";
			}
			int entityId = nativeSpawnEntity((float) x, (float) y, (float) z, 10, tex);
			return new NativeEntity(entityId);
		}

		@JSFunction
		public NativeEntity spawnCow(double x, double y, double z, String tex) { //Textures not supported
			if (invalidTexName(tex)) {
				tex = "mob/cow.png";
			}
			int entityId = nativeSpawnEntity((float) x, (float) y, (float) z, 11, tex);
			return new NativeEntity(entityId);
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

		//standard methods introduced in API level 0.2
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
		public void setPositionRelative(NativeEntity ent, double x, double y, double z) {
			nativeSetPositionRelative(ent.entityId, (float) x, (float) y, (float) z);
		}

		@JSFunction
		public void setRot(NativeEntity ent, double yaw, double pitch) {
			nativeSetRot(ent.entityId, (float) yaw, (float) pitch);
		}

		//standard methods introduced in API level 0.3
		@JSFunction
		public double getPitch(NativeEntity ent) {
			if (ent == null) ent = getPlayerEnt();
			return nativeGetPitch(ent.entityId);
		}

		@JSFunction
		public double getYaw(NativeEntity ent) {
			if (ent == null) ent = getPlayerEnt();
			return nativeGetYaw(ent.entityId);
		}
		//standard methods introduced in 0.4 and 0.5
		@JSFunction
		public NativeEntity spawnPigZombie(double x, double y, double z, int item, String tex) { //Textures not supported yet
			if (invalidTexName(tex)) {
				tex = "mob/pigzombie.png";
			}
			int entityId = nativeSpawnEntity((float) x, (float) y, (float) z, 36, tex);
			nativeSetCarriedItem(entityId, item, 1, 0);
			return new NativeEntity(entityId);
		}

		//nonstandard methods

		@JSFunction
		public NativeEntity bl_spawnMob(double x, double y, double z, int typeId, String tex) {
			print("Nag: update to Level.spawnMob");
			if (invalidTexName(tex)) {
				tex = null;
			}
			int entityId = nativeSpawnEntity((float) x, (float) y, (float) z, typeId, tex);
			return new NativeEntity(entityId);
		}
		@JSFunction
		public void bl_setMobSkin(NativeEntity entity, String tex) {
			print("Nag: update to Entity.setMobSkin");
			nativeSetMobSkin(entity.entityId, tex);
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

	private static class NativeEntity extends ScriptableObject {
		public int entityId;
		public NativeEntity(int entityId) {
			this.entityId = entityId;
		}
		@Override
		public String getClassName() {	
			return "NativeEntity";
		}
		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (obj instanceof NativeEntity) {
				return ((NativeEntity) obj).entityId == this.entityId;
			}
			return false;
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
		public static void setTile(int x, int y, int z, int id) {
			nativeSetTile(x, y, z, id, 0);
		}
		@JSStaticFunction
		public static NativePointer getAddress() {
			return new NativePointer(nativeGetLevel()); //TODO: I still don't know WTF this does.
		}
		@JSStaticFunction
		public static NativeEntity spawnChicken(double x, double y, double z, String tex) { //Textures not supported
			if (invalidTexName(tex)) {
				tex = "mob/chicken.png";
			}
			int entityId = nativeSpawnEntity((float) x, (float) y, (float) z, 10, tex);
			return new NativeEntity(entityId);
		}

		@JSStaticFunction
		public static NativeEntity spawnCow(double x, double y, double z, String tex) { //Textures not supported
			if (invalidTexName(tex)) {
				tex = "mob/cow.png";
			}
			int entityId = nativeSpawnEntity((float) x, (float) y, (float) z, 11, tex);
			return new NativeEntity(entityId);
		}
		//nonstandard methods

		@JSStaticFunction
		public static NativeEntity spawnMob(double x, double y, double z, int typeId, String tex) {
			if (invalidTexName(tex)) {
				tex = null;
			}
			int entityId = nativeSpawnEntity((float) x, (float) y, (float) z, typeId, tex);
			return new NativeEntity(entityId);
		}

		@JSStaticFunction
		public static String getSignText(int x, int y, int z, int line) {
			if (line < 0 || line >= 4) throw new RuntimeException("Invalid line for sign: must be in the range of 0 to 3");
			return nativeGetSignText(x, y, z, line);
		}

		@JSStaticFunction
		public static void setSignText(int x, int y, int z, int line, String newText) {
			if (line < 0 || line >= 4) throw new RuntimeException("Invalid line for sign: must be in the range of 0 to 3");
			nativeSetSignText(x, y, z, line, newText);
		}	

		//thanks to MrARM

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
		public static void dropItem(double x, double y, double z, double range, int id, int count, int damage) {
			nativeDropItem((float) x, (float) y, (float) z, (float)range, id, count, damage);
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
			return (int)nativeGetTime();
		}

		@JSStaticFunction
		public static void setTime(int time) {
			nativeSetTime((long)time);
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
			if(shouldDrop) dropItem(((double)x)+0.5, y, ((double)z)+0.5, 1, itmId, 1, itmDmg);
		}

		@JSStaticFunction
		public static void setChestSlot(int x, int y, int z, int slot, int id, int damage, int amount) {
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
		public static void playSound(double x, double y, double z, String sound, double volume, double pitch) {
			nativePlaySound((float) x, (float) y, (float) z, sound, (float) volume, (float) pitch);
		}

		@JSStaticFunction
		public static void playSoundEnt(NativeEntity ent, String sound, double volume, double pitch) {
			float x = nativeGetEntityLoc(ent.entityId, AXIS_X);
			float y = nativeGetEntityLoc(ent.entityId, AXIS_Y);
			float z = nativeGetEntityLoc(ent.entityId, AXIS_Z);
			
			nativePlaySound(x, y, z, sound, (float) volume, (float) pitch);
		}
		
		//InusualZ's additions
		/*
		@JSStaticFunction
		public static void extinguishFire(int x, int y, int z, int side){
			nativeExtinguishFire(x, y, z, side);
		}
		Commented out: This is useless and can be done with just setTile */
		
		@Override
		public String getClassName() {
			return "Level";
		}
	}

	private static class NativePlayerApi extends ScriptableObject {
		private static NativeEntity playerEnt = new NativeEntity(0);
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
		public static NativeEntity getEntity() {
			playerEnt.entityId = nativeGetPlayerEnt();
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
		//nonstandard
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
		
		//InusualZ's additions
		
		@JSStaticFunction
		public static int getArmorSlot(int slot){
			return nativeGetSlotArmor(slot, ITEMID);
		}
		
		@JSStaticFunction
		public static int getArmorSlotDamage(int slot){
			return nativeGetSlotArmor(slot, DAMAGE);
		}
		
		@JSStaticFunction
		public static void setArmorSlot(int slot, int id, int damage){
			nativeSetArmorSlot(slot, id, damage);
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
		public static void setVelX(NativeEntity ent, double amount) {
			nativeSetVel(ent.entityId, (float) amount, AXIS_X);
		}
		@JSStaticFunction
		public static void setVelY(NativeEntity ent, double amount) {
			nativeSetVel(ent.entityId, (float) amount, AXIS_Y);
		}
		@JSStaticFunction
		public static void setVelZ(NativeEntity ent, double amount) {
			nativeSetVel(ent.entityId, (float) amount, AXIS_Z);
		}
		@JSStaticFunction
		public static void setRot(NativeEntity ent, double yaw, double pitch) {
			nativeSetRot(ent.entityId, (float) yaw, (float) pitch);
		}
		@JSStaticFunction
		public static void rideAnimal(NativeEntity /*insert funny reference*/rider, NativeEntity mount) {
			nativeRideAnimal(rider.entityId, mount.entityId);
		}
		@JSStaticFunction
		public static void setPosition(NativeEntity ent, double x, double y, double z) {
			nativeSetPosition(ent.entityId, (float) x, (float) y, (float) z);
		}
		@JSStaticFunction
		public static void setPositionRelative(NativeEntity ent, double x, double y, double z) {
			nativeSetPositionRelative(ent.entityId, (float) x, (float) y, (float) z);
		}
		@JSStaticFunction
		public static double getPitch(NativeEntity ent) {
			return nativeGetPitch(ent.entityId);
		}

		@JSStaticFunction
		public static double getYaw(NativeEntity ent) {
			return nativeGetYaw(ent.entityId);
		}

		//nonstandard

		@JSStaticFunction
		public static void setFireTicks(NativeEntity ent, int howLong) {
			nativeSetOnFire(ent.entityId, howLong);
		}

		@JSStaticFunction
		public static double getX(NativeEntity ent) {
			return nativeGetEntityLoc(ent.entityId, AXIS_X);
		}
		@JSStaticFunction
		public static double getY(NativeEntity ent) {
			return nativeGetEntityLoc(ent.entityId, AXIS_Y);
		}
		@JSStaticFunction
		public static double getZ(NativeEntity ent) {
			return nativeGetEntityLoc(ent.entityId, AXIS_Z);
		}

		@JSStaticFunction
		public static void setCarriedItem(NativeEntity ent, int id, int count, int damage) {
			nativeSetCarriedItem(ent.entityId, id, 1, damage);
		}

		@JSStaticFunction
		public static int getEntityTypeId(NativeEntity ent) {
			return nativeGetEntityTypeId(ent.entityId);
		}

		@JSStaticFunction
		public static NativeEntity spawnMob(double x, double y, double z, int typeId, String tex) {
			scriptPrint("Nag: update to Level.spawnMob");
			if (invalidTexName(tex)) {
				tex = null;
			}
			int entityId = nativeSpawnEntity((float) x, (float) y, (float) z, typeId, tex);
			return new NativeEntity(entityId);
		}

		@JSStaticFunction
		public static void setAnimalAge(NativeEntity animal, int age) {
			nativeSetAnimalAge(animal.entityId, age);
		}

		@JSStaticFunction
		public static int getAnimalAge(NativeEntity animal) {
			return nativeGetAnimalAge(animal.entityId);
		}

		@JSStaticFunction
		public static void setMobSkin(NativeEntity entity, String tex) {
			nativeSetMobSkin(entity.entityId, tex);
		}

		@JSStaticFunction
		public static void remove(NativeEntity ent) {
			if(ent == null) return;
			nativeRemoveEntity(ent.entityId);
		}

		/*@JSStaticFunction
		public static NativeEntity[] getAllEntities() {
			nativeGetAllEntities();
			return null;
		}*/
		@JSStaticFunction
		public static int getHealth(NativeEntity ent) {
			return nativeGetMobHealth(ent.entityId);
		}

		@JSStaticFunction
		public static void setHealth(NativeEntity ent, int halfhearts) {
			nativeSetMobHealth(ent.entityId, halfhearts);
		}

		@JSStaticFunction
		public static void setRenderType(NativeEntity ent, int renderType) {
			nativeSetEntityRenderType(ent.entityId, renderType);
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
			overrideTexture("terrain.png", url);
		}
		@JSStaticFunction
		public static void setItems(String url) {
			overrideTexture("gui/items.png", url);
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
		public static void setItem(int id, int icony, int iconx, String name) {
			nativeDefineItem(id, (icony * 16) + iconx, name);
		}

		@JSStaticFunction
		public static void setFoodItem(int id, int icony, int iconx, int halfhearts, String name) {
			nativeDefineFoodItem(id, (icony * 16) + iconx, halfhearts, name);
		}

		//nonstandard

		@JSStaticFunction
		public static void selectLevel(String levelDir, String levelName, String levelSeed, int gamemode) {
			if (levelDir.equals(ScriptManager.worldDir)) {
				System.err.println("Attempted to load level that is already loaded - ignore");
				return;
			}
			requestLeaveGame = true;
			//nativeSelectLevel(levelDir);
			requestSelectLevel = new SelectLevelRequest();
			requestSelectLevel.dir = levelDir;
			if (isValidStringParameter(levelName)) {
				requestSelectLevel.name = levelName;
				requestSelectLevel.seed = levelSeed;
				requestSelectLevel.gameMode = gamemode;
			}
		}

		@JSStaticFunction
		public static String readData(String prefName) {
			SharedPreferences sPrefs = androidContext.getSharedPreferences("BlockLauncherModPEScript"+currentScript, 0);
			return sPrefs.getString(prefName, "");
		}

		@JSStaticFunction
		public static void saveData(String prefName, String prefValue) {
			SharedPreferences sPrefs = androidContext.getSharedPreferences("BlockLauncherModPEScript"+currentScript, 0);
			SharedPreferences.Editor prefsEditor = sPrefs.edit();
			prefsEditor.putString(prefName, prefValue);
			prefsEditor.commit();
		}

		@JSStaticFunction
		public static void removeData(String prefName) {
			SharedPreferences sPrefs = androidContext.getSharedPreferences("BlockLauncherModPEScript"+currentScript, 0);
			SharedPreferences.Editor prefsEditor = sPrefs.edit();
			prefsEditor.remove(prefName);
			prefsEditor.commit();
		}

		@JSStaticFunction
		public static void leaveGame() {
			requestLeaveGame = true;
		}

		@JSStaticFunction
		public static void joinServer(String serverAddress, int port) {
			requestLeaveGame = true;
			requestJoinServer = new JoinServerRequest();
			requestJoinServer.serverAddress = serverAddress;
			requestJoinServer.serverPort = port;
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

		@Override
		public String getClassName() {
			return "ModPE";
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
}
