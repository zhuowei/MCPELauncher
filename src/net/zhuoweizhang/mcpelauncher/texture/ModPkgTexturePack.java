package net.zhuoweizhang.mcpelauncher.texture;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipException;

import net.zhuoweizhang.mcpelauncher.*;

public class ModPkgTexturePack implements TexturePack {

	public List<ZipTexturePack> subPacks = new ArrayList<ZipTexturePack>();

	public void addPackage(File file) throws ZipException, IOException {
		subPacks.add(new ZipTexturePack(file));
	}

	public void removePackage(String fileName) throws IOException {
		for (int i = subPacks.size() - 1; i >= 0; --i) {
			ZipTexturePack pack = subPacks.get(i);
			if (!pack.getZipName().equals(fileName)) continue;
			pack.close();
			subPacks.remove(i);
			break;
		}
	}

	public InputStream getInputStream(String fileName) throws IOException {
		for (ZipTexturePack pack: subPacks) {
			InputStream is = pack.getInputStream(fileName);
			if (is != null) return is;
		}
		return null;
	}

	public long getSize(String fileName) throws IOException {
		for (ZipTexturePack pack: subPacks) {
			long size = pack.getSize(fileName);
			if (size != -1) return size;
		}
		return -1;
	}

	public void close() throws IOException {
		for (ZipTexturePack pack: subPacks) {
			pack.close();
		}
		subPacks.clear();
	}

	public List<String> listFiles() throws IOException {
		List<String> retval = new ArrayList<String>();
		for (ZipTexturePack pack: subPacks) {
			retval.addAll(pack.listFiles());
		}
		return retval;
	}
}
