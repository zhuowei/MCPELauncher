LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)  
LOCAL_LDLIBS := -llog
LOCAL_MODULE    := mcpelauncher
LOCAL_SRC_FILES := nativepatch.c 

include $(BUILD_SHARED_LIBRARY)  
