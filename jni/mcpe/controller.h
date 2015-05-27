#pragma once
class Controller {
public:
	static void feed(int axis, int type, float x, float y);
	static void feedTrigger(int axis, float value);
};
