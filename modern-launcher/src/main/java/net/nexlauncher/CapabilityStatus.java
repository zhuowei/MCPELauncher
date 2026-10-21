package net.nexlauncher;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Explicit capability reporting; a JNI load is not proof of in-game support. */
public final class CapabilityStatus {
    public enum State { AVAILABLE, LIMITED, UNAVAILABLE, UNVERIFIED }

    public static final class Entry {
        public final String feature;
        public final State state;
        public final String reason;
        public final String targetVersion;

        private Entry(String feature, State state, String reason, String targetVersion) {
            this.feature = feature;
            this.state = state;
            this.reason = reason;
            this.targetVersion = targetVersion;
        }

        @Override public String toString() {
            return feature + ": " + state + " - " + reason + " [target " + targetVersion + "]";
        }
    }

    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private final String targetVersion;

    public CapabilityStatus(String targetVersion) {
        this.targetVersion = targetVersion == null ? "unknown" : targetVersion;
    }

    public void register(String feature, State state, String reason) {
        if (feature == null || feature.trim().isEmpty())
            throw new IllegalArgumentException("feature must not be blank");
        if (state == null) throw new IllegalArgumentException("state must not be null");
        entries.put(feature, new Entry(feature, state,
            reason == null || reason.trim().isEmpty() ? "No reason supplied" : reason,
            targetVersion));
    }

    public Entry get(String feature) { return entries.get(feature); }

    public Map<String, Entry> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(entries));
    }

    public String report() {
        StringBuilder out = new StringBuilder("Nex-Launcher capability report\nTarget: ")
            .append(targetVersion).append('\n');
        for (Entry entry : entries.values()) out.append("- ").append(entry).append('\n');
        return out.toString().trim();
    }

    public static CapabilityStatus forCurrentLauncher(String installedVersion) {
        CapabilityStatus status = new CapabilityStatus("MCPE 1.21.132.1");
        status.register("public_launch_intent", State.AVAILABLE,
            "Uses Android public launch intent.");
        status.register("pack_import", State.LIMITED,
            "Archive validation and import handoff exist; on-device acceptance remains unverified.");
        status.register("launcher_rhino", State.LIMITED,
            "Sandboxed launcher-side JavaScript only; not connected to Minecraft.");
        status.register("in_game_modpe_api", State.UNAVAILABLE,
            "Live world, player, entity, and game-event APIs are not implemented.");
        status.register("native_bedrock_integration", State.UNVERIFIED,
            "No validated profile exists for the exact target build.");
        status.register("installed_client_version", State.UNVERIFIED,
            "Detected version: " + (installedVersion == null ? "unknown" : installedVersion)
                + ". Version-name matching alone does not prove binary compatibility.");
        return status;
    }
}
