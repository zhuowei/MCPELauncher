#pragma once
class Options {
public:
	void setDevRenderGoalState(bool);
	void setDevRenderBoundingBoxes(bool);
	void setDevRenderPaths(bool);
	void setRenderDebug(bool);
	bool getUseLocalServer() const;
};
class ScreenView {
public:
	static void setDebugRendering(bool);
};
