#include "com_gomdev_gles_GLESShader.h"

#include <jni.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <EGL/egl.h>

#include <stdio.h>
#include <stdlib.h>
#include <android/log.h>
#include <android/asset_manager.h>

#include <list>
#include <string>

#define  LOG_TAG    "gomdev"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#define MAX_NUM_OF_CACHED_BINARY    10

#define DEBUG

#ifdef __cplusplus
extern "C" {
#endif

typedef struct _BinaryInfo {
    int length;
    std::string* name;
    void* binary;
} BinaryInfo;

static std::list<BinaryInfo*> sBinaryList;

typedef std::list<BinaryInfo*>::iterator BinaryIter;

static PFNGLGETPROGRAMBINARYOESPROC glGetProgramBinaryOES = NULL;
static PFNGLPROGRAMBINARYOESPROC glProgramBinaryOES = NULL;

static int sBinaryFormat = -1;

void add(BinaryInfo* info) {
    sBinaryList.push_back(info);

    if (sBinaryList.size() > MAX_NUM_OF_CACHED_BINARY) {
        sBinaryList.pop_front();
    }
}

void removeBinary(BinaryIter iter) {
    BinaryInfo* info = *iter;
    delete info->name;
    delete info->binary;
    free(info);
    sBinaryList.erase(iter);
}

BinaryIter find(std::string name) {
    BinaryIter iter;

    BinaryInfo* info = NULL;
    for (iter = sBinaryList.begin(); iter != sBinaryList.end(); iter++) {
        info = *iter;

        if (name.compare(*info->name) == 0) {
            return iter;
        }
    }

    return sBinaryList.end();
}

void clear() {
    BinaryIter iter;
    for (iter = sBinaryList.begin(); iter != sBinaryList.end(); iter++) {
        BinaryInfo* info = *iter;
        delete info->name;
        delete info->binary;
        delete info;
    }

    sBinaryList.clear();
}

BinaryInfo* readFile(char* fileName) {
    FILE *fp = fopen(fileName, "rb");
    if (fp == NULL) {
        LOGE("\treadFile() file open fail");
        return NULL;
    }

    fseek(fp, 0, SEEK_END);
    int length = ftell(fp);
#ifdef DEBUG
    LOGI("\treadFile() length=%d", length);
#endif
    fseek(fp, 0, SEEK_SET);

    void* binary = (GLvoid*) malloc(length);
    if (binary == NULL) {
        LOGE("\treadFile() malloc fail");
        return NULL;
    }

    ssize_t size = fread(binary, 1, length, fp);

    if (size == -1) {
        LOGE("\treadFile() file read fail %d", size);
        free(binary);
        return NULL;
    }

    fclose(fp);

    std::string name = fileName;

    BinaryInfo* binaryInfo = (BinaryInfo*) malloc(sizeof(BinaryInfo));
    if (binaryInfo != NULL) {
        binaryInfo->length = length;
        binaryInfo->binary = binary;
        binaryInfo->name = new std::string(fileName);
    }

    return binaryInfo;
}

void checkGLError(char* str) {
#ifdef DEBUG
    int error = 0;
    if ((error = glGetError()) != 0x0) {
        LOGE("%s error=0x%x", str, error);
    }
#endif
}

void dump(char* str) {
    LOGI("dump() %s", str);

    BinaryInfo* info = NULL;
    BinaryIter iter;
    for (iter = sBinaryList.begin(); iter != sBinaryList.end(); iter++) {
        info = *iter;
        LOGI("\tName=%s BinaryInfo=%p", (info->name)->c_str(), info);
    }
}

jstring JNICALL Java_com_gomdev_gles_GLESShader_nGetShaderCompileLog(
        JNIEnv * env, jobject obj, jint shader) {
    GLint infoLen = 0;

    glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);
    if (infoLen <= 1) {
        infoLen = 1024;
    }

    char* infoLog = (char*) malloc(sizeof(char) * infoLen);

    glGetShaderInfoLog(shader, infoLen, NULL, infoLog);
    jstring result = env->NewStringUTF(infoLog);

    free(infoLog);

    return result;

}

