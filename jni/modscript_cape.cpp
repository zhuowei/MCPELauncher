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
#include <android/log.h>
#include <vector>

// HumanoidMobRenderer::prepareArmor

class ScreenContext;
static int (*bl_HumanoidMobRenderer_prepareArmor_real)(HumanoidMobRenderer* self, ScreenContext&, Entity* mob, int armorPart, float partialTicks);
static bool (*bl_ItemInstance_isArmorItem)(ItemInstance*);

std::array<mce::TexturePtr*, BL_ITEMS_EXPANDED_COUNT> bl_armorRenders;
bool bl_setArmorTexture(int, std::string const&);
bool bl_setArmorTexture(int, mce::TexturePtr*);

static std::vector<std::pair<int, std::string>> bl_queuedArmorTextures;

static bool needsReload = true;

bool bl_setArmorTexture(int id, std::string const& filename) {
	bl_queuedArmorTextures.emplace_back(id, filename);
	return true;
}
bool bl_setArmorTextureReal(int id, std::string const& filename) {
	__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "set armor texture real id: %d, %s", id, filename.c_str());
	mce::TexturePtr* texturePtr = new mce::TexturePtr(bl_minecraft->getTextures(), ResourceLocation(filename));
	return bl_setArmorTexture(id, texturePtr);
}

bool bl_setArmorTexture(int id, mce::TexturePtr* texturePtr) {
	if (id < 0 || id >= bl_item_id_count) return false;
	if (bl_armorRenders[id] != nullptr) delete bl_armorRenders[id];
	bl_armorRenders[id] = texturePtr;
	return true;
}

extern "C" {

static void bl_reload_armor_textures_real() {
/*	auto& textures = bl_minecraft->getTextures();
	for (auto t: bl_armorRenders) {
		if (!t) continue;
		textures.loadTexture(ResourceLocation(t->textureName), false);
	}
*/
}

void bl_reload_armor_textures() {
	needsReload = true;
}
// armour
#if 0
int bl_HumanoidMobRenderer_prepareArmor_hook(HumanoidMobRenderer* self, ScreenContext& screenContext,
		Mob* mob, int armorPart, float partialTicks) {
	int retval = bl_HumanoidMobRenderer_prepareArmor_real(self, screenContext, mob, armorPart, partialTicks);
	ItemInstance* armor = mob->getArmor((ArmorSlot)armorPart);
	if (!armor->isArmorItem()) return retval; // no armour

	ArmorItem* armorItem = (ArmorItem*) armor->item;
	if (armorItem->renderIndex != 42) return retval;

	HumanoidModel* armorModel = armorItem->armorType == 2? self->modelArmorChestplate: self->modelArmor;

	if (needsReload) {
		needsReload = false;
		bl_reload_armor_textures_real();
	}

	armorModel->activeTexture = bl_armorRenders[armorItem->itemId];

	return retval;
}
#endif
// end

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetCape
  (JNIEnv *env, jclass clazz, int entity, jstring value) {
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeClearCapes
  (JNIEnv *env, jclass clazz) {
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeArmorAddQueuedTextures
  (JNIEnv *env, jclass clazz) {
}
void bl_armorInit_postLoad() {
	if (!bl_minecraft) return; // WTF
	for (auto& t: bl_queuedArmorTextures) {
		bl_setArmorTextureReal(t.first, t.second);
	}
	//bl_queuedArmorTextures.clear();
}

void bl_cape_init(void* mcpelibinfo) {
	// FIXME 1.2.13
#if 0
	void* prepareArmor = dlsym(mcpelibinfo, "_ZN19HumanoidMobRenderer12prepareArmorER13ScreenContextR3Mob9ArmorSlotf");
	mcpelauncher_hook(prepareArmor, (void*) &bl_HumanoidMobRenderer_prepareArmor_hook,
		(void**) &bl_HumanoidMobRenderer_prepareArmor_real);
#endif
}

} // extern "C"
