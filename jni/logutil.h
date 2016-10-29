#pragma once
#include <android/log.h>
#define BL_LOG(...) __android_log_print(ANDROID_LOG_INFO, "BlockLauncher", __VA_ARGS__);
