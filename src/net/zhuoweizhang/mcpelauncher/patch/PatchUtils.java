package net.zhuoweizhang.mcpelauncher.patch;

import java.io.*;
import java.nio.*;

import com.mojang.minecraftpe.MainActivity;

import net.zhuoweizhang.mcpelauncher.MaraudersMap;
import net.zhuoweizhang.mcpelauncher.MinecraftVersion;

public final class PatchUtils {

	public static MinecraftVersion minecraftVersion = null;

	private PatchUtils(){}

	private static ByteBuffer positionBuf(ByteBuffer buf, int addr) {
		if (buf == MainActivity.minecraftLibBuffer && addr >= 0 && addr < MaraudersMap.minecraftTextBuffer.capacity()) {
			buf = MaraudersMap.minecraftTextBuffer;
		}
		buf.position(addr);
		return buf;
	}

	public static void patch(ByteBuffer buf, com.joshuahuelsman.patchtool.PTPatch patch) {
		MinecraftVersion.PatchTranslator translator = minecraftVersion.translator;
		for(patch.count = 0; patch.count < patch.getNumPatches(); patch.count++){
			int addr = patch.getNextAddr();
			if (translator != null) addr = translator.get(addr);
			ByteBuffer newBuf = positionBuf(buf, addr);
			newBuf.put(patch.getNextData());
		}
	}

	public static void unpatch(ByteBuffer buf, byte[] original, com.joshuahuelsman.patchtool.PTPatch patch) {
		MinecraftVersion.PatchTranslator translator = minecraftVersion.translator;
		ByteBuffer originalBuf = ByteBuffer.wrap(original);
		for(patch.count = 0; patch.count < patch.getNumPatches(); patch.count++){
			int addr = patch.getNextAddr();
			if (translator != null) addr = translator.get(addr);
			ByteBuffer newBuf = positionBuf(buf, addr);
			originalBuf.position(addr);
			byte[] nextData = new byte[patch.getDataLength()];
			originalBuf.get(nextData);
			newBuf.put(nextData);
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
		/*
		MinecraftVersion.PatchTranslator translator = minecraftVersion.translator;
		com.joshuahuelsman.patchtool.PTPatch patch = new com.joshuahuelsman.patchtool.PTPatch();
		patch.loadPatch(file);
		for(patch.count = 0; patch.count < patch.getNumPatches(); patch.count++){
			int address = patch.getNextAddr();
			if (translator != null) address = translator.get(address);
			if (address >= minecraftVersion.libLoadOffsetBegin) {
				return false;
			}
		}
		*/
		return true;
	}

	public static byte[] createMovwInstr(int rd, int imm) {
		long instr = 0xf2400000L;
		instr |= (rd << 8); //RD
		instr |= (imm & 0xff); //Imm8
		instr |= ((imm >> 8) & 0x7) << 12; //Imm3
		instr |= ((imm >> 11) & 0x1) << 26; //i
		instr |= ((imm >> 12) & 0xf) << 16; //Imm4
		byte[] finalByte = intToLEByteArray(instr);
		System.out.println("Port patch: " + Long.toString(instr, 16));
		return finalByte;
	}

	public static final byte[] intToLEByteArray(long value) {
        return new byte[] {
                (byte)(value >>> 16),
                (byte)(value >>> 24),
                (byte)value,
                (byte)(value >>> 8)};

	}
		
}
