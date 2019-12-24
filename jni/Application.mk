
APP_ABI := armeabi-v7a arm64-v8a x86_64 x86
APP_PLATFORM := android-14
APP_CFLAGS := -O2 -std=gnu99 -Wall
APP_CPPFLAGS += -std=c++14 -Wno-invalid-offsetof -Wno-pmf-conversions

APP_STL := gnustl_shared

NDK_TOOLCHAIN_VERSION=4.9
