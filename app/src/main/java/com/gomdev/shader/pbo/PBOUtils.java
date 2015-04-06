package com.gomdev.shader.pbo;

import android.opengl.GLES20;
import android.opengl.GLES30;

import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVertexInfo;

/**
 * Created by gomdev on 15. 4. 9..
 */
public class PBOUtils {
    private PBOUtils() {

    }

    static GLESVertexInfo createGridPlane(PBOObject object) {

        float left = object.getX();
        float right = left + object.getWidth();
        float top = object.getY();
        float bottom = top - object.getHeight();
        float z = 0.0f;

        float[] vertex = {
                left, bottom, z,
                right, bottom, z,
                left, top, z,
                right, top, z
        };

        GLESVertexInfo vertexInfo = new GLESVertexInfo();

        GLESShader shader = object.getShader();
        vertexInfo.setBuffer(shader.getPositionAttribIndex(), vertex, 3);

        float[] texCoord = {
                0f, 1f,
                1f, 1f,
                0f, 0f,
                1f, 0f
        };

        vertexInfo.setBuffer(shader.getTexCoordAttribIndex(), texCoord, 2);

        vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);
        vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.TRIANGLE_STRIP);

        return vertexInfo;
    }

    static GLESVertexInfo createLineVertexInfo(GLESShader shader,
            float x, float y, float width, float height,
            int numOfObjectInWidth, int numOfObjectInHeight,
            int gridSize) {
        float left = x;
        float right = x + width;
        float top = y;
        float bottom = y - height;
        float z = 0.0f;

        int numOfVertex = (numOfObjectInWidth - 1) * 2 + (numOfObjectInHeight - 1) * 2;
        float[] position = new float[numOfVertex * 3];

        int i = 0;
        for (i = 0; i < (numOfObjectInHeight - 1); i++) {
            position[i * 6 + 0] = left;
            position[i * 6 + 1] = top - gridSize * (i + 1);
            position[i * 6 + 2] = 0f;

            position[i * 6 + 3] = right;
            position[i * 6 + 4] = top - gridSize * (i + 1);
            position[i * 6 + 5] = 0f;
        }

        for (int j = 0; j < (numOfObjectInWidth - 1); i++, j++) {
            position[i * 6 + 0] = left + gridSize * (j + 1);
            position[i * 6 + 1] = top;
            position[i * 6 + 2] = 0f;

            position[i * 6 + 3] = left + gridSize * (j + 1);
            position[i * 6 + 4] = bottom;
            position[i * 6 + 5] = 0f;
        }

        GLESVertexInfo vertexInfo = new GLESVertexInfo();

        vertexInfo.setBuffer(shader.getPositionAttribIndex(), position, 3);

        vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);
        vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.LINES);

        return vertexInfo;
    }

    static GLESVertexInfo createScreenVertexInfo(GLESShader shader,
            float x, float y, float width, float height) {
        float left = x;
        float right = x + width;
        float top = y;
        float bottom = y - height;
        float z = 0.0f;

        float[] vertex = {
                left, bottom, z,
                right, bottom, z,
                right, top, z,
                left, top, z
        };

        GLESVertexInfo vertexInfo = new GLESVertexInfo();

        vertexInfo.setBuffer(shader.getPositionAttribIndex(), vertex, 3);

        vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);
        vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.LINE_LOOP);

        return vertexInfo;
    }

    static boolean isInScreen(PBOObject object, float screenWidth, float screenHeight) {
        boolean isInScreen = false;

        float left = object.getX();
        float right = left + object.getWidth();
        float top = object.getY();
        float bottom = top - object.getHeight();

        isInScreen(left, right, bottom, top, screenWidth, screenHeight);

        return isInScreen;
    }

    static boolean isInScreen(float left, float right, float bottom, float top,
                              float screenWidth, float screenHeight) {
        boolean isInScreen = false;

        float screenLeft = -screenWidth * 0.5f;
        float screenRight = screenWidth * 0.5f;
        float screenTop = screenHeight * 0.5f;
        float screenBottom = -screenHeight * 0.5f;

        if (left < screenRight && right > screenLeft
                && top > screenBottom && bottom < screenTop) {
            isInScreen = true;
        }

        return isInScreen;
    }

    static void createTexture(PBOObject object) {
        int[] ids = new int[1];

        GLES30.glGenTextures(1, ids, 0);
        int textureID = ids[0];
        object.setTextureID(textureID);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureID);
        GLESUtils.checkGLError("glBindTexture()");

        int width = object.getWidth();
        int height = object.getHeight();
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, width, height, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
        GLESUtils.checkGLError("glTexImage2D()");

        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);

        GLESUtils.checkGLError("glTexParameteri()");

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);

        GLESUtils.checkGLError("glBindTexture() ");
    }

    static void destroyTexture(PBOObject object) {
        int textureID = object.getTextureID();

        if (GLES30.glIsTexture(textureID) == true) {
            int[] ids = new int[]{
                    textureID
            };
            GLES30.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES30.glDeleteTextures(1, ids, 0);
        }
    }
}
