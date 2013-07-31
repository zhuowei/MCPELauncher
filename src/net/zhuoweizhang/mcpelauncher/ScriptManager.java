package net.zhuoweizhang.mcpelauncher;

import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

import android.content.SharedPreferences;

import org.mozilla.javascript.*;
import org.mozilla.javascript.annotations.JSFunction;

import com.mojang.minecraftpe.MainActivity;

import static net.zhuoweizhang.mcpelauncher.PatchManager.join;
import static net.zhuoweizhang.mcpelauncher.PatchManager.blankArray;

public class ScriptManager {

	public static final String SCRIPTS_DIR = "modscripts";

	public static List<ScriptState> scripts = new ArrayList<ScriptState>();

	public static android.content.Context androidContext;

	public static Set<String> enabledScripts = new HashSet<String>();

	public static void loadScript(Reader in, String sourceName) throws IOException {
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
		String[] names = { "setTile", "print" };
		((ScriptableObject) scope).defineFunctionProperties(names, BlockHostObject.class, ScriptableObject.DONTENUM);

		ScriptState state = new ScriptState(script, scope, sourceName);
		script.exec(ctx, scope);
		scripts.add(state);
	}

	public static void callScriptMethod(String functionName, Object... args) {
		Context ctx = Context.enter();
		for (ScriptState state: scripts) {
			Scriptable scope = state.scope;
			Object obj = scope.get(functionName, scope);
			if (obj != null && obj instanceof Function) {
				try {
					((Function) obj).call(ctx, scope, scope, args);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void useItemOnCallback(int x, int y, int z, int itemid, int blockid) {
		callScriptMethod("useItem", x, y, z, itemid, blockid);
	}

	public static void setLevelCallback(boolean hasLevel) {
		System.out.println("Level: " + hasLevel);
		callScriptMethod("newLevel", hasLevel);
		if (MainActivity.currentMainActivity != null) {
			MainActivity main = MainActivity.currentMainActivity.get();
			if (main != null) {
				main.setLevelCallback();
			}
		}
	}

	public static void leaveGameCallback(boolean thatboolean) {
		callScriptMethod("leaveGame");
		if (MainActivity.currentMainActivity != null) {
			MainActivity main = MainActivity.currentMainActivity.get();
			if (main != null) {
				main.leaveGameCallback();
			}
		}
	}

	public static void init(android.content.Context cxt) throws IOException {
		//set up hooks
		nativeSetupHooks();
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


	//following taken from the patch manager
	public static Set<String> getEnabledScripts() {
		return enabledScripts;
	}

	private static void setEnabled(String name, boolean state) throws IOException {
		if (state) {
			enabledScripts.add(name);
			reloadScript(getScriptFile(name));
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
				enabledScripts.add(name);
				reloadScript(getScriptFile(name));
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


	public static native void nativeSetTile(int x, int y, int z, int id, int damage);

	public static native void nativeSetupHooks();

	public static class ScriptState {
		public Script script;
		public Scriptable scope;
		public String name;
		protected ScriptState(Script script, Scriptable scope, String name) {
			this.script = script;
			this.scope = scope;
			this.name = name;
		}
	}

	private static class BlockHostObject extends ScriptableObject {
		@Override
		public String getClassName() {
			return "BlockHostObject";
		}
		@JSFunction
		public void setTile(int x, int y, int z, int id) {
			nativeSetTile(x, y, z, id, 0);
		}
		//@JSFunction
		//public void setTile(int x, int y, int z, int id, int damage) {
		//	nativeSetTile(x, y, z, id, damage);
		//}
		@JSFunction
		public void print(String str) {
			System.out.println(str);
		}
	}
}
