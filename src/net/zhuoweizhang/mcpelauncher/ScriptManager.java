package net.zhuoweizhang.mcpelauncher;

import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;

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

	private static String currentScript = "Unknown";

	private static boolean requestedGraphicsReset = false;

	public static boolean sensorEnabled = false;

	public static float newPlayerYaw = 0;
	public static float newPlayerPitch = 0;

	private static SelectLevelRequest requestSelectLevel = null;

	public static void loadScript(Reader in, String sourceName) throws IOException {
		if (isRemote) throw new RuntimeException("Not available in multiplayer");
		Context ctx = Context.enter();
		Script script = ctx.compileReader(in, sourceName, 0, null);
		initJustLoadedScript(ctx, script, sourceName);
		Context.exit();
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
		} catch (Exception e) {
			e.printStackTrace();
			reportScriptError(state, e);
		}

		script.exec(ctx, scope);
		scripts.add(state);
	}

	public static void callScriptMethod(String functionName, Object... args) {
		if (isRemote) return; //No script loading/callbacks when in a remote world
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

	public static void useItemOnCallback(int x, int y, int z, int itemid, int blockid, int side) {
		callScriptMethod("useItem", x, y, z, itemid, blockid, side);
	}

	public static void setLevelCallback(boolean hasLevel, boolean isRemote) {
		System.out.println("Level: " + hasLevel);
		ScriptManager.isRemote = isRemote;
		callScriptMethod("newLevel", hasLevel);
		if (MainActivity.currentMainActivity != null) {
			MainActivity main = MainActivity.currentMainActivity.get();
			if (main != null) {
				main.setLevelCallback(isRemote);
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
		if (requestSelectLevel != null) {
			nativeSelectLevel(requestSelectLevel.dir);
			requestSelectLevel = null;
		}
	}

	private static void updatePlayerOrientation() {
		nativeSetRot(nativeGetPlayerEnt(), newPlayerYaw, newPlayerPitch);
	}

	public static void chatCallback(String str) {
		if (str == null || str.length() < 1 || str.charAt(0) != '/') return;
		callScriptMethod("procCmd", str.substring(1));
		if (!isRemote) nativePreventDefault();
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

	private static void reportScriptError(ScriptState state, Throwable t) {
		state.errors++;
		if (MainActivity.currentMainActivity != null) {
			MainActivity main = MainActivity.currentMainActivity.get();
			if (main != null) {
				main.scriptErrorCallback(state.name, t);
				if (state.errors >= MAX_NUM_ERRORS) { //too many errors
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

	private static void wordWrapClientMessage(String msg) {
		String[] portions = msg.split("\n");
		for(int i = 0; i < portions.length; i++) {
			String line = portions[i];
			while(line.length() > 40) {
				nativeClientMessage(line.substring(0, 40));
				line = line.substring(40);
			}
			if (line.length() > 0) {
				nativeClientMessage(line);
			}
		}
	}

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

	public static native float nativeGetPlayerLoc(int axis);
	public static native int nativeGetPlayerEnt();
	public static native long nativeGetLevel();
	public static native void nativeSetPosition(int entity, float x, float y, float z);
	public static native void nativeSetVel(int ent, float vel, int axis);
	public static native void nativeExplode(float x, float y, float z, float radius);
	public static native void nativeAddItemInventory(int id, int amount, int damage);
	public static native void nativeRideAnimal(int rider, int mount);
	public static native int nativeGetCarriedItem();
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
	public static native int nativeGetData(int x, int y, int z);
	public static native void nativeHurtTo(int to);
	public static native void nativeRemoveEntity(int entityId);
	public static native int nativeGetEntityTypeId(int entityId);
	public static native void nativeSetAnimalAge(int entityId, int age);
	public static native int nativeGetAnimalAge(int entityId);
	public static native void nativeSelectLevel(String levelName);

	//setup
	public static native void nativeSetupHooks(int versionCode);

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
			return nativeGetCarriedItem();
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
		public int getData(int x, int y, int z) {
			return nativeGetData(x, y, z);
		}

		@JSFunction
		public void setPlayerHealth(int value) {
			nativeHurtTo(value);
		}


		@JSFunction
		public NativeEntity bl_spawnMob(double x, double y, double z, int typeId, String tex) {
			if (invalidTexName(tex)) {
				tex = null;
			}
			int entityId = nativeSpawnEntity((float) x, (float) y, (float) z, typeId, tex);
			return new NativeEntity(entityId);
		}
		@JSFunction
		public void bl_setMobSkin(NativeEntity entity, String tex) {
			nativeSetMobSkin(entity.entityId, tex);
		}
		
		@JSFunction
		public String bl_readData(String prefName) {
			SharedPreferences sPrefs = androidContext.getSharedPreferences("BlockLauncherModPEScript"+currentScript, 0);
			return sPrefs.getString(prefName, "");
		}

		@JSFunction
		public void bl_saveData(String prefName, String prefValue) {
			SharedPreferences sPrefs = androidContext.getSharedPreferences("BlockLauncherModPEScript"+currentScript, 0);
			SharedPreferences.Editor prefsEditor = sPrefs.edit();
			prefsEditor.putString(prefName, prefValue);
			prefsEditor.commit();
		}

		@JSFunction
		public String bl_getWorldName() {
			return worldName;
		}


		@JSFunction
		public String bl_getWorldDir() {
			return worldDir;
		}
		
		@JSFunction
		public void bl_removeEntity(NativeEntity ent) {
			if(ent == null) return;
			nativeRemoveEntity(ent.entityId);
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
			return nativeGetCarriedItem();
		}
		@JSStaticFunction
		public static void addItemInventory(int id, int amount, int damage) {
			nativeAddItemInventory(id, amount, damage);
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
			if (MainActivity.currentMainActivity != null) {
				MainActivity main = MainActivity.currentMainActivity.get();
				if (main != null) {
					main.scriptOverrideTexture(theOverridden, url);
				}
			}
		}
		@JSStaticFunction
		public static void resetImages() {
			if (MainActivity.currentMainActivity != null) {
				MainActivity main = MainActivity.currentMainActivity.get();
				if (main != null) {
					main.scriptResetImages();
				}
			}
		}

		@JSStaticFunction
		public static void setItem(int id, int iconx, int icony, String name) {
			nativeDefineItem(id, (icony * 16) + iconx, name);
		}

		@JSStaticFunction
		public static void setFoodItem(int id, int iconx, int icony, int halfhearts, String name) {
			nativeDefineFoodItem(id, (icony * 16) + iconx, halfhearts, name);
		}

		@JSStaticFunction
		public static void selectLevel(String levelDir, String unused_levelName, String unused_levelSeed, int unused_gamemode) {
			if (levelDir.equals(ScriptManager.worldDir)) {
				System.err.println("Attempted to load level that is already loaded - ignore");
				return;
			}
			//nativeSelectLevel(levelDir);
			requestSelectLevel = new SelectLevelRequest();
			requestSelectLevel.dir = levelDir;
		}

		@Override
		public String getClassName() {
			return "ModPE";
		}
	}

	private static class SelectLevelRequest {
		public String dir;
	}
}
