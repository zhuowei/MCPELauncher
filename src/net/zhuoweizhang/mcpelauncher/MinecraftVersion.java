package net.zhuoweizhang.mcpelauncher;

import java.util.*;

import android.content.*;
import android.content.pm.*;

import static net.zhuoweizhang.mcpelauncher.MinecraftConstants.*;

public final class MinecraftVersion {

	public int versionCode, libLoadOffsetBegin, libLoadOffset, ipAddressOffset;
	public byte[] guiBlocksPatch, guiBlocksUnpatch, noAnimationPatch, noAnimationUnpatch;
	public boolean needsWarning;

	public static Map<Integer, MinecraftVersion> versions = new HashMap<Integer, MinecraftVersion>();

	public MinecraftVersion(int versionCode, boolean needsWarning, int libLoadOffsetBegin, int libLoadOffset, 
		int ipAddressOffset, byte[] guiBlocksPatch, byte[] guiBlocksUnpatch, byte[] noAnimationPatch, byte[] noAnimationUnpatch) {
		this.versionCode = versionCode;
		this.needsWarning = needsWarning;
		this.libLoadOffsetBegin = libLoadOffsetBegin;
		this.libLoadOffset = libLoadOffset;
		this.ipAddressOffset = ipAddressOffset;
		this.guiBlocksPatch = guiBlocksPatch;
		this.guiBlocksUnpatch = guiBlocksUnpatch;
		this.noAnimationPatch = noAnimationPatch;
		this.noAnimationUnpatch = noAnimationUnpatch;
	}

	public static void add(MinecraftVersion version) {
		versions.put(version.versionCode, version);
	}

	public static MinecraftVersion getRaw(int versionCode) {
		return versions.get(versionCode);
	}

	public static MinecraftVersion get(int versionCode) {
		MinecraftVersion ver = versions.get(versionCode);
		if (ver == null) {
			ver = getDefault();
		}
		return ver;
	}

	public static MinecraftVersion get(Context context) {
		try {
			PackageInfo mcPkgInfo = context.getPackageManager().getPackageInfo("com.mojang.minecraftpe", 0);
			int minecraftVersionCode = mcPkgInfo.versionCode;
			return get(minecraftVersionCode);
		} catch (Exception e) {
			return getDefault();
		}
	}

	public static MinecraftVersion getDefault() {
		return versions.get(MINECRAFT_VERSION_CODE);
	}

	static {
		add(new MinecraftVersion(MINECRAFT_VERSION_CODE, false, LIB_LOAD_OFFSET_BEGIN, LIB_LOAD_OFFSET,
			0x1E7E3A, GUI_BLOCKS_PATCH, GUI_BLOCKS_UNPATCH, null, null));
	}
}
