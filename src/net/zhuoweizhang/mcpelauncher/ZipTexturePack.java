package net.zhuoweizhang.mcpelauncher;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.*;
import java.util.zip.*;

public class ZipTexturePack implements TexturePack {

	private HashMap<String, ZipEntry> entries = new HashMap<String, ZipEntry>();
	private HashMap<String, ZipEntry> fullNameEntries = new HashMap<String, ZipEntry>();
	private ZipFile zipFile;
	private File file;

	public ZipTexturePack(File file) throws ZipException, IOException {
		this.file = file;
		this.zipFile = new ZipFile(file);
		putZipEntriesIntoMap();
	}

	private void putZipEntriesIntoMap() {
		Enumeration<? extends ZipEntry> i = zipFile.entries();
		while (i.hasMoreElements()) {
			ZipEntry entry = i.nextElement();
			if (entry.getName().contains("__MACOSX")) continue; //A pox on ye, Mac OS X
			fullNameEntries.put(entry.getName(), entry);
			entries.put(getFilenameOnly(entry.getName()), entry);
		}
	}

	public InputStream getInputStream(String fileName) throws IOException {
		ZipEntry realEntry = getEntry(fileName);
		if (realEntry == null) return null;
		return zipFile.getInputStream(realEntry);
	}

	public long getSize(String fileName) throws IOException {
		ZipEntry realEntry = getEntry(fileName);
		if (realEntry == null) return -1;
		return realEntry.getSize();
	}

	private ZipEntry getEntry(String fileName) {
		ZipEntry entry = fullNameEntries.get(fileName);
		if (entry != null) return entry;
		entry = fullNameEntries.get("assets/" + fileName);
		if (entry != null) return entry;
		if (fileName.startsWith("resource_packs/vanilla/")) {
			String newName = fileName.substring("resource_packs/vanilla/".length());
			entry = fullNameEntries.get(newName);
			if (entry != null) return entry;
		}
		//String name = getFilenameOnly(fileName);
		//return entries.get(name);
		return null;
	}

	public void close() throws IOException {
		zipFile.close();
	}

	private static String getFilenameOnly(String location) {
		String[] segments = location.split("/");
		return segments[segments.length - 1];
	}

	public List<String> listFiles() throws IOException {
		Enumeration<? extends ZipEntry> i = zipFile.entries();
		List<String> list = new ArrayList<String>();
		while (i.hasMoreElements()) {
			ZipEntry entry = i.nextElement();
			if (entry.getName().contains("__MACOSX")) continue; //A pox on ye, Mac OS X
			list.add(entry.getName());
		}
		return list;
	}

	public String getZipName() {
		return file.getName();
	}
}
