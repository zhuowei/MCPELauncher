LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)  
LOCAL_LDLIBS := -llog
LOCAL_MODULE    := mcpelauncher
LOCAL_SRC_FILES := nativepatch.c modscript.c modscript_nextgen.cpp modscript_ScriptLevelListener.cpp utf8proc_slim.c dobby.cpp marauders_map.c \
	modscript_renderer.cpp

LOCAL_SHARED_LIBRARIES := tinysubstrate-bin

include $(BUILD_SHARED_LIBRARY)

$(call import-add-path, prebuilts)

$(call import-module, tinysubstrate-bin)
