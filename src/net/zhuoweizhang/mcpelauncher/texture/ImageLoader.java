package net.zhuoweizhang.mcpelauncher.texture;

import java.io.*;
import android.graphics.*;

public interface ImageLoader {
	public Bitmap load(InputStream is) throws IOException;
	public void save(Bitmap bmp, OutputStream os) throws IOException;
}
