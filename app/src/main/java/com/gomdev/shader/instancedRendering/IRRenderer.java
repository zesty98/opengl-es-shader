package com.gomdev.shader.instancedRendering;

import android.annotation.TargetApi;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.Build;
import android.util.Log;

import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESConfig;
import com.gomdev.gles.GLESConfig.Version;
import com.gomdev.gles.GLESContext;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESMeshUtils;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESObject;
import com.gomdev.gles.GLESObjectListener;
import com.gomdev.gles.GLESRect;
import com.gomdev.gles.GLESRendererListener;
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

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class IRRenderer extends SampleRenderer implements GLESRendererListener {
    private static final String CLASS = "IRRenderer";
    private static final String TAG = IRConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = IRConfig.DEBUG;

    private final static int NUM_OF_INSTANCE = 1000;
    private final static int NUM_OF_ELEMENT = 3;

    private final static int USER_ATTRIB_LOCATION = 4;

    private GLESSceneManager mSM;

    private GLESObject mObject;
    private GLESShader mShader;

    private Version mVersion;

    private boolean mIsTouchDown = false;

    private float mDownX = 0f;
    private float mDownY = 0f;

    private float mMoveX = 0f;
    private float mMoveY = 0f;

    private float mScreenRatio = 0f;

    private float[] mTrans = new float[NUM_OF_ELEMENT * NUM_OF_INSTANCE];
    private FloatBuffer mTransBuffer = null;
    private Random mRandom = new Random();
    private int mVBOID = -1;

    private int mNormalMatrixHandle = -1;

    private GLESVector4 mLightPos = new GLESVector4(1f, 1f, 1f, 0f);

    public IRRenderer(Context context) {
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

        mRenderer.setListener(this);
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
                        mTrans[i * NUM_OF_ELEMENT + 0],
                        mTrans[i * NUM_OF_ELEMENT + 1],
                        mTrans[i * NUM_OF_ELEMENT + 2]);
                transform.setRotate(mMoveX * 0.2f, 0f, 1f, 0f);
                transform.rotate(mMoveY * 0.2f, 1f, 0f, 0f);

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

        makeTransBuffer();

        GLESVertexInfo vertexInfo = GLESMeshUtils.createCube(mShader,
                0.1f, true, false, true);
        mObject.setVertexInfo(vertexInfo, true, true);

        if (mVersion == Version.GLES_30) {
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

    private void makeTransBuffer() {
        for (int i = 0; i < NUM_OF_INSTANCE; i++) {
            mTrans[i * NUM_OF_ELEMENT + 0] = (mRandom.nextFloat() - 0.5f)
                    * mScreenRatio * 2f;
            mTrans[i * NUM_OF_ELEMENT + 1] = (mRandom.nextFloat() - 0.5f) * 2f;
            mTrans[i * NUM_OF_ELEMENT + 2] = (mRandom.nextFloat() - 0.5f);
        }

        mTransBuffer = GLESUtils.makeFloatBuffer(mTrans);
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
                mLightPos.getX(),
                mLightPos.getY(),
                mLightPos.getZ(),
                mLightPos.getW());
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

    @Override
    public void setupVBO(GLESShader shader, GLESVertexInfo vertexInfo) {
        if (mVersion == Version.GLES_20) {
            return;
        }

        int[] vboIDs = new int[1];
        GLES30.glGenBuffers(1, vboIDs, 0);
        mVBOID = vboIDs[0];
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboIDs[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, mTransBuffer.capacity()
                        * GLESConfig.FLOAT_SIZE_BYTES,
                mTransBuffer, GLES30.GL_STATIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
    }

    @Override
    public void setupVAO(GLESShader shader, GLESVertexInfo vertexInfo) {
        if (mVersion == Version.GLES_20) {
            return;
        }

        GLES30.glEnableVertexAttribArray(USER_ATTRIB_LOCATION);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,
                mVBOID);
        GLES30.glVertexAttribPointer(USER_ATTRIB_LOCATION,
                NUM_OF_ELEMENT, GLES30.GL_FLOAT, false,
                NUM_OF_ELEMENT * GLESConfig.FLOAT_SIZE_BYTES,
                0);


        GLES30.glVertexAttribDivisor(USER_ATTRIB_LOCATION, 1);
    }

    @Override
    public void enableVertexAttribute(GLESObject object) {
        if (mVersion == Version.GLES_20) {
            return;
        }

        boolean useVBO = object.useVBO();

        if (useVBO == true) {
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVBOID);

            GLES30.glVertexAttribPointer(USER_ATTRIB_LOCATION,
                    NUM_OF_ELEMENT, GLES30.GL_FLOAT, false,
                    NUM_OF_ELEMENT * GLESConfig.FLOAT_SIZE_BYTES,
                    0);
            GLES30.glEnableVertexAttribArray(USER_ATTRIB_LOCATION);
        } else {
            GLES30.glVertexAttribPointer(USER_ATTRIB_LOCATION,
                    NUM_OF_ELEMENT, GLES30.GL_FLOAT, false,
                    NUM_OF_ELEMENT * GLESConfig.FLOAT_SIZE_BYTES,
                    mTransBuffer);
            GLES30.glEnableVertexAttribArray(USER_ATTRIB_LOCATION);
        }
    }

    @Override
    public void disableVertexAttribute(GLESObject object) {
        if (mVersion == Version.GLES_20) {
            return;
        }

        if (object.useVAO() == true) {
            GLES30.glBindVertexArray(0);
            return;
        }

        GLES30.glDisableVertexAttribArray(USER_ATTRIB_LOCATION);
    }

    GLESObjectListener mObjListener = new GLESObjectListener() {

        @Override
        public void update(GLESObject object) {
            // TODO Auto-generated method stub

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
