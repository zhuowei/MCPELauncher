package net.nexlauncher;

/**
 * Fail-closed compatibility checks for the MCPE 1.21.132.1 test target.
 * This class only evaluates metadata and never modifies another process.
 */
public final class McpeCompatibilityGate {
    public static final String TARGET_VERSION = "1.21.132.1";
    public static final String TARGET_PACKAGE = "com.mojang.minecraftpe";
    public static final String TARGET_ABI = "arm64-v8a";

    public enum Result {
        ELIGIBLE_FOR_MANUAL_VALIDATION,
        UNSUPPORTED_PACKAGE,
        UNSUPPORTED_VERSION,
        UNSUPPORTED_ABI,
        INCOMPLETE_PROFILE
    }

    public static final class Profile {
        public final String packageName;
        public final String versionName;
        public final String abi;
        public final boolean libraryIdentityVerified;
        public final boolean deviceTestPassed;

        public Profile(String packageName, String versionName, String abi,
                       boolean libraryIdentityVerified, boolean deviceTestPassed) {
            this.packageName = packageName;
            this.versionName = versionName;
            this.abi = abi;
            this.libraryIdentityVerified = libraryIdentityVerified;
            this.deviceTestPassed = deviceTestPassed;
        }
    }

    private McpeCompatibilityGate() {}

    public static Result evaluate(Profile profile) {
        if (profile == null || profile.packageName == null ||
            profile.versionName == null || profile.abi == null) {
            return Result.INCOMPLETE_PROFILE;
        }
        if (!TARGET_PACKAGE.equals(profile.packageName)) return Result.UNSUPPORTED_PACKAGE;
        if (!TARGET_VERSION.equals(profile.versionName)) return Result.UNSUPPORTED_VERSION;
        if (!TARGET_ABI.equals(profile.abi)) return Result.UNSUPPORTED_ABI;
        if (!profile.libraryIdentityVerified || !profile.deviceTestPassed) {
            return Result.INCOMPLETE_PROFILE;
        }
        // This is a prerequisite for further manual review, not proof of compatibility.
        return Result.ELIGIBLE_FOR_MANUAL_VALIDATION;
    }
}
