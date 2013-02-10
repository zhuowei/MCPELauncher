package net.zhuoweizhang.mcpelauncher.patch;

public class PatchSegment {

	public int offset;

	public byte[] data;

	public PatchSegment(int offset, byte[] data) {
		this.offset = offset;
		this.data = data;
	}

}
