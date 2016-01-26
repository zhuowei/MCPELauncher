#include <stdlib.h>
#include <string.h>
#include <android/log.h>
#include <jni.h>
#include "fmod_hdr.h"
extern "C" {
extern JavaVM* bl_JavaVM;
extern jclass bl_scriptmanager_class;
}

struct RamFileHandle {
	size_t size;
	size_t offset;
	unsigned char data[];
	size_t read(void *ptr, size_t s) {
		if (offset >= size) return 0;
		size_t newoffset = offset + s;
		if (newoffset > size) newoffset = size;
		size_t sizetocopy = newoffset - offset;
		memcpy(ptr, data + offset, sizetocopy);
		offset = newoffset;
		return sizetocopy;
	}

	int seek(size_t off) {
		if (off > size) return -1;
		offset = off;
		return 0;
	}
	static RamFileHandle* alloc(size_t size) {
		RamFileHandle* out = (RamFileHandle*) ::operator new(sizeof(RamFileHandle) + size);
		out->size = size;
		out->offset = 0;
		return out;
	}
};

static FMOD_RESULT fmodopen(const char *name,
  unsigned int *filesize,
  void **handle,
  void *userdata
) {
	JNIEnv *env;
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}
	jclass clazz = bl_scriptmanager_class;
	jstring nameJString = env->NewStringUTF(name);
	jmethodID mid = env->GetStaticMethodID(clazz, "getSoundBytes", "(Ljava/lang/String;)[B");
	jbyteArray soundBytes = (jbyteArray) env->CallStaticObjectMethod(clazz, mid, nameJString);
	if (soundBytes != nullptr) {
		unsigned int thesize = (unsigned int) env->GetArrayLength(soundBytes);
		*filesize = thesize;
		RamFileHandle* ramfile = RamFileHandle::alloc(thesize);
		env->GetByteArrayRegion(soundBytes, 0, thesize, (jbyte*) ramfile->data);
		*handle = (void*) ramfile;
		env->DeleteLocalRef(soundBytes);
	}
	env->DeleteLocalRef(nameJString);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
	return soundBytes == nullptr? FMOD_ERR_FILE_NOTFOUND: FMOD_OK;
}

static FMOD_RESULT fmodclose(void *handle, void *userdata) {
	delete ((RamFileHandle*) handle);
	return FMOD_OK;
}

static FMOD_RESULT fmodread(
  void *handle,
  void *buffer,
  unsigned int sizebytes,
  unsigned int *bytesread,
  void *userdata
) {
	RamFileHandle* ramfile = (RamFileHandle*) handle;
	size_t readsize = ramfile->read(buffer, sizebytes);
	*bytesread = (unsigned int) readsize;
	return sizebytes == *bytesread? FMOD_OK: FMOD_ERR_FILE_EOF;
}

static FMOD_RESULT fmodseek(
  void *handle,
  unsigned int pos,
  void *userdata
) {
	RamFileHandle* ramfile = (RamFileHandle*) handle;
	int result = ramfile->seek(pos);
	return result == 0? FMOD_OK: FMOD_ERR_FILE_EOF;
}

extern "C" {

FMOD_RESULT bl_FMOD_System_init_hook(FMOD::System* system, int maxchannels, FMOD_INITFLAGS flags, void *extradriverdata) {
	FMOD_RESULT res = system->init(maxchannels, flags, extradriverdata);
	if (res != FMOD_OK) return res;
	system->setFileSystem(fmodopen, fmodclose, fmodread, fmodseek, nullptr, nullptr, -1);
	return res;
}

}
