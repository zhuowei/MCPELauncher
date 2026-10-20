package net.nexlauncher;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;

import java.util.function.Consumer;

/**
 * Sandboxed local JavaScript runtime for launcher-side scripts.
 * No Minecraft process, Java reflection, filesystem, networking, or process APIs
 * are exposed. ModPeCompat is a deliberately small logging-only compatibility subset.
 */
public final class ScriptRuntime {
    private static final int MAX_INSTRUCTIONS = 200_000;
    private static final int OBSERVER_THRESHOLD = 10_000;

    private ScriptRuntime() {}

    private static final class BudgetContextFactory extends ContextFactory {
        private final ThreadLocal<Integer> instructionCount =
            ThreadLocal.withInitial(() -> 0);

        void beginRun() { instructionCount.set(0); }
        void endRun() { instructionCount.remove(); }

        @Override
        protected void observeInstructionCount(Context context, int count) {
            int total = instructionCount.get() + count;
            instructionCount.set(total);
            if (total > MAX_INSTRUCTIONS) {
                throw new EvaluatorException("Script stopped: instruction budget exceeded.");
            }
        }
    }

    private static final BudgetContextFactory FACTORY = new BudgetContextFactory();

    public static String execute(String source, Consumer<String> logger) {
        if (source == null) return "ERROR: Script source is null.";
        if (source.length() > 1_000_000) return "ERROR: Script exceeds the 1 MiB source limit.";

        Consumer<String> output = logger != null ? logger : ignored -> {};
        FACTORY.beginRun();
        Context context = FACTORY.enterContext();
        try {
            context.setLanguageVersion(Context.VERSION_ES6);
            context.setOptimizationLevel(-1);
            context.setInstructionObserverThreshold(OBSERVER_THRESHOLD);
            context.setClassShutter(className -> false);

            Scriptable scope = context.initStandardObjects();
            ModPeCompat.install(scope, output);
            context.evaluateString(scope, source, "nex-user-script.js", 1, null);
            return "OK: Script completed in isolated launcher runtime. In-game APIs are not connected.";
        } catch (RuntimeException error) {
            String message = error.getMessage();
            return "ERROR: " + (message != null ? message : error.getClass().getSimpleName());
        } finally {
            Context.exit();
            FACTORY.endRun();
        }
    }
}
