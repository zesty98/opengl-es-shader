package com.gomdev.shader.galleryIcon;

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
import com.gomdev.gles.GLESObject;
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

public class GalleryIconRenderer extends SampleRenderer {
    private static final String CLASS = "GalleryIconRenderer";
    private static final String TAG = GalleryIconConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = GalleryIconConfig.DEBUG;

    private GLESSceneManager mSM = null;

    private GLESNode mBGNode = null;
    private GLESNode mIconNode = null;

    private GLESObject mFirst = null;
    private GLESObject mSecond = null;
    private GLESObject mThird = null;

    private GLESObject mMountain = null;
    private GLESObject mSun = null;

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

    public GalleryIconRenderer(Context context) {
        super(context);

        if (DEBUG) {
            Log.d(TAG, "GalleryIconRenderer()");
        }

        mVersion = GLESContext.getInstance().getVersion();

        mSM = GLESSceneManager.createSceneManager();
        GLESNode root = mSM.createRootNode("Root");

        {
            mBGNode = mSM.createNode("BGNode");
            root.addChild(mBGNode);

            {
                mBG = mSM.createObject("BG");

                GLESGLState state = new GLESGLState();
                state.setCullFaceState(true);
                state.setCullFace(GLES20.GL_BACK);
                state.setDepthState(false);
                mBG.setGLState(state);
            }

            mBGNode.addChild(mBG);
        }

        {
            mIconNode = mSM.createNode("CubeNode");
            root.addChild(mIconNode);

            {
                mThird = mSM.createObject("Third");

                GLESGLState state = new GLESGLState();
                state.setCullFaceState(true);
                state.setCullFace(GLES20.GL_BACK);
                state.setDepthState(false);
                mThird.setGLState(state);

                mIconNode.addChild(mThird);
            }

            {
                mSecond = mSM.createObject("Second");

                GLESGLState state = new GLESGLState();
                state.setCullFaceState(true);
                state.setCullFace(GLES20.GL_BACK);
                state.setDepthState(false);
                mSecond.setGLState(state);

                mIconNode.addChild(mSecond);
            }

            {
                mFirst = mSM.createObject("First");

                GLESGLState state = new GLESGLState();
                state.setCullFaceState(true);
                state.setCullFace(GLES20.GL_BACK);
                state.setDepthState(false);
                mFirst.setGLState(state);

                mIconNode.addChild(mFirst);
            }

            {
                mMountain = mSM.createObject("Mountain");

                GLESGLState state = new GLESGLState();
                state.setCullFaceState(true);
                state.setCullFace(GLES20.GL_BACK);
                state.setDepthState(false);
                mMountain.setGLState(state);

                mIconNode.addChild(mMountain);
            }

            {
                mSun = mSM.createObject("Sun");

                GLESGLState state = new GLESGLState();
                state.setCullFaceState(true);
                state.setCullFace(GLES20.GL_BACK);
                state.setDepthState(false);
                mSun.setGLState(state);

                mIconNode.addChild(mSun);
            }
        }
    }

