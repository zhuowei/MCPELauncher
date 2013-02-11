package net.zhuoweizhang.mcpelauncher.patch;

import java.util.Arrays;

public class PatchSegment {

	public int offset;

	public byte[] data;

	public PatchSegment(int offset, byte[] data) {
		this.offset = offset;
		this.data = data;
	}

	public String toString() {
		return super.toString() + " offset=" + Integer.toString(offset, 16) + " data=" + Arrays.toString(data);
	}

}
