package net.zhuoweizhang.mcpelauncher.texture;

import java.util.*;
import org.json.*;

public class AtlasMeta {

	public JSONArray data;
	private Map<String, JSONObject> nameMap = new HashMap<String, JSONObject>();
	public boolean[] occupied;
	public int width, height;
	public int tileWidth;

	public AtlasMeta(JSONArray data) {
		this.data = data;
		try {
			calculateStuff();
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	private void calculateStuff() throws JSONException {
		JSONObject firstEntry = data.getJSONObject(0);
		JSONArray uvs = firstEntry.getJSONArray("uvs");
		JSONArray firstUv = uvs.getJSONArray(0);
		width = firstUv.getInt(4);
		height = firstUv.getInt(5);
		tileWidth = (int) (((firstUv.getDouble(2) - firstUv.getDouble(0)) * width) + 0.5);
		if (!isPowerOfTwo(tileWidth)) {
			throw new RuntimeException("Non power of two value in icon width: " + tileWidth);
		}
		occupied = new boolean[(width / tileWidth) * (height / tileWidth)];
		calculateOccupied();
	}

	private void calculateOccupied() throws JSONException {
		for (int i = 0; i < data.length(); i++) {
			JSONObject entry = data.getJSONObject(i);
			nameMap.put(entry.getString("name"), entry);
			JSONArray uvs = entry.getJSONArray("uvs");
			for (int u = 0; u < uvs.length(); u++) {
				JSONArray uv = uvs.getJSONArray(u);
				int index = uvToIndex(uv.getDouble(0), uv.getDouble(1));
				occupied[index] = true;
			}
		}
	}

	private static boolean isPowerOfTwo(int i) {
		return (i & (i - 1)) == 0; // http://stackoverflow.com/questions/600293/how-to-check-if-a-number-is-a-power-of-2
	}

	private int uvToIndex(double x, double y) {
		int xCoord = (int) (((x * width) / tileWidth) + 0.5);
		int yCoord = (int) (((y * height) / tileWidth) + 0.5);
		return yCoord * (width / tileWidth) + xCoord;
	}

	public JSONArray getOrAddIcon(String name, int index) throws JSONException {
		JSONObject entry = nameMap.get(name);
		if (entry == null) {
			entry = new JSONObject().put("name", name).put("uvs", new JSONArray());
			nameMap.put(name, entry);
			data.put(entry);
		}
		JSONArray uvs = entry.getJSONArray("uvs");
		if (!uvs.isNull(index)) return uvs.getJSONArray(index); // already added
		// find an empty spot
		int emptyIndex = 0;
		for (; emptyIndex < occupied.length; emptyIndex++) {
			if (!occupied[emptyIndex]) break;
		}
		if (emptyIndex >= occupied.length) {
			throw new RuntimeException("No more space in texture atlas; can't add " + name + "_" + index + " :(");
		}
		JSONArray uv = indexToUv(emptyIndex);
		uvs.put(index, uv);
		occupied[emptyIndex] = true;
		return uv;
	}

	private JSONArray indexToUv(int index) {
		int xCoord = index % (width / tileWidth);
		int yCoord = index / (width / tileWidth);
		double x1 = ((xCoord * tileWidth) / (double) width);
		double x2 = (((xCoord + 1) * tileWidth) / (double) width);
		double y1 = ((yCoord * tileWidth) / (double) height);
		double y2 = (((yCoord + 1) * tileWidth) / (double) height);
		try {
			JSONArray arr = new JSONArray().
				put(x1).put(y1).put(x2).put(y2).put(width).put(height);
			return arr;
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean hasIcon(String name, int index) {
		try {
			JSONObject texItem = nameMap.get(name);
			if (texItem == null) return false;
			JSONArray uvs = texItem.getJSONArray("uvs");
			if (index >= uvs.length()) return false;
			return true;
		} catch (JSONException e) {
			return false;
		}
	}
}
