package net.zhuoweizhang.mcpelauncher.texture;

import java.io.*;
import android.graphics.*;

public class PNGImageLoader implements ImageLoader {

	public Bitmap load(InputStream is) {
		return BitmapFactory.decodeStream(is);
	}

	public void save(Bitmap bmp, OutputStream os) {
		bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
	}
}
