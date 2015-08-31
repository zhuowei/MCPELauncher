// needs:
// map of name to position
// map of empty positions
// some way to add and remove positions from the atlas
// some sort of do init function that pulls all files from textures loaded currently, and adds/removes the meta files into the atlas
package net.zhuoweizhang.mcpelauncher.texture;

import java.io.*;
import java.util.*;

import android.graphics.*;

import org.json.*;

import com.mojang.minecraftpe.*;
import net.zhuoweizhang.mcpelauncher.*;

public class AtlasProvider implements TexturePack {

	public String metaName, atlasName, importDir;
	public boolean hasChanges = false;
	public Bitmap atlasImg;
	public AtlasMeta metaObj;
	public ImageLoader loader;
	public int xscale;
	private Canvas atlasCanvas;
	private Rect tempRect = new Rect();
	private Rect tempRect2 = new Rect();
	private Paint tempPaint = new Paint();
	private String mipPrefix;
	private int mipLevels;
	public AtlasProvider(String metaName, String atlasName, String importDir, ImageLoader loader, int xscale, int mipLevels) {
		this.metaName = metaName;
		this.atlasName = atlasName;
		this.importDir = importDir;
		this.loader = loader;
		this.xscale = xscale;
		this.mipLevels = mipLevels;
		this.mipPrefix = getMipMapPrefix(atlasName);
	}

