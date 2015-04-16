package net.zhuoweizhang.mcpelauncher;
import java.io.*;
import java.net.*;
import android.content.Context;

public class ScriptTextureDownloader implements Runnable {

	public File file;
	public URL url;
	public Runnable afterDownloadAction;
	public boolean canUseStale;

	public ScriptTextureDownloader(URL url, File file) {
		this(url, file, null, true);
	}

	public ScriptTextureDownloader(URL url, File file, Runnable afterDownloadAction, boolean canUseStale) {
		this.url = url;
		this.file = file;
		this.afterDownloadAction = afterDownloadAction;
		this.canUseStale = canUseStale;
	}

	public void run() {
		//fetches the resource, stores it into the required file
		//and then schedule a graphics reset.
		try {
			fetch();
			if (afterDownloadAction == null) {
				ScriptManager.requestGraphicsReset();
			} else {
				ScriptManager.runOnMainThread(afterDownloadAction);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void fetch() throws Exception {
		HttpURLConnection conn;
		InputStream is = null;
		FileOutputStream fos = null;
		int response = 0;
		try {
			System.out.println(url);
			String urlPath = url.getPath();
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("User-Agent", 
				"Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:24.0) Gecko/20100101 Firefox/24.0 BlockLauncher");
			int maxStale = 60 * 60 * 24 * 28; // tolerate 4-weeks stale
			if (canUseStale) {
				conn.setRequestProperty("Cache-Control", "max-stale=" + maxStale);
			}
			conn.setUseCaches(true);
			conn.setDoInput(true);

			conn.connect();
			try {
				response = conn.getResponseCode();
				is = conn.getInputStream();
			} catch (Exception e) {
				//maybe try cache?
				/*try {

				} catch (Exception ee) {
					e.printStackTrace();
					System.err.println("Also tried the cache: got");
					ee.printStackTrace();
					ScriptManager.reportScriptError(null, e);
					return;
				}*/
				e.printStackTrace();
			}

			if (response >= 400 || is == null) {
				//ScriptManager.scriptPrint("Failed to load " + url + " " + response);
				System.err.println("Failed to load " + url + " " + response + " :(");
				return;
			}

			file.getParentFile().mkdirs();

			fos = new FileOutputStream(file);

			byte[] buffer = new byte[4096];
			int count;
			while ((count = is.read(buffer)) != -1) {
				fos.write(buffer, 0, count);
			}

			fos.flush();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception e) {
				}
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception e) {
				}
			}
		}
	}

	public static void attachCache(Context context) {
		final long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
		final File httpCacheDir = new File(context.getExternalCacheDir(), "http");
		try {
			Class.forName("android.net.http.HttpResponseCache")
				.getMethod("install", File.class, long.class)
				.invoke(null, httpCacheDir, httpCacheSize);
		} catch (Exception httpResponseCacheNotAvailable) {
			try{
				com.integralblue.httpresponsecache.HttpResponseCache.install(httpCacheDir, httpCacheSize);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	public static void flushCache() {
		try {
			ResponseCache cache = ResponseCache.getDefault();
			if (cache == null) return;
			cache.getClass().getMethod("flush").invoke(cache);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
