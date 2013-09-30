#include "modscript_ScriptLevelListener.hpp"

#include <jni.h>
#include <modscript_shared.h>
#include <android/log.h>

void ScriptLevelListener::levelEvent(Player* player, int type, int x, int y, int z, int data) {
	JNIEnv *env;
	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "levelEventCallback", "(IIIIII)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, player->entityId, type, x, y, z, data);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
}

void ScriptLevelListener::entityAdded(Entity* entity) {
	JNIEnv *env;
	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "entityAddedCallback", "(I)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, entity->entityId);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
}
void ScriptLevelListener::entityRemoved(Entity* entity) {
	JNIEnv *env;
	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	bl_removedEntity = entity;

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "entityRemovedCallback", "(I)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, entity->entityId);

	bl_removedEntity = NULL;

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
}

void ScriptLevelListener::tileEvent(int x, int y, int z, int type, int data) {
	JNIEnv *env;
	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "blockEventCallback", "(IIIII)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, x, y, z, type, data);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
}
