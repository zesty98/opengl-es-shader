package com.gomdev.shader.multiTexture;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.util.Log;

import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESConfig.Version;
import com.gomdev.gles.GLESContext;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESObject;
import com.gomdev.gles.GLESObjectListener;
import com.gomdev.gles.GLESRect;
import com.gomdev.gles.GLESRendererListener;
import com.gomdev.gles.GLESSceneManager;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESShaderConstant;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVertexInfo;
import com.gomdev.shader.R;
import com.gomdev.shader.SampleRenderer;
import com.gomdev.shader.ShaderUtils;

import java.util.Random;

public class MultiTextureRenderer extends SampleRenderer implements GLESRendererListener {
    private static final String CLASS = "MultiTextureRenderer";
    private static final String TAG = MultiTextureConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = MultiTextureConfig.DEBUG;

    private static final int SPACING = 1;           // dp
    private static final int DEFAULT_WIDTH = 100;   // dp

    private GLESSceneManager mSM = null;
    private GLESNode mRoot = null;

    private MultiTextureObject[] mObjects = null;
    private GLESShader mShader = null;
    private GLESTexture mCheckTexture = null;

    private Random mRandom = new Random();

    private int mSpacing = 0;
    private int mObjectWidth = 0;
    private int mNumOfObjectsInWidth = 0;
    private int mNumOfObjectsInHeight = 0;
    private int mNumOfObjects = 0;

    private int mActionBarHeight = 0;
    private int mStatusBarHeight = 0;

    private Version mVersion;

    private float mScreenRatio = 0f;

    public MultiTextureRenderer(Context context) {
        super(context);

        if (DEBUG) {
            Log.d(TAG, "MultiTextureRenderer()");
        }

        mVersion = GLESContext.getInstance().getVersion();

        mSM = GLESSceneManager.createSceneManager();
        mRoot = mSM.createRootNode("Root");

        mRenderer.setListener(this);

        mActionBarHeight = MultiTextureUtils.getActionBarHeight(context);
        mStatusBarHeight = MultiTextureUtils.getStatusBarHeight(context);
    }

    public void destroy() {
        mObjects = null;
    }

    @Override
    protected void onDrawFrame() {
        super.updateFPS();

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        mRenderer.updateScene(mSM);
        mRenderer.drawScene(mSM);
    }

    @Override
    protected void onSurfaceChanged(int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceChanged() width=" + width + " height=" + height);
        }

        mRenderer.reset();

        mScreenRatio = (float) width / height;

        GLESCamera camera = setupCamera(width, height);

        mSpacing = GLESUtils.getPixelFromDpi(mContext, SPACING);
        mObjectWidth = GLESUtils.getPixelFromDpi(mContext, DEFAULT_WIDTH);
        mNumOfObjectsInWidth = width / (mObjectWidth + mSpacing);
        mObjectWidth = (width - mSpacing * (mNumOfObjectsInWidth + 1)) / mNumOfObjectsInWidth;
        mNumOfObjectsInHeight = (int) Math.ceil((double) height / (mObjectWidth + mSpacing));
        mNumOfObjects = mNumOfObjectsInWidth * mNumOfObjectsInHeight;

        mObjects = new MultiTextureObject[mNumOfObjects];

        GLESGLState state = new GLESGLState();
        state.setCullFaceState(true);
        state.setCullFace(GLES20.GL_BACK);
        state.setDepthState(false);
        state.setBlendState(true);
        state.setBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        mRoot.removeAll();

