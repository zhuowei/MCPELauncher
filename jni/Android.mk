LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)  
LOCAL_LDLIBS := -llog -lmcpelauncher_tinysubstrate
LOCAL_MODULE    := mcpelauncher
LOCAL_SRC_FILES := nativepatch.c modscript.c modscript_nextgen.cpp modscript_ScriptLevelListener.cpp

include $(BUILD_SHARED_LIBRARY)  
