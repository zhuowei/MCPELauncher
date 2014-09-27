package net.zhuoweizhang.mcpelauncher;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;

class Scrambler {
	static native void nativeScramble(ByteBuffer dataToScramble, MpepInfo info);
	static Reader scramble(byte[] input, MpepInfo info) throws IOException {
		ByteBuffer dataBuffer = ByteBuffer.allocateDirect(input.length);
		dataBuffer.put(input);
		dataBuffer.rewind();
		nativeScramble(dataBuffer, info);
		byte[] output = new byte[input.length];
		dataBuffer.get(output);
		String mpepStr = new String(output, Charset.forName("UTF-8"));
		if (BuildConfig.DEBUG) System.out.println(mpepStr);
		return new StringReader(mpepStr);
	}
}
