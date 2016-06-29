package net.zhuoweizhang.mcpelauncher;

import java.io.*;
import java.nio.*;
import java.util.*;

import android.content.Context;

import net.zhuoweizhang.pokerface.PokerFace;

import com.mojang.minecraftpe.MainActivity;

public final class MaraudersMap {
	public static ByteBuffer minecraftTextBuffer = null;
	private static boolean patchingInitialized = false;
	public static boolean initPatching(Context context, long minecraftLibLength) throws Exception {
		/* plan: loop through libminecraftpe.so. 
		 * for each map, if executable, use marauder to re-map it to a file
		 * otherwise set to writable
		 */
		if (patchingInitialized) return true;
		PokerFace.init();
		boolean useOldCode = Utils.getPrefs(0).getBoolean("zz_legacy_live_patch", false) || 
			ScriptManager.nativeGetArch() == ScriptManager.ARCH_I386 ||
			new File("/sdcard/blocklauncher_marauders_map_legacy").exists() ||
			android.os.Build.VERSION.SDK_INT >= 23;
			// Marshmallow - thanks BasixKor https://github.com/zhuowei/MCPELauncher/issues/284#issuecomment-154613087
		System.out.println("Live patching is running in " + (useOldCode? "legacy" : "normal") + " mode");
		boolean success = true;
		patchingInitialized = true;
		Scanner scan = new Scanner(new File("/proc/self/maps"));
		File patchedDir = context.getDir("patched", 0);
		while (scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] parts = line.split(" ");
			if (parts[parts.length - 1].indexOf("libminecraftpe.so") >= 0) {
				long loc = Long.parseLong(parts[0].substring(0, parts[0].indexOf("-")), 16);
				long end = Long.parseLong(parts[0].substring(parts[0].indexOf("-") + 1), 16);
				long len = end - loc;
				if (parts[1].indexOf("x") >= 0) {
					long newLoc;
					if (!useOldCode) {
						newLoc = remapText(loc, len, new File(patchedDir, "libminecraftpe_text_section").getAbsolutePath());
					} else {
						newLoc = loc;
						int returnStatus = PokerFace.mprotect(loc, len, PokerFace.PROT_WRITE | PokerFace.PROT_READ | PokerFace.PROT_EXEC);
						if (returnStatus < 0) success = false;
					}
					success = success && (newLoc >= 0);
					if (newLoc > 0) {
						MainActivity.minecraftLibBuffer = PokerFace.createDirectByteBuffer(loc, minecraftLibLength);
						minecraftTextBuffer = PokerFace.createDirectByteBuffer(newLoc, len);
						android.util.Log.i("BlockLauncher", "libminecraftpe.so mapped at 0x" + Long.toString(loc, 16));
					}
				} else {
					int returnStatus = PokerFace.mprotect(loc, len, PokerFace.PROT_WRITE | PokerFace.PROT_READ);
					if (returnStatus < 0) success = false;
				}
			}
		}
		scan.close();
		setTranslationFunction(new File(patchedDir, "tempXXXXXX").getAbsolutePath());
		return success;
	}
	private static native long remapText(long loc, long len, String tempPath);
	private static native void setTranslationFunction(String tempPath);
}
