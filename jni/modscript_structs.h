#ifdef __cplusplus
#define cppstr std::string
#else
typedef void cppstr;
#endif

#ifdef __cplusplus
extern "C" {
#endif
typedef struct {
	void** vtable; //0
	char filler[13];
	cppbool isRemote;
} Level;
typedef struct {
	void** vtable; //0
	int filler3[7];//28 extra bytes. All others are shifted by 28.
	float x; //4+28
	float y; //8
	float z; //12
	char filler[12];//16
	int entityId; //28
	char filler2[20];//32
	float motionX; //52
	float motionY; //56
	float motionZ; //60
	float yaw; //64
	float pitch; //68
	float prevYaw; //72
	float prevPitch; //76
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
	cppbool transparent;
	char filler1[67]; //1
	int textureOffsetX;//68
	int textureOffsetY;//72
	char filler2[52];//76
} ModelPart;

typedef struct {
	void** vtable; //0
	char filler[24]; //4
	ModelPart bipedHead;//28
	ModelPart bipedBody;//156
	ModelPart bipedRightArm;//284
	ModelPart bipedLeftArm;//412
	ModelPart bipedRightLeg;//540
	ModelPart bipedLeftLeg;//668
} HumanoidModel;

typedef struct {
	Item* item; //0
	int wtf2; //4
	ItemInstance itemInstance; //8
	char letter; //28
} RecipesType;

typedef struct {
} FurnaceRecipes;

#ifdef __cplusplus
}
#endif
