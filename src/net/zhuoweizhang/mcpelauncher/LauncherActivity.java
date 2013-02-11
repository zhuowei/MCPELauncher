package net.zhuoweizhang.mcpelauncher;

import java.nio.*;
import java.io.*;

import net.zhuoweizhang.mcpelauncher.patch.*;
public class LauncherActivity extends com.mojang.minecraftpe.MainActivity {

	@Override
	public void applyPatches() throws Exception {
		super.applyPatches();
		ByteBuffer buffer = minecraftLibBuffer;
		File file = new File("/sdcard/patch.mod");
		PTPatch patch = new PTPatch(file);
		PatchUtils.patch(buffer, patch);
	}

}
