package net.nexlauncher;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Validates a .mcpack archive before offering it to the installed Minecraft app. */
public final class PackImporter {
    public static final long MAX_ARCHIVE_BYTES = 512L * 1024L * 1024L;
    private static final int MAX_ENTRIES = 10_000;
    private static final int MAX_MANIFEST_BYTES = 1_048_576;

    private PackImporter() {}

    public static final class Result {
        public final boolean success;
        public final String message;
        public final Uri contentUri;

        private Result(boolean success, String message, Uri contentUri) {
            this.success = success;
            this.message = message;
            this.contentUri = contentUri;
        }

        static Result failure(String message) {
            return new Result(false, message, null);
        }
    }

    public static Result validateAndPrepare(Context context, Uri source) {
        if (source == null) return Result.failure("No file was selected.");
        File directory = new File(context.getCacheDir(), "mcpack-imports");
        if (!directory.exists() && !directory.mkdirs()) {
            return Result.failure("Could not create the temporary import folder.");
        }
        File archive = new File(directory, "import-" + System.nanoTime() + ".mcpack");
        try {
            copyBounded(context.getContentResolver(), source, archive);
            validateArchive(archive);
            Uri shareUri = FileProvider.getUriForFile(
                context, context.getPackageName() + ".fileprovider", archive);
            return new Result(true, "Archive and manifest checks passed. Choose Minecraft to import the pack.",
                shareUri);
        } catch (IOException | JSONException | IllegalArgumentException error) {
            // A failed import must not leave a partial or rejected archive behind.
            //noinspection ResultOfMethodCallIgnored
            archive.delete();
            String detail = error.getMessage();
            return Result.failure("Pack rejected: " +
                (detail == null || detail.trim().isEmpty() ? "invalid archive or manifest." : detail));
        }
    }

    private static void copyBounded(ContentResolver resolver, Uri source, File target)
            throws IOException {
        try (InputStream input = resolver.openInputStream(source)) {
            if (input == null) throw new IOException("The selected file could not be opened.");
            try (FileOutputStream output = new FileOutputStream(target)) {
                byte[] buffer = new byte[32 * 1024];
                long total = 0;
                int count;
                while ((count = input.read(buffer)) != -1) {
                    total += count;
                    if (total > MAX_ARCHIVE_BYTES) {
                        throw new IOException("Archive exceeds the 512 MiB limit.");
                    }
                    output.write(buffer, 0, count);
                }
                if (total == 0) throw new IOException("The selected file is empty.");
            }
        }
    }

    private static void validateArchive(File archive) throws IOException, JSONException {
        try (ZipFile zip = new ZipFile(archive)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            int count = 0;
            ZipEntry manifestEntry = null;
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                count++;
                if (count > MAX_ENTRIES) throw new IOException("Archive contains too many entries.");
                String name = entry.getName();
                if (name == null || name.isEmpty() || name.startsWith("/") ||
                    name.indexOf('\\') >= 0 || hasTraversalSegment(name)) {
                    throw new IOException("Archive contains an unsafe file path.");
                }
                if (!entry.isDirectory() && "manifest.json".equals(name)) {
                    if (manifestEntry != null) throw new IOException("Archive has duplicate root manifests.");
                    manifestEntry = entry;
                }
            }
            if (manifestEntry == null) {
                throw new IOException("manifest.json is missing from the archive root.");
            }
            if (manifestEntry.getSize() > MAX_MANIFEST_BYTES) {
                throw new IOException("manifest.json exceeds the 1 MiB limit.");
            }
            byte[] bytes;
            try (InputStream input = zip.getInputStream(manifestEntry)) {
                bytes = readBounded(input, MAX_MANIFEST_BYTES);
            }
            validateManifest(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private static boolean hasTraversalSegment(String name) {
        String[] parts = name.split("/", -1);
        for (String part : parts) {
            if ("..".equals(part) || ".".equals(part)) return true;
        }
        return false;
    }

    private static byte[] readBounded(InputStream input, int limit) throws IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (output.size() + count > limit) throw new IOException("manifest.json exceeds the 1 MiB limit.");
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    private static void validateManifest(String json) throws JSONException, IOException {
        JSONObject root = new JSONObject(json);
        JSONObject header = root.optJSONObject("header");
        if (header == null) throw new IOException("manifest.json has no header object.");
        requireUuid(header.optString("uuid", ""), "header.uuid");
        JSONArray version = header.optJSONArray("version");
        if (version == null || version.length() != 3) {
            throw new IOException("header.version must contain three integers.");
        }
        for (int i = 0; i < 3; i++) {
            if (version.optInt(i, -1) < 0) throw new IOException("header.version contains an invalid number.");
        }
        if (header.optString("name", "").trim().isEmpty()) {
            throw new IOException("header.name is missing.");
        }
        JSONArray modules = root.optJSONArray("modules");
        if (modules == null || modules.length() == 0) {
            throw new IOException("manifest.json must contain at least one module.");
        }
        for (int i = 0; i < modules.length(); i++) {
            JSONObject module = modules.optJSONObject(i);
            if (module == null) throw new IOException("A module entry is not an object.");
            requireUuid(module.optString("uuid", ""), "modules[" + i + "].uuid");
            JSONArray moduleVersion = module.optJSONArray("version");
            if (moduleVersion == null || moduleVersion.length() != 3) {
                throw new IOException("A module version must contain three integers.");
            }
            for (int j = 0; j < 3; j++) {
                if (moduleVersion.optInt(j, -1) < 0) {
                    throw new IOException("A module version contains an invalid number.");
                }
            }
            String type = module.optString("type", "");
            if (!("resources".equals(type) || "data".equals(type) ||
                  "script".equals(type) || "client_data".equals(type) ||
                  "interface".equals(type) || "world_template".equals(type))) {
                throw new IOException("Unsupported or missing module type: " + type);
            }
        }
    }

    private static void requireUuid(String value, String field) throws IOException {
        if (!value.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            throw new IOException(field + " is not a valid UUID.");
        }
    }
}
