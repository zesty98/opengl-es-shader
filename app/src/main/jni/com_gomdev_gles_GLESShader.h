/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_gomdev_gles_GLESShader */

#ifndef _Included_com_gomdev_gles_GLESShader
#define _Included_com_gomdev_gles_GLESShader
#ifdef __cplusplus
extern "C" {
#endif
#undef com_gomdev_gles_GLESShader_DEBUG
#define com_gomdev_gles_GLESShader_DEBUG 1L

/*
 * Class:     com_gomdev_gles_GLESShader
 * Method:    nGetShaderCompileLog
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_gomdev_gles_GLESShader_nGetShaderInfoLog
  (JNIEnv *, jobject, jint);



/*
 * Class:     com_gomdev_gles_GLESShader
 * Method:    nGetShaderCompileLog
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_gomdev_gles_GLESShader_nGetProgramInfoLog
  (JNIEnv *, jobject, jint);

/*
 * Class:     com_gomdev_gles_GLESShader
 * Method:    nTexSubImage2D
*/
JNIEXPORT void JNICALL Java_com_gomdev_gles_GLESShader_nTexSubImage2D
  (JNIEnv *_env, jobject _this, jint target, jint level, jint xoffset, jint yoffset, jint width, jint height, jint format, jint type, jint offset);

/*
 * Class:     com_gomdev_gles_GLESShader
 * Method:    nRetrieveProgramBinary
 * Signature: (ILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_gomdev_gles_GLESShader_nRetrieveProgramBinary
  (JNIEnv *, jobject, jint, jstring);

/*
 * Class:     com_gomdev_gles_GLESShader
 * Method:    nLoadProgramBinary
 * Signature: (IILjava/lang/String;Landroid/content/res/AssetManager;)I
 */
JNIEXPORT jint JNICALL Java_com_gomdev_gles_GLESShader_nLoadProgramBinary
  (JNIEnv *, jobject, jint, jint, jstring);

JNIEXPORT jint JNICALL Java_com_gomdev_gles_GLESShader_nFreeBinary
  (JNIEnv * env, jobject obj);

JNIEXPORT void JNICALL Java_com_gomdev_gles_GLESShader_nMapBufferRange
          (JNIEnv * env, jobject obj, jint size);

JNIEXPORT void JNICALL Java_com_gomdev_gles_GLESShader_nUnmapBuffer
          (JNIEnv * env, jobject obj);

JNIEXPORT void JNICALL Java_com_gomdev_gles_GLESShader_nUploadBuffer
          (JNIEnv * env, jobject obj,  jobject bitmap, jint stride, jint x, jint y, jint width, jint height);

#ifdef __cplusplus
}
#endif
#endif
