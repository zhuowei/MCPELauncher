#include <string>
#define cppbool bool
#include "modscript_shared.h"
typedef Entity TripodCamera;
typedef void TileEntity;
typedef Entity Mob;

enum ParticleType {
};

class LevelListener {
	public:
		virtual ~LevelListener() {} ; //2, 3
		virtual void onSkyColorChanged() {} ; //11
		virtual void allChanged() {} ; //12
		virtual void* addParticle(ParticleType, float, float, float, float, float, float, int) {return NULL;} ; //13
		virtual void playSound(std::string const&, float, float, float, float, float) {} ; //14
		virtual void playMusic(std::string const&, float, float, float, float) {} ; //15
		virtual void playStreamingMusic(std::string const&, int, int, int) {} ; //16
		virtual void onEntityAdded(Entity*) {} ; //17
		virtual void onEntityRemoved(Entity*) {} ; //18
		virtual void onNewChunkFor(Player&, LevelChunk&) {} ; //19
		virtual void levelEvent(Mob*, int, int, int, int, int) {} ; //20
};