int JNICALL Java_com_gomdev_gles_GLESShader_nRetrieveProgramBinary
(JNIEnv * env, jobject obj, jint program, jstring str)
{
    GLint binaryLength;
    GLvoid* binary;
    FILE* outfile;
    GLenum binaryFormat;

    const char* fileName = env->GetStringUTFChars(str, NULL);
    if(fileName != NULL)
    {
        glGetProgramiv(program, GL_PROGRAM_BINARY_LENGTH_OES, &binaryLength);

        checkGLError("retrieve() glGetProgramiv");

#ifdef DEBUG
        LOGI("retrieve() binaryLength=%d", binaryLength);
#endif
        binary = (GLvoid*)malloc(binaryLength);
        if(binary == NULL)
        {
            LOGE("nRetrieveProgramBinary() malloc fail");
        }

        if (glGetProgramBinaryOES == NULL) {
            glGetProgramBinaryOES = (PFNGLGETPROGRAMBINARYOESPROC) eglGetProcAddress("glGetProgramBinaryOES");
        }
        glGetProgramBinaryOES(program, binaryLength, NULL, &binaryFormat, binary);

        checkGLError("retrieve() glGetProgramBinaryOES");

        outfile = fopen(fileName, "wb");
        if(outfile == NULL)
        {
            LOGE("nRetrieveProgramBinary() fileName=%s", fileName);
            LOGE("nRetrieveProgramBinary() fopen error");
            free(binary);
            return 0;
        }
        fwrite(binary, binaryLength, 1, outfile);
        fclose(outfile);

        // if binary is already cached, remove cached binary
        std::string name = fileName;
        BinaryIter iter = find(name);
        if (iter != sBinaryList.end()) {
            removeBinary(iter);
        }

        BinaryInfo* binaryInfo = (BinaryInfo*)malloc(sizeof(BinaryInfo));
        if (binaryInfo != NULL ) {
            binaryInfo->name = new std::string(fileName);
            binaryInfo->length = binaryLength;
            binaryInfo->binary = binary;

            add(binaryInfo);
        }

        env->ReleaseStringUTFChars(str, fileName);
    }

    sBinaryFormat = binaryFormat;

    return binaryFormat;
}

int JNICALL Java_com_gomdev_gles_GLESShader_nLoadProgramBinary
(JNIEnv * env, jobject obj, jint program, jint binaryFormat, jstring str)
{
    char* fileName = (char*)env->GetStringUTFChars(str, NULL);

#ifdef DEBUG
    dump("before");
#endif

    if(fileName != NULL)
    {
        BinaryInfo* binaryInfo = NULL;
        std::string name = fileName;
        BinaryIter iter = find(name);
        if(iter != sBinaryList.end()) {
#ifdef DEBUG
            LOGI("Load() cache hit!!! - %s", fileName);
#endif
            binaryInfo = *iter;
            sBinaryList.erase(iter);
            add(binaryInfo);
        } else {
#ifdef DEBUG
            LOGI("Load() cache miss!! - %s", fileName);
#endif
            binaryInfo = readFile(fileName);
            if(binaryInfo == NULL) {
                LOGE("Load() binaryInfo is NULL");
                return 0;
            }

            add(binaryInfo);
        }

        if (glProgramBinaryOES == NULL) {
            glProgramBinaryOES = (PFNGLPROGRAMBINARYOESPROC) eglGetProcAddress("glProgramBinaryOES");
        }
        glProgramBinaryOES(program,
                sBinaryFormat,
                binaryInfo->binary,
                binaryInfo->length);

        GLint success;
        glGetProgramiv(program, GL_LINK_STATUS, &success);

        env->ReleaseStringUTFChars(str, fileName);

        if (!success)
        {
            LOGE("nLoadProgramBinary() link fail");
            return 0;
        }

#ifdef DEBUG
        dump("after");
#endif
        return 1;
    }
    else {
        LOGE("nLoadProgramBinary() fileName is NULL");
        return 0;
    }
}

int JNICALL Java_com_gomdev_gles_GLESShader_nFreeBinary(JNIEnv * env, jobject obj) {
    clear();
}

#ifdef __cplusplus
}
#endif
