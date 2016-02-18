package net.zhuoweizhang.mcpelauncher;

import org.mozilla.javascript.*;

public class BlockContextFactory extends ContextFactory {
	@Override
	protected Context makeContext() {
		Context cx = super.makeContext();
		cx.setWrapFactory(ScriptManager.getWrapFactory());
		return cx;
	}
}
