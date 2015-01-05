package com.gomdev.shader.instancedRendering2;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESConfig.Version;
import com.gomdev.gles.GLESContext;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESMeshUtils;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESObject;
import com.gomdev.gles.GLESObjectListener;
import com.gomdev.gles.GLESRect;
import com.gomdev.gles.GLESSceneManager;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESShaderConstant;
import com.gomdev.gles.GLESTransform;
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVector4;
import com.gomdev.gles.GLESVertexInfo;
import com.gomdev.gles.GLESVertexInfo.RenderType;
import com.gomdev.shader.SampleRenderer;
import com.gomdev.shader.ShaderUtils;

import java.nio.FloatBuffer;
import java.util.Random;

public class IR2Renderer extends SampleRenderer {
    private static final String CLASS = "IR2Renderer";
    private static final String TAG = IR2Config.TAG + "_" + CLASS;
    private static final boolean DEBUG = IR2Config.DEBUG;

    private final static int NUM_OF_INSTANCE = 1000;
    private final static int NUM_OF_ELEMENT = 4;

    private GLESSceneManager mSM = null;
    private GLESObject mObject = null;
    private GLESShader mShader = null;

    private Version mVersion;

    private boolean mIsTouchDown = false;

    private float mDownX = 0f;
    private float mDownY = 0f;

    private float mMoveX = 0f;
    private float mMoveY = 0f;

    private float mScreenRatio = 0f;

    private float[] mTranslateDatas = new float[NUM_OF_ELEMENT
            * NUM_OF_INSTANCE];
    private float[] mColorDatas = new float[NUM_OF_ELEMENT * NUM_OF_INSTANCE];
    private FloatBuffer mTranslateBuffer = null;
    private FloatBuffer mColorBuffer = null;
    private int mUniformBufferOffsetAlignment = 1;

    private Random mRandom = new Random();

    private int mNormalMatrixHandle = -1;
    private int mColorHandle = -1;

    private GLESVector4 mLightPos = new GLESVector4(1f, 1f, 1f, 0f);

    public IR2Renderer(Context context) {
        super(context);

        mVersion = GLESContext.getInstance().getVersion();

        mSM = GLESSceneManager.createSceneManager();
        GLESNode root = mSM.createRootNode("Root");

        mObject = mSM.createObject("Cube");

        GLESGLState state = new GLESGLState();
        state.setCullFaceState(true);
        state.setCullFace(GLES20.GL_BACK);
        state.setDepthState(true);
        state.setDepthFunc(GLES20.GL_LEQUAL);
        mObject.setGLState(state);

        mObject.setListener(mObjListener);

        root.addChild(mObject);
    }

    public void destroy() {
        mObject = null;
    }

    @Override
    protected void onDrawFrame() {
        super.updateFPS();

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (mVersion == Version.GLES_30) {
            GLESTransform transform = mObject.getTransform();
            transform.setIdentity();
            transform.setRotate(mMoveX * 0.2f, 0f, 1f, 0f);
            transform.rotate(mMoveY * 0.2f, 1f, 0f, 0f);

            mRenderer.updateScene(mSM);
            mRenderer.drawScene(mSM);
        } else {
            for (int i = 0; i < NUM_OF_INSTANCE; i++) {
                GLESTransform transform = mObject.getTransform();
                transform.setIdentity();
                transform.setTranslate(
                        mTranslateDatas[i * NUM_OF_ELEMENT + 0],
                        mTranslateDatas[i * NUM_OF_ELEMENT + 1],
                        mTranslateDatas[i * NUM_OF_ELEMENT + 2]);
                transform.setRotate(mMoveX * 0.2f, 0f, 1f, 0f);
                transform.rotate(mMoveY * 0.2f, 1f, 0f, 0f);

                GLES20.glUniform4f(mColorHandle,
                        mColorDatas[i * NUM_OF_ELEMENT + 0],
                        mColorDatas[i * NUM_OF_ELEMENT + 1],
                        mColorDatas[i * NUM_OF_ELEMENT + 2],
                        mColorDatas[i * NUM_OF_ELEMENT + 3]);

                mRenderer.updateScene(mSM);
                mRenderer.drawScene(mSM);
            }
        }
    }

