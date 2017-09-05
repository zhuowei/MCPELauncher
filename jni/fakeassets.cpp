#include <jni.h>
#include <android/asset_manager.h>
#include <string.h>
#include "logutil.h"
#include "modscript_shared.h"
#include <cstdlib>
#include <dlfcn.h>

extern "C" {

// http://stackoverflow.com/questions/4770985/how-to-check-if-a-string-starts-with-another-string-in-c
static bool prefix(const char *pre, const char *str)
{
    return strncmp(pre, str, strlen(pre)) == 0;
}
static const char kResourcePackPrefix[] = "resource_packs/vanilla/";
static const char kResourcePackDirPrefix[] = "resource_packs/vanilla";
AAsset* bl_AAssetManager_open_hook(AAssetManager *mgr, const char *filename, int mode) {
	//BL_LOG("Asset: open %s", filename);

	// zero length filename or an absolute path (on the filesystem) definitely won't be in the assets.
	if (strlen(filename) == 0 || filename[0] == '/') return nullptr;

	std::string newFilename = filename;
	AAsset *in = AAssetManager_open(mgr, newFilename.c_str(), mode);
	if (in) return in;
	if (prefix("resource_packs/skins", filename)) return in;
	if (prefix("resource_packs/vanilla_loading_screens", filename)) return in;
	if (prefix("resource_packs/vanilla_base", filename)) return in;
	//BL_LOG("Asset: open %s", filename);
	if (true||prefix(kResourcePackPrefix, filename)) {
		JNIEnv *env;
		int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
		if (attachStatus == JNI_EDETACHED) {
			bl_JavaVM->AttachCurrentThread(&env, NULL);
		}

		//Call back across JNI into the ScriptManager
		jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "assetFileExists", "(Ljava/lang/String;)Z");
		jstring str = env->NewStringUTF(filename);

		bool exists = env->CallStaticBooleanMethod(bl_scriptmanager_class, mid, str);
		env->DeleteLocalRef(str);

		if (attachStatus == JNI_EDETACHED) {
			bl_JavaVM->DetachCurrentThread();
		}
		//if (exists) BL_LOG("Assets: exists %s", filename);
		if (exists) filename = "resource_packs/vanilla/manifest.json"; // known to exist.
	}
	return AAssetManager_open(mgr, filename, mode);
};

struct BLAssetDir {
	std::vector<std::string> entries;
	int index;
};

// BLAssetDir instance pointers have the lower bit set - since on Android dlmalloc always align to even addresses

AAssetDir* bl_AAssetManager_openDir_hook(AAssetManager *mgr, const char *dirName) {
	//BL_LOG("Asset: Opening dir %s", dirName);
	if (true||prefix(kResourcePackDirPrefix, dirName)) {
		JNIEnv *env;
		int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
		if (attachStatus == JNI_EDETACHED) {
			bl_JavaVM->AttachCurrentThread(&env, NULL);
		}

		//Call back across JNI into the ScriptManager
		jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "assetListDir", "(Ljava/lang/String;)[Ljava/lang/String;");
		jstring str = env->NewStringUTF(dirName);

		jobjectArray fileList = (jobjectArray) env->CallStaticObjectMethod(bl_scriptmanager_class, mid, str);
		env->DeleteLocalRef(str);

		BLAssetDir* myDirEnt = nullptr;
		if (fileList) {
			myDirEnt = new BLAssetDir();
			myDirEnt->index = 0;
			auto arrayLength = env->GetArrayLength(fileList);
			myDirEnt->entries.reserve(arrayLength);
			for (int i = 0; i < arrayLength; i++) {
				jstring javaStr = (jstring) env->GetObjectArrayElement(fileList, i);
				const char* javaUtf = env->GetStringUTFChars(javaStr, nullptr);
				myDirEnt->entries.emplace_back(javaUtf);
				env->ReleaseStringUTFChars(javaStr, javaUtf);
				env->DeleteLocalRef(javaStr);
			}
			env->DeleteLocalRef(fileList);
		}

		if (attachStatus == JNI_EDETACHED) {
			bl_JavaVM->DetachCurrentThread();
		}
		if (myDirEnt) {
			/*
			BL_LOG("Asset: returning %p", myDirEnt);
			for (auto const& s: myDirEnt->entries) {
				BL_LOG("Entry: %s", s.c_str());
			}
			*/
			return (AAssetDir*) (((uintptr_t)myDirEnt) | 1);
		}
	}
	return AAssetManager_openDir(mgr, dirName);
}

const char* bl_AAssetDir_getNextFileName_hook(AAssetDir* dir) {
	uintptr_t dirRaw = (uintptr_t) dir;
	if ((dirRaw & 1) != 0) {
		BLAssetDir* myDir = (BLAssetDir*)(dirRaw & ~1);
		//BL_LOG("Asset: Custom assetdir found: %p", myDir);
		if (myDir->index >= myDir->entries.size()) return nullptr;
		const char* retval = myDir->entries[myDir->index++].c_str();
		//BL_LOG("Asset: dir custom got %s", retval);
		return retval;
	}
	//BL_LOG("Asset: Original assetdir found");
	const char* retval = AAssetDir_getNextFileName(dir);
	//BL_LOG("Asset: dir got %s", retval);
	return retval;
}

void bl_AAssetDir_close_hook(AAssetDir* dir) {
	uintptr_t dirRaw = (uintptr_t) dir;
	if ((dirRaw & 1) != 0) {
		BLAssetDir* myDir = (BLAssetDir*)(dirRaw & ~1);
		delete myDir;
		return;
	}
	return AAssetDir_close(dir);
}

int bl_AAsset_read_hook(AAsset* asset, void* buf, size_t size) {
	BL_LOG("tried to read asset! failing!");
	return -1;
}

void bl_AAsset_getLength64_hook(AAsset* asset) {
	abort();
}

void bl_AAsset_getBuffer_hook(AAsset* asset) {
	abort();
}

void bl_AAsset_getLength_hook(AAsset* asset) {
	abort();
}

bool bl_patch_got(soinfo2* mcpelibhandle, void* original, void* newptr);

void bl_prepatch_fakeassets(soinfo2* mcpelibhandle) {
	bool success = true;
	success = success && bl_patch_got(mcpelibhandle, (void*)AAssetManager_openDir, (void*)bl_AAssetManager_openDir_hook);
	success = success && bl_patch_got(mcpelibhandle, (void*)AAssetDir_getNextFileName, (void*)bl_AAssetDir_getNextFileName_hook);
	success = success && bl_patch_got(mcpelibhandle, (void*)AAssetDir_close, (void*)bl_AAssetDir_close_hook);
	success = success && bl_patch_got(mcpelibhandle, (void*)AAssetManager_open, (void*)bl_AAssetManager_open_hook);
	success = success && bl_patch_got(mcpelibhandle, (void*)AAsset_read, (void*)bl_AAsset_read_hook);
	success = success && bl_patch_got(mcpelibhandle, (void*)AAsset_getLength64, (void*)bl_AAsset_getLength64_hook);
	success = success && bl_patch_got(mcpelibhandle, (void*)AAsset_getLength, (void*)bl_AAsset_getLength_hook);
	success = success && bl_patch_got(mcpelibhandle, (void*)AAsset_getBuffer, (void*)bl_AAsset_getBuffer_hook);
	if (!success) BL_LOG("Failed to patch textures");
}
} // extern "C"
