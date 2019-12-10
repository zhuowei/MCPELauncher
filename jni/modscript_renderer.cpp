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
#include "mcpe/itemregistry.h"
#include "logutil.h"

// search for HumanoidMobRenderer::HumanoidMobRenderer
#define MOBRENDERER_SIZE (sizeof(HumanoidMobRenderer))
// ModelPart destructor
#define MODELPART_MESHBUFFER_OFFSET 148
static const int kEntityRenderDispatcher_renderersOffset = 4;

extern "C" {
#define DLSYM_DEBUG
#ifdef DLSYM_DEBUG

void* debug_dlsym(void* handle, const char* symbol);

#define dlsym debug_dlsym
#endif //DLSYM_DEBUG

EntityRenderer* (*bl_EntityRenderDispatcher_getRenderer)(void*, int);

static void (*bl_Mesh_reset)(void*);

static void (*bl_ModelPart_reset)(ModelPart*);

static std::vector<EntityRenderer*> bl_entityRenderers;

static std::map<long long, int> bl_renderTypeMap;
static std::unordered_map<int, int> bl_itemSpriteRendererTypeMap;

static EntityRenderer* (*bl_EntityRenderDispatcher_getRenderer_entity_real)(void*, Entity*);
class BaseActorRenderContext;
static void* (*bl_EntityRenderDispatcher_render_real)(void* renderDispatcher, BaseActorRenderContext&, Actor&, Vec3 const&, Vec2 const&);
class GeometryGroup;
class BlockTessellator;
static void (*bl_EntityRenderDispatcher_initializeEntityRenderers_real)(void* self, GeometryGroup& geometryGroup,
	mce::TextureGroup& textureGroup, BlockTessellator& blockTessellator);

static ModelPart* bl_renderManager_getModelPart_impl(int rendererId, const char* modelPartName, HumanoidModel** modelPtr) {
#if 0
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
#endif
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
	return -1;
#if 0
	BL_LOG("Create renderer");
	HumanoidModel* model = new HumanoidModel(0, 0, 64, 64);

	HumanoidModel* model2 = new HumanoidModel(0, 0, 64, 64);

	HumanoidModel* model3 = new HumanoidModel(0, 0, 64, 64);

	//MobRenderer* sacrificialRenderer =
	//	(MobRenderer*) bl_EntityRenderDispatcher_getRenderer(*bl_EntityRenderDispatcher_instance, 3 /* human */);

	MobRenderer* renderer = new HumanoidMobRenderer(std::unique_ptr<HumanoidModel>(model),
		std::unique_ptr<HumanoidModel>(model2),
		std::unique_ptr<HumanoidModel>(model3),
		mce::TexturePtr(bl_minecraft->getTextures(), ResourceLocation("textures/entity/chest/double_normal")),
		Vec2{0, 0}, Vec3{0, 0, 0});

	BL_LOG("Adding renderer");
	int retval = bl_renderManager_addRenderer((EntityRenderer*) renderer);
	BL_LOG("Created renderer %d", retval);
	return retval;
#endif
}

Item** bl_getItemsArray();

int bl_renderManager_createItemSpriteRenderer(int itemId) {
	return -1; // FIXME 1.8
	Item* item = ItemRegistry::getItem((short)itemId).get();
	if (!item) {
		BL_LOG("No item sprite item?");
		return -1;
	}
	ItemSpriteRenderer* renderer = new ItemSpriteRenderer(bl_minecraft->getTextures(), item, false);
	int retval = bl_renderManager_addRenderer((EntityRenderer*) renderer);
	bl_itemSpriteRendererTypeMap[itemId] = retval;
	return retval;
}

int bl_renderManager_renderTypeForItemSprite(int itemId) {
	return bl_itemSpriteRendererTypeMap[itemId];
}
//static MobRenderer* bl_stupidRenderer;
EntityRenderer* bl_EntityRenderDispatcher_getRenderer_hook(void* dispatcher, Entity* entity) {
	abort();
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

void* bl_EntityRenderDispatcher_render_hook(void* renderDispatcher, BaseActorRenderContext& context, Actor& entity,
	Vec3 const& pos, Vec2 const& rot) {
	abort();
}

bool bl_renderManager_setRenderType(Entity* entity, int renderType) {
	return false; // FIXME 1.8
}

int bl_renderManager_getRenderType(Entity* entity) {
	return -1;
}

void bl_renderManager_clearRenderTypes() {
	bl_renderTypeMap.clear();
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_api_modpe_RendererManager_nativeModelAddBox
  (JNIEnv *env, jclass clazz, jint rendererId, jstring modelPartName, jfloat xOffset, jfloat yOffset, jfloat zOffset,
    jint width, jint height, jint depth, jfloat scale, jint textureX, jint textureY, jboolean transparent,
    jfloat textureWidth, jfloat textureHeight) {
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_api_modpe_RendererManager_nativeModelClear
  (JNIEnv *env, jclass clazz, jint rendererId, jstring modelPartName) {
}

JNIEXPORT jboolean JNICALL Java_net_zhuoweizhang_mcpelauncher_api_modpe_RendererManager_nativeModelPartExists
  (JNIEnv *env, jclass clazz, jint rendererId, jstring modelPartName) {
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_api_modpe_RendererManager_nativeCreateHumanoidRenderer
  (JNIEnv *env, jclass clazz) {
	return -1;
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_api_modpe_RendererManager_nativeCreateItemSpriteRenderer
  (JNIEnv *env, jclass clazz, jint itemId) {
	return bl_renderManager_createItemSpriteRenderer(itemId);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_api_modpe_RendererManager_nativeModelSetRotationPoint
  (JNIEnv *env, jclass clazz, jint rendererId, jstring modelPartName, jfloat x, jfloat y, jfloat z) {
}
class GeometryPtr {
	char filler[8];
};
class GeometryGroup {
public:
	GeometryPtr getGeometry(std::string const&);
};

void bl_renderManager_init(void* mcpelibhandle) {
#if 0
	bl_EntityRenderDispatcher_getRenderer = (EntityRenderer* (*) (void*, int))
		dlsym(mcpelibhandle, "_ZNK21ActorRenderDispatcher11getRendererE15ActorRendererId");
	bl_EntityRenderDispatcher_getRenderer_entity_real = (EntityRenderer* (*)(void*, Entity*))
		dlsym(mcpelibhandle, "_ZNK21ActorRenderDispatcher11getRendererER5Actor");
	bl_Mesh_reset = (void (*)(void*))
		dlsym(mcpelibhandle, "_ZN3mce4Mesh5resetEv");
#endif
/*
	void* getRenderer = dlsym(mcpelibhandle, "_ZN22EntityRenderDispatcher11getRendererER5Actor");
	mcpelauncher_hook(getRenderer, (void*) bl_EntityRenderDispatcher_getRenderer_hook,
		(void**) &bl_EntityRenderDispatcher_getRenderer_real);
*/
	// FIXME 1.2
#if 0
	bl_EntityRenderDispatcher_render_real = (void* (*)(void*, BaseActorRenderContext&, Actor&, Vec3 const&, Vec2 const&))
		dlsym(mcpelibhandle, "_ZN21ActorRenderDispatcher6renderER22BaseActorRenderContextR5ActorRK4Vec3RK4Vec2");
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
#endif
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
