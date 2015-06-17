package com.gomdev.shader;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gomdev.gles.GLESConfig.Version;
import com.gomdev.gles.GLESContext;
import com.gomdev.gles.GLESUtils;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public abstract class SampleRenderer implements Renderer {
    static final String CLASS = "SampleRenderer";
    static final String TAG = ShaderConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = ShaderConfig.DEBUG;

    protected static final int COMPILE_OR_LINK_ERROR = 1;
    protected static final int COMPILE_AND_LINK_SUCCESS = 2;
    protected static final int UPDATE_FPS = 3;

    static {
        System.loadLibrary("gomdev");
    }

    protected Context mContext;
    protected GLSurfaceView mView;

    private boolean mIsShaderCompiled = false;
    private boolean mIsOnSurfaceCreatedCalled = false;

    protected Handler mHandler = null;

    public SampleRenderer(Context context) {
        mContext = context;

        GLESContext.getInstance().setContext(context);

        mHandler = new SampleHandler(context);
    }

    public void setSurfaceView(GLSurfaceView surfaceView) {
        mView = surfaceView;
    }

    protected void updateFPS() {
        int fps = (int) GLESUtils.getFPS();

        Message msg = mHandler.obtainMessage(UPDATE_FPS);
        msg.arg1 = fps;
        mHandler.sendMessage(msg);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCreated()");
        }

        mIsShaderCompiled = createShader();

        if (mIsShaderCompiled == true) {
            mHandler.sendEmptyMessage(SampleRenderer.COMPILE_AND_LINK_SUCCESS);
            onSurfaceCreated();
        } else {
            mHandler.sendEmptyMessage(SampleRenderer.COMPILE_OR_LINK_ERROR);
            Log.e(TAG, "\t shader compiliation fails");
        }

        mIsOnSurfaceCreatedCalled = true;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceChanged()");
        }

        if (mIsOnSurfaceCreatedCalled == false) {
            mIsShaderCompiled = createShader();
        }

        if (mIsShaderCompiled == true) {
            mHandler.sendEmptyMessage(SampleRenderer.COMPILE_AND_LINK_SUCCESS);
            onSurfaceChanged(width, height);
        } else {
            mHandler.sendEmptyMessage(SampleRenderer.COMPILE_OR_LINK_ERROR);
        }

        mIsOnSurfaceCreatedCalled = false;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (DEBUG) {
            Log.d(TAG, "onDrawFrame()");
        }

        if (mIsShaderCompiled == true) {
            onDrawFrame();
        }
    }

    protected boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    protected abstract void onSurfaceCreated();

    protected abstract void onSurfaceChanged(int width, int height);

    protected abstract void onDrawFrame();

    protected abstract boolean createShader();
}