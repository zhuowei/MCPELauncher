package net.zhuoweizhang.mcpelauncher.patch;

import java.io.*;
import java.util.*;

public abstract class Patch {

	protected List<PatchSegment> segments = new ArrayList<PatchSegment>();

	public List<PatchSegment> getSegments() {
		return segments;
	}

}
