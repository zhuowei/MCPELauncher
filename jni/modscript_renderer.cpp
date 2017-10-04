#include "jni.h"
#include <cstdlib>
#include <cstring>
#include <string>
#include <vector>
#include <map>
#include <dlfcn.h>
#include <android/log.h>

#include "mcpelauncher.h"
#define cppbool bool

#include "modscript_renderer_jni.h"
#include "modscript_shared.h"
#include "mcpe/mce/textureptr.h"
#include "mcpe/itemspriterenderer.h"
#include "mcpe/guidata.h"
#include "logutil.h"

// search for HumanoidMobRenderer::HumanoidMobRenderer
#define MOBRENDERER_SIZE (sizeof(HumanoidMobRenderer))
// ModelPart::addBox
#define MODELPART_CUBEVECTOR_OFFSET 56
// ModelPart destructor
#define MODELPART_MESHBUFFER_OFFSET 104
static const int kEntityRenderDispatcher_renderersOffset = 4;

extern "C" {
#define DLSYM_DEBUG
#ifdef DLSYM_DEBUG

void* debug_dlsym(void* handle, const char* symbol);

#define dlsym debug_dlsym
#endif //DLSYM_DEBUG

EntityRenderer* (*bl_EntityRenderDispatcher_getRenderer)(void*, int);

static void (*bl_Mesh_reset)(void*);

static void (*bl_HumanoidMobRenderer_HumanoidMobRenderer)(MobRenderer*, std::unique_ptr<HumanoidModel>,
		std::unique_ptr<HumanoidModel>, std::unique_ptr<HumanoidModel>, mce::TexturePtr, float);
static void (*bl_ModelPart_reset)(ModelPart*);

static std::vector<EntityRenderer*> bl_entityRenderers;

static std::map<long long, int> bl_renderTypeMap;
static std::unordered_map<int, int> bl_itemSpriteRendererTypeMap;

static EntityRenderer* (*bl_EntityRenderDispatcher_getRenderer_entity_real)(void*, Entity*);
class BaseEntityRenderContext;
static void* (*bl_EntityRenderDispatcher_render_real)(void* renderDispatcher, BaseEntityRenderContext&, Entity&, Vec3 const&, Vec2 const&);
class GeometryGroup;
class BlockTessellator;
static void (*bl_EntityRenderDispatcher_initializeEntityRenderers_real)(void* self, GeometryGroup& geometryGroup,
	mce::TextureGroup& textureGroup, BlockTessellator& blockTessellator);

static ModelPart* bl_renderManager_getModelPart_impl(int rendererId, const char* modelPartName, HumanoidModel** modelPtr) {
	MobRenderer* renderer;
	if (rendererId < 0x1000) {
		renderer = (MobRenderer*) bl_EntityRenderDispatcher_getRenderer(&bl_minecraft->getEntityRenderDispatcher(), rendererId);
	} else {
		renderer = (MobRenderer*) bl_entityRenderers[rendererId - 0x1000];
	}
	HumanoidModel* model = (HumanoidModel*) renderer->model; //TODO: make sure that this is indeed a humanoid model
	*modelPtr = model;
	if (strcmp(modelPartName, "head") == 0) {
		return &model->bipedHead;
	} else if (strcmp(modelPartName, "headwear") == 0) {
		return &model->bipedHeadwear;
	} else if (strcmp(modelPartName, "body") == 0) {
		return &model->bipedBody;
	} else if (strcmp(modelPartName, "rightArm") == 0) {
		return &model->bipedRightArm;
	} else if (strcmp(modelPartName, "leftArm") == 0) {
		return &model->bipedLeftArm;
	} else if (strcmp(modelPartName, "rightLeg") == 0) {
		return &model->bipedRightLeg;
	} else if (strcmp(modelPartName, "leftLeg") == 0) {
		return &model->bipedLeftLeg;
	} else {
		return NULL;
	}
	return NULL;
}

ModelPart* bl_renderManager_getModelPart(int rendererId, const char* modelPartName) {
	HumanoidModel* modelPtr = nullptr;
	ModelPart* retval = bl_renderManager_getModelPart_impl(rendererId, modelPartName, &modelPtr);
	/*
	if (retval && !retval->model) {
		__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "Renderer id %d part %s doesn't have a model\n",
			rendererId, modelPartName);
		retval->model = modelPtr;
	}
	*/
	return retval;
}

void bl_renderManager_invalidateModelPart(ModelPart* part) {
	void* meshBuffer = (void*) ((uintptr_t) part + MODELPART_MESHBUFFER_OFFSET);
	bl_Mesh_reset(meshBuffer);
}

int bl_renderManager_addRenderer(EntityRenderer* renderer) {
	bl_entityRenderers.push_back(renderer);
	int rendererId = 0x1000 + (bl_entityRenderers.size() - 1);
	return rendererId;
}

int bl_renderManager_createHumanoidRenderer() {
	BL_LOG("Create renderer");
	HumanoidModel* model = new HumanoidModel(0, 0, 64, 64);

	HumanoidModel* model2 = new HumanoidModel(0, 0, 64, 64);

	HumanoidModel* model3 = new HumanoidModel(0, 0, 64, 64);

	//MobRenderer* sacrificialRenderer =
	//	(MobRenderer*) bl_EntityRenderDispatcher_getRenderer(*bl_EntityRenderDispatcher_instance, 3 /* human */);

	MobRenderer* renderer = (MobRenderer*) operator new(MOBRENDERER_SIZE);
	bl_HumanoidMobRenderer_HumanoidMobRenderer(renderer, std::unique_ptr<HumanoidModel>(model),
		std::unique_ptr<HumanoidModel>(model2),
		std::unique_ptr<HumanoidModel>(model3),
		mce::TexturePtr(bl_minecraft->getTextures(), ResourceLocation("textures/entity/chest/double_normal")), 0);
	BL_LOG("Adding renderer");
	int retval = bl_renderManager_addRenderer((EntityRenderer*) renderer);
	BL_LOG("Created renderer %d", retval);
	return retval;
}

Item** bl_getItemsArray();

int bl_renderManager_createItemSpriteRenderer(int itemId) {
	Item** mItems = bl_getItemsArray();
	if (!mItems[itemId]) return -1;
	ItemSpriteRenderer* renderer = new ItemSpriteRenderer(bl_minecraft->getTextures(), mItems[itemId], false);
	int retval = bl_renderManager_addRenderer((EntityRenderer*) renderer);
	bl_itemSpriteRendererTypeMap[itemId] = retval;
	return retval;
}

int bl_renderManager_renderTypeForItemSprite(int itemId) {
	return bl_itemSpriteRendererTypeMap[itemId];
}
//static MobRenderer* bl_stupidRenderer;
EntityRenderer* bl_EntityRenderDispatcher_getRenderer_hook(void* dispatcher, Entity* entity) {
	int renderType = entity->renderType;
	if (renderType >= 0x1000) {
		return bl_entityRenderers[renderType - 0x1000];
	}
	return bl_EntityRenderDispatcher_getRenderer_entity_real(dispatcher, entity);
}

EntityRenderer* bl_EntityRenderDispatcher_getRenderer_EntityRendererId_hook(void* dispatcher, int renderType) {
	if (renderType >= 0x1000) {
		return bl_entityRenderers[renderType - 0x1000];
	}
	return bl_EntityRenderDispatcher_getRenderer(dispatcher, renderType);
}
//static void* getMCPERenderType(int renderType) {
//	return bl_EntityRenderDispatcher_getRenderer_EntityRenderId(*bl_EntityRenderDispatcher_instance, renderType);
//}
static const int kTempRenderType = 16; // zombie

void* bl_EntityRenderDispatcher_render_hook(void* renderDispatcher, BaseEntityRenderContext& context, Entity& entity,
	Vec3 const& pos, Vec2 const& rot) {
	auto entityId = entity.getUniqueID();
	int oldRenderType = entity.renderType;
	if (entity.renderType == kTempRenderType || bl_renderTypeMap.count(entityId) == 0) {
		return bl_EntityRenderDispatcher_render_real(renderDispatcher, context, entity, pos, rot);
	}
	auto renderers = (EntityRenderer**)(((uintptr_t)renderDispatcher) + kEntityRenderDispatcher_renderersOffset);
	MobRenderer* tntRenderer = (MobRenderer*)renderers[kTempRenderType]; // Zombie
	MobRenderer* newRenderer = (MobRenderer*)bl_entityRenderers[bl_renderTypeMap[entityId] - 0x1000];
	renderers[kTempRenderType] = newRenderer;
	entity.renderType = kTempRenderType; // steal Zombie's render type

	auto savedMat = static_cast<HumanoidModel*>(newRenderer->model)->activeMaterial;

	static_cast<HumanoidModel*>(newRenderer->model)->activeMaterial =
		static_cast<HumanoidModel*>(tntRenderer->model)->activeMaterial;

	void* retval = bl_EntityRenderDispatcher_render_real(renderDispatcher, context, entity, pos, rot);
/*
	BL_LOG("Done rendering custom!");
*/

	renderers[kTempRenderType] = tntRenderer; // restore
	//memcpy(newRenderer, tmpBlob, sizeToBackup);
	//tntRenderer->model = oldModel;
	entity.renderType = oldRenderType;
	static_cast<HumanoidModel*>(newRenderer->model)->activeMaterial = savedMat;
	return retval;
}

bool bl_renderManager_setRenderType(Entity* entity, int renderType) {
	long long entityId = entity->getUniqueID();
	if (renderType >= 0x1000) {
		if ((renderType - 0x1000) >= bl_entityRenderers.size()) {
			__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "Renderer id %d is over size of %d",
				renderType, bl_entityRenderers.size());
			return false;
		}
		bl_renderTypeMap[entityId] = renderType;
		entity->renderType = renderType;
	} else {
		//if (!getMCPERenderType(renderType)) return false;
		bl_renderTypeMap.erase(entityId);
		entity->renderType = renderType;
	}
	return true;
}

