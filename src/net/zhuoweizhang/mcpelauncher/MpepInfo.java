package net.zhuoweizhang.mcpelauncher;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import java.nio.charset.*;
import org.json.*;

public class MpepInfo {
	public String modName;
	public String authorName;
	public String modVersion;
	public String modNote;
	public String scrambleCode;
	public List<String> modFiles;

	public static MpepInfo fromZip(ZipFile zip) throws IOException, JSONException {
		ZipEntry entry = zip.getEntry("metadata/metadata.json");
		if (entry == null) return null;
		InputStream is = zip.getInputStream(entry);
		byte[] buf = new byte[(int) entry.getSize()];
		is.read(buf);
		is.close();
		JSONObject jsonObj = new JSONObject(new String(buf, Charset.forName("UTF-8")));
		return fromJSON(jsonObj);
	}

	public static MpepInfo fromJSON(JSONObject json) throws JSONException {
		MpepInfo info = new MpepInfo();
		JSONObject m = json.getJSONObject("mod");
		info.modName = m.optString("mod_name");
		info.authorName = m.optString("author_name");
		info.modVersion = m.optString("mod_version");
		info.modNote = m.optString("mod_note");
		info.scrambleCode = m.optString("scramble_code");
		return info;
	}

}
