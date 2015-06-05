#include <string>
#include <array>
#include <unordered_map>
#include <cmath>
#include <cstring>
#include <dlfcn.h>
#include <jni.h>
#include "modscript_shared.h"
#include "mcpelauncher.h"
#include "dobby_public.h"

// Mob::getTexture
#define MOB_TEXTURE_OFFSET 2996
// Use IDA, NOT MobRenderer::render
#define PLAYER_RENDERER_VTABLE_PREPARE_ARMOR 9

static std::unordered_map<HumanoidModel*, ModelPart*> modelPartMap;
static std::unordered_map<int, std::string> capesMap;
static void (*bl_HumanoidModel_render_real)(HumanoidModel* self, Entity* entity, float a, float b, float c, float d, float e, float f);
static void (*bl_ModelPart_ModelPart)(ModelPart*, HumanoidModel*, int, int, int, int);
static void (*bl_ModelPart_render)(ModelPart*, float);
static void (*bl_EntityRenderer_bindTexture)(ModelRenderer*, std::string const&, int);
static void (*bl_HumanoidMobRenderer_render_real)(ModelRenderer* self, Entity* entity, Vec3* v, float a, float b);
static int (*bl_PlayerRenderer_prepareArmor_real)(PlayerRenderer* self, Entity* mob, int armorPart, float partialTicks);
static void (*bl_MobRenderer_setArmor)(MobRenderer*, HumanoidModel*);
static bool (*bl_ItemInstance_isArmorItem)(ItemInstance*);

std::array<std::string, BL_ITEMS_EXPANDED_COUNT> bl_armorRenders;

