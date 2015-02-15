#include <jni.h>
#include <sys/mman.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <malloc.h>
#include <unistd.h>
#include <android/log.h>

#include <mcpelauncher.h>

#include <dlfcn.h>

//I can haz Substrate?
void MSHookFunction(void *symbol, void *replace, void **result);

JavaVM* bl_JavaVM;

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_pokerface_PokerFace_mprotect
  (JNIEnv *env, jclass clazz, jlong addr, jlong len, jint prot) {
	return mprotect((void *)(uintptr_t) addr, len, prot);
}

JNIEXPORT jlong JNICALL Java_net_zhuoweizhang_pokerface_PokerFace_sysconf
  (JNIEnv *env, jclass clazz, jint name) {
	long result = sysconf(name);
	return result;
}

void mcpelauncher_hook(void *orig_fcn, void* new_fcn, void **orig_fcn_ptr)
{
#if 0
	static int num = 0;
	int max = 3;
	Dl_info info;
	memset(&info, 0, sizeof(info));
	dladdr(orig_fcn, &info);
	if (num++ >= max) {
		__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "not hooking %s %d", info.dli_sname, num);
		return;
	}
	__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "hooking %s %d", info.dli_sname, num);
#endif
	MSHookFunction(orig_fcn, new_fcn, orig_fcn_ptr);
	__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "hooked %p %p %p %p", orig_fcn, new_fcn, orig_fcn_ptr, *orig_fcn_ptr);
}

int mcpelauncher_get_version() {
	return 1;
}

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
	bl_JavaVM = vm;

	return JNI_VERSION_1_2;
}
