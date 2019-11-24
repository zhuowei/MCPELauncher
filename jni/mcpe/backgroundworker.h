#pragma once

class BackgroundTask {
public:
	char filler[104];
};

class BackgroundWorker {
public:
	char filler0[20-0]; // 0
	pthread_t myThread; // 20
	char filler[48-24]; // 24
	void* runQueue; // 48
	bool _workerThread() const;
	void* queue(std::shared_ptr<BackgroundTask>);
};
// BackgroundWorker::queue, look at when _workerThread returns false
// or BackgroundWorker::~BackgroundWorker, look at the pthread_equal
static_assert(offsetof(BackgroundWorker, myThread) == 20, "myThread");
// when the queue segfaults due to null pointer, look at lr in BackgroundWorker::queue
// TODO 1.13
static_assert(offsetof(BackgroundWorker, runQueue) == 48, "background worker");
extern bool ON_MAIN_THREAD();
