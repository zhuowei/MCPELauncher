package net.zhuoweizhang.mcpelauncher;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Compatibility gate for the inherited BlockLauncher runtime.
 *
 * Old offsets and native hooks must never be assumed to work on an unknown
 * Bedrock release. This class intentionally only permits versions that already
 * have an exact entry in the legacy MinecraftVersion registry.
 */
public final class LegacyCompatibilityProfile {
    private static final String TAG = "NexCompatibility";
    private static final String MINECRAFT_PACKAGE = "com.mojang.minecraftpe";

    private LegacyCompatibilityProfile() {}

    public static final class Report {
        public final String versionName;
        public final int versionCode;
        public final boolean packageInstalled;
        public final boolean exactLegacyProfile;
        public final boolean nativeHooksSupported;

        private Report(String versionName, int versionCode, boolean packageInstalled,
                       boolean exactLegacyProfile) {
            this.versionName = versionName;
            this.versionCode = versionCode;
            this.packageInstalled = packageInstalled;
            this.exactLegacyProfile = exactLegacyProfile;
            this.nativeHooksSupported = packageInstalled && exactLegacyProfile;
        }

        public String describe() {
            if (!packageInstalled) return "Minecraft is not installed.";
            if (nativeHooksSupported) {
                return "Minecraft " + versionName + " matches an exact legacy profile. " +
                    "Native support is limited to the registered legacy profile.";
            }
            return "Minecraft " + versionName + " (versionCode " + versionCode + ") is not " +
                "registered for legacy native hooks. Native patching and in-game ModPE scripts " +
                "are disabled to avoid applying obsolete offsets. Use Bedrock Add-Ons/resource packs.";
        }
    }

    public static Report inspect(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(MINECRAFT_PACKAGE, 0);
            int code = info.versionCode;
            boolean exact = MinecraftVersion.hasExactVersionCode(code);
            Report report = new Report(info.versionName == null ? "unknown" : info.versionName,
                code, true, exact);
            Log.i(TAG, report.describe());
            return report;
        } catch (PackageManager.NameNotFoundException error) {
            Report report = new Report("not installed", -1, false, false);
            Log.w(TAG, report.describe());
            return report;
        } catch (RuntimeException error) {
            Report report = new Report("unknown", -1, false, false);
            Log.e(TAG, "Unable to inspect Minecraft package; legacy hooks remain disabled.", error);
            return report;
        }
    }

    public static boolean supportsLegacyNativeHooks(Context context) {
        return inspect(context).nativeHooksSupported;
    }
}
