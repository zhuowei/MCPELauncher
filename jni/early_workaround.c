#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <dlfcn.h>
#include <android/log.h>
#include "logutil.h"

static JavaVM* earlyJavaVM;

struct GrabMethodArg {
	jclass javaClass;
	const char* methodName;
	const char* signature;
};

JNIEnv* bl_early_attach_jnienv() {
	JNIEnv *env;
	(*earlyJavaVM)->AttachCurrentThread(earlyJavaVM, &env, NULL);
	return env;
}

void* bl_early_grab_static_method_run(void* arg) {
	struct GrabMethodArg* methodArg = arg;
	JNIEnv *env = bl_early_attach_jnienv();
	// hopefully there's tail call optimization
	jmethodID mid = (*env)->GetStaticMethodID(env, methodArg->javaClass, methodArg->methodName, methodArg->signature);
	return (void*)mid;
}

void* bl_early_grab_method_run(void* arg) {
	struct GrabMethodArg* methodArg = arg;
	JNIEnv *env = bl_early_attach_jnienv();
	// hopefully there's tail call optimization
	jmethodID mid = (*env)->GetMethodID(env, methodArg->javaClass, methodArg->methodName, methodArg->signature);
	return (void*)mid;
}

JNIEXPORT jobject JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGrabMethod
  (JNIEnv *env, jclass clazz, jclass targetClass, jobject methodNameString, jobject methodSigString, jboolean isStatic) {
	jobject globalTargetClass = (*env)->NewGlobalRef(env, targetClass);
	struct GrabMethodArg methodArg = {
		.javaClass = globalTargetClass,
		.methodName = "setTargetSdkVersion",
		.signature = "(I)V",
	};
	// terrible idea: Android Q whitelists APEX modules... like libc.
	// So create a fresh thread with a stack with only libc below us, and use a tail call
	pthread_t thread;
	int error = pthread_create(&thread, NULL,
		(isStatic? bl_early_grab_static_method_run : bl_early_grab_method_run),
		(void*)&methodArg);
	if (error) {
		BL_LOG("pthread_create failed: %s\n", strerror(error));
		(*env)->DeleteGlobalRef(env, globalTargetClass);
		return NULL;
	}

	jmethodID returnedMethod = 0;
	error = pthread_join(thread, (void**)&returnedMethod);
	if (error) {
		BL_LOG("pthread_join failed: %s\n", strerror(error));
	}
	//BL_LOG("Grab method: %p", returnedMethod);
	if (!returnedMethod) {
		(*env)->DeleteGlobalRef(env, globalTargetClass);
		return NULL;
	}
	jobject retval = (*env)->ToReflectedMethod(env, targetClass, returnedMethod, isStatic);
	(*env)->DeleteGlobalRef(env, globalTargetClass);
	return retval;
}

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
	earlyJavaVM = vm;
	return JNI_VERSION_1_2;
}
