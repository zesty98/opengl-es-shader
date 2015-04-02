package com.gomdev.shader.shaderIcon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.util.Log;

import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESConfig.Version;
import com.gomdev.gles.GLESContext;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESMeshUtils;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESNodeListener;
import com.gomdev.gles.GLESObject;
import com.gomdev.gles.GLESObjectListener;
import com.gomdev.gles.GLESRect;
import com.gomdev.gles.GLESSceneManager;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESShaderConstant;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESTransform;
import com.gomdev.gles.GLESVertexInfo;
import com.gomdev.shader.R;
import com.gomdev.shader.SampleRenderer;
import com.gomdev.shader.ShaderUtils;

public class ShaderIconRenderer extends SampleRenderer {
    private static final String CLASS = "ShaderIconRenderer";
    private static final String TAG = ShaderIconConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = ShaderIconConfig.DEBUG;

    private GLESSceneManager mSM = null;

    private GLESNode mBGNode = null;
    private GLESNode mCubeNode = null;

    private GLESObject mFront = null;
    private GLESObject mLeft = null;
    private GLESObject mRight = null;
    private GLESObject mBack = null;
    private GLESObject mTop = null;
    private GLESObject mBottom = null;

    private GLESObject mBG = null;

    private GLESShader mShader = null;
    private GLESShader mBGShader = null;
    private Version mVersion;

    private boolean mIsTouchDown = false;

    private float mDownX = 0f;
    private float mDownY = 0f;

    private float mMoveX = 0f;
    private float mMoveY = 0f;

    private float mScreenRatio = 0f;

    private float mPreTranslate = 0f;

    public ShaderIconRenderer(Context context) {
        super(context);

        mVersion = GLESContext.getInstance().getVersion();

        mSM = GLESSceneManager.createSceneManager();
        GLESNode root = mSM.createRootNode("Root");

        {
            mBGNode = mSM.createNode("BGNode");
            root.addChild(mBGNode);

            {
                mBG = mSM.createObject("BG");
                mBG.setListener(mBGListener);

                GLESGLState state = new GLESGLState();
                state.setCullFaceState(true);
                state.setCullFace(GLES20.GL_BACK);
                state.setDepthState(false);
                mBG.setGLState(state);
            }

            mBGNode.addChild(mBG);
        }

        {
            mCubeNode = mSM.createNode("CubeNode");
            mCubeNode.setListener(mCubeNodeListener);
            root.addChild(mCubeNode);

            {
                mFront = mSM.createObject("Front");
                mFront.setListener(mFrontListener);

                GLESGLState state = new GLESGLState();
                state.setCullFaceState(true);
                state.setCullFace(GLES20.GL_BACK);
                state.setDepthState(true);
                state.setDepthFunc(GLES20.GL_LEQUAL);
                mFront.setGLState(state);

                mCubeNode.addChild(mFront);
            }

            {
                mLeft = mSM.createObject("Left");
                mLeft.setListener(mLeftListener);

                GLESGLState state = new GLESGLState();
                state.setCullFaceState(true);
                state.setCullFace(GLES20.GL_BACK);
                state.setDepthState(true);
                state.setDepthFunc(GLES20.GL_LEQUAL);
                mLeft.setGLState(state);

                mCubeNode.addChild(mLeft);
            }

            {
                mRight = mSM.createObject("Right");
                mRight.setListener(mRightListener);

                GLESGLState state = new GLESGLState();
                state.setCullFaceState(true);
                state.setCullFace(GLES20.GL_BACK);
                state.setDepthState(true);
                state.setDepthFunc(GLES20.GL_LEQUAL);
                mRight.setGLState(state);

                mCubeNode.addChild(mRight);
            }

            {
                mBack = mSM.createObject("Back");
                mBack.setListener(mBackListener);

                GLESGLState state = new GLESGLState();
                state.setCullFaceState(true);
                state.setCullFace(GLES20.GL_BACK);
                state.setDepthState(true);
                state.setDepthFunc(GLES20.GL_LEQUAL);
                mBack.setGLState(state);

                mCubeNode.addChild(mBack);
            }

            {
                mTop = mSM.createObject("Top");
                mTop.setListener(mTopListener);

                GLESGLState state = new GLESGLState();
                state.setCullFaceState(true);
                state.setCullFace(GLES20.GL_BACK);
                state.setDepthState(true);
                state.setDepthFunc(GLES20.GL_LEQUAL);
                mTop.setGLState(state);

                mCubeNode.addChild(mTop);
            }

            {
                mBottom = mSM.createObject("Bottom");
                mBottom.setListener(mBottomListener);

                GLESGLState state = new GLESGLState();
                state.setCullFaceState(true);
                state.setCullFace(GLES20.GL_BACK);
                state.setDepthState(true);
                state.setDepthFunc(GLES20.GL_LEQUAL);
                mBottom.setGLState(state);

                mCubeNode.addChild(mBottom);
            }
        }
    }

