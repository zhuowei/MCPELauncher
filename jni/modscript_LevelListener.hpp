#include <string>
#define cppbool bool
#include "modscript_shared.h"
typedef Entity TripodCamera;
typedef void TileEntity;

class ParticleType {
};

class LevelListener {
	public:
		virtual ~LevelListener() {} ;
		virtual void setTilesDirty(int, int, int, int, int, int) {} ; //4
		virtual void tileChanged(int, int, int) {} ; //5
		virtual void tileBrightnessChanged(int, int, int) {} ; //6
		virtual void skyColorChanged() {} ; //7
		virtual void allChanged() {} ; //8
		virtual void takePicture(TripodCamera*, Entity*) {} ; //9
		//removed - subtract 1 from the below
		virtual void addParticle(ParticleType, float, float, float, float, float, float, int) {} ; //11
		virtual void playSound(std::string const&, float, float, float, float, float) {} ; //12
		virtual void playMusic(std::string const&, float, float, float, float) {} ; //13
		virtual void playStreamingMusic(std::string const&, int, int, int) {} ; //14
		virtual void entityAdded(Entity*) {} ; //15
		virtual void entityRemoved(Entity*) {} ; //16
		virtual void levelEvent(Player*, int, int, int, int, int) {} ; //17: doors opening/closing
		virtual void tileEvent(int, int, int, int, int) {} ; //18: chests' open/closed state, broadcast every 3 seconds
		virtual void tileEntityChanged(int, int, int, TileEntity*) {} ; //19
};
