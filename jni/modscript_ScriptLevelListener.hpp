#include "modscript_LevelListener.hpp"

class ScriptLevelListener : public LevelListener {
	public:
		virtual void entityAdded(Entity*); //15
		virtual void entityRemoved(Entity*); //16
		virtual void levelEvent(Player*, int, int, int, int, int); //17
		virtual void tileEvent(int, int, int, int, int); //18
};
