LOCAL_PATH := $(call my-dir)/src/main/cpp

include $(CLEAR_VARS)

LOCAL_CPPFLAGS := -O3 -Wno-sign-compare -Wno-unused-parameter

LOCAL_MODULE := triedictionary-lib
LOCAL_SRC_FILES := triedictionary-lib.cpp

include $(BUILD_SHARED_LIBRARY)

#----------------------------------------------------------------------
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE := libwnndict

LOCAL_SRC_FILES := \
	libwnnDictionary/OpenWnnDictionaryImplJni.c \
	libwnnDictionary/engine/ndapi.c \
	libwnnDictionary/engine/neapi.c \
	libwnnDictionary/engine/ndbdic.c \
	libwnnDictionary/engine/ndfdic.c \
	libwnnDictionary/engine/ndldic.c \
	libwnnDictionary/engine/ndrdic.c \
	libwnnDictionary/engine/necode.c \
	libwnnDictionary/engine/ndcommon.c \
	libwnnDictionary/engine/nj_str.c


LOCAL_C_INCLUDES += \
	$(LOCAL_PATH)/libwnnDictionary/include

LOCAL_CFLAGS += \
	 -O -Wno-unused-parameter -Wall -Werror

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)

#----------------------------------------------------------------------
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE := libWnnJpnDic

LOCAL_SRC_FILES := \
	WnnJpnDic.c

LOCAL_C_INCLUDES += \
	$(LOCAL_PATH)/libwnnDictionary/include

LOCAL_CFLAGS += \
	-O

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)

#----------------------------------------------------------------------
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE := libWnnZHCNDic

LOCAL_SRC_FILES := \
	WnnZHCNDic.c

LOCAL_SHARED_LIBRARIES :=

LOCAL_STATIC_LIBRARIES :=

LOCAL_C_INCLUDES += \
	$(LOCAL_PATH)/libwnnDictionary/include

LOCAL_CFLAGS += \
	-O

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)

#----------------------------------------------------------------------
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE := libWnnEngDic

LOCAL_SRC_FILES := \
	WnnEngDic.c

LOCAL_SHARED_LIBRARIES :=

LOCAL_STATIC_LIBRARIES :=

LOCAL_C_INCLUDES += \
	$(LOCAL_PATH)/libwnnDictionary/include

LOCAL_CFLAGS += \
	-O

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)