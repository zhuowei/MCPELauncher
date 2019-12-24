LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)  
LOCAL_LDLIBS := -L$(LOCAL_PATH)/$(TARGET_ARCH_ABI) -llog -lminecraftpe -landroid -lfmod
LOCAL_MODULE    := mcpelauncher

BL_WITH_UNDEFINED_SYMS := no

#corkscrew
corkscrew_generic_src_files := \
	libcorkscrew/backtrace.c \
	libcorkscrew/backtrace-helper.c \
	libcorkscrew/demangle.c \
	libcorkscrew/map_info.c \
	libcorkscrew/ptrace.c \
	libcorkscrew/symbol_table.c

corkscrew_arm_src_files := \
	libcorkscrew/arch-arm/backtrace-arm.c \
	libcorkscrew/arch-arm/ptrace-arm.c

corkscrew_x86_src_files := \
	libcorkscrew/arch-x86/backtrace-x86.c \
	libcorkscrew/arch-x86/ptrace-x86.c

LOCAL_SRC_FILES := nativepatch.c modscript.cpp modscript_nextgen.cpp utf8proc_slim.c dobby.cpp marauders_map.c \
	modscript_renderer.cpp simpleuuid.c signalhandler.cpp modscript_cape.cpp controller_jni.cpp kamcord_fixer.cpp fmod_filesystem.cpp prepatch.cpp link_stubs.c fakeassets.cpp $(corkscrew_generic_src_files)

ifneq ($(BL_WITH_UNDEFINED_SYMS),yes)
	LOCAL_SRC_FILES += fakesymstubs_arm32.s fakesym_ptrs.c fakesym_lookup.c
endif

ifneq (,$(wildcard $(LOCAL_PATH)/scriptscramble.c))
    LOCAL_SRC_FILES += scriptscramble.c
endif
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    LOCAL_SRC_FILES += signalhandler_arm.cpp tamperpanic.s
else
    LOCAL_SRC_FILES += signalhandler_x86.cpp
endif

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
LOCAL_SRC_FILES += $(corkscrew_arm_src_files)
LOCAL_CFLAGS += -DCORKSCREW_HAVE_ARCH
endif

ifeq ($(TARGET_ARCH_ABI),x86)
LOCAL_SRC_FILES += $(corkscrew_x86_src_files)
LOCAL_CFLAGS += -DCORKSCREW_HAVE_ARCH
endif


LOCAL_C_INCLUDES += $(LOCAL_PATH)/libcorkscrew

LOCAL_SHARED_LIBRARIES := tinysubstrate-bin

# Build only on x86 and ARM.

ifeq ($(TARGET_ARCH_ABI),x86)
# ignore undefined symbols.
# workaround for method not found errors.
TARGET_NO_UNDEFINED_LDFLAGS :=
# HACK: don't build full lib on x86
# include $(BUILD_SHARED_LIBRARY)
endif

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)

ifeq ($(BL_WITH_UNDEFINED_SYMS),yes)
TARGET_NO_UNDEFINED_LDFLAGS :=
endif

include $(BUILD_SHARED_LIBRARY)
endif

# end building main library.

ifeq ("x","x")

include $(CLEAR_VARS)

LOCAL_MODULE    := mcpelauncher_lite
LOCAL_SRC_FILES := nativepatch.c dobby.cpp marauders_map.c fmod_filesystem.cpp prepatch.cpp fakeassets.cpp
LOCAL_LDLIBS := -L$(LOCAL_PATH)/$(TARGET_ARCH_ABI) -llog -landroid -lfmod -Wl,-soname,libmcpelauncher.so
LOCAL_SHARED_LIBRARIES := tinysubstrate-bin
LOCAL_CFLAGS += -DMCPELAUNCHER_LITE

# only build lite on armeabi-v7a and x86.

ifeq ($(TARGET_ARCH_ABI),x86)
include $(BUILD_SHARED_LIBRARY)
endif

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
LOCAL_SRC_FILES += fakesymstubs_arm32.s fakesym_ptrs.c fakesym_lookup.c
include $(BUILD_SHARED_LIBRARY)
endif

endif

include $(CLEAR_VARS)

LOCAL_MODULE    := mcpelauncher_early
LOCAL_SRC_FILES := early_workaround.c
LOCAL_LDLIBS := -llog -landroid
# the early workaround is always built.
include $(BUILD_SHARED_LIBRARY)

$(call import-add-path, prebuilts)

# only import on armeabi-v7a and x86
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
$(call import-module, tinysubstrate-bin)
endif
ifeq ($(TARGET_ARCH_ABI),x86)
$(call import-module, tinysubstrate-bin)
endif
