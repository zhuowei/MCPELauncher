package net.nexlauncher;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

import java.util.function.Consumer;

/**
 * Launcher-side compatibility subset for legacy script authors.
 * These functions do not connect to or modify the Minecraft process.
 */
final class ModPeCompat {
    private ModPeCompat() {}

    static void install(Scriptable scope, Consumer<String> logger) {
        ScriptableObject.putProperty(scope, "ModPE", makeModPeObject(scope));
        installLogFunction(scope, "print", "[print] ", logger);
        installLogFunction(scope, "clientMessage", "[clientMessage] ", logger);
    }

    private static Scriptable makeModPeObject(Scriptable scope) {
        ScriptableObject object = new ScriptableObject() {
            @Override public String getClassName() { return "ModPE"; }
        };
        object.setParentScope(scope);
        object.setPrototype(ScriptableObject.getObjectPrototype(scope));

        BaseFunction status = new BaseFunction() {
            @Override public Object call(Context cx, Scriptable callScope,
                                         Scriptable thisObj, Object[] args) {
                return "launcher-only subset: print, clientMessage, ModPE.getApiStatus; " +
                    "in-game ModPE/Level/Player/Entity APIs are unavailable";
            }
            @Override public String getFunctionName() { return "getApiStatus"; }
        };
        status.setParentScope(scope);
        status.setPrototype(ScriptableObject.getFunctionPrototype(scope));
        object.defineProperty("getApiStatus", status, ScriptableObject.READONLY);
        return object;
    }

    private static void installLogFunction(Scriptable scope, String name,
                                           String prefix, Consumer<String> logger) {
        BaseFunction function = new BaseFunction() {
            @Override public Object call(Context cx, Scriptable callScope,
                                         Scriptable thisObj, Object[] args) {
                StringBuilder line = new StringBuilder(prefix);
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) line.append(' ');
                    Object value = args[i];
                    line.append(value == Undefined.instance ? "undefined" : Context.toString(value));
                }
                logger.accept(line.toString());
                return Undefined.instance;
            }
            @Override public String getFunctionName() { return name; }
        };
        function.setParentScope(scope);
        function.setPrototype(ScriptableObject.getFunctionPrototype(scope));
        ScriptableObject.putProperty(scope, name, function);
    }
}
