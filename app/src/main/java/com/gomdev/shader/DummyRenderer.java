package com.gomdev.shader;

import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Handler;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class DummyRenderer implements Renderer {
    static final String CLASS = "DummyRenderer";
    static final String TAG = ShaderConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = ShaderConfig.DEBUG;

    private Context mContext = null;
    private Handler mHandler = null;

    public DummyRenderer(Context context) {
        mContext = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCrated()");
        }

        mHandler.sendEmptyMessage(MainActivity.REMOVE_DUMMY_GL_SURFACE);

        saveGPUInfoToPreferences();
    }

    private void saveGPUInfoToPreferences() {
        SharedPreferences pref = mContext.getSharedPreferences(
                ShaderConfig.PREF_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();

        // extensions
        String extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);
        ShaderContext.getInstance().setExtensions(extensions);

        editor.putString(ShaderConfig.PREF_GLES_EXTENSION, extensions);

        // renderer
        String renderer = GLES20.glGetString(GLES20.GL_RENDERER);
        ShaderContext.getInstance().setRenderer(renderer);

        editor.putString(ShaderConfig.PREF_GLES_RENDERER, renderer);

        // vendor
        String vendor = GLES20.glGetString(GLES20.GL_VENDOR);
        ShaderContext.getInstance().setVendor(vendor);

        editor.putString(ShaderConfig.PREF_GLES_VENDOR, vendor);

        // version
        String version = GLES20.glGetString(GLES20.GL_VERSION);
        ShaderContext.getInstance().setVersionStr(version);

        editor.putString(ShaderConfig.PREF_GLES_VERSION, version);

        editor.commit();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceChanged() width=" + width + " height="
                    + height);
        }

        GLES20.glViewport(0, 0, 1, 1);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (DEBUG) {
            Log.d(TAG, "onDrawFrame()");
        }
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }
}
