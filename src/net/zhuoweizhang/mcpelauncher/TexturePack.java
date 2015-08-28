package net.zhuoweizhang.mcpelauncher;

import java.io.Closeable;
import java.io.InputStream;
import java.io.IOException;
import java.util.*;

public interface TexturePack extends Closeable {

	public InputStream getInputStream(String fileName) throws IOException;

	public List<String> listFiles() throws IOException;

	public long getSize(String fileName) throws IOException;

}
