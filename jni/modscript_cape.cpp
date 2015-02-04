#include <string>
#include <map>
#include <cmath>
#include <cstring>
#include <dlfcn.h>
#include "modscript_shared.h"
#include "mcpelauncher.h"

// from MobRenderer::MobRenderer(Model*, float)
#define MOB_RENDERER_MODEL_OFFSET 52

static std::map<HumanoidModel*, ModelPart*> modelPartMap;
//static void (*bl_HumanoidMobRenderer_additionalRendering_real)(ModelRenderer* renderer, Entity* entity, float partialTicks);
static void (*bl_HumanoidModel_render_real)(HumanoidModel* self, Entity* entity, float a, float b, float c, float d, float e, float f);
static void (*bl_ModelPart_ModelPart)(ModelPart*, HumanoidModel*, int, int, int, int);
static void (*bl_ModelPart_render)(ModelPart*, float);
static void (*bl_EntityRenderer_bindTexture)(ModelRenderer*, std::string);
static void (*bl_HumanoidMobRenderer_render_real)(ModelRenderer* self, Entity* entity, Vec3* v, float a, float b);

extern "C" {
void bl_cape_hook(HumanoidModel* self, float scale, float y) {
	ModelPart* part = new ModelPart();
	memset(part, 0, sizeof(ModelPart));
	bl_ModelPart_ModelPart(part, self, 0, 0, 64, 32);
	bl_ModelPart_addBox(part, -5.0F, 0.0F, /*-1.0F*/ -3.0F, 10, 16, 1, scale);
	modelPartMap[self] = part;
}
static ModelRenderer* currentRenderer;
void bl_HumanoidMobRenderer_render_hook(ModelRenderer* self, Entity* entity, Vec3* v, float a, float b) {
	currentRenderer = self;
	bl_HumanoidMobRenderer_render_real(self, entity, v, a, b);
}
void bl_HumanoidModel_render_hook(HumanoidModel* self, Entity* entity, float a, float b, float c, float d, float e, float f) {
	bl_HumanoidModel_render_real(self, entity, a, b, c, d, e, f);
	ModelPart* part = modelPartMap[self];
	if (!part) return;
	part->rotateAngleY = M_PI;
	part->rotateAngleX = -M_PI/8.0f - b;
	bl_EntityRenderer_bindTexture(currentRenderer, "mob/creeper.png");
	bl_ModelPart_render(part, f);
}

void bl_cape_init(void* mcpelibinfo) {
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
	bl_EntityRenderer_bindTexture = (void (*)(ModelRenderer*, std::string))
		dlsym(mcpelibinfo, "_ZN14EntityRenderer11bindTextureERKSs");
}

} // extern "C"
