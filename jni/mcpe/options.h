#pragma once
class Options {
public:
	void setDevRenderGoalState(bool);
	void setDevRenderBoundingBoxes(bool);
	void setDevRenderPaths(bool);
	void setRenderDebug(bool);
};
class ScreenView {
public:
	static void setDebugRendering(bool);
};
