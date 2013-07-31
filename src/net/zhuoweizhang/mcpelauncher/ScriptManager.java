package net.zhuoweizhang.mcpelauncher;

import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.*;

import com.mojang.minecraftpe.MainActivity;

public class ScriptManager {

	public static List<ScriptState> scripts = new ArrayList<ScriptState>();

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
		Scriptable scope = ctx.initStandardObjects(null, false);
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

	public static void init() {
		//set up hooks
		nativeSetupHooks();
	}

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
}