extern "C" {
// hooked outside of this file: hooks HumanoidModel::HumanoidModel
void bl_cape_hook(HumanoidModel* self, float scale, float y) {
	ModelPart* part = new ModelPart();
	memset(part, 0, sizeof(ModelPart));
	bl_ModelPart_ModelPart(part, self, 0, 0, 64, 32);
	bl_ModelPart_addBox(part, -5.0F, 0.0F, /*-1.0F*/ -3.0F, 10, 16, 1, scale);
	modelPartMap[self] = part;
}
static ModelRenderer* currentRenderer;
static int renderCount = 0;
void bl_HumanoidMobRenderer_render_hook(ModelRenderer* self, Entity* entity, Vec3* v, float a, float b) {
	currentRenderer = self;
	renderCount = 0;
	bl_HumanoidMobRenderer_render_real(self, entity, v, a, b);
}
/*
void bl_HumanoidModel_render_hook(HumanoidModel* self, Entity* entity, float swingTime, float swingMaxAngle,
	float armSwingTime, float headYaw, float headPitch, float partialTicks) {
	bl_HumanoidModel_render_real(self, entity, swingTime, swingMaxAngle, armSwingTime, headYaw, headPitch, partialTicks);
	if (renderCount++ > 0) return;

	auto capeTextureIter = capesMap.find(entity->entityId);
	if (capeTextureIter == capesMap.end()) return;

	std::string capeTexture = capeTextureIter->second;
	ModelPart* part = modelPartMap[self];
	if (!part) return;
	part->rotateAngleY = M_PI;
	part->rotateAngleX = -M_PI/16.0f - swingMaxAngle;
	bl_EntityRenderer_bindTexture(currentRenderer, capeTexture);
	bl_ModelPart_render(part, partialTicks);
}
*/

// armour

int bl_PlayerRenderer_prepareArmor_hook(PlayerRenderer* self, Entity* mob, int armorPart, float partialTicks) {
	ItemInstance* armor = bl_Player_getArmor(mob, armorPart);
	if (!bl_ItemInstance_isArmorItem(armor)) return -1; // no armour

	ArmorItem* armorItem = (ArmorItem*) armor->item;
	if (armorItem->renderIndex != 42) return bl_PlayerRenderer_prepareArmor_real(self, mob, armorPart, partialTicks);

	std::string const& texture = bl_armorRenders[armorItem->itemId];
	bl_EntityRenderer_bindTexture(self, texture, 0);

	HumanoidModel* armorModel = armorPart == 2 ? self->modelArmor : self->modelArmorChestplate;
	armorModel->bipedHead.showModel = armorPart == 0;
	armorModel->bipedHeadwear.showModel = armorPart == 0;
	armorModel->bipedBody.showModel = armorPart == 1 || armorPart == 2;
	armorModel->bipedRightArm.showModel = armorPart == 1;
	armorModel->bipedLeftArm.showModel = armorPart == 1;
	armorModel->bipedRightLeg.showModel = armorPart == 2 || armorPart == 3;
	armorModel->bipedLeftLeg.showModel = armorPart == 2 || armorPart == 3;
	armorModel->activeMaterial = &armorModel->materialAlphaTest;

	bl_MobRenderer_setArmor(self, armorModel);
	((HumanoidModel*) self->model)->riding = (mob->riding != nullptr);

	return 1; // has armour
}
// end

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetCape
  (JNIEnv *env, jclass clazz, int entity, jstring value) {
	if (value == nullptr) {
		capesMap.erase(entity);
		return;
	}
	const char * valueUTFChars = env->GetStringUTFChars(value, NULL);
	std::string valueNameString = std::string(valueUTFChars);
	capesMap[entity] = valueNameString;
	env->ReleaseStringUTFChars(value, valueUTFChars);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeClearCapes
  (JNIEnv *env, jclass clazz) {
	capesMap.clear();
}


void bl_cape_init(void* mcpelibinfo) {
/*
	FIXME 0.11
	void* render = dlsym(mcpelibinfo, "_ZN13HumanoidModel6renderER6Entityffffff");
	mcpelauncher_hook(render, (void*) &bl_HumanoidModel_render_hook,
		(void**) &bl_HumanoidModel_render_real);
	void* rendererRender = dlsym(mcpelibinfo, "_ZN19HumanoidMobRenderer6renderER6EntityRK4Vec3ff");
	mcpelauncher_hook(rendererRender, (void*) &bl_HumanoidMobRenderer_render_hook,
		(void**) &bl_HumanoidMobRenderer_render_real);
	bl_ModelPart_ModelPart = (void (*)(ModelPart*, HumanoidModel*, int, int, int, int))
		dlsym(mcpelibinfo, "_ZN9ModelPartC2EP5Modeliiii");
	bl_ModelPart_render = (void (*)(ModelPart*, float))
		dlsym(mcpelibinfo, "_ZN9ModelPart6renderEf");
*/
	bl_EntityRenderer_bindTexture = (void (*)(ModelRenderer*, std::string const&, int))
		dlsym(mcpelibinfo, "_ZN14EntityRenderer11bindTextureERKSsi");

	void** playerRendererVtable = (void**) dobby_dlsym(mcpelibinfo, "_ZTV14PlayerRenderer");
	bl_PlayerRenderer_prepareArmor_real = (int (*)(PlayerRenderer*, Entity*, int, float))
		playerRendererVtable[PLAYER_RENDERER_VTABLE_PREPARE_ARMOR];
	playerRendererVtable[PLAYER_RENDERER_VTABLE_PREPARE_ARMOR] = (void*) &bl_PlayerRenderer_prepareArmor_hook;
	//bl_dumpVtable(playerRendererVtable, 0x40);
	bl_MobRenderer_setArmor = (void (*)(MobRenderer*, HumanoidModel*))
		dlsym(mcpelibinfo, "_ZN11MobRenderer8setArmorEP5Model");
	bl_ItemInstance_isArmorItem = (bool (*)(ItemInstance*))
		dlsym(mcpelibinfo, "_ZN12ItemInstance11isArmorItemEPKS_");
}

} // extern "C"
