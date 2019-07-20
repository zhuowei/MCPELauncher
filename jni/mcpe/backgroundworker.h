#pragma once

class BackgroundTaskHandle {
public:
	void* task; // 0
};

class BackgroundWorker {
public:
	char filler0[16-0]; // 0
	pthread_t myThread; // 16
	char filler[48-20]; // 20
	void* runQueue; // 48
	bool _workerThread() const;
	void* queue(BackgroundTaskHandle);
};
// BackgroundWorker::queue, look at when _workerThread returns false
static_assert(offsetof(BackgroundWorker, myThread) == 16, "myThread");
// when the queue segfaults due to null pointer, look at lr in BackgroundWorker::queue
static_assert(offsetof(BackgroundWorker, runQueue) == 48, "background worker");
extern bool ON_MAIN_THREAD();