    public void destroy() {
        mBG = null;

        mFirst = null;
        mSecond = null;
        mThird = null;
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

        mScreenRatio = (float) width / height;

        mRenderer.reset();

        GLESCamera camera = setupCamera(width, height);

        {
            mBG.setCamera(camera);

            GLESVertexInfo vertexInfo = GLESMeshUtils.createPlane(mBGShader,
                    width, height,
                    false, true, false, false);
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

        float imageWidth = width * 0.4f;
        float imageHeight = width * 0.4f;

        float translate = width * 0.02f;

        {
            mFirst.setCamera(camera);

            GLESVertexInfo vertexInfo = GLESMeshUtils.createPlane(mShader,
                    imageWidth, imageHeight,
                    false, false, true, false,
                    1f, 1f, 1f);
            mFirst.setVertexInfo(vertexInfo, true, true);

            GLESTransform transform = mFirst.getTransform();
            transform.setIdentity();
            transform.setTranslate(0f, 0f, 0f);
        }

        {
            mSecond.setCamera(camera);

            GLESVertexInfo vertexInfo = GLESMeshUtils.createPlane(mShader,
                    imageWidth, imageHeight,
                    false, false, true, false,
                    0.8f, 0.8f, 0.8f);
            mSecond.setVertexInfo(vertexInfo, true, true);

            GLESTransform transform = mSecond.getTransform();
            transform.setIdentity();
            transform.setTranslate(translate, -translate, 0f);
        }

        {
            mThird.setCamera(camera);

            GLESVertexInfo vertexInfo = GLESMeshUtils.createPlane(mShader,
                    imageWidth, imageHeight,
                    false, false, true, false,
                    0.6f, 0.6f, 0.6f);
            mThird.setVertexInfo(vertexInfo, true, true);

            GLESTransform transform = mThird.getTransform();
            transform.setIdentity();
            transform.setTranslate(translate * 2f, -translate * 2f, 0f);
        }

        {
            float red = 0.4f;
            float green = 0.4f;
            float blue = 0.4f;

            mMountain.setCamera(camera);

            GLESVertexInfo vertexInfo = createMountain(imageWidth, imageHeight,
                    red, green, blue);
            mMountain.setVertexInfo(vertexInfo, true, true);
        }

        {
            mSun.setCamera(camera);

            float red = 0.890f;
            float green = 0.713f;
            float blue = 0.255f;
            float alpha = 1f;

            GLESVertexInfo vertexInfo = createSun(imageWidth * 0.25f, imageHeight * 0.22f, imageWidth * 0.13f,
                    red, green, blue, alpha);
            mSun.setVertexInfo(vertexInfo, true, true);
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

    private GLESVertexInfo createMountain(float width, float height, float red, float green, float blue) {
        float right = width * 0.5f;
        float left = -right;
        float top = height * 0.5f;
        float bottom = -top;
        float z = 0;

        float offset = width * 0.02f;

        float[] vertex = {
                left, bottom * 0.66f, z,
                left, bottom, z,
                left * 0.2f, top * 0.33f, z,
                left * 0.2f, bottom, z,
                right * 0.5f, bottom * 0.33f, z,
                right * 0.5f, bottom, z,
                right, 0f, z,
                right, bottom, z,

//                right, bottom, z,
//                right, 0f - offset, z,
//                right * 0.5f, bottom, z,
//                right * 0.5f, bottom * 0.33f - offset, z,
//                left * 0.2f, bottom, z,
//                left * 0.2f, top * 0.33f - offset, z,
//                left, bottom, z,
//                left, bottom * 0.66f - offset, z,
        };

        GLESVertexInfo vertexInfo = new GLESVertexInfo();

        vertexInfo.setBuffer(mShader.getPositionAttribIndex(), vertex, 3);


//        float red = 0.321f;
//        float green = 0.380f;
//        float blue = 0.901f;

//        float red = 0.282f;
//        float green = 0.553f;
//        float blue = 0.839f;

        float[] color = {
                red, green, blue, 1f,
                red, green, blue, 1f,
                red, green, blue, 1f,
                red, green, blue, 1f,
                red, green, blue, 1f,
                red, green, blue, 1f,
                red, green, blue, 1f,
                red, green, blue, 1f,

//                1f, 1f, 1f, 1f,
//                1f, 1f, 1f, 1f,
//                1f, 1f, 1f, 1f,
//                1f, 1f, 1f, 1f,
//                1f, 1f, 1f, 1f,
//                1f, 1f, 1f, 1f,
//                1f, 1f, 1f, 1f,
//                1f, 1f, 1f, 1f,
        };

        vertexInfo.setBuffer(mShader.getColorAttribIndex(), color, 4);


        vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);
        vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.TRIANGLE_STRIP);

        return vertexInfo;
    }

    private GLESVertexInfo createSun(float centerX, float centerY, float radius,
                                     float red, float green, float blue, float alpha) {
        int numOfTriangle = 30;
        int numOfVertex = numOfTriangle + 2;
        int numOfPositionElements = numOfVertex * 3;

        double angleUnit = 2 * Math.PI / numOfTriangle;

        float[] positions = new float[numOfPositionElements];

        positions[0] = centerX;
        positions[1] = centerY;
        positions[2] = 0f;

        for (int i = 1, j = 0; i < numOfVertex; i++, j++) {
            positions[i * 3 + 0] = centerX + (float) Math.cos(j * angleUnit) * radius;
            positions[i * 3 + 1] = centerY + (float) Math.sin(j * angleUnit) * radius;
            positions[i * 3 + 2] = 0f;
        }

        GLESVertexInfo vertexInfo = new GLESVertexInfo();

        vertexInfo.setBuffer(mShader.getPositionAttribIndex(), positions, 3);

        float[] color = new float[numOfVertex * 4];

        for (int i = 0; i < numOfVertex; i++) {
            color[i * 4 + 0] = red;
            color[i * 4 + 1] = green;
            color[i * 4 + 2] = blue;
            color[i * 4 + 3] = alpha;
        }

        vertexInfo.setBuffer(mShader.getColorAttribIndex(), color, 4);

        vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);
        vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.TRIANGLE_FAN);

        return vertexInfo;
    }

    @Override
    protected void onSurfaceCreated() {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCreated()");
        }

        GLES20.glClearColor(0.7f, 0.7f, 0.7f, 0.0f);

        mBG.setShader(mBGShader);
        mFirst.setShader(mShader);
        mSecond.setShader(mShader);
        mThird.setShader(mShader);
        mMountain.setShader(mShader);
        mSun.setShader(mShader);

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
}
