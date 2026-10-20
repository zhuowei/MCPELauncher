package net.nexlauncher;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private static final String MINECRAFT_PACKAGE = "com.mojang.minecraftpe";
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

        Button launch = new Button(this);
        launch.setText("LAUNCH MINECRAFT");
        launch.setOnClickListener(v -> launchMinecraft());
        root.addView(launch, matchWrap());

        Button nativeStatus = new Button(this);
        nativeStatus.setText("NATIVE BRIDGE STATUS");
        nativeStatus.setOnClickListener(v -> {
            try {
                status.setText("Native library loaded. Interface version: " + nativeGetAbiVersion()
                    + "\nJNI bridge only; Bedrock hooks are not implemented or verified.");
            } catch (UnsatisfiedLinkError error) {
                status.setText("Native library unavailable: " + error.getMessage());
            }
        });
        LinearLayout.LayoutParams np = matchWrap();
        np.topMargin = pad / 3;
        root.addView(nativeStatus, np);

        Button jsTest = new Button(this);
        jsTest.setText("TEST JAVASCRIPT RUNTIME");
        jsTest.setOnClickListener(v -> {
            StringBuilder logs = new StringBuilder();
            String result = ScriptRuntime.execute(
                "log('Rhino runtime ready'); var version = 'launcher-js-1'; log(version);",
                line -> logs.append(line).append('\n'));
            status.setText(result + (logs.length() == 0 ? "" : "\n" + logs));
        });
        LinearLayout.LayoutParams jp = matchWrap();
        jp.topMargin = pad / 3;
        root.addView(jsTest, jp);

        Button compatibility = new Button(this);
        compatibility.setText("MCPE COMPATIBILITY STATUS");
        compatibility.setOnClickListener(v -> status.setText(
            "Target: Minecraft Bedrock 1.21.132.1\n"
            + "Launcher, JNI diagnostics, and isolated launcher-side JS only. "
            + "Bedrock memory hooks and in-game APIs are not implemented or verified."));
        LinearLayout.LayoutParams cp = matchWrap();
        cp.topMargin = pad / 3;
        root.addView(compatibility, cp);

        setContentView(root);
        refreshStatus();
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void refreshStatus() {
        status.setText(getPackageManager().getLaunchIntentForPackage(MINECRAFT_PACKAGE) != null
            ? "Minecraft detected. Ready to launch."
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
