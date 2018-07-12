#pragma once

class BackgroundTask {
public:
	void* task; // 0
};

class BackgroundWorker {
public:
	char filler0[16-0]; // 0
	pthread_t myThread; // 16
	char filler[40-20]; // 20
	void* runQueue; // 40
	bool _workerThread() const;
	void* queue(BackgroundTask);
};
// BackgroundWorker::queue, look at when _workerThread returns false
static_assert(offsetof(BackgroundWorker, myThread) == 16, "myThread");
static_assert(offsetof(BackgroundWorker, runQueue) == 40, "background worker");
extern bool ON_MAIN_THREAD();
