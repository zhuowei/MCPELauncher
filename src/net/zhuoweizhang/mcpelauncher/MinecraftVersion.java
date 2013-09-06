package net.zhuoweizhang.mcpelauncher;

import java.io.File;

import java.util.*;

import android.content.*;
import android.content.pm.*;

import static net.zhuoweizhang.mcpelauncher.MinecraftConstants.*;

public final class MinecraftVersion {

	public int versionCode, libLoadOffsetBegin, libLoadOffset, ipAddressOffset;
	public byte[] guiBlocksPatch, guiBlocksUnpatch, noAnimationPatch, noAnimationUnpatch;
	public boolean needsWarning;
	public int portOffset;
	public PatchTranslator translator;

	public static Map<Integer, MinecraftVersion> versions = new HashMap<Integer, MinecraftVersion>();

	public final static boolean FUZZY_VERSION = false;

	public static MinecraftVersion amazonVer;
	public static Context context; //TODO remove this

	public MinecraftVersion(int versionCode, boolean needsWarning, int libLoadOffsetBegin, int libLoadOffset, PatchTranslator translator,
		int ipAddressOffset, byte[] guiBlocksPatch, byte[] guiBlocksUnpatch, byte[] noAnimationPatch, byte[] noAnimationUnpatch, int portOffset) {
		this.versionCode = versionCode;
		this.needsWarning = needsWarning;
		this.libLoadOffsetBegin = libLoadOffsetBegin;
		this.libLoadOffset = libLoadOffset;
		this.ipAddressOffset = ipAddressOffset;
		this.guiBlocksPatch = guiBlocksPatch;
		this.guiBlocksUnpatch = guiBlocksUnpatch;
		this.noAnimationPatch = noAnimationPatch;
		this.noAnimationUnpatch = noAnimationUnpatch;
		this.portOffset = portOffset;
		this.translator = translator;
	}

	public static void add(MinecraftVersion version) {
		versions.put(version.versionCode, version);
	}

	public static MinecraftVersion getRaw(int versionCode) {
		MinecraftVersion ver = versions.get(versionCode);
		if (ver == null && FUZZY_VERSION) {
			ver = getDefault();
		}
		//CHECK FOR AMAZON
		if (ver != null && ver.versionCode == MINECRAFT_VERSION_CODE && isAmazon()) {
			ver = amazonVer;
		}
		return ver;
	}

	public static MinecraftVersion get(int versionCode) {
		MinecraftVersion ver = versions.get(versionCode);
		if (ver == null) {
			ver = getDefault();
		}
		//CHECK FOR AMAZON
		if (ver.versionCode == MINECRAFT_VERSION_CODE && isAmazon()) {
			ver = amazonVer;
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
		MinecraftVersion ver = versions.get(MINECRAFT_VERSION_CODE);
		//CHECK FOR AMAZON
		if (isAmazon()) {
			ver = amazonVer;
		}
		return ver;
	}

	public static boolean isAmazon() {
		try {
			if (context == null) return false; //The main activity sets the context, prepatching is only done there so otherwise doesn't matter much
			PackageInfo mcPkgInfo = context.getPackageManager().getPackageInfo("com.mojang.minecraftpe", 0);
			return mcPkgInfo.versionCode == 40007030 && mcPkgInfo.applicationInfo.targetSdkVersion == 9; //The Amazon version shares a version code but targets Gingerbread
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	static {
		//0.7.5 Play
		add(new MinecraftVersion(MINECRAFT_VERSION_CODE, false, LIB_LOAD_OFFSET_BEGIN, LIB_LOAD_OFFSET, null,
			0x215634, GUI_BLOCKS_PATCH, GUI_BLOCKS_UNPATCH, null, null, 0xe8912));
		//0.7.5 Play Gingerbread
		add(new MinecraftVersion(30007050, false, LIB_LOAD_OFFSET_BEGIN, LIB_LOAD_OFFSET, null,
			0x215634, GUI_BLOCKS_PATCH, GUI_BLOCKS_UNPATCH, null, null, 0xe8912));
		//0.7.3 and previous versions
		add(new MinecraftVersion(40007030, false, LIB_LOAD_OFFSET_BEGIN_0_7_3, LIB_LOAD_OFFSET, null,
			0x20E6E3, GUI_BLOCKS_PATCH_0_7_3, GUI_BLOCKS_UNPATCH_0_7_3, null, null, PORT_OFFSET_0_7_3));
		add(new MinecraftVersion(30007030, false, LIB_LOAD_OFFSET_BEGIN_0_7_3, LIB_LOAD_OFFSET, null,
			0x20E6E3, GUI_BLOCKS_PATCH_0_7_3, GUI_BLOCKS_UNPATCH_0_7_3, null, null, PORT_OFFSET_0_7_3));
		add(new MinecraftVersion(MINECRAFT_VERSION_CODE_0_7_2, false, LIB_LOAD_OFFSET_BEGIN_0_7_2, LIB_LOAD_OFFSET, null,
			0x1F8624, GUI_BLOCKS_PATCH_0_7_2, GUI_BLOCKS_UNPATCH_0_7_2, null, null, PORT_OFFSET_0_7_2));
		add(new MinecraftVersion(30007010, false, 0x1f0b18, 0x1000, null,
			0x1E7E3A, GUI_BLOCKS_PATCH_0_7_1, GUI_BLOCKS_UNPATCH_0_7_1, null, null, PORT_OFFSET_0_7_1));
		add(new MinecraftVersion(40007010, true, 0x001f0b18, 0x1000, new AmazonTranslator(), 
			0x1E7E52, GUI_BLOCKS_PATCH_0_7_1, GUI_BLOCKS_UNPATCH_0_7_1, null, null, PORT_OFFSET_0_7_1_AMAZON));

		/* Amazon 0.7.3 shares a version code with Play, special case needed */
		amazonVer = new MinecraftVersion(MINECRAFT_VERSION_CODE, false, 0x243380, LIB_LOAD_OFFSET, new AmazonTranslator073(),
			0x20e6ab, GUI_BLOCKS_PATCH_0_7_3, GUI_BLOCKS_UNPATCH_0_7_3, null, null, PORT_OFFSET_0_7_3);
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

	public static class AmazonTranslator073 extends PatchTranslator {
		public int get(int addr) {
			if (addr <= 0xdd39e) {
				return addr;
			} else {
				return addr - (0xee2d8 - 0xee2a0);
			}
		}
	}
}
