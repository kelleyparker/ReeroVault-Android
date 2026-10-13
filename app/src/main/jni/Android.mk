LOCAL_PATH := $(call my-dir)
RETROVAULT_JNI_PATH := $(LOCAL_PATH)

include $(LOCAL_PATH)/../../../../third_party/libretro-fceumm/jni/Android.mk

LOCAL_PATH := $(RETROVAULT_JNI_PATH)
include $(CLEAR_VARS)
LOCAL_MODULE := retrovault
LOCAL_SRC_FILES := retrovault_bridge.cpp
LOCAL_C_INCLUDES := $(LOCAL_PATH)/../../../../third_party/libretro-fceumm/src/drivers/libretro/libretro-common/include
LOCAL_SHARED_LIBRARIES := retro
LOCAL_CPPFLAGS := -std=c++17 -Wall -Wextra
LOCAL_LDLIBS := -llog
LOCAL_LDFLAGS := -Wl,-z,max-page-size=16384
include $(BUILD_SHARED_LIBRARY)
