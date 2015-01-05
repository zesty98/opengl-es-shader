package com.gomdev.shader.compressedTexture;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.opengl.GLES20;
import android.support.v4.view.GestureDetectorCompat;
import android.text.InputType;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESCompressedTextureInfo;
import com.gomdev.gles.GLESConfig;
import com.gomdev.gles.GLESConfig.Version;
import com.gomdev.gles.GLESContext;
import com.gomdev.gles.GLESDDSDecoder;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESMeshUtils;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESObject;
import com.gomdev.gles.GLESRect;
import com.gomdev.gles.GLESSceneManager;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESShaderConstant;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVertexInfo;
import com.gomdev.shader.R;
import com.gomdev.shader.SampleRenderer;
import com.gomdev.shader.ShaderUtils;

import java.io.IOException;
import java.io.InputStream;

public class CompressedTextureRenderer extends SampleRenderer {
    private static final String CLASS = "CompressedTextureRenderer";
    private static final String TAG = CompressedTextureConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = CompressedTextureConfig.DEBUG;

    private GLESSceneManager mSM = null;

    private GLESNode mRoot = null;
    private GLESObject mObject = null;
    private GLESObject mBGObject = null;
    private GLESShader mShader = null;

    private GLESTexture mBGTexture = null;
    private GLESTexture mOriginalTexture = null;
    private GLESTexture mATITCTexture = null;
    private GLESTexture mETC1Texture = null;
    private GLESTexture mETC2Texture = null;

    private GestureDetectorCompat mGestureDetector = null;

    private Version mVersion;

    private float mScreenRatio = 0f;

    private int mIndex = 0;

    private Object mLockObject = new Object();

    private LinearLayout mLayout = null;
    private TextView mInfoView = null;

    private String[] mInfos = null;

    public CompressedTextureRenderer(Context context) {
        super(context);

        mVersion = GLESContext.getInstance().getVersion();

        if (mVersion == Version.GLES_20) {
            mInfos = new String[]{
                    "BG",
                    "BG + Original Texture",
                    "BG + Compressed Texture ATITC",
                    "BG + Compressed Texture ETC1"
            };
        } else {
            mInfos = new String[]{
                    "BG",
                    "BG + Original Texture",
                    "BG + Compressed Texture ATITC",
                    "BG + Compressed Texture ETC1",
                    "BG + Compressed Texture ETC2"
            };
        }

        createScene();

        mGestureDetector = new GestureDetectorCompat(context, mGestureListener);

        setupInfoView();
    }

    private void setupInfoView() {
        mLayout = (LinearLayout) ((Activity) mContext)
                .findViewById(R.id.layout_info);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        mInfoView = new TextView(mContext);
        mLayout.addView(mInfoView, params);
        mInfoView.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        mInfoView.setTextColor(Color.WHITE);
        mInfoView.setVisibility(View.VISIBLE);
        mInfoView.setText(mInfos[0]);

        mIndex = 0;
    }

    private void createScene() {
        mSM = GLESSceneManager.createSceneManager();
        mRoot = mSM.createRootNode("Root");

        mBGObject = mSM.createObject("BG");
        mObject = mSM.createObject("Object");

        {
            GLESGLState state = new GLESGLState();
            state.setCullFaceState(true);
            state.setCullFace(GLES20.GL_BACK);
            state.setDepthState(true);
            state.setDepthFunc(GLES20.GL_LEQUAL);
            state.setBlendState(true);
            state.setBlendFunc(
                    GLES20.GL_SRC_ALPHA,
                    GLES20.GL_ONE_MINUS_SRC_ALPHA);

            mObject.setGLState(state);
        }

        {
            GLESGLState state = new GLESGLState();
            state.setCullFaceState(true);
            state.setCullFace(GLES20.GL_BACK);
            state.setDepthState(true);
            state.setDepthFunc(GLES20.GL_LEQUAL);
            state.setBlendState(true);
            state.setBlendFunc(
                    GLES20.GL_SRC_ALPHA,
                    GLES20.GL_ONE_MINUS_SRC_ALPHA);

            mBGObject.setGLState(state);
        }

        mRoot.addChild(mBGObject);
        mRoot.addChild(mObject);
    }

    public void destroy() {
        mObject = null;
        mBGObject = null;
    }

    @Override
    protected void onDrawFrame() {
        synchronized (mLockObject) {
            super.updateFPS();

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            mRenderer.updateScene(mSM);
            mRenderer.drawScene(mSM);
        }
    }

    @Override
    protected void onSurfaceChanged(int width, int height) {
        mRenderer.reset();

        mScreenRatio = (float) width / height;

        GLESCamera camera = setupCamera(width, height);

        mObject.setCamera(camera);
        mBGObject.setCamera(camera);

        GLESVertexInfo vertexInfo = GLESMeshUtils.createPlane(mShader,
                width, height, false, true, false, false);

        mObject.setVertexInfo(vertexInfo, true, true);
        mObject.hide();

        mBGObject.setVertexInfo(vertexInfo, true, true);
    }

