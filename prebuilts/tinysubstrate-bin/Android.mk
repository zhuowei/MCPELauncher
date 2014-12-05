LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := tinysubstrate-bin
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libmcpelauncher_tinysubstrate.so
include $(PREBUILT_SHARED_LIBRARY)

