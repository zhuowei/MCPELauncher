#pragma once
// SharedPtr<TallGrass>::make

template<class T> class SharedPtrImpl {
public:
	T* ptr; // 0
	std::atomic_uint referenceCount; // 4
	void* destructor; // 8
};

template<class T> class SharedPtr {
public:
	SharedPtrImpl<T>* impl;
};
static_assert(sizeof(SharedPtrImpl<void>) == 12, "shared ptr impl size");
static_assert(sizeof(SharedPtr<void>) == 4), "shared ptr size");
