LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)


LOCAL_MODULE :=	gomdev
LOCAL_SRC_FILES := com_gomdev_gles_GLESShader.cpp
LOCAL_LDLIBS :=  -llog -lGLESv2 -lGLESv3 -lEGL -landroid -ljnigraphics


include $(BUILD_SHARED_LIBRARY)