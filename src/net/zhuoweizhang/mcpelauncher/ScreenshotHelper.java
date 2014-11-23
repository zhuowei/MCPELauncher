package net.zhuoweizhang.mcpelauncher;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Locale;


import android.graphics.Bitmap;

import static android.opengl.GLES20.*;

import android.os.Environment;

import com.mojang.minecraftpe.MainActivity;

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

			File file = createOutputFile(fileName);
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
			runCallBack(file);
		}

		private File createOutputFile(String prefix) {
			File allPicturesFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
			File picturesFolder = new File(allPicturesFolder, "BlockLauncher");
			picturesFolder.mkdirs();

			String currentTime = new SimpleDateFormat("yyyy-MM-dd-HH-mm", Locale.US).format(new Date());
			File retFile = new File(picturesFolder, prefix + "-" + currentTime + ".png");
			int postFix = 1;
			while (retFile.exists()) {
				postFix++;
				retFile = new File(picturesFolder, prefix + "-" + currentTime + "_" + postFix + ".png");
			}
			return retFile;
		}

		private void runCallBack(File file) {
			if (MainActivity.currentMainActivity != null) {
				MainActivity main = MainActivity.currentMainActivity.get();
				if (main != null) {
					main.screenshotCallback(file);
				}
			}
		}
	}
}
