package net.zhuoweizhang.mcpelauncher.texture;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import android.graphics.*;

import net.zhuoweizhang.mcpelauncher.texture.tga.*;

public class TGAImageLoader implements ImageLoader {

	public Bitmap load(InputStream is) throws IOException {
		return TGALoader.load(is, false);
	}

	public void save(Bitmap outBmp, OutputStream os) throws IOException {
		ByteBuffer data = ByteBuffer.allocate(outBmp.getWidth() * outBmp.getHeight() * 4);
		int[] tempArr = new int[outBmp.getWidth() * outBmp.getHeight()];
		outBmp.getPixels(tempArr, 0, outBmp.getWidth(), 0, 0, outBmp.getWidth(), outBmp.getHeight());
		data.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(tempArr);
		tempArr = null;
		invertBuffer(data, outBmp.getWidth(), outBmp.getHeight());
		// we used to need to convert RGB -> BGR
		// byte[] dataBytes = data.array();
		// TGAImage.swapBGR(dataBytes, outBmp.getWidth() * 4, outBmp.getHeight(), 4);
		TGAImage tgaImage = TGAImage.createFromData(outBmp.getWidth(), outBmp.getHeight(),
			true, false, data);
		tgaImage.write(Channels.newChannel(os));
	}

	private static void invertBuffer(ByteBuffer buf, int width, int height) {
		//taken from BlockLauncher's screenshot function
		byte[] rowBuffer = new byte[width * 4 * 2];
		int stride = width * 4;
		for (int y = 0; y < height / 2; ++y) {
			//exchange the rows to
			//invert the image somewhat in-place.
			buf.position(y * stride);
			buf.get(rowBuffer, 0, stride); //top row
			buf.position((height - y - 1) * stride);
			buf.get(rowBuffer, stride, stride); //bottom row
			buf.position((height - y - 1) * stride);
			buf.put(rowBuffer, 0, stride);
			buf.position(y * stride);
			buf.put(rowBuffer, stride, stride);
		}
		rowBuffer = null;
		buf.rewind();
	}
}