int bl_renderManager_getRenderType(Entity* entity) {
	long long entityId = entity->getUniqueID();
	if (bl_renderTypeMap.count(entityId) != 0) {
		return bl_renderTypeMap[entityId];
	}
	return entity->renderType;
}

void bl_renderManager_clearRenderTypes() {
	bl_renderTypeMap.clear();
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_api_modpe_RendererManager_nativeModelAddBox
  (JNIEnv *env, jclass clazz, jint rendererId, jstring modelPartName, jfloat xOffset, jfloat yOffset, jfloat zOffset,
    jint width, jint height, jint depth, jfloat scale, jint textureX, jint textureY, jboolean transparent,
    jfloat textureWidth, jfloat textureHeight) {
	const char * utfChars = env->GetStringUTFChars(modelPartName, NULL);
	HumanoidModel* model = nullptr;
	ModelPart* part = bl_renderManager_getModelPart_impl(rendererId, utfChars, &model);
	part->textureOffsetX = textureX;
	part->textureOffsetY = textureY;
	//part->transparent = transparent;
	//if (transparent) part->material = &(model->materialAlphaTest);
	if (textureWidth > 0) part->textureWidth = textureWidth;
	if (textureHeight > 0) part->textureHeight = textureHeight;
	part->addBox(Vec3(xOffset, yOffset, zOffset), Vec3(width, height, depth), scale);
	bl_renderManager_invalidateModelPart(part);
	env->ReleaseStringUTFChars(modelPartName, utfChars);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_api_modpe_RendererManager_nativeModelClear
  (JNIEnv *env, jclass clazz, jint rendererId, jstring modelPartName) {
	const char * utfChars = env->GetStringUTFChars(modelPartName, NULL);
	ModelPart* part = bl_renderManager_getModelPart(rendererId, utfChars);
	std::vector<Cube*>* cubeVector = (std::vector<Cube*>*) ((uintptr_t) part + MODELPART_CUBEVECTOR_OFFSET);
	cubeVector->clear();
	bl_renderManager_invalidateModelPart(part);
	env->ReleaseStringUTFChars(modelPartName, utfChars);
}

JNIEXPORT jboolean JNICALL Java_net_zhuoweizhang_mcpelauncher_api_modpe_RendererManager_nativeModelPartExists
  (JNIEnv *env, jclass clazz, jint rendererId, jstring modelPartName) {
	jboolean exists;
	const char * utfChars = env->GetStringUTFChars(modelPartName, NULL);
	ModelPart* part = bl_renderManager_getModelPart(rendererId, utfChars);
	exists = part != NULL;
	env->ReleaseStringUTFChars(modelPartName, utfChars);
	return exists;
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_api_modpe_RendererManager_nativeCreateHumanoidRenderer
  (JNIEnv *env, jclass clazz) {
	return bl_renderManager_createHumanoidRenderer();
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_api_modpe_RendererManager_nativeCreateItemSpriteRenderer
  (JNIEnv *env, jclass clazz, jint itemId) {
	return bl_renderManager_createItemSpriteRenderer(itemId);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_api_modpe_RendererManager_nativeModelSetRotationPoint
  (JNIEnv *env, jclass clazz, jint rendererId, jstring modelPartName, jfloat x, jfloat y, jfloat z) {
	/* FIXME 1.2.0
	const char * utfChars = env->GetStringUTFChars(modelPartName, NULL);
	ModelPart* part = bl_renderManager_getModelPart(rendererId, utfChars);
	part->offsetX = x;
	part->offsetY = y;
	part->offsetZ = z;
	bl_renderManager_invalidateModelPart(part);
	env->ReleaseStringUTFChars(modelPartName, utfChars);
	*/
}
class GeometryPtr {
	char filler[8];
};
class GeometryGroup {
public:
	GeometryPtr getGeometry(std::string const&);
};

#if 0
static void bl_buildAStupidRenderer(void* renderDispatcher, GeometryGroup& geometryGroup,
	mce::TextureGroup& textureGroup) {
	auto renderers = (EntityRenderer**)(((uintptr_t)renderDispatcher) + kEntityRenderDispatcher_renderersOffset);
	BL_LOG("Building a stupid renderer");
	auto group = geometryGroup.getGeometry("geometry.zombie");
	BL_LOG("Zero!");
	HumanoidModel* model = new HumanoidModel(group/*0, 0, 64, 64*/);
	BL_LOG("One!");

	HumanoidModel* model2 = new HumanoidModel(geometryGroup.getGeometry("geometry.humanoid.armor1")/*0, 0, 64, 64*/);
	BL_LOG("Two!");

	HumanoidModel* model3 = new HumanoidModel(geometryGroup.getGeometry("geometry.humanoid.armor2")/*0, 0, 64, 64*/);
	BL_LOG("Three!");

	MobRenderer* renderer = (MobRenderer*) operator new(MOBRENDERER_SIZE);
	bl_HumanoidMobRenderer_HumanoidMobRenderer(renderer, std::unique_ptr<HumanoidModel>(model),
		std::unique_ptr<HumanoidModel>(model2),
		std::unique_ptr<HumanoidModel>(model3),
		textureGroup.getTexture(ResourceLocation("textures/entity/zombie/zombie"), false), 0);
	BL_LOG("Four!");
	bl_stupidRenderer = renderer;
	//renderers[kTempRenderType] = renderer;
}

void bl_EntityRenderDispatcher_initializeEntityRenderers_hook(void* self, GeometryGroup& geometryGroup,
	mce::TextureGroup& textureGroup, BlockTessellator& blockTessellator) {
	bl_EntityRenderDispatcher_initializeEntityRenderers_real(self, geometryGroup, textureGroup, blockTessellator);
	bl_buildAStupidRenderer(self, geometryGroup, textureGroup);
}

#endif

void bl_renderManager_init(void* mcpelibhandle) {
	bl_EntityRenderDispatcher_getRenderer = (EntityRenderer* (*) (void*, int))
		dlsym(mcpelibhandle, "_ZN22EntityRenderDispatcher11getRendererE16EntityRendererId");
	bl_EntityRenderDispatcher_getRenderer_entity_real = (EntityRenderer* (*)(void*, Entity*))
		dlsym(mcpelibhandle, "_ZN22EntityRenderDispatcher11getRendererER6Entity");
	bl_Mesh_reset = (void (*)(void*))
		dlsym(mcpelibhandle, "_ZN3mce4Mesh5resetEv");
	bl_HumanoidMobRenderer_HumanoidMobRenderer = (void (*)(MobRenderer*, std::unique_ptr<HumanoidModel>,
		std::unique_ptr<HumanoidModel>, std::unique_ptr<HumanoidModel>, mce::TexturePtr, float))
		dlsym(mcpelibhandle,
			"_ZN19HumanoidMobRendererC1ESt10unique_ptrI13HumanoidModelSt14default_deleteIS1_EES4_S4_N3mce10TexturePtrEf");
/*
	void* getRenderer = dlsym(mcpelibhandle, "_ZN22EntityRenderDispatcher11getRendererER6Entity");
	mcpelauncher_hook(getRenderer, (void*) bl_EntityRenderDispatcher_getRenderer_hook,
		(void**) &bl_EntityRenderDispatcher_getRenderer_real);
*/
	// FIXME 1.2
	bl_EntityRenderDispatcher_render_real = (void* (*)(void*, BaseEntityRenderContext&, Entity&, Vec3 const&, Vec2 const&))
		dlsym(mcpelibhandle, "_ZN22EntityRenderDispatcher6renderER23BaseEntityRenderContextR6EntityRK4Vec3RK4Vec2");
	if (!bl_patch_got((soinfo2*)mcpelibhandle, (void*)bl_EntityRenderDispatcher_render_real, (void*)&bl_EntityRenderDispatcher_render_hook)) {
		__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "can't hook render");
		abort();
	}
	bl_ModelPart_reset = (void (*)(ModelPart*))
		dlsym(mcpelibhandle, "_ZN9ModelPart5resetEv");

	if (!bl_patch_got((soinfo2*)mcpelibhandle, (void*)bl_EntityRenderDispatcher_getRenderer, (void*)&bl_EntityRenderDispatcher_getRenderer_EntityRendererId_hook)) {
		__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "can't hook getRenderer");
		abort();
	}
	/* Crashes on Intel!
	bl_patch_got((soinfo2*)mcpelibhandle, (void*)bl_EntityRenderDispatcher_getRenderer_entity_real,
		(void*)&bl_EntityRenderDispatcher_getRenderer_hook);
	*/
/*
	void* initializeEntityRenderers = dlsym(mcpelibhandle,
		"_ZN22EntityRenderDispatcher25initializeEntityRenderersER13GeometryGroupRN3mce12TextureGroupER16BlockTessellator");
	mcpelauncher_hook((void*)initializeEntityRenderers, (void*)&bl_EntityRenderDispatcher_initializeEntityRenderers_hook,
		(void**)&bl_EntityRenderDispatcher_initializeEntityRenderers_real);
*/
}

} //extern "C"