    private GLESCamera setupCamera(int width, int height) {
        GLESCamera camera = new GLESCamera();

        float fovy = 30f;
        float eyeZ = (height / 2f) / (float) Math.tan(Math.toRadians(fovy * 0.5));

        camera.setLookAt(0f, 0f, eyeZ, 0f, 0f, 0f, 0f, 1f, 0f);

        camera.setFrustum(fovy, mScreenRatio, eyeZ * 0.01f, eyeZ * 10f);

        camera.setViewport(new GLESRect(0, 0, width, height));

        return camera;
    }

    @Override
    protected void onSurfaceCreated() {
        GLES20.glClearColor(0.7f, 0.7f, 0.7f, 0.0f);

        createTextures();

        mObject.setShader(mShader);
        mObject.setTexture(mOriginalTexture);

        mBGObject.setShader(mShader);
        mBGObject.setTexture(mBGTexture);
    }

    private void createTextures() {
        {
            Bitmap bitmap = GLESUtils.makeBitmap(512, 512, Bitmap.Config.ARGB_8888, 0xFFFFFF00);
            GLESTexture.Builder builder = new GLESTexture.Builder(
                    GLES20.GL_TEXTURE_2D, bitmap.getWidth(), bitmap.getHeight());
            mBGTexture = builder.load(bitmap);
        }
        {
            Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.transparent);
            GLESTexture.Builder builder = new GLESTexture.Builder(
                    GLES20.GL_TEXTURE_2D, bitmap.getWidth(), bitmap.getHeight());
            mOriginalTexture = builder.load(bitmap);
        }

        {
            InputStream inputStream = mContext.getResources().openRawResource(R.raw.transparent_atitc);
            GLESCompressedTextureInfo textureInfo = null;
            try {
                textureInfo = GLESDDSDecoder.decode(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            GLESTexture.Builder builder = new GLESTexture.Builder(
                    GLES20.GL_TEXTURE_2D, textureInfo.getWidth(), textureInfo.getHeight())
                    .setInternalFormat(GLESConfig.GL_ATC_RGBA_EXPLICIT_ALPHA_AMD);
            mATITCTexture = builder.load(textureInfo.getData());
        }

        {
            InputStream inputStream = mContext.getResources().openRawResource(R.raw.transparent_etc1);
            GLESCompressedTextureInfo textureInfo = null;
            try {
                textureInfo = GLESDDSDecoder.decode(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            GLESTexture.Builder builder = new GLESTexture.Builder(
                    GLES20.GL_TEXTURE_2D, textureInfo.getWidth(), textureInfo.getHeight())
                    .setInternalFormat(GLESConfig.GL_ETC1_RGB8_OES);
            mETC1Texture = builder.load(textureInfo.getData());
        }

        if (mVersion == Version.GLES_30) {
            InputStream inputStream = mContext.getResources().openRawResource(R.raw.transparent_etc2);
            GLESCompressedTextureInfo textureInfo = null;
            try {
                textureInfo = GLESDDSDecoder.decode(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            GLESTexture.Builder builder = new GLESTexture.Builder(
                    GLES20.GL_TEXTURE_2D, textureInfo.getWidth(), textureInfo.getHeight())
                    .setInternalFormat(GLESConfig.GL_COMPRESSED_RGBA8_ETC2_EAC);
            mETC2Texture = builder.load(textureInfo.getData());
        }
    }

    @Override
    protected boolean createShader() {
        if (DEBUG) {
            Log.d(TAG, "createShader()");
        }

        mShader = new GLESShader(mContext);

        String vsSource = ShaderUtils.getShaderSource(mContext, 0);
        String fsSource = ShaderUtils.getShaderSource(mContext, 1);

        mShader.setShaderSource(vsSource, fsSource);
        if (mShader.load() == false) {
            return false;
        }

        if (mVersion == Version.GLES_20) {
            String attribName = GLESShaderConstant.ATTRIB_POSITION;
            mShader.setPositionAttribIndex(attribName);

            attribName = GLESShaderConstant.ATTRIB_TEXCOORD;
            mShader.setTexCoordAttribIndex(attribName);
        }

        return true;
    }

    @Override
    protected boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }


    public GestureDetector.SimpleOnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            mIndex++;

            if (mIndex > (mInfos.length - 1)) {
                mIndex = 0;
            }

            synchronized (mLockObject) {
                switch (mIndex) {
                    case 0:
                        mObject.hide();
                        mInfoView.setText(mInfos[mIndex]);
                        break;
                    case 1:
                        mObject.show();
                        mObject.setTexture(mOriginalTexture);
                        mInfoView.setText(mInfos[mIndex]);
                        break;
                    case 2:
                        mObject.show();
                        mObject.setTexture(mATITCTexture);
                        mInfoView.setText(mInfos[mIndex]);
                        break;
                    case 3:
                        mObject.show();
                        mObject.setTexture(mETC1Texture);
                        mInfoView.setText(mInfos[mIndex]);
                        break;
                    case 4:
                        mObject.show();
                        mObject.setTexture(mETC2Texture);
                        mInfoView.setText(mInfos[mIndex]);
                        break;
                }

                mView.requestRender();
            }
            return super.onSingleTapUp(e);
        }
    };
}
