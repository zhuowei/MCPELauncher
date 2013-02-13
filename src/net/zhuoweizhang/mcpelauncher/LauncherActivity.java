package net.zhuoweizhang.mcpelauncher;

import java.nio.*;
import java.io.*;

import net.zhuoweizhang.mcpelauncher.patch.*;
public class LauncherActivity extends com.mojang.minecraftpe.MainActivity {

	public static final String PT_PATCHES_DIR = "ptpatches";

	@Override
	public void applyPatches() throws Exception {
		super.applyPatches();
		ByteBuffer buffer = minecraftLibBuffer;
		PatchUtils.patchAll(buffer, this.getDir(PT_PATCHES_DIR, 0), getMaxNumPatches());
	}

}
