package net.zhuoweizhang.mcpelauncher;

import java.util.*;

import android.content.*;
import android.content.pm.*;

import static net.zhuoweizhang.mcpelauncher.MinecraftConstants.*;

public final class MinecraftVersion {

	public int versionCode, libLoadOffsetBegin, libLoadOffset, ipAddressOffset;
	public byte[] guiBlocksPatch, guiBlocksUnpatch, noAnimationPatch, noAnimationUnpatch;
	public boolean needsWarning;
	public PatchTranslator translator;

	public static Map<Integer, MinecraftVersion> versions = new HashMap<Integer, MinecraftVersion>();

	public final static boolean FUZZY_VERSION = false;

	public MinecraftVersion(int versionCode, boolean needsWarning, int libLoadOffsetBegin, int libLoadOffset, PatchTranslator translator,
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
		this.translator = translator;
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
		add(new MinecraftVersion(MINECRAFT_VERSION_CODE, false, LIB_LOAD_OFFSET_BEGIN, LIB_LOAD_OFFSET, null,
			0x1E7E3A, GUI_BLOCKS_PATCH, GUI_BLOCKS_UNPATCH, null, null));
		add(new MinecraftVersion(40007010, true, 0x001f0b18, 0x1000, new AmazonTranslator(), 
			0x1E7E52, GUI_BLOCKS_PATCH, GUI_BLOCKS_UNPATCH, null, null));
	}

	public static abstract class PatchTranslator {
		public abstract int get(int addr);
	}

	public static class AmazonTranslator extends PatchTranslator {
		public int get(int addr) {
			if (addr < 0xdae60) {
				return addr + (0xdadb4 - 0xdad74); // there's one more, but I really don't give a care
			} else {
				return addr + (0x174de0 - 0x174dc8);
			}
		}
	}
}
