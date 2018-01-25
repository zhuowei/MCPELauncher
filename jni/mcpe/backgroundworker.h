#pragma once
class BackgroundWorker {
public:
	char filler[40-0]; // 0
	void* runQueue; // 40
	bool _workerThread() const;
};
static_assert(offsetof(BackgroundWorker, runQueue) == 40, "background worker");
extern bool ON_MAIN_THREAD();
