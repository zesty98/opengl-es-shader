package com.gomdev.shader.multiTexture;

import android.content.Context;
import android.util.TypedValue;

import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESVertexInfo;

/**
 * Created by gomdev on 15. 4. 21..
 */
public class MultiTextureUtils {
    private static final String CLASS = "MultiTextureUtils";
    private static final String TAG = MultiTextureConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = MultiTextureConfig.DEBUG;

    static int getActionBarHeight(Context context) {
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(
                android.R.attr.actionBarSize, tv, true)) {
            int actionBarHeight = TypedValue.complexToDimensionPixelSize(
                    tv.data, context.getResources().getDisplayMetrics());

            return actionBarHeight;
        }

        return 0;
    }

    static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }

        return result;
    }

    public static GLESVertexInfo createObject(GLESShader shader,
                                              float x, float y,
                                              float width, float height) {

        float left = x;
        float right = x + width;
        float top = y;
        float bottom = y - height;
        float z = 0f;

        float[] vertex = {
                left, bottom, z,
                right, bottom, z,
                left, top, z,
                right, top, z
        };

        GLESVertexInfo vertexInfo = new GLESVertexInfo();

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
}
