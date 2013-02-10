package net.zhuoweizhang.mcpelauncher.patch;

import java.nio.*;

public final class PatchUtils {

	public static void patch(ByteBuffer buffer, Patch patch) {
		for (PatchSegment segment: patch.getSegments()) {
			buffer.position(segment.offset);
			buffer.put(segment.data);
		}
	}
}
