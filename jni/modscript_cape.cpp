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
#include "mcpe/mce/textureptr.h"

// HumanoidMobRenderer::prepareArmor

static int (*bl_HumanoidMobRenderer_prepareArmor_real)(HumanoidMobRenderer* self, Entity* mob, int armorPart, float partialTicks);
static bool (*bl_ItemInstance_isArmorItem)(ItemInstance*);

std::array<mce::TexturePtr*, BL_ITEMS_EXPANDED_COUNT> bl_armorRenders;
bool bl_setArmorTexture(int, std::string const&);
bool bl_setArmorTexture(int, mce::TexturePtr*);

static std::vector<std::pair<int, std::string>> bl_queuedArmorTextures;

bool bl_setArmorTexture(int id, std::string const& filename) {
	if (!bl_minecraft) {
		bl_queuedArmorTextures.emplace_back(id, filename);
		return true;
	}
	mce::TexturePtr* texturePtr = new mce::TexturePtr(bl_minecraft->getTextures(), filename);
	return bl_setArmorTexture(id, texturePtr);
}

bool bl_setArmorTexture(int id, mce::TexturePtr* texturePtr) {
	if (id < 0 || id >= bl_item_id_count) return false;
	if (bl_armorRenders[id] != nullptr) delete bl_armorRenders[id];
	bl_armorRenders[id] = texturePtr;
	return true;
}

extern "C" {

// armour
int bl_HumanoidMobRenderer_prepareArmor_hook(HumanoidMobRenderer* self, Entity* mob, int armorPart, float partialTicks) {
	int retval = bl_HumanoidMobRenderer_prepareArmor_real(self, mob, armorPart, partialTicks);
	ItemInstance* armor = bl_Mob_getArmor(mob, armorPart);
	if (!bl_ItemInstance_isArmorItem(armor)) return retval; // no armour

	ArmorItem* armorItem = (ArmorItem*) armor->item;
	if (armorItem->renderIndex != 42) return retval;

	HumanoidModel* armorModel = armorItem->armorType == 2? self->modelArmorChestplate: self->modelArmor;

	armorModel->activeTexture = bl_armorRenders[armorItem->itemId];

	return retval;
}
// end

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetCape
  (JNIEnv *env, jclass clazz, int entity, jstring value) {
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeClearCapes
  (JNIEnv *env, jclass clazz) {
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeArmorAddQueuedTextures
  (JNIEnv *env, jclass clazz) {
	if (!bl_minecraft) return; // WTF
	for (auto& t: bl_queuedArmorTextures) {
		bl_setArmorTexture(t.first, t.second);
	}
	bl_queuedArmorTextures.clear();
}


void bl_cape_init(void* mcpelibinfo) {
	void* prepareArmor = dlsym(mcpelibinfo, "_ZN19HumanoidMobRenderer12prepareArmorER3Mob9ArmorSlotf");
	mcpelauncher_hook(prepareArmor, (void*) &bl_HumanoidMobRenderer_prepareArmor_hook,
		(void**) &bl_HumanoidMobRenderer_prepareArmor_real);
	bl_ItemInstance_isArmorItem = (bool (*)(ItemInstance*))
		dlsym(mcpelibinfo, "_ZN12ItemInstance11isArmorItemEPKS_");
}

} // extern "C"
