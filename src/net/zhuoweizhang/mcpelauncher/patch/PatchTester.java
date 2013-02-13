package net.zhuoweizhang.mcpelauncher.patch;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
public final class PatchTester {

	public static void main(String[] args) {
		try {
			PTPatch patch = new PTPatch(new File(args[1]));
			RandomAccessFile mpeFile = new RandomAccessFile(new File(args[0]), "rw");
			FileChannel mpeChannel = mpeFile.getChannel();
			MappedByteBuffer buffer = mpeChannel.map(FileChannel.MapMode.READ_WRITE, 0, mpeChannel.size());
			PatchUtils.patch(buffer, patch);
			buffer.force();
			mpeChannel.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
