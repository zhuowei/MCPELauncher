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

static void (*bl_EntityRenderer_bindTexture)(ModelRenderer*, std::string const&, int);
static int (*bl_HumanoidMobRenderer_prepareArmor_real)(HumanoidMobRenderer* self, Entity* mob, int armorPart, float partialTicks);
static void (*bl_MobRenderer_setArmor)(MobRenderer*, HumanoidModel*);
static bool (*bl_ItemInstance_isArmorItem)(ItemInstance*);
static bool (*bl_Entity_isRiding)(Entity*);

std::array<std::string, BL_ITEMS_EXPANDED_COUNT> bl_armorRenders;

extern "C" {

// armour

int bl_HumanoidMobRenderer_prepareArmor_hook(HumanoidMobRenderer* self, Entity* mob, int armorPart, float partialTicks) {
	ItemInstance* armor = bl_Player_getArmor(mob, armorPart);
	if (!bl_ItemInstance_isArmorItem(armor)) return -1; // no armour

	ArmorItem* armorItem = (ArmorItem*) armor->item;
	if (armorItem->renderIndex != 42) return bl_HumanoidMobRenderer_prepareArmor_real(self, mob, armorPart, partialTicks);

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
	bool is_enchanted = false;
	armorModel->activeMaterial = is_enchanted? &armorModel->materialAlphaTestGlint : &armorModel->materialAlphaTest;

	bl_MobRenderer_setArmor(self, armorModel);
	((HumanoidModel*) self->model)->riding = bl_Entity_isRiding(mob);

	return 1; // has armour
}
// end

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetCape
  (JNIEnv *env, jclass clazz, int entity, jstring value) {
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeClearCapes
  (JNIEnv *env, jclass clazz) {
}


void bl_cape_init(void* mcpelibinfo) {
	bl_EntityRenderer_bindTexture = (void (*)(ModelRenderer*, std::string const&, int))
		dlsym(mcpelibinfo, "_ZN14EntityRenderer11bindTextureERKSsi");
	bl_Entity_isRiding = (bool (*)(Entity*))
		dlsym(mcpelibinfo, "_ZNK6Entity8isRidingEv");
	void* prepareArmor = dlsym(mcpelibinfo, "_ZN19HumanoidMobRenderer12prepareArmorER3Mobif");
	mcpelauncher_hook(prepareArmor, (void*) &bl_HumanoidMobRenderer_prepareArmor_hook,
		(void**) &bl_HumanoidMobRenderer_prepareArmor_real);
	bl_MobRenderer_setArmor = (void (*)(MobRenderer*, HumanoidModel*))
		dlsym(mcpelibinfo, "_ZN11MobRenderer8setArmorEP5Model");
	bl_ItemInstance_isArmorItem = (bool (*)(ItemInstance*))
		dlsym(mcpelibinfo, "_ZN12ItemInstance11isArmorItemEPKS_");
}

} // extern "C"
