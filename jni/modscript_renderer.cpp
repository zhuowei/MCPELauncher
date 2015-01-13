#include "jni.h"
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

// search for HumanoidModel::HumanoidModel
#define HUMANOIDMODEL_SIZE 868
// search for HumanoidMobRenderer::HumanoidMobRenderer
#define MOBRENDERER_SIZE 92
// ModelPart::addBox
#define MODELPART_CUBEVECTOR_OFFSET 28


extern "C" {

static void* bl_EntityRenderDispatcher_instance;

static EntityRenderer* (*bl_EntityRenderDispatcher_getRenderer)(void*, int);

static void (*bl_Mesh_reset)(void*);

static void (*bl_HumanoidModel_HumanoidModel)(HumanoidModel*, float, float);

static void (*bl_HumanoidMobRenderer_HumanoidMobRenderer)(MobRenderer*, HumanoidModel*, float);

static std::vector<EntityRenderer*> bl_entityRenderers;

static std::map<int, int> bl_renderTypeMap;

static EntityRenderer* (*bl_EntityRenderDispatcher_getRenderer_real)(void*, Entity*);

ModelPart* bl_renderManager_getModelPart(int rendererId, const char* modelPartName) {
	MobRenderer* renderer;
	if (rendererId < 0x1000) {
		renderer = (MobRenderer*) bl_EntityRenderDispatcher_getRenderer(bl_EntityRenderDispatcher_instance, rendererId);
	} else {
		renderer = (MobRenderer*) bl_entityRenderers[rendererId - 0x1000];
	}
	HumanoidModel* model = (HumanoidModel*) renderer->model; //TODO: make sure that this is indeed a humanoid model
	if (strcmp(modelPartName, "head") == 0) {
		return &model->bipedHead;
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

void bl_renderManager_invalidateModelPart(ModelPart* part) {
	void* meshBuffer = (void*) ((uintptr_t) part + 84);
	bl_Mesh_reset(meshBuffer);
}

int bl_renderManager_addRenderer(EntityRenderer* renderer) {
	bl_entityRenderers.push_back(renderer);
	int rendererId = 0x1000 + (bl_entityRenderers.size() - 1);
	return rendererId;
}

int bl_renderManager_createHumanoidRenderer() {
	HumanoidModel* model = (HumanoidModel*) operator new(HUMANOIDMODEL_SIZE);
	bl_HumanoidModel_HumanoidModel(model, 0, 0);
	MobRenderer* renderer = (MobRenderer*) operator new(MOBRENDERER_SIZE);
	bl_HumanoidMobRenderer_HumanoidMobRenderer(renderer, model, 0);
	return bl_renderManager_addRenderer((EntityRenderer*) renderer);
}

EntityRenderer* bl_EntityRenderDispatcher_getRenderer_hook(void* dispatcher, Entity* entity) {
	int entityId = entity->entityId;
	if (bl_renderTypeMap.count(entityId) != 0) {
		return bl_entityRenderers[bl_renderTypeMap[entityId] - 0x1000];
	}
	return bl_EntityRenderDispatcher_getRenderer_real(dispatcher, entity);
}

void bl_renderManager_setRenderType(Entity* entity, int renderType) {
	int entityId = entity->entityId;
	if (renderType >= 0x1000) {
		bl_renderTypeMap[entityId] = renderType;
	} else {
		bl_renderTypeMap.erase(entityId);
		entity->renderType = renderType;
	}
}

int bl_renderManager_getRenderType(Entity* entity) {
	int entityId = entity->entityId;
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
	ModelPart* part = bl_renderManager_getModelPart(rendererId, utfChars);
	part->textureOffsetX = textureX;
	part->textureOffsetY = textureY;
	//part->transparent = transparent;
	if (textureWidth > 0) part->textureWidth = textureWidth;
	if (textureHeight > 0) part->textureHeight = textureHeight;
	bl_ModelPart_addBox(part, xOffset, yOffset, zOffset, width, height, depth, scale);
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

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_api_modpe_RendererManager_nativeModelSetRotationPoint
  (JNIEnv *env, jclass clazz, jint rendererId, jstring modelPartName, jfloat x, jfloat y, jfloat z) {
	const char * utfChars = env->GetStringUTFChars(modelPartName, NULL);
	ModelPart* part = bl_renderManager_getModelPart(rendererId, utfChars);
	part->offsetX = x;
	part->offsetY = y;
	part->offsetZ = z;
	bl_renderManager_invalidateModelPart(part);
	env->ReleaseStringUTFChars(modelPartName, utfChars);
}

void bl_renderManager_init(void* mcpelibhandle) {
	bl_EntityRenderDispatcher_getRenderer = (EntityRenderer* (*) (void*, int))
		dlsym(mcpelibhandle, "_ZN22EntityRenderDispatcher11getRendererE16EntityRendererId");
	bl_EntityRenderDispatcher_instance =
		dlsym(mcpelibhandle, "_ZN22EntityRenderDispatcher8instanceE");
	bl_Mesh_reset = (void (*)(void*))
		dlsym(mcpelibhandle, "_ZN4Mesh5resetEv");
	bl_HumanoidModel_HumanoidModel = (void (*)(HumanoidModel*, float, float))
		dlsym(mcpelibhandle, "_ZN13HumanoidModelC1Eff");
	bl_HumanoidMobRenderer_HumanoidMobRenderer = (void (*)(MobRenderer*, HumanoidModel*, float))
		dlsym(mcpelibhandle, "_ZN19HumanoidMobRendererC1EP13HumanoidModelf");
	void* getRenderer = dlsym(mcpelibhandle, "_ZN22EntityRenderDispatcher11getRendererER6Entity");
	mcpelauncher_hook(getRenderer, (void*) bl_EntityRenderDispatcher_getRenderer_hook,
		(void**) &bl_EntityRenderDispatcher_getRenderer_real);
}

} //extern "C"
