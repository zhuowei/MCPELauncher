package net.zhuoweizhang.mcpelauncher.api.modpe;

import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.*;
import org.mozilla.javascript.annotations.*;

public class RendererManager {

	private static native void nativeModelAddBox(int rendererId, String modelPart, float xOffset, float yOffset, float zOffset,
		int width, int height, int depth, float scale, int textureX, int textureY, boolean transparent,
		float textureWidth, float textureHeight);
	private static native void nativeModelClear(int rendererId, String modelPart);
	private static native boolean nativeModelPartExists(int rendererId, String modelPart);
	private static native int nativeCreateHumanoidRenderer();
	public static native int nativeCreateItemSpriteRenderer(int itemId);
	public static native void nativeModelSetRotationPoint(int rendererId, String modelPart, float x, float y, float z);

	public static void defineClasses(Scriptable scope) throws Exception {
		ScriptableObject.defineClass(scope, NativeRendererApi.class);
		/*ScriptableObject.defineClass(scope, NativeModel.class);
		ScriptableObject.defineClass(scope, NativeModelPart.class);*/
	}

	public static class NativeRendererApi extends ScriptableObject {
		public static Map<String, NativeRenderer> renderersByName = new HashMap<String, NativeRenderer>();
		public static Map<Integer, NativeRenderer> renderersById = new HashMap<Integer, NativeRenderer>();
		public NativeRendererApi() {
		}
		@JSStaticFunction
		public static NativeRenderer get(String name) {
			try {
				int id = Integer.parseInt(name);
				return new NativeRenderer(id);
			} catch (NumberFormatException e) {
			}
			return renderersByName.get(name);
		}

		public static NativeRenderer getById(int id) {
			return renderersById.get(id);
		}

		public static NativeRenderer getByName(String name) {
			return renderersByName.get(name);
		}

		@JSStaticFunction
		public static NativeRenderer createHumanoidRenderer() {
			int id = nativeCreateHumanoidRenderer();
			return new NativeRenderer(id);
		}

		public static void register(String name, NativeRenderer renderer) {
			renderersByName.put(name, renderer);
			renderersById.put(renderer.getRenderType(), renderer);
		}
		@Override
		public String getClassName() {
			return "Renderer";
		}
	}

	public static class NativeRenderer {
		private final int rendererId;
		private String name = null;
		public NativeRenderer(int id) {
			this.rendererId = id;
		}
		public int getRenderType() {
			return rendererId;
		}
		public NativeModel getModel() {
			return new NativeModel(this.rendererId); //TODO: secondary models - e.g. armour
		}
		public void setName(String name) {
			if (name.indexOf(".") == -1) {
				throw new RuntimeException("Renderer name must be in format of author.modname.name;" +
					" for example, coolmcpemodder.sz.SwagYolo");
			}
			this.name = name;
			NativeRendererApi.register(name, this);
		}
		public String getName() {
			return name;
		}
	}
	public static class NativeModel {
		private final int rendererId;
		private NativeModel(int rendererId) {
			this.rendererId = rendererId;
		}
		public NativeModelPart getPart(String name) {
			boolean partExists = true;//nativeModelPartExists(rendererId, name);
			if (!partExists) throw new RuntimeException("The model part " + name + " does not exist.");
			return new NativeModelPart(rendererId, name);
		}
	}

	public static class NativeModelPart {
		private int rendererId;
		private String modelPartName;
		private int textureX, textureY;
		private boolean transparent;
		private float textureWidth = 64.0f, textureHeight = 32.0f;
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
				this.textureX, this.textureY, this.transparent,
				this.textureWidth, this.textureHeight);
		}

		public NativeModelPart clear() {
			nativeModelClear(rendererId, modelPartName);
			return this;
		}

		public NativeModelPart setTextureSize(float width, float height) {
			this.textureWidth = width;
			this.textureHeight = height;
			return this;
		}

		public NativeModelPart setRotationPoint(float x, float y, float z) {
			nativeModelSetRotationPoint(rendererId, modelPartName, x, y, z);
			return this;
		}
	}
}
