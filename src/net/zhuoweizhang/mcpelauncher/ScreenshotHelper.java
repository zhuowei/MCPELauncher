package net.zhuoweizhang.mcpelauncher;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;

import android.graphics.Bitmap;

import android.opengl.GLES11;

import static android.opengl.GLES11.*;

import android.util.Log;

public class ScreenshotHelper {

	public static void takeScreenshot(String fileName) {
		//grab the current screen size
		int[] screenDim = new int[4];
		glGetIntegerv(GL_VIEWPORT, screenDim, 0);
		//build a buffer to hold the screenshot
		ByteBuffer buf = ByteBuffer.allocateDirect(screenDim[2] * screenDim[3] * 4); //4 bytes per pixel
		//based partially on Spout's Caustic screenshot utility
		glReadPixels(screenDim[0], screenDim[1], screenDim[2], screenDim[3], GL_RGBA, GL_UNSIGNED_BYTE, buf);

		//now write this to a file - hand off to new thread for processing

		new Thread(new ScreenshotWriter(screenDim, buf, fileName)).start();

	}

	private static final class ScreenshotWriter implements Runnable {
		private int[] screenDim;
		private ByteBuffer buf;
		private String fileName;
		public ScreenshotWriter (int[] screenDim, ByteBuffer buf, String fileName) {
			this.screenDim = screenDim;
			this.buf = buf;
			this.fileName = fileName;
		}

		public void run() {
			int width = screenDim[2];
			int height = screenDim[3];
			
			Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			buf.rewind();
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

			bmp.copyPixelsFromBuffer(buf);
			buf = null;

			File file = new File("/sdcard/" + fileName + "-" + System.currentTimeMillis() + ".png");
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(file);
				bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (fos != null) {
					try {
						fos.close();
					} catch (IOException e) {}
				}
			}
			bmp.recycle();
			System.gc();
		}
	}
}
