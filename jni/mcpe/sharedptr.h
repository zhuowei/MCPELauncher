#pragma once
#include <atomic>
// SharedPtr<TallGrass>::make
// We're not going to make a real shared pointer implementation.
// Just enough to retain/release pointers by hand, call already implemented ::make method
// and get a weak pointer out.

template<class T> class SharedPtrImpl {
public:
	T* ptr; // 0
	void* destructor; // 4
	std::atomic_uint referenceCount; // 8
};

template<class T> class SharedPtr {
public:
	SharedPtrImpl<T>* impl;
	~SharedPtr() {
		// don't care since we're treating the SharedPtr as a weak anyways
		// this is enough to get it to pass via temporary
	}
	T* get() {
		if (!impl) return nullptr;
		return impl->ptr;
	}
	template <class... Args>
	static SharedPtr<T> make(Args&&... args);
};
static_assert(sizeof(SharedPtrImpl<void>) == 12, "shared ptr impl size");
static_assert(sizeof(SharedPtr<void>) == 4, "shared ptr size");

template<class T> class WeakPtr {
public:
	SharedPtrImpl<T>* impl;
	~WeakPtr() {
	}
	T* get() {
		if (!impl) return nullptr;
		return impl->ptr;
	}
};
static_assert(sizeof(WeakPtr<void>) == 4, "weak ptr size");
