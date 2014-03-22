package net.zhuoweizhang.mcpelauncher;

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

	public final static boolean FUZZY_VERSION = true;

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
		/*try {
			if (context == null) return false; //The main activity sets the context, prepatching is only done there so otherwise doesn't matter much
			PackageInfo mcPkgInfo = context.getPackageManager().getPackageInfo("com.mojang.minecraftpe", 0);
			return mcPkgInfo.versionCode == 40007050 && mcPkgInfo.applicationInfo.targetSdkVersion == 9; //The Amazon version shares a version code but targets Gingerbread
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}*/
		return false;
	}

	static {
		//0.8.1 Play
		add(new MinecraftVersion(MINECRAFT_VERSION_CODE, false, LIB_LOAD_OFFSET_BEGIN, LIB_LOAD_OFFSET, null,
			-1, null, null, null, null, -1));
		//0.8.1 Gingerbread
		add(new MinecraftVersion(300801011, false, LIB_LOAD_OFFSET_BEGIN, LIB_LOAD_OFFSET, null,
			-1, null, null, null, null, -1));
		//0.8.1 Amazon
		add(new MinecraftVersion(400801011, false, LIB_LOAD_OFFSET_BEGIN, LIB_LOAD_OFFSET, new AmazonTranslator080(),
			-1, null, null, null, null, -1));
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

	public static class AmazonTranslator080 extends PatchTranslator {
		//Amazon is missing code for vibrate, so everything is shifted by a few bytes (0x38, I think)
		public int get(int addr) {
			return addr - (0x1102c8 - 0x1102a0);
		}
	}
}
