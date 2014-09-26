package net.zhuoweizhang.mcpelauncher;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;

public class Scrambler {
	public static native void nativeScramble(ByteBuffer dataToScramble, ByteBuffer key);
	public static Reader scramble(byte[] input, MpepInfo info) throws IOException {
		byte[] key = (info.authorName + Integer.parseInt(info.scrambleCode)).getBytes(Charset.forName("UTF-8"));
		ByteBuffer keyBuffer = ByteBuffer.allocateDirect(key.length);
		keyBuffer.put(key);
		keyBuffer.rewind();
		ByteBuffer dataBuffer = ByteBuffer.allocateDirect(input.length);
		dataBuffer.put(input);
		dataBuffer.rewind();
		nativeScramble(dataBuffer, keyBuffer);
		byte[] output = new byte[input.length];
		dataBuffer.get(output);
		String mpepStr = new String(output, Charset.forName("UTF-8"));
		if (BuildConfig.DEBUG) System.out.println(mpepStr);
		return new StringReader(mpepStr);
	}
}
