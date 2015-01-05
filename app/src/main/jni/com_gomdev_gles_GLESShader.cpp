#include "com_gomdev_gles_GLESShader.h"

#include <jni.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <EGL/egl.h>

#include <stdio.h>
#include <stdlib.h>
#include <android/log.h>
#include <android/asset_manager.h>

#include <string.h>

#define  LOG_TAG    "gomdev"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#define MAX_NUM_OF_CACHED_BINARY    5

#define DEBUG

#ifdef __cplusplus
extern "C" {
#endif

typedef struct _BinaryInfo {
    struct _BinaryInfo* next;
    struct _BinaryInfo* prev;
    int length;
    char* name;
    void* binary;
} BinaryInfo;

static BinaryInfo* spRoot = NULL;

static PFNGLGETPROGRAMBINARYOESPROC glGetProgramBinaryOES = NULL;
static PFNGLPROGRAMBINARYOESPROC glProgramBinaryOES = NULL;

static int sBinaryFormat = -1;

void removeBinary(BinaryInfo* info) {
    free(info->name);
    free(info->binary);
    free(info);
}

void removeBinaryFromList(BinaryInfo* info, bool needToDelete) {
    BinaryInfo* prevInfo = info->prev;
    BinaryInfo* nextInfo = info->next;

    prevInfo->next = nextInfo;

    if (nextInfo == NULL) {
        if (prevInfo == spRoot) {
            spRoot->prev = NULL;
        } else {
            spRoot->prev = prevInfo;
        }
    } else {
        nextInfo->prev = prevInfo;
    }

    if (needToDelete == true) {
        removeBinary(info);
    }
}

void dump(char* str) {
    LOGI("dump() %s", str);

    if (spRoot == NULL) {
        return;
    }

    BinaryInfo* info = spRoot->next;

    while(info != NULL) {
        info = info->next;
    }
}

char* copyFileName(const char* fileName) {
    int length = strlen(fileName);
    char* name = (char*) malloc(length + 1);
    memset(name, '\0', length + 1);
    strcpy(name, fileName);

    return name;
}

int getNumOfBinaryInfo() {
    int numOfBinaryInfo = 0;

    if (spRoot == NULL) {
        return 0;
    }

    BinaryInfo* info = spRoot->next;

    while (info != NULL) {
        ++numOfBinaryInfo;
        info = info->next;
    }

    return numOfBinaryInfo;
}

void add(BinaryInfo* info) {
    if (spRoot == NULL) {
        spRoot = (BinaryInfo*) malloc(sizeof(BinaryInfo));
        spRoot->next = NULL;
        spRoot->prev = NULL;
    }

    if (spRoot->prev != NULL) {
        BinaryInfo* lastNode = spRoot->prev;

        lastNode->next = info;
        info->prev = lastNode;
        info->next = NULL;
        spRoot->prev = info;
    } else {
        spRoot->prev = info;
        spRoot->next = info;

        info->next = NULL;
        info->prev = spRoot;
    }

    int numOfBinaryInfo = getNumOfBinaryInfo();
    if (numOfBinaryInfo > MAX_NUM_OF_CACHED_BINARY) {
        if (spRoot->next != NULL) {
            removeBinaryFromList(spRoot->next, true);
        }
    }
    numOfBinaryInfo = getNumOfBinaryInfo();
}

BinaryInfo* find(char* name) {
    if (spRoot == NULL) {
        return NULL;
    }

    BinaryInfo* info = spRoot->next;

    while(info != NULL) {
        if (strcmp(name, info->name) == 0) {
            return info;
        }
        info = info->next;
    }

    return NULL;
}

void clear() {
    BinaryInfo* info = spRoot->next;
    BinaryInfo* temp = NULL;

    do {
        temp = info->next;
        removeBinary(info);
    } while(temp != NULL);

    free(spRoot);
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

    char* name = copyFileName(fileName);

    BinaryInfo* binaryInfo = (BinaryInfo*) malloc(sizeof(BinaryInfo));
    if (binaryInfo != NULL) {
        binaryInfo->length = length;
        binaryInfo->binary = binary;
        binaryInfo->name = name;
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
        char* name = copyFileName(fileName);

        BinaryInfo* info = find(name);
        if (info != NULL) {
            removeBinaryFromList(info, true);
        }

        BinaryInfo* binaryInfo = (BinaryInfo*)malloc(sizeof(BinaryInfo));
        if (binaryInfo != NULL ) {
            binaryInfo->name = name;
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
        BinaryInfo* binaryInfo = find(fileName);
        if(binaryInfo != NULL) {
#ifdef DEBUG
            LOGI("Load() cache hit!!! - %s", fileName);
#endif
            removeBinaryFromList(binaryInfo, false);
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