    public void destroy() {
        mBG = null;

        mFront = null;
        mLeft = null;
        mRight = null;
        mBack = null;
        mTop = null;
        mBottom = null;
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
        mScreenRatio = (float) width / height;
        mPreTranslate = mScreenRatio * 0.4f;

        float cubeSize = mScreenRatio * 0.7f;

        mRenderer.reset();

        GLESCamera camera = setupCamera(width, height);

        {
            mBG.setCamera(camera);

            GLESVertexInfo vertexInfo = GLESMeshUtils.createPlane(mBGShader,
                    mScreenRatio * 2f, 2f, false, true, false, false);
            mBG.setVertexInfo(vertexInfo, true, true);

            Bitmap bitmap = BitmapFactory.decodeResource(
                    mContext.getResources(), R.drawable.bg);
            GLESTexture.Builder builder = new GLESTexture.Builder(
                    GLES20.GL_TEXTURE_2D, bitmap.getWidth(), bitmap.getHeight());
            GLESTexture texture = builder.load(bitmap);

            if (mVersion == Version.GLES_30) {
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,
                        texture.getTextureID());
                GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
                GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,
                        GLES30.GL_TEXTURE_MIN_FILTER,
                        GLES30.GL_LINEAR_MIPMAP_LINEAR);
            }
            mBG.setTexture(texture);
        }

        {
            mFront.setCamera(camera);

            GLESVertexInfo vertexInfo = GLESMeshUtils.createPlane(mShader,
                    cubeSize, cubeSize,
                    false, false, true, false,
                    0.5f, 0.5f, 0.5f);
            mFront.setVertexInfo(vertexInfo, true, true);
        }

        {
            mLeft.setCamera(camera);

            GLESVertexInfo vertexInfo = GLESMeshUtils.createPlane(mShader,
                    cubeSize, cubeSize,
                    false, false, true, false,
                    1f, 1f, 1f);
            mLeft.setVertexInfo(vertexInfo, true, true);
        }

        {
            mRight.setCamera(camera);

            GLESVertexInfo vertexInfo = GLESMeshUtils.createPlane(mShader,
                    cubeSize, cubeSize,
                    false, false, true, false,
                    0.5f, 0.5f, 0.5f);
            mRight.setVertexInfo(vertexInfo, true, true);
        }

        {
            mBack.setCamera(camera);

            GLESVertexInfo vertexInfo = GLESMeshUtils.createPlane(mShader,
                    cubeSize, cubeSize,
                    false, false, true, false,
                    1f, 1f, 1f);
            mBack.setVertexInfo(vertexInfo, true, true);
        }

        {
            mTop.setCamera(camera);

            GLESVertexInfo vertexInfo = GLESMeshUtils.createPlane(mShader,
                    cubeSize, cubeSize,
                    false, false, true, false,
                    1f, 1f, 1f);
            mTop.setVertexInfo(vertexInfo, true, true);
        }

        {
            mBottom.setCamera(camera);

            GLESVertexInfo vertexInfo = GLESMeshUtils.createPlane(mShader,
                    cubeSize, cubeSize,
                    false, false, true, false,
                    0.5f, 0.5f, 0.5f);
            mBottom.setVertexInfo(vertexInfo, true, true);
        }
    }

    private GLESCamera setupCamera(int width, int height) {
        GLESCamera camera = new GLESCamera();

        float fovy = 30f;
        float eyeZ = 1f / (float) Math.tan(Math.toRadians(fovy * 0.5));

        camera.setLookAt(0f, 0f, eyeZ, 0f, 0f, 0f, 0f, 1f, 0f);

        camera.setFrustum(fovy, mScreenRatio, 1f, 400f);

        camera.setViewport(new GLESRect(0, 0, width, height));

        return camera;
    }

    @Override
    protected void onSurfaceCreated() {
        GLES20.glClearColor(0.7f, 0.7f, 0.7f, 0.0f);

        mFront.setShader(mShader);
        mLeft.setShader(mShader);
        mRight.setShader(mShader);
        mBack.setShader(mShader);
        mTop.setShader(mShader);
        mBottom.setShader(mShader);

        mBG.setShader(mBGShader);
    }

    @Override
    protected boolean createShader() {
        if (DEBUG) {
            Log.d(TAG, "createShader()");
        }

        {
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

                attribName = GLESShaderConstant.ATTRIB_COLOR;
                mShader.setColorAttribIndex(attribName);
            }
        }

        {
            mBGShader = new GLESShader(mContext);

            String vsSource = ShaderUtils.getShaderSource(mContext, 2);
            String fsSource = ShaderUtils.getShaderSource(mContext, 3);

            mBGShader.setShaderSource(vsSource, fsSource);
            if (mBGShader.load() == false) {
                mHandler.sendEmptyMessage(SampleRenderer.COMPILE_OR_LINK_ERROR);
                return false;
            }

            if (mVersion == Version.GLES_20) {
                String attribName = GLESShaderConstant.ATTRIB_POSITION;
                mBGShader.setPositionAttribIndex(attribName);

                attribName = GLESShaderConstant.ATTRIB_TEXCOORD;
                mBGShader.setTexCoordAttribIndex(attribName);
            }
        }

        return true;
    }

    public void touchDown(float x, float y) {
        if (DEBUG) {
            Log.d(TAG, "touchDown() x=" + x + " y=" + y);
        }

        mIsTouchDown = true;

        mDownX = x;
        mDownY = y;

        mView.requestRender();
    }

    public void touchUp(float x, float y) {
        if (mIsTouchDown == false) {
            return;
        }

        mView.requestRender();

        mIsTouchDown = false;
    }

    public void touchMove(float x, float y) {
        if (mIsTouchDown == false) {
            return;
        }

        mMoveX = x - mDownX;
        mMoveY = y - mDownY;

        mView.requestRender();
    }

    public void touchCancel(float x, float y) {
    }

    private GLESNodeListener mCubeNodeListener = new GLESNodeListener() {

        @Override
        public void update(GLESNode node) {
            GLESTransform transform = node.getTransform();

            transform.setIdentity();

            transform.setRotate(mMoveX * 0.2f + 45f, 0f, 1f, 0f);
            transform.rotate(mMoveY * 0.2f + 35f, 1f, 0f, 0f);
        }
    };

    private GLESObjectListener mFrontListener = new GLESObjectListener() {

        @Override
        public void update(GLESObject object) {
            GLESTransform transform = object.getTransform();

            transform.setIdentity();
            transform.setPreTranslate(0f, 0f, mPreTranslate);
        }

        @Override
        public void apply(GLESObject object) {
        }
    };

    private GLESObjectListener mLeftListener = new GLESObjectListener() {

        @Override
        public void update(GLESObject object) {
            GLESTransform transform = object.getTransform();

            transform.setIdentity();
            transform.setPreTranslate(0f, 0f, mPreTranslate);
            transform.setRotate(-90f, 0f, 1f, 0f);
        }

        @Override
        public void apply(GLESObject object) {
        }
    };

    private GLESObjectListener mRightListener = new GLESObjectListener() {

        @Override
        public void update(GLESObject object) {
            GLESTransform transform = object.getTransform();

            transform.setIdentity();
            transform.setPreTranslate(0f, 0f, mPreTranslate);
            transform.setRotate(90f, 0f, 1f, 0f);
        }

        @Override
        public void apply(GLESObject object) {
        }
    };

    private GLESObjectListener mTopListener = new GLESObjectListener() {

        @Override
        public void update(GLESObject object) {
            GLESTransform transform = object.getTransform();

            transform.setIdentity();
            transform.setPreTranslate(0f, 0f, mPreTranslate);
            transform.setRotate(-90f, 1f, 0f, 0f);
        }

        @Override
        public void apply(GLESObject object) {
        }
    };

    private GLESObjectListener mBackListener = new GLESObjectListener() {

        @Override
        public void update(GLESObject object) {
            GLESTransform transform = object.getTransform();

            transform.setIdentity();
            transform.setPreTranslate(0f, 0f, mPreTranslate);
            transform.setRotate(180f, 0f, 1f, 0f);
        }

        @Override
        public void apply(GLESObject object) {
        }
    };

    private GLESObjectListener mBottomListener = new GLESObjectListener() {

        @Override
        public void update(GLESObject object) {
            GLESTransform transform = object.getTransform();

            transform.setIdentity();
            transform.setPreTranslate(0f, 0f, mPreTranslate);
            transform.setRotate(90f, 1f, 0f, 0f);
        }

        @Override
        public void apply(GLESObject object) {
        }
    };

    private GLESObjectListener mBGListener = new GLESObjectListener() {

        @Override
        public void update(GLESObject object) {
            GLESTransform transform = object.getTransform();

            transform.setIdentity();
        }

        @Override
        public void apply(GLESObject object) {
        }
    };
}
