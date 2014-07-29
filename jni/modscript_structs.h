#ifdef __cplusplus
#define cppstr std::string
#else
typedef void cppstr;
#endif

#ifdef __cplusplus
extern "C" {
#endif
typedef unsigned int FullTile;
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
	cppbool isRemote; //8?
	char filler2[2963];//9
	TileSource* tileSource;//2972
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
	cppbool idunnobool1; //64
	cppbool stackedByData; //65
} Item;

typedef struct {
	int count; //0
	int damage; //4
	Item* item;//8
	void* block; //12
	cppbool wtf; //16
	cppbool wtf2; //17
	cppbool wtf3; //18
	cppbool wtf4; //19
} ItemInstance;

typedef struct {
	void** vtable; //0
	float idunnofloat1; //4
	char aabbfiller[28]; //8
	TextureUVCoordinateSet texture; //36
	unsigned char id; //68
	char filler[39]; //69 (insert rude joke here)
	void* material; //108
	int filler2; //112
	float destroyTime; //116
	float explosionResistance; //120
	int category1; //124
	char filler3[8]; //128
#ifdef __cplusplus
	std::string descriptionId; //136
#else
	int descriptionId; //136
#endif
} Tile;

typedef struct {
	char filler0[692]; //0
} Cube;

// from ModelPart::setPos, ModelPart::setTexSize
typedef struct {
	cppbool transparent; //0
	char filler0[3]; //1
	float offsetX; //4
	float offsetY; //8
	float offsetZ; //12
	char filler1[44]; //16: note that 32 contains a std::vector
	int textureWidth; //60
	int textureHeight; //64
	int textureOffsetX;//68
	int textureOffsetY;//72
	char filler2[40];//76
} ModelPart;

// from HumanoidModel::render

typedef struct {
	void** vtable; //0
	char filler[24]; //4
	ModelPart bipedHead;//28
	ModelPart bipedBody;//144
	ModelPart bipedRightArm;//260
	ModelPart bipedLeftArm;//376
	ModelPart bipedRightLeg;//492
	ModelPart bipedLeftLeg;//608
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
	void** vtable;
	void* model;
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

#ifdef __cplusplus
}
#endif
