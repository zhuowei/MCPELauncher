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
typedef struct {
	unsigned char id;
	unsigned char data;
} FullTile;
typedef void TileSource;

typedef struct {
	float x;
	float y;
	float z;
} Vec3;

typedef struct {
	int x;
	int y;
	int z;
} TilePos;

typedef struct {
	void** vtable; //0
	char filler[4]; //4
	bool isRemote; //8?
	char filler2[2967];//9
	TileSource* tileSource;//2976 from Level::getChunkSource
} Level;
typedef struct Entity_t{
//todo: 60 = tile source, 68 = level
	void** vtable; //0
	int filler3[7];//28 extra bytes. All others are shifted by 28.
	float x; //4+28
	float y; //8
	float z; //12
	char filler[8];//16
	int entityId; //28 - 4 + 28 - argh, too hard. found in Arrow::Arrow(Mob*)
	char filler2[28];//32
	float motionX; //52 - 4 + 28 + 8 = 84 found in Entity::rideTick()
	float motionY; //56
	float motionZ; //60
	float yaw; //64 - 4 + 28 + 8 = 96
	float pitch; //68
	float prevYaw; //72
	float prevPitch; //76 (104 + 4 after shift)
	char filler4[132]; //108 +4
	int renderType; //236 + 8 = 244; found in the render dispatcher
	struct Entity_t* rider; //240 + 8
	struct Entity_t* riding; //244 + 8
} Entity;
typedef Entity Player;

typedef struct {
	float bounds[6];
	int idunno;
	void* textureFile;
} TextureUVCoordinateSet;

typedef struct {
	void** vtable; //0
	int maxStackSize; //4
	int idunno2; //8
	int idunno; //12
	int icon; //16
	int itemId; //20
	int maxDamage; //24
	int idunno4; //28 <- actually a texture UV coordinate set
	int idunno5; //32
	cppstr* description; //36
	char filler[20]; //40
	int category1; //60
	bool idunnobool1; //64
	bool stackedByData; //65
} Item;

typedef struct {
	int count; //0
	int damage; //4
	Item* item;//8
	void* block; //12
	bool wtf; //16
	bool wtf2; //17
	bool wtf3; //18
	bool wtf4; //19
} ItemInstance;

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
	char filler0[692]; //0
} Cube;

// from ModelPart::setPos, ModelPart::setTexSize
typedef struct {
	float offsetX; //0
	float offsetY; //4
	float offsetZ; //8
	float rotateAngleX; // 12
	float rotateAngleY; // 16
	char filler0[4]; //20: note that 32 contains a std::vector
	bool transparent; //24
	bool wtf1; //25
	char filler1[30]; //26
	int textureWidth; //56
	int textureHeight; //60
	char filler2[4]; //64
	int textureOffsetX;//68
	int textureOffsetY;//72
	bool wtf2; //76
	char filler3[51];//77; 84 is mesh
} ModelPart;

// from HumanoidModel::render

typedef struct {
	void** vtable; //0
	char filler[92]; //4
	ModelPart bipedHead;//96
	ModelPart bipedBody;//224
	ModelPart bipedRightArm;//352
	ModelPart bipedLeftArm;//480
	ModelPart bipedRightLeg;//608
	ModelPart bipedLeftLeg;//736
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
	char filler[48]; //4
	void* model; // 52 (from MobRenderer::MobRenderer)
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

typedef void MaterialPtr;

typedef struct {
} AABB;

typedef struct {
	unsigned char x;
	unsigned char z;
	unsigned char y;
} ChunkTilePos;

typedef struct LevelChunk_t LevelChunk;

typedef void ModelRenderer;

#ifdef __cplusplus
}
#endif
