#include <pthread.h>
#include <dlfcn.h>
#include <android/log.h>
#include <sys/mman.h>
#include "dl_internal.h"
#include "modscript_shared.h"

// http://rabbit-hole.blogspot.ca/2012/09/thread-termination-handlers.html

static pthread_key_t key;
static pthread_once_t key_once_control = PTHREAD_ONCE_INIT;

static const bool kExtraLogging = false;

static void detach_thread(void* arg) {
	if (kExtraLogging) __android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Kamcord fix: thread cleanup");
	JNIEnv *env;
	JavaVM* jvm = (JavaVM*) arg;
	int attachStatus = jvm->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus != JNI_EDETACHED) {
		jvm->DetachCurrentThread();
	}
}

extern "C" {

static void create_key() {
	pthread_key_create(&key, detach_thread);
}

void bl_nativeAttachDestructor() {
	pthread_once(&key_once_control, create_key);
	pthread_setspecific(key, bl_JavaVM);
}

JNIEXPORT void JNICALL Java_com_kamcord_android_core_KamcordFix_nativeAttachDestructor
  (JNIEnv *env, jclass clazz) {
	pthread_once(&key_once_control, create_key);
	pthread_setspecific(key, bl_JavaVM);
}
}
