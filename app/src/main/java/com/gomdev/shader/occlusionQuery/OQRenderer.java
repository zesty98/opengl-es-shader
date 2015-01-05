package com.gomdev.shader.occlusionQuery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
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
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESTransform;
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVector4;
import com.gomdev.gles.GLESVertexInfo;
import com.gomdev.shader.SampleRenderer;
import com.gomdev.shader.ShaderUtils;

import java.util.Random;

public class OQRenderer extends SampleRenderer {
    private static final String CLASS = "OQRenderer";
    private static final String TAG = OQConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = OQConfig.DEBUG;

    private final static int NUM_OF_INSTANCE = 1000;
    private final static int NUM_OF_ELEMENT = 3;
    private static final int MAX_NUM_OF_FRAMES = 5;

    private Version mVersion;

    private GLESSceneManager mSM = null;

    private GLESObject mObject = null;
    private GLESShader mShader = null;

    private boolean mIsTouchDown = false;

    private float mDownX = 0f;
    private float mDownY = 0f;

    private float mMoveX = 0f;
    private float mMoveY = 0f;

    private float mScreenRatio = 0f;

    private float[] mTrans = new float[NUM_OF_ELEMENT * NUM_OF_INSTANCE];
    private float[] mScales = new float[NUM_OF_INSTANCE];
    private Random mRandom = new Random();

    private int mQueryID[] = new int[NUM_OF_INSTANCE];
    private int mNumOfPass[] = new int[NUM_OF_INSTANCE];

    private int mNumOfFrames = 0;
    private boolean mIsVisibilityChecked = false;

    private int mNormalMatrixHandle = -1;

    private GLESVector4 mLightPos = new GLESVector4(1f, 1f, 1f, 0f);

    public OQRenderer(Context context) {
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
            if (MAX_NUM_OF_FRAMES > mNumOfFrames) {
                mNumOfFrames++;
                GLES30.glGenQueries(NUM_OF_INSTANCE, mQueryID, 0);

                for (int i = 0; i < NUM_OF_INSTANCE; i++) {
                    GLES30.glBeginQuery(GLES30.GL_ANY_SAMPLES_PASSED,
                            mQueryID[i]);

                    drawObjects(i);

                    GLES30.glEndQuery(GLES30.GL_ANY_SAMPLES_PASSED);
                }
            } else {
                for (int i = 0; i < NUM_OF_INSTANCE; i++) {
                    if (mIsVisibilityChecked == false) {
                        GLES30.glGetQueryObjectuiv(mQueryID[i],
                                GLES30.GL_QUERY_RESULT, mNumOfPass, i);

                        if (mNumOfPass[i] == 0) {
                            if (DEBUG) {
                                Log.d(TAG, "\t i=" + i + " skipped");
                            }
                        }
                    }

                    if (mNumOfPass[i] > 0) {
                        drawObjects(i);
                    }
                }

                mIsVisibilityChecked = true;
            }
        } else {
            for (int i = 0; i < NUM_OF_INSTANCE; i++) {
                drawObjects(i);
            }
        }
    }

    private void drawObjects(int index) {
        GLESTransform transform = mObject.getTransform();
        transform.setIdentity();
        transform.setTranslate(
                mTrans[index * NUM_OF_ELEMENT + 0],
                mTrans[index * NUM_OF_ELEMENT + 1],
                mTrans[index * NUM_OF_ELEMENT + 2]);

        transform.setRotate(mMoveX * 0.2f, 0f, 1f, 0f);
        transform.rotate(mMoveY * 0.2f, 1f, 0f, 0f);

        transform.setScale(mScales[index]);

        mRenderer.updateScene(mSM);
        mRenderer.drawScene(mSM);
    }

    @Override
    protected void onSurfaceChanged(int width, int height) {
        mScreenRatio = (float) width / height;

        mRenderer.reset();

        GLESCamera camera = setupCamera(width, height);

        mObject.setCamera(camera);

        makeTransformInfo();

        GLESVertexInfo vertexInfo = GLESMeshUtils.createCube(mShader,
                0.1f, true, true, false);
        mObject.setVertexInfo(vertexInfo, true, true);
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

    private void makeTransformInfo() {
        for (int i = 0; i < NUM_OF_INSTANCE; i++) {
            mTrans[i * NUM_OF_ELEMENT + 0] = (mRandom.nextFloat() - 0.5f)
                    * mScreenRatio * 2f;
            mTrans[i * NUM_OF_ELEMENT + 1] = (mRandom.nextFloat() - 0.5f) * 2f;
            mTrans[i * NUM_OF_ELEMENT + 2] = (mRandom.nextFloat() - 0.5f) * 4f;

            mScales[i] = 1f;// (mRandom.nextFloat() + 0.1f) * 6f;
        }
    }

    @Override
    protected void onSurfaceCreated() {
        GLES20.glClearColor(0.7f, 0.7f, 0.7f, 0.0f);

        mObject.setShader(mShader);

        Bitmap bitmap = GLESUtils.makeBitmap(512, 512, Config.ARGB_8888,
                Color.GREEN);
        GLESTexture.Builder builder = new GLESTexture.Builder(
                GLES20.GL_TEXTURE_2D, bitmap.getWidth(), bitmap.getHeight());
        GLESTexture texture = builder.load(bitmap);
        mObject.setTexture(texture);

        mIsVisibilityChecked = false;
        mNumOfFrames = 0;

        int program = mShader.getProgram();
        mNormalMatrixHandle = GLES20.glGetUniformLocation(program,
                "uNormalMatrix");

        int location = GLES20.glGetUniformLocation(program,
                "uLightPos");
        GLES20.glUniform4f(location,
                mLightPos.mX,
                mLightPos.mY,
                mLightPos.mZ,
                mLightPos.mW);
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
