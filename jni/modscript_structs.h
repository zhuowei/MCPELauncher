#ifdef __cplusplus
#pragma once
#include <memory>
#endif
#ifdef __cplusplus
#define cppstr std::string
#else
typedef struct {
	char* contents;
} cppstr;
#endif

#ifdef __cplusplus
extern "C" {
#endif

#ifdef __cplusplus
struct FullTile {
	unsigned char id;
	unsigned char data;
	FullTile(): id(0), data(0) {
	}
	FullTile(FullTile const& other): id(other.id), data(other.data) {
	}
};
#else
typedef struct {
	unsigned char id;
	unsigned char data;
} FullTile;
#endif
typedef struct TileSource TileSource;

#ifdef __cplusplus
class EntityUniqueID {
public:
	long long id;
	EntityUniqueID(long long id) : id(id) {
	}

	operator long long() const {
		return this->id;
	}
};
#else
typedef struct {
	long long id;
} EntityUniqueID;
#endif

typedef struct {
	float x;
	float y;
	float z;
} Vec3;

typedef struct {
	float x;
	float y;
} Vec2;

#ifdef __cplusplus
struct TilePos {
	int x;
	int y;
	int z;
	TilePos(TilePos const& other) : x(other.x), y(other.y), z(other.z) {
	}
	TilePos(): x(0), y(0), z(0) {
	}
};
#else
typedef struct {
	int x;
	int y;
	int z;
} TilePos;
#endif

typedef struct Entity{
	void** vtable; //0
	int filler3[5];//4
	float x; //24
	float y; //28
	float z; //32
	char filler2[48-36]; //36
	TileSource* tileSource; // 48
	int dimension; // 52
	char filler2_[76-56]; // 56
	float motionX; //76 found in Entity::rideTick(); should be set to 0 there
	float motionY; //80
	float motionZ; //84
	float yaw; //88
	float pitch; //92
	float prevYaw; //96
	float prevPitch; //100
	char filler4[268-104]; //104
	int renderType; //268
	char filler5[284-272]; // 272
	struct Entity* rider; //284
	struct Entity* riding; //288
	char filler6[320-292]; //292
#ifdef __cplusplus
	EntityUniqueID entityId; // 320
#else
	long long entityId; // 320
#endif
} Entity;

typedef Entity Player;
#ifdef __cplusplus
struct TextureUVCoordinateSet {
	float bounds[6];
	int idunno;
	void* textureFile;
	TextureUVCoordinateSet(TextureUVCoordinateSet const& other) {
		*this = other; // yeah I know, bad memory management habit. Deal with it
	};
};
#else
typedef struct {
	float bounds[6];
	int idunno;
	void* textureFile;
} TextureUVCoordinateSet;
#endif

typedef struct {
	void** vtable; //0
	unsigned char maxStackSize; //4
	char filler0[24-5]; //5
	short itemId; //24
	short maxDamage; //26
	char filler[60-28]; //28
	int category1; //60
	bool handEquipped; //64
	bool stackedByData; //65
	char filler1[72-66]; //66
} Item;

typedef void CompoundTag;

typedef struct {
	unsigned char count; //0
	short damage; //2
	CompoundTag* tag; // 4
	Item* item; // 8
	void* block; //12
	bool wtf; //16
} ItemInstance; // see ServerCommandParser::give for size

typedef struct {
	void** vtable; //0
	float idunnofloat1; //4
	char aabbfiller[28]; //8
	TextureUVCoordinateSet texture; //36
	unsigned char id; //68
	char filler[76-69]; //69 (insert rude joke here)
	int renderPass; //76
	int filler_afterRenderPass; //80
	int renderType; //84
	char filler2[108-88]; //88
	void* material; //108
	int filler3; //112
	float destroyTime; //116
	float explosionResistance; //120
	int category1; //124
	char filler4[8]; //128
	cppstr descriptionId; //136
} Tile;

typedef struct {
	char filler0[668]; //0
} Cube;

typedef struct {
	char filler[12]; // 0
} MaterialPtr;

// from ModelPart::setPos, ModelPart::setTexSize
typedef struct {
	float offsetX; //0
	float offsetY; //4
	float offsetZ; //8
	float rotateAngleX; // 12
	float rotateAngleY; // 16
	char filler0[4]; //20: note that 32 contains a std::vector
	bool transparent; //24
	bool showModel; //25
	char filler1[30]; //26
	float textureWidth; //56
	float textureHeight; //60
	MaterialPtr* material; //64
	int textureOffsetX;//68
	int textureOffsetY;//72
	bool wtf2; //76
	char filler3[47];//77; 84 is mesh
	void* model; // 124
} ModelPart;

// from HumanoidModel::render

typedef struct {
	void** vtable; //0
	char filler0[12-4]; // 4
	bool riding; // 12
	char filler[32-13]; // 13
	MaterialPtr* activeMaterial; // 32
	MaterialPtr materialNormal; // 36
	MaterialPtr materialAlphaTest; // 48
	MaterialPtr materialAlphaBlend; // 60
	MaterialPtr materialStatic; // 72
	MaterialPtr materialEmissive; // 84
	MaterialPtr materialEmissiveAlpha; // 96
	MaterialPtr materialChangeColor; // 108
	ModelPart bipedHead;//120
	ModelPart bipedHeadwear;//248
	ModelPart bipedBody;//376
	ModelPart bipedRightArm;//504
	ModelPart bipedLeftArm;//632
	ModelPart bipedRightLeg;//760
	ModelPart bipedLeftLeg;//888
} HumanoidModel;

typedef struct {
	Item* item; //0
	int wtf2; //4
	ItemInstance itemInstance; //8
	char letter; //28
} RecipesType;

typedef struct {
} FurnaceRecipes;

typedef struct {
	char filler[16]; //0
	char* text; //16;
} RakString;

typedef void EntityRenderer;

typedef struct {
	void** vtable; //0
	char filler[72-4]; //4
	void* model; // 72 (from MobRenderer::MobRenderer)
} MobRenderer;

typedef void Tag;

typedef struct {
	void** vtable; //0
	char filler[12]; //4
	int64_t value; //16
} LongTag;

#define HIT_RESULT_BLOCK 0
#define HIT_RESULT_ENTITY 1
#define HIT_RESULT_NONE 2

typedef struct {
	int type; //0
	int side; //4
	int x; //8
	int y; //12
	int z; //16
	Vec3 hitVec; //20
	Entity* entity; //32
	unsigned char filler1; //36
} HitResult;

typedef struct {
	void** vtable; //0
	char filler[40]; //4
	cppstr name; //44 from Biome::setName
	char filler2[120-48]; //48
	int id; //120 from Biome::Biome
} Biome;

typedef struct {
	bool invulnerable; // from Desktop Edition's PlayerAbilities and also CreativeMode::initAbilities
	bool flying;
	bool mayFly;
	bool instabuild;
} Abilities;

// from LocalServerListItemElement::serverMainPressed 25c7d4
typedef struct {
	int wtf1; // 0 always negative 1?
	int wtf2; // 4 always negative 1?
	int wtf3; // 8 always 3
	int wtf4; // 12 always 4
	int wtf5; // 16 always 0
	int wtf6; // 16 always 0
	int wtf7; // 16 always 0
} LevelSettings;

typedef struct {
	float r;
	float g;
	float b;
	float a;
} Color;

typedef struct {
} AABB;

typedef struct {
	unsigned char x;
	unsigned char z;
	unsigned char y;
} ChunkTilePos;

typedef struct LevelChunk_t LevelChunk;

typedef void ModelRenderer;

typedef void ScreenChooser;

#ifdef __cplusplus
struct ArmorItem : public Item {
	int armorType; // 76
	int damageReduceAmount; // 80
	int renderIndex; // 84
	void* armorMaterial; // 88
};

#ifdef __arm__
static_assert(sizeof(ArmorItem) == 88, "armor item size");
#endif

struct PlayerRenderer : public MobRenderer {
	char filler[112-76]; // 76
	HumanoidModel* modelArmor; // 112
	HumanoidModel* modelArmorChestplate; // 116
};
#endif

#ifdef __cplusplus
}
#endif

#ifdef __cplusplus
#define CLASS_TYPEDEF(name) class name {public:
#define CLASS_FOOTER(name) }
#else
#define CLASS_TYPEDEF(name) typedef struct {
#define CLASS_FOOTER(name) } name
#endif

CLASS_TYPEDEF(Level)
	void** vtable; //0
	char filler[12-4]; //4
	bool isRemote; //12 PrimedTnT::normalTick
	char filler2[2908-13];//13

#ifdef __cplusplus
	Entity* getEntity(EntityUniqueID, bool) const;
	void addEntity(std::unique_ptr<Entity>);
#endif
CLASS_FOOTER(Level);
