package net.nexlauncher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private static final String MINECRAFT_PACKAGE = "com.mojang.minecraftpe";
    private static final int REQUEST_PICK_PACK = 4102;
    private TextView status;

    static { System.loadLibrary("nexnative"); }
    private native int nativeGetAbiVersion();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(0xFF101820);

        TextView title = new TextView(this);
        title.setText("NEX-LAUNCHER");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("BlockLauncher revival • Android edition");
        subtitle.setTextColor(0xFFB7C6D0);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sub = matchWrap();
        sub.topMargin = pad / 3;
        root.addView(subtitle, sub);

        status = new TextView(this);
        status.setTextColor(0xFFE5EDF2);
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sp = matchWrap();
        sp.topMargin = pad;
        sp.bottomMargin = pad;
        root.addView(status, sp);

        addButton(root, "LAUNCH MINECRAFT", pad, v -> launchMinecraft());
        addButton(root, "IMPORT .MCPACK", pad, v -> choosePack());
        addButton(root, "NATIVE BRIDGE STATUS", pad, v -> {
            try {
                status.setText("Native library loaded. Interface version: " + nativeGetAbiVersion()
                    + "\nJNI bridge only; Bedrock process hooks are not implemented or verified.");
            } catch (UnsatisfiedLinkError error) {
                status.setText("Native library unavailable: " + error.getMessage());
            }
        });
        addButton(root, "TEST MODPE JS SHIM", pad, v -> {
            StringBuilder logs = new StringBuilder();
            String result = ScriptRuntime.execute(
                "print('Rhino runtime ready'); clientMessage('Launcher-only test'); "
                    + "logStatus = ModPE.getApiStatus(); print(logStatus);",
                line -> logs.append(line).append('\n'));
            status.setText(result + (logs.length() == 0 ? "" : "\n" + logs));
        });
        addButton(root, "MCPE COMPATIBILITY STATUS", pad, v -> status.setText(compatibilityReport()));

        setContentView(root);
        refreshStatus();
    }

    private void addButton(LinearLayout root, String label, int pad,
                           android.view.View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = pad / 3;
        root.addView(button, params);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void choosePack() {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("*/*");
        picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivityForResult(picker, REQUEST_PICK_PACK);
        } catch (ActivityNotFoundException error) {
            status.setText("No file picker is available on this device.");
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_PACK || resultCode != RESULT_OK ||
            data == null || data.getData() == null) return;

        status.setText("Validating archive and manifest…");
        PackImporter.Result result = PackImporter.validateAndPrepare(this, data.getData());
        status.setText(result.message);
        if (!result.success) return;

        Intent open = new Intent(Intent.ACTION_VIEW);
        open.setDataAndType(result.contentUri, "application/octet-stream");
        open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(open, "Import pack with Minecraft"));
        } catch (ActivityNotFoundException error) {
            status.setText("Pack validated, but no app accepted the import intent. "
                + "Minecraft may need to be installed or its .mcpack association may be unavailable.");
        }
    }

    private String compatibilityReport() {
        String installedVersion = "not installed";
        String versionCode = "n/a";
        try {
            android.content.pm.PackageInfo info =
                getPackageManager().getPackageInfo(MINECRAFT_PACKAGE, 0);
            installedVersion = info.versionName == null ? "unknown" : info.versionName;
            versionCode = String.valueOf(info.versionCode);
        } catch (android.content.pm.PackageManager.NameNotFoundException ignored) {
            return "Minecraft was not detected.\nInstall the official Minecraft app first.";
        }

        boolean targetVersion = installedVersion.startsWith("1.21.132.1");
        return "Installed Minecraft: " + installedVersion + " (code " + versionCode + ")\n"
            + "Requested test target: 1.21.132.1 — "
            + (targetVersion ? "version-name match; still needs device tests." : "version does not match.") + "\n"
            + "Available: public launch intent, isolated Rhino diagnostics, bounded .mcpack validation.\n"
            + "Not available: verified in-game ModPE APIs, Bedrock asset-loader hooks, native process hooks.";
    }

    private void refreshStatus() {
        status.setText(getPackageManager().getLaunchIntentForPackage(MINECRAFT_PACKAGE) != null
            ? "Minecraft detected. Ready to launch or import a validated pack.\n"
                + compatibilityReport()
            : "Minecraft was not detected. Install the official Minecraft app, then reopen Nex-Launcher.");
    }

    private void launchMinecraft() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(MINECRAFT_PACKAGE);
        if (intent == null) {
            status.setText("Minecraft was not found. Install or restore the official app and try again.");
            return;
        }
        try {
            startActivity(intent);
        } catch (RuntimeException error) {
            status.setText("Could not open Minecraft: " + error.getClass().getSimpleName());
        }
    }
}
