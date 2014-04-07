package net.zhuoweizhang.mcpelauncher.api.modpe;

import org.mozilla.javascript.*;
import org.mozilla.javascript.annotations.*;

public class RendererManager {

	private static native void nativeModelAddBox(int rendererId, String modelPart, float xOffset, float yOffset, float zOffset,
		int width, int height, int depth, float scale, int textureX, int textureY, boolean transparent);
	private static native void nativeModelClear(int rendererId, String modelPart);
	private static native boolean nativeModelPartExists(int rendererId, String modelPart);

	public static void defineClasses(Scriptable scope) throws Exception {
		ScriptableObject.defineClass(scope, NativeRendererApi.class);
		/*ScriptableObject.defineClass(scope, NativeModel.class);
		ScriptableObject.defineClass(scope, NativeModelPart.class);*/
	}

	private static class NativeRendererApi extends ScriptableObject {
		public NativeRendererApi() {
		}
		@JSStaticFunction
		public static NativeRenderer get(String name) {
			try {
				int id = Integer.parseInt(name);
				return new NativeRenderer(id);
			} catch (NumberFormatException e) {
			}
			return null; //TODO: named renderers
		}
		@Override
		public String getClassName() {
			return "Renderer";
		}
	}

	public static class NativeRenderer {
		private final int rendererId;
		public NativeRenderer(int id) {
			this.rendererId = id;
		}
		public int renderType() {
			return rendererId;
		}
		public NativeModel getModel() {
			return new NativeModel(this.rendererId); //TODO: secondary models - e.g. armour
		}
	}
	public static class NativeModel {
		private final int rendererId;
		private NativeModel(int rendererId) {
			this.rendererId = rendererId;
		}
		public NativeModelPart getPart(String name) {
			boolean partExists = nativeModelPartExists(rendererId, name);
			if (!partExists) throw new RuntimeException("The model part " + name + " does not exist.");
			return new NativeModelPart(rendererId, name);
		}
	}

	public static class NativeModelPart {
		private int rendererId;
		private String modelPartName;
		private int textureX, textureY;
		private boolean transparent;
		private NativeModelPart(int rendererId, String modelPartName) {
			this.rendererId = rendererId;
			this.modelPartName = modelPartName;
		}

		public NativeModelPart setTextureOffset(int textureX, int textureY) {
			return this.setTextureOffset(textureX, textureY, false);
		}

		public NativeModelPart setTextureOffset(int textureX, int textureY, boolean transparent) {
			this.textureX = textureX;
			this.textureY = textureY;
			this.transparent = transparent;
			return this;
		}

		public void addBox(float xOffset, float yOffset, float zOffset, int width, int height, int depth) {
			addBox(xOffset, yOffset, zOffset, width, height, depth, 0.0f);
		}

		public void addBox(float xOffset, float yOffset, float zOffset, int width, int height, int depth, float scale) {
			nativeModelAddBox(rendererId, modelPartName, xOffset, yOffset, zOffset, 
				width, height, depth, scale,
				this.textureX, this.textureY, this.transparent);
		}

		public NativeModelPart clear() {
			nativeModelClear(rendererId, modelPartName);
			return this;
		}
	}
}