	public InputStream getInputStream(String fileName) throws IOException {
		if (!hasChanges) return null;
		if (fileName.equals(metaName)) {
			return new ByteArrayInputStream(metaObj.data.toString().getBytes("UTF-8"));
		} else if (fileName.equals(atlasName)) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			loader.save(atlasImg, bos);
			if (BuildConfig.DEBUG && fileName.contains("terrain")) {
				FileOutputStream fos = new FileOutputStream(new File("/sdcard/terrain.tga"));
				fos.write(bos.toByteArray());
				fos.close();
			}
			return new ByteArrayInputStream(bos.toByteArray());
		} else if (mipLevels > 0 && fileName.startsWith(mipPrefix)) {
			try {
				int mipLevel = Integer.parseInt(
					fileName.substring(fileName.lastIndexOf("_mip") + 4, fileName.lastIndexOf(".")));
				if (!(mipLevel >= 0 && mipLevel < mipLevels)) return null;
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				loader.save(getMipMap(mipLevel), bos);
				return new ByteArrayInputStream(bos.toByteArray());
			} catch (NumberFormatException nfe) {
				nfe.printStackTrace();
				return null;
			}
		} else {
			return null;
		}
	}

	public List<String> listFiles() throws IOException {
		return new ArrayList<String>();
	}

	public void initAtlas(MainActivity activity) throws Exception {
		hasChanges = false;
		tempPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
		loadAtlas(activity);
		hasChanges = addAllToMeta(activity);
		atlasCanvas = null;
	}

	private void calcXScale(MainActivity activity, AtlasMeta metaArr) throws IOException {
		List<String> pathsForMeta = TextureUtils.getAllFilesFilter(activity.textureOverrides, importDir);
		int curMetaSize = (metaArr.width / metaArr.tileWidth) * (metaArr.height / metaArr.tileWidth);
		int curRemaining = (curMetaSize - metaArr.originalUVCount);
		int needed = pathsForMeta.size();
		xscale = 1;
		int cr = curRemaining;
		while (cr < needed && xscale < 64) {
			xscale *= 2;
			cr = curRemaining + (curMetaSize*(xscale-1));
		}
	}

	private void loadAtlas(MainActivity activity) throws Exception {
		// read the meta file
		InputStream metaIs = activity.getInputStreamForAsset(metaName);
		byte[] a = new byte[0x1000];
		int p;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		while ((p = metaIs.read(a)) != -1) {
			bos.write(a, 0, p);
		}
		metaIs.close();
		JSONArray metaArr = new JSONArray(new String(bos.toByteArray(), "UTF-8"));
		calcXScale(activity, new AtlasMeta(metaArr));
		scaleMeta(metaArr);
		metaObj = new AtlasMeta(metaArr);

		// read and decode the original atlas
		InputStream atlasIs = activity.getInputStreamForAsset(atlasName);
		Bitmap atlasImgRaw = loader.load(atlasIs);
		atlasImg = scaleAtlas(atlasImgRaw);
		atlasCanvas = new Canvas(atlasImg);
		atlasIs.close();
	}

	private boolean addAllToMeta(MainActivity activity) throws Exception {
		// list all files to be added
		// for each,
		// call AtlasMeta.getIconPosition to get icon position to edit into
		// then load icon and copy into texture meta
		List<String> pathsForMeta = TextureUtils.getAllFilesFilter(activity.textureOverrides, importDir);
		if (pathsForMeta.size() == 0) return false;
		Object[] nameParts = new Object[2];
		for (int i = pathsForMeta.size() - 1; i >= 0; i--) {
			String filePath = pathsForMeta.get(i);
			if (!filePath.toLowerCase().endsWith(".png")) continue;
			parseNameParts(filePath, nameParts);
			if (nameParts[0] == null) continue;
			JSONArray uv = metaObj.getOrAddIcon((String) nameParts[0], (Integer) nameParts[1]);
			Bitmap bmp = readBitmap(activity, filePath);
			if (bmp == null) continue;
			placeIntoAtlas(bmp, uv);
		}
		return true;
	}

	private void parseNameParts(String filePath, Object[] nameParts) {
		nameParts[0] = null;
		String fileName = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."));
		int underscoreIndex = fileName.lastIndexOf("_");
		if (underscoreIndex < 0) return;
		String name = fileName.substring(0, underscoreIndex);
		String number = fileName.substring(underscoreIndex + 1);
		try {
			nameParts[1] = Integer.parseInt(number);
		} catch (NumberFormatException nfe) {
			return;
		}
		nameParts[0] = name;
	}

	private Bitmap readBitmap(MainActivity activity, String filePath) throws IOException {
		InputStream is = activity.getInputStreamForAsset(filePath);
		if (is == null) return null;
		Bitmap bmp = BitmapFactory.decodeStream(is);
		is.close();
		return bmp;
	}

	private void placeIntoAtlas(Bitmap bmp, JSONArray uv) throws JSONException {
		tempRect2.left = tempRect2.top = 0;
		tempRect2.right = tempRect2.bottom = bmp.getWidth();
		tempRect.left = (int) ((uv.getDouble(0) * metaObj.width) + 0.5);
		tempRect.right = (int) ((uv.getDouble(2) * metaObj.width) + 0.5);
		tempRect.top = (int) ((uv.getDouble(1) * metaObj.height) + 0.5);
		tempRect.bottom = (int) ((uv.getDouble(3) * metaObj.height) + 0.5);
		atlasCanvas.drawBitmap(bmp, tempRect2, tempRect, tempPaint);
	}

	private void scaleMeta(JSONArray arr) throws JSONException {
		if (xscale == 1) return;
		int arrsize = arr.length();
		for (int i = 0; i < arrsize; i++) {
			JSONObject obj = arr.getJSONObject(i);
			JSONArray uvs = obj.getJSONArray("uvs");
			int uvslength = uvs.length();
			for (int j = 0; j < uvslength; j++) {
				JSONArray uv = uvs.getJSONArray(j);
				uv.put(0, uv.getDouble(0) / xscale); // x1
				uv.put(2, uv.getDouble(2) / xscale); // x2
				uv.put(4, uv.getDouble(4) * xscale); // xwidth
			}
		}
	}

	private Bitmap scaleAtlas(Bitmap in) {
		int inW = in.getWidth();
		int inH = in.getHeight();
		Bitmap newBmp = Bitmap.createBitmap(inW * xscale, inH, Bitmap.Config.ARGB_8888);
		int[] buf = new int[inW * inH];
		in.getPixels(buf, 0, inW, 0, 0, inW, inH);
		newBmp.setPixels(buf, 0, inW, 0, 0, inW, inH);
		return newBmp;
	}

	public void dumpAtlas() throws IOException {
		FileOutputStream os = new FileOutputStream(
			new File("/sdcard", "bl_atlas_dump_" + new File(atlasName).getName() + ".png"));
		atlasImg.compress(Bitmap.CompressFormat.PNG, 100, os);
		os.close();
		FileOutputStream os2 = new FileOutputStream(
			new File("/sdcard", "bl_atlas_dump_" + new File(atlasName).getName() + "mip0.png"));
		getMipMap(0).compress(Bitmap.CompressFormat.PNG, 100, os2);
		os2.close();
		FileOutputStream os3 = new FileOutputStream(
			new File("/sdcard", "bl_atlas_dump_" + new File(metaName).getName()));
		os3.write(metaObj.data.toString().getBytes("UTF-8"));
		os3.close();
	}

	private String getMipMapPrefix(String atlasName) {
		int dotIndex = atlasName.lastIndexOf(".");
		return atlasName.substring(0, dotIndex) + "_mip";
	}

	private Bitmap getMipMap(int level) {
		int newwidth = atlasImg.getWidth() >> (level + 1);
		int newheight = atlasImg.getHeight() >> (level + 1);
		return Bitmap.createScaledBitmap(atlasImg, newwidth, newheight, true);
	}

	public void close() throws IOException {
		atlasImg = null;
		metaObj = null;
		atlasCanvas = null;
		hasChanges = false;
	}

	public long getSize(String name) {
		return 0;
	}

}