    @Override
    protected void onSurfaceChanged(int width, int height) {
        mScreenRatio = (float) width / height;

        mRenderer.reset();

        GLESCamera camera = setupCamera(width, height);

        mObject.setCamera(camera);

        makeInstanceDataBuffer();

        if (mVersion == Version.GLES_20) {
            GLESVertexInfo vertexInfo = GLESMeshUtils.createCube(mShader,
                    0.1f, true, false, false);
            mObject.setVertexInfo(vertexInfo, true, true);
        } else {
            updateInstanceUniform();

            GLESVertexInfo vertexInfo = GLESMeshUtils.createCube(mShader,
                    0.1f, true, false, false);
            mObject.setVertexInfo(vertexInfo, true, true);

            vertexInfo.setRenderType(RenderType.DRAW_ELEMENTS_INSTANCED);
            vertexInfo.setNumOfInstance(NUM_OF_INSTANCE);
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

    private void makeInstanceDataBuffer() {
        for (int i = 0; i < NUM_OF_INSTANCE; i++) {
            mTranslateDatas[i * NUM_OF_ELEMENT + 0] = (mRandom.nextFloat() - 0.5f)
                    * mScreenRatio * 2f;
            mTranslateDatas[i * NUM_OF_ELEMENT + 1] = (mRandom.nextFloat() - 0.5f) * 2f;
            mTranslateDatas[i * NUM_OF_ELEMENT + 2] = (mRandom.nextFloat() - 0.5f);
            mTranslateDatas[i * NUM_OF_ELEMENT + 3] = 0f;
        }

        mTranslateBuffer = GLESUtils.makeFloatBuffer(mTranslateDatas);

        for (int i = 0; i < NUM_OF_INSTANCE; i++) {
            mColorDatas[i * NUM_OF_ELEMENT + 0] = mRandom.nextFloat();
            mColorDatas[i * NUM_OF_ELEMENT + 1] = mRandom.nextFloat();
            mColorDatas[i * NUM_OF_ELEMENT + 2] = mRandom.nextFloat();
            mColorDatas[i * NUM_OF_ELEMENT + 3] = 1f;
        }

        mColorBuffer = GLESUtils.makeFloatBuffer(mColorDatas);
    }

    private void updateInstanceUniform() {
        if (mVersion == Version.GLES_30) {
            // buffer object
            int uBufferID1 = -1;
            int uBufferID2 = -1;
            int[] uniformBufIDs = new int[2];
            GLES30.glGenBuffers(2, uniformBufIDs, 0);
            uBufferID1 = uniformBufIDs[0];
            uBufferID2 = uniformBufIDs[1];

            GLES30.glBindBuffer(GLES30.GL_UNIFORM_BUFFER, uBufferID1);
            GLES30.glBufferData(GLES30.GL_UNIFORM_BUFFER,
                    mTranslateBuffer.capacity() * 4,
                    mTranslateBuffer,
                    GLES30.GL_DYNAMIC_DRAW);

            GLES30.glBindBuffer(GLES30.GL_UNIFORM_BUFFER, uBufferID2);
            GLES30.glBufferData(GLES30.GL_UNIFORM_BUFFER,
                    mColorBuffer.capacity() * 4,
                    mColorBuffer,
                    GLES30.GL_DYNAMIC_DRAW);

            int program = mShader.getProgram();

            // translate
            GLES30.glBindBuffer(GLES30.GL_UNIFORM_BUFFER, uBufferID1);

            int bindingPoint1 = 1;
            int location = GLES30.glGetUniformBlockIndex(program,
                    "uTranslateBlock");
            GLES30.glUniformBlockBinding(program, location, bindingPoint1);

            int[] blockSizes = new int[1];
            GLES30.glGetActiveUniformBlockiv(program, location,
                    GLES30.GL_UNIFORM_BLOCK_DATA_SIZE, blockSizes, 0);
            int blockSize = blockSizes[0];

            GLES30.glBindBufferRange(GLES30.GL_UNIFORM_BUFFER, bindingPoint1,
                    uBufferID1, 0, blockSize);

            // color
            GLES30.glBindBuffer(GLES30.GL_UNIFORM_BUFFER, uBufferID2);

            int bindingPoint2 = 2;
            location = GLES30.glGetUniformBlockIndex(program, "uColorBlock");
            GLES30.glUniformBlockBinding(program, location, bindingPoint2);

            GLES30.glGetActiveUniformBlockiv(program, location,
                    GLES30.GL_UNIFORM_BLOCK_DATA_SIZE, blockSizes, 0);
            blockSize = blockSizes[0];

            GLES30.glBindBufferRange(GLES30.GL_UNIFORM_BUFFER, bindingPoint2,
                    uBufferID2, 0, blockSize);

            long[] sizes = new long[1];
            GLES30.glGetInteger64v(GLES30.GL_MAX_UNIFORM_BLOCK_SIZE, sizes, 0);
            if (DEBUG) {
                Log.d(TAG, "updateTransUniform() max uniform block size="
                        + sizes[0]);
            }
        }
    }

    @Override
    protected void onSurfaceCreated() {
        GLES20.glClearColor(0.7f, 0.7f, 0.7f, 0.0f);

        mObject.setShader(mShader);

        int program = mShader.getProgram();
        mNormalMatrixHandle = GLES20.glGetUniformLocation(program,
                "uNormalMatrix");
        int location = GLES20.glGetUniformLocation(program, "uLightPos");
        GLES20.glUniform4f(location,
                mLightPos.mX,
                mLightPos.mY,
                mLightPos.mZ,
                mLightPos.mW);

        location = GLES20.glGetUniformLocation(program, "uLightState");
        int[] lightState = new int[]{
                1,
                0,
                0,
                0,
                0,
                0,
                0,
                0
        };
        GLES20.glUniform1iv(location, 8, lightState, 0);

        if (mVersion == Version.GLES_20) {
            mColorHandle = GLES20.glGetUniformLocation(program, "uColor");
        }

        if (DEBUG) {
            int[] res = new int[1];
            GLES30.glGetIntegerv(GLES30.GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT,
                    res, 0);
            mUniformBufferOffsetAlignment = res[0];
            Log.d(TAG, "mUniformBufferOffsetAlignment="
                    + mUniformBufferOffsetAlignment);
        }
    }

    @Override
    protected boolean createShader() {
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

            attribName = GLESShaderConstant.ATTRIB_NORMAL;
            mShader.setNormalAttribIndex(attribName);
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

    GLESObjectListener mObjListener = new GLESObjectListener() {

        @Override
        public void update(GLESObject object) {
        }

        @Override
        public void apply(GLESObject object) {
            GLESShader shader = object.getShader();
            GLESTransform transform = object.getTransform();

            GLESCamera camera = object.getCamera();
            float[] vMatrix = camera.getViewMatrix();
            float[] mMatrix = transform.getMatrix();

            float[] vmMatrix = new float[16];
            Matrix.multiplyMM(vmMatrix, 0, vMatrix, 0, mMatrix, 0);
            float[] normalMatrix = new float[9];

            for (int i = 0; i < 3; i++) {
                normalMatrix[i * 3 + 0] = vmMatrix[i * 4 + 0];
                normalMatrix[i * 3 + 1] = vmMatrix[i * 4 + 1];
                normalMatrix[i * 3 + 2] = vmMatrix[i * 4 + 2];
            }

            shader.useProgram();

            GLES20.glUniformMatrix3fv(mNormalMatrixHandle, 1, false,
                    normalMatrix, 0);
        }
    };
}
