#ifdef __cplusplus
#define cppstr std::string
#else
typedef void cppstr;
#endif

#ifdef __cplusplus
extern "C" {
#endif
typedef unsigned short FullTile;
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
	char filler2[2959];//9
	TileSource* tileSource;//2968
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
} TextureUVCoordinateSet;

typedef struct {
	void** vtable; //0
	int itemId; //4
	int idunno; //8
	int icon; //12
	int idunno2; //16
	int idunno3; //20
	int idunno4; //24
	int idunno5; //28
	cppstr* description; //32
	char filler[18]; //34
	int category1; //52
	int category2; //56
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
	TextureUVCoordinateSet texture; //4
	int id; //28
	char filler[32]; //32
	void* material; //64
	int filler1; //48
	float destroyTime; //52
	float explosionResistance; //56
	char filler2[20]; //60
	int category1;//80
	int category2;//84
#ifdef __cplusplus
	std::string descriptionId; //88
#else
	int descriptionId; //88
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

#ifdef __cplusplus
}
#endif
