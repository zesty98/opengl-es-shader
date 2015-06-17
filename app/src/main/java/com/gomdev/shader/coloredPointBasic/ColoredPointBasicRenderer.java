package com.gomdev.shader.coloredPointBasic;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESConfig.Version;
import com.gomdev.gles.GLESContext;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESObject;
import com.gomdev.gles.GLESRect;
import com.gomdev.gles.GLESRenderer;
import com.gomdev.gles.GLESSceneManager;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESShaderConstant;
import com.gomdev.gles.GLESTransform;
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVertexInfo;
import com.gomdev.gles.GLESVertexInfo.PrimitiveMode;
import com.gomdev.gles.GLESVertexInfo.RenderType;
import com.gomdev.shader.SampleRenderer;
import com.gomdev.shader.ShaderUtils;

import java.util.Random;

public class ColoredPointBasicRenderer extends SampleRenderer {
    private static final String CLASS = "ColoredPointRenderer";
    private static final String TAG = ColoredPointBasicConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = ColoredPointBasicConfig.DEBUG;

    private static final int NUM_OF_PARTICLES = 1500;
    private static final int NUM_ELEMENT_OF_POSITION = 3;
    private static final int NUM_ELEMENT_OF_COLOR = 4;

    private GLESRenderer mRenderer = null;
    private GLESSceneManager mSM = null;
    private GLESObject mObject;
    private GLESShader mShader;
    private Version mVersion;
    private Random mRandom = new Random();

    private boolean mIsTouchDown = false;

    private float mDownX = 0f;
    private float mDownY = 0f;

    private float mMoveX = 0f;
    private float mMoveY = 0f;

    private float mScreenRatio = 0f;

    public ColoredPointBasicRenderer(Context context) {
        super(context);

        mVersion = GLESContext.getInstance().getVersion();

        mRenderer = GLESRenderer.createRenderer();
        mSM = GLESSceneManager.createSceneManager();
        GLESNode root = mSM.createRootNode("Root");

        mObject = mSM.createObject("BasicObject");

        GLESGLState state = new GLESGLState();
        state.setCullFaceState(true);
        state.setCullFace(GLES20.GL_BACK);
        state.setDepthState(true);
        state.setDepthFunc(GLES20.GL_LEQUAL);
        state.setBlendState(true);
        state.setBlendFuncSeperate(GLES20.GL_SRC_ALPHA,
                GLES20.GL_ONE_MINUS_SRC_ALPHA, GLES20.GL_ONE,
                GLES20.GL_ONE_MINUS_SRC_ALPHA);
        mObject.setGLState(state);

        root.addChild(mObject);
    }

    public void destroy() {
        mObject = null;
    }

    @Override
    protected void onDrawFrame() {
        super.updateFPS();

        update();

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        mRenderer.updateScene(mSM);
        mRenderer.drawScene(mSM);
    }

    private void update() {
        GLESTransform transform = mObject.getTransform();

        transform.setIdentity();

        transform.setRotate(mMoveX * 0.2f, 0f, 1f, 0f);
        transform.rotate(mMoveY * 0.2f, 1f, 0f, 0f);
    }

    @Override
    protected void onSurfaceChanged(int width, int height) {
        mRenderer.reset();

        mScreenRatio = (float) width / height;

        GLESCamera camera = setupCamera(width, height);

        mObject.setCamera(camera);

        GLESVertexInfo vertexInfo = createParticles(width, height);
        mObject.setVertexInfo(vertexInfo, false, false);

        float pointSize = GLESUtils.getPixelFromDpi(mContext, 10);
        int location = GLES20.glGetUniformLocation(mShader.getProgram(),
                "uPointSize");
        GLES20.glUniform1f(location, pointSize);
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

    private GLESVertexInfo createParticles(float width, float height) {
        GLESVertexInfo vertexInfo = new GLESVertexInfo();

        float[] position = new float[NUM_OF_PARTICLES * NUM_ELEMENT_OF_POSITION];
        float[] color = new float[NUM_OF_PARTICLES * NUM_ELEMENT_OF_COLOR];

        float posX = 0f;
        float posY = 0f;
        float posZ = 0f;

        float r = 0f;
        float g = 0f;
        float b = 0f;

        for (int i = 0; i < NUM_OF_PARTICLES; i++) {
            posX = (mRandom.nextFloat() * 2f - 1f) * mScreenRatio;
            posY = mRandom.nextFloat() * 2f - 1f;
            posZ = mRandom.nextFloat() * 0.01f;

            position[i * NUM_ELEMENT_OF_POSITION + 0] = posX;
            position[i * NUM_ELEMENT_OF_POSITION + 1] = posY;
            position[i * NUM_ELEMENT_OF_POSITION + 2] = posZ;

            r = mRandom.nextFloat();
            g = mRandom.nextFloat();
            b = mRandom.nextFloat();

            color[i * NUM_ELEMENT_OF_COLOR + 0] = r;
            color[i * NUM_ELEMENT_OF_COLOR + 1] = g;
            color[i * NUM_ELEMENT_OF_COLOR + 2] = b;
            color[i * NUM_ELEMENT_OF_COLOR + 3] = 1.0f;
        }
        vertexInfo.setBuffer(mShader.getPositionAttribIndex(), position,
                NUM_ELEMENT_OF_POSITION);

        vertexInfo.setBuffer(mShader.getColorAttribIndex(), color,
                NUM_ELEMENT_OF_COLOR);

        vertexInfo.setPrimitiveMode(PrimitiveMode.POINTS);
        vertexInfo.setRenderType(RenderType.DRAW_ARRAYS);

        return vertexInfo;
    }

    @Override
    protected void onSurfaceCreated() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        mObject.setShader(mShader);
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

            attribName = GLESShaderConstant.ATTRIB_COLOR;
            mShader.setColorAttribIndex(attribName);
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
