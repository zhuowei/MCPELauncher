package net.zhuoweizhang.mcpelauncher.patch;

import java.io.*;

public class PTPatch extends Patch {

	public static final int MAGIC_NUMBER = 0xff505450; //ff + "PTP"

	public PTPatch() {
	}

	public PTPatch(InputStream instr) throws IOException {
		this();
		load(instr);
	}

	public void load(InputStream instr) throws IOException {
		DataInputStream in = new DataInputStream(instr);
		int myMagic = in.readInt();
		if (myMagic != MAGIC_NUMBER) throw new IOException("Magic number is wrong");
		int patchCount = in.readShort() & 0xffff;
		int[] patchOffsets = new int[patchCount];
		for (int i = 0; i < patchCount; i++) {
			patchOffsets[i] = in.readInt();
		}

		for (int i = 0; i < patchOffsets.length; i++) {
			int offset = in.readInt();
			byte[] patchData;
			if (i == patchOffsets.length - 1) { //last patch
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				int b;
				while((b = in.read()) != -1) {
					bos.write(b);
				}
				patchData = bos.toByteArray();
			} else {
				int patchLength = (patchOffsets[i + 1] - patchOffsets[i]);
				patchData = new byte[patchLength];
				in.read(patchData, 0, patchLength);
			}
			segments.add(new PatchSegment(offset, patchData));
		}
	}

}