        for (int i = 0; i < mNumOfObjects; i++) {
            mObjects[i] = new MultiTextureObject("Object " + i);
            mObjects[i].setGLState(state);
            mObjects[i].setCamera(camera);
            mObjects[i].setShader(mShader);
            mObjects[i].setListener(mListener);
            mObjects[i].setIndex(i);

            mRoot.addChild(mObjects[i]);

            float x = -width * 0.5f + mSpacing + (i % mNumOfObjectsInWidth) * (mObjectWidth + mSpacing);
            float y = height * 0.5f - mSpacing - (i / mNumOfObjectsInWidth) * (mObjectWidth + mSpacing);

            GLESVertexInfo vertexInfo = MultiTextureUtils.createObject(mShader,
                    x, y, mObjectWidth, mObjectWidth);
            mObjects[i].setVertexInfo(vertexInfo, true, true);

            int color = mRandom.nextInt() | 0xFF000000;
            Bitmap bitmap = GLESUtils.makeBitmap(16, 16, Bitmap.Config.ARGB_8888, color);
            GLESTexture texture = new GLESTexture.Builder(GLES20.GL_TEXTURE_2D, 16, 16)
                    .load(bitmap);
            bitmap.recycle();

            mObjects[i].setTexture(texture);
        }
    }

    private GLESCamera setupCamera(int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "setupCamera() width=" + width + " hegiht=" + height);
        }

        GLESCamera camera = new GLESCamera();

        float fovy = 30f;
        float eyeZ = (height / 2f) / (float) Math.tan(Math.toRadians(fovy * 0.5));

        camera.setLookAt(0f, 0f, eyeZ, 0f, 0f, 0f, 0f, 1f, 0f);

        camera.setFrustum(fovy, mScreenRatio, eyeZ * 0.001f, eyeZ * 3f);

        camera.setViewport(new GLESRect(0, 0, width, height));

        return camera;
    }

    @Override
    protected void onSurfaceCreated() {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCreated()");
        }

        GLES20.glClearColor(1f, 1f, 1f, 1f);

        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.check_white_80);
        mCheckTexture = new GLESTexture.Builder(GLES20.GL_TEXTURE_2D, bitmap.getWidth(), bitmap.getHeight())
                .load(bitmap);
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

        int location = mShader.getUniformLocation("uTexture");
        GLES20.glUniform1i(location, 0);

        location = mShader.getUniformLocation("uTexture2");
        GLES20.glUniform1i(location, 1);

        location = mShader.getUniformLocation("uUseMultiTexture");
        GLES20.glUniform1i(location, 1);

        return true;
    }

    int getIndex(float x, float y) {
        int xOffset = (int) (x / mObjectWidth);
        int yOffset = (int) (y / mObjectWidth);

        int index = yOffset * mNumOfObjectsInWidth + xOffset;

        return index;
    }

    public void touchDown(float x, float y) {
        if (DEBUG) {
            Log.d(TAG, "touchDown() x=" + x + " y=" + y);
        }

        y -= mStatusBarHeight;
        y -= mActionBarHeight;

        int index = getIndex(x, y);
        boolean isChecked = mObjects[index].isChecked();
        if (isChecked == true) {
            mObjects[index].setCheck(false);
        } else {
            mObjects[index].setCheck(true);
        }

        mView.requestRender();
    }

    public void touchUp(float x, float y) {
        mView.requestRender();
    }

    public void touchMove(float x, float y) {
    }

    public void touchCancel(float x, float y) {
    }

    @Override
    public void setupVBO(GLESShader shader, GLESVertexInfo vertexInfo) {

    }

    @Override
    public void setupVAO(GLESShader shader, GLESVertexInfo vertexInfo) {

    }

    @Override
    public void enableVertexAttribute(GLESObject object) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mCheckTexture.getTextureID());
    }

    @Override
    public void disableVertexAttribute(GLESObject object) {

    }

    private GLESObjectListener mListener = new GLESObjectListener() {
        @Override
        public void update(GLESObject object) {

        }

        @Override
        public void apply(GLESObject object) {
            MultiTextureObject multiTextureObject = (MultiTextureObject) object;
            if (multiTextureObject.isChecked() == true) {
                int location = mShader.getUniformLocation("uUseMultiTexture");
                GLES20.glUniform1i(location, 1);
            } else {
                int location = mShader.getUniformLocation("uUseMultiTexture");
                GLES20.glUniform1i(location, 0);
            }
        }
    };
}
