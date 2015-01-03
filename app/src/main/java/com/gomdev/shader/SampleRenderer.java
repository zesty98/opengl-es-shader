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
import com.gomdev.gles.GLESRenderer;
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
    protected GLESRenderer mRenderer;

    protected TextView mFPS = null;

    private boolean mIsShaderCompiled = false;
    private boolean mIsOnSurfaceCreatedCalled = false;

    protected Handler mHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case COMPILE_OR_LINK_ERROR:
                    Toast.makeText(mContext, "Compile or Link fails",
                            Toast.LENGTH_SHORT).show();

                    showCompileLog();
                    break;
                case COMPILE_AND_LINK_SUCCESS:
                    setupInformation();
                    break;
                case UPDATE_FPS:
                    ShaderContext context = ShaderContext.getInstance();
                    boolean showInfo = context.showInfo();
                    boolean showFPS = context.showFPS();
                    if (showInfo == true) {
                        LinearLayout layout = (LinearLayout) ((Activity) mContext)
                                .findViewById(R.id.layout_fps);
                        if (showFPS == false) {
                            layout.setVisibility(View.INVISIBLE);
                        } else {
                            layout.setVisibility(View.VISIBLE);
                        }
                    }

                    if (showFPS == false || showInfo == false) {
                        return;
                    }

                    if (mFPS == null) {
                        mFPS = (TextView) ((Activity) mContext)
                                .findViewById(R.id.fps);
                    }
                    mFPS.setText("" + msg.arg1);
                    break;
            }
        }

        private void setupInformation() {
            LinearLayout layout = (LinearLayout) ((Activity) mContext)
                    .findViewById(R.id.layout_info);
            boolean showInfo = ShaderContext.getInstance().showInfo();
            if (showInfo == true) {
                layout.setVisibility(View.VISIBLE);
                showGLESVersion();
            } else {
                layout.setVisibility(View.INVISIBLE);
            }
        }

        private void showGLESVersion() {
            TextView textView = (TextView) ((Activity) mContext)
                    .findViewById(R.id.layout_version);

            Version version = GLESContext.getInstance().getVersion();
            switch (version) {
                case GLES_20:
                    textView.setText("OpenGL ES 2.0");
                    break;
                case GLES_30:
                    textView.setText("OpenGL ES 3.0");
                    break;
                default:

            }
        }

        private void showCompileLog() {
            TextView view = (TextView) ((Activity) mContext)
                    .findViewById(R.id.error_log);
            LinearLayout layout = (LinearLayout) ((Activity) mContext)
                    .findViewById(R.id.layout_info);

            String compileLog = GLESContext.getInstance()
                    .getShaderErrorLog();
            if (compileLog != null) {
                view.setText(compileLog);
                view.setVisibility(View.VISIBLE);
                layout.setVisibility(View.INVISIBLE);
            } else {
                view.setVisibility(View.INVISIBLE);
                layout.setVisibility(View.VISIBLE);
            }
        }

    };

    public SampleRenderer(Context context) {
        mContext = context;

        GLESContext.getInstance().setContext(context);

        mRenderer = GLESRenderer.createRenderer();
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