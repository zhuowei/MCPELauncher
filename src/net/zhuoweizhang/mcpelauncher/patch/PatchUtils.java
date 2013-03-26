package net.zhuoweizhang.mcpelauncher.patch;

import java.io.*;
import java.nio.*;

import static net.zhuoweizhang.mcpelauncher.MinecraftConstants.*;

public final class PatchUtils {

	public static void patch(ByteBuffer buffer, Patch patch) {
		for (PatchSegment segment: patch.getSegments()) {
			int newOffset = segment.offset;
			System.out.println("offset: " + Integer.toString(newOffset, 16));
			if (newOffset >= LIB_LOAD_OFFSET_BEGIN) {
				newOffset += LIB_LOAD_OFFSET;
				System.out.println("relocated offset: " + Integer.toString(newOffset, 16));
			}
			buffer.position(newOffset);
			buffer.put(segment.data);
		}
	}

	public static void patchAll(ByteBuffer buffer, File folder, int maxCount) throws IOException {
		int curCount = 0;
		for (File f: folder.listFiles()) {
			if (maxCount >= 0 && curCount >= maxCount) return;
			String fileName = f.getName().toLowerCase();
			if (!fileName.substring(fileName.length() - 4).equals(".mod")) 
				continue;
			Patch p = new PTPatch(f);
			patch(buffer, p);
			curCount++;
			System.out.println("Successfully patched " + fileName);
		}
	}

	public static void copy(File from, File to) throws IOException {
		InputStream is = new FileInputStream(from);
		int length = (int) from.length();
		byte[] data = new byte[length];
		is.read(data);
		is.close();
		OutputStream os = new FileOutputStream(to);
		os.write(data);
		os.close();
	}

	public static boolean canLivePatch(File file) throws IOException {
		com.joshuahuelsman.patchtool.PTPatch patch = new com.joshuahuelsman.patchtool.PTPatch();
		patch.loadPatch(file);
		for(int count = 0; count < patch.getNumPatches(); count++){
			int address = patch.getNextAddr();
			if (address >= LIB_LOAD_OFFSET_BEGIN) {
				return false;
			}
		}
		return true;
	}
		
}
