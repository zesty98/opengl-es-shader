package com.gomdev.shader.multiLighting;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.Matrix;

import com.gomdev.gles.GLESAnimator;
import com.gomdev.gles.GLESAnimatorCallback;
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
import com.gomdev.gles.GLESTransform;
import com.gomdev.gles.GLESVector3;
import com.gomdev.gles.GLESVector4;
import com.gomdev.gles.GLESVertexInfo;
import com.gomdev.shader.SampleRenderer;
import com.gomdev.shader.ShaderUtils;

public class MultiLightingRenderer extends SampleRenderer {
    static final String CLASS = "PVLRenderer";
    static final String TAG = MultiLightingConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = MultiLightingConfig.DEBUG;

    private Version mVersion;

    private GLESSceneManager mSM = null;
    private GLESShader mShader = null;

    private GLESObject mCube = null;
    private GLESObject mLight1 = null;
    private GLESObject mLight2 = null;
    private GLESNode mNode = null;

    private boolean mIsTouchDown = false;

    private float mDownX = 0f;
    private float mDownY = 0f;

    private float mMoveX = 0f;
    private float mMoveY = 0f;

    private float mScreenRatio = 0f;

    private int mNormalMatrixHandle = -1;

    private int mLightPosHandle = -1;
    private int mLightStateHandle = -1;
    private int mLightPos1Handle = -1;
    private int mLightPos2Handle = -1;

    private float mLight1Radius = 0f;
    private float mLight2Radius = 0f;

    private GLESVector4 mLight1Pos = new GLESVector4();
    private GLESVector4 mLight2Pos = new GLESVector4();

    private float mDegree = 0f;

    public MultiLightingRenderer(Context context) {
        super(context);

        mVersion = GLESContext.getInstance().getVersion();

        mSM = GLESSceneManager.createSceneManager();
        GLESNode root = mSM.createRootNode("Root");

        GLESGLState state = new GLESGLState();
        state.setCullFaceState(true);
        state.setCullFace(GLES20.GL_BACK);
        state.setDepthState(true);
        state.setDepthFunc(GLES20.GL_LEQUAL);

        {
            mCube = mSM.createObject("Cube");
            mCube.setGLState(state);
            mCube.setListener(mCubeListener);

            root.addChild(mCube);
        }

        {
            mNode = mSM.createNode("Node");
            mNode.setListener(mNodeListener);

            root.addChild(mNode);

            {
                mLight1 = mSM.createObject("Light");
                mLight1.setGLState(state);
                mLight1.setListener(mLightListener);

                mNode.addChild(mLight1);
            }

            {
                mLight2 = mSM.createObject("Light2");
                mLight2.setGLState(state);
                mLight2.setListener(mLight2Listener);

                mNode.addChild(mLight2);
            }
        }

        mAnimator.setDuration(0, 10000);
        mAnimator.setRepeat(true);
    }

    public void destroy() {
        mCube = null;
        mLight1 = null;
        mLight2 = null;
    }

    @Override
    protected void onDrawFrame() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        super.updateFPS();

        if (mAnimator.doAnimation() == true) {
            mView.requestRender();
        }

        mRenderer.updateScene(mSM);
        mRenderer.drawScene(mSM);
    }

    @Override
    protected void onSurfaceChanged(int width, int height) {
        mScreenRatio = (float) width / height;
        mLight1Radius = mScreenRatio;
        mLight2Radius = mScreenRatio * 0.3f;

        mRenderer.reset();

        GLESCamera camera = setupCamera(width, height);

        {
            mCube.setCamera(camera);

            GLESVertexInfo vertexInfo = GLESMeshUtils.createCube(mShader,
                    mScreenRatio * 0.5f, true, false, true);
            mCube.setVertexInfo(vertexInfo, true, true);
        }

        {
            mLight1.setCamera(camera);

            GLESVertexInfo vertexInfo = GLESMeshUtils.createSphere(mShader,
                    0.1f, 10, 10,
                    false, true, true,
                    1f, 0f, 0f, 1f);
            mLight1.setVertexInfo(vertexInfo, true, false);
        }

        {
            mLight2.setCamera(camera);

            GLESVertexInfo vertexInfo = GLESMeshUtils.createSphere(mShader,
                    0.04f, 10, 10,
                    false, true, true,
                    0, 1f, 0f, 1f);
            mLight2.setVertexInfo(vertexInfo, true, false);
        }

        mAnimator.start(0f, (float) (Math.PI * 2f));
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

        mShader.useProgram();

        mCube.setShader(mShader);
        mLight1.setShader(mShader);
        mLight2.setShader(mShader);

        int program = mShader.getProgram();
        mNormalMatrixHandle = GLES20.glGetUniformLocation(program,
                "uNormalMatrix");

        mLightStateHandle = GLES20.glGetUniformLocation(program, "uLightState");
        if (mVersion == Version.GLES_20) {
            mLightPosHandle = GLES20.glGetUniformLocation(program, "uLightPos");

        } else {
            mLightPos1Handle = GLES20.glGetUniformLocation(program,
                    "uLightPos");
            mLightPos2Handle = GLES20.glGetUniformLocation(program,
                    "uLight2Pos");
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

            attribName = GLESShaderConstant.ATTRIB_COLOR;
            mShader.setColorAttribIndex(attribName);
        }

        return true;
    }

    public void touchDown(float x, float y) {
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

    private GLESAnimator mAnimator = new GLESAnimator(
            new GLESAnimatorCallback() {

                @Override
                public void onFinished() {

                }

                @Override
                public void onCancel() {

                }

                @Override
                public void onAnimation(GLESVector3 vector) {
                    mDegree = (float) Math.toDegrees(vector.getX());

                    float x = (float) Math.cos(vector.getX()) * mLight1Radius;
                    float y = (float) Math.sin(vector.getX()) * mLight1Radius;
                    float z = 0f;
                    float w = 1f;

                    mLight1Pos.set(x, y, z, w);

                    x = (float) Math.cos(vector.getX() * 2f) * mLight2Radius + x;
                    y = (float) Math.sin(vector.getX() * 2f) * mLight2Radius + y;
                    z = 0f;
                    w = 1f;

                    mLight2Pos.set(x, y, z, w);
                }
            });

    private GLESObjectListener mCubeListener = new GLESObjectListener() {

        @Override
        public void update(GLESObject object) {
            GLESTransform transform = object.getTransform();

            transform.setIdentity();
            transform.setRotate(mMoveX * 0.2f, 0f, 1f, 0f);
            transform.rotate(mMoveY * 0.2f, 1f, 0f, 0f);

        }

        @Override
        public void apply(GLESObject object) {
            GLESShader shader = object.getShader();
            GLESTransform transform = object.getWorldTransform();

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

            if (mVersion == Version.GLES_20) {
                float[] lightPos = new float[]{
                        mLight1Pos.getX(),
                        mLight1Pos.getY(),
                        mLight1Pos.getZ(),
                        mLight1Pos.getW(),
                        mLight2Pos.getX(),
                        mLight2Pos.getY(),
                        mLight2Pos.getZ(),
                        mLight2Pos.getW(),
                };
                GLES20.glUniform4fv(mLightPosHandle, 2, lightPos, 0);

                int[] lightState = new int[]{
                        1,
                        1,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0
                };
                GLES20.glUniform1iv(mLightStateHandle, 8, lightState, 0);
            } else {
                GLES30.glUniform4f(mLightPos1Handle, mLight1Pos.getX(),
                        mLight1Pos.getY(), mLight1Pos.getZ(), mLight1Pos.getW());
                GLES30.glUniform4f(mLightPos2Handle, mLight2Pos.getX(),
                        mLight2Pos.getY(), mLight2Pos.getZ(), mLight2Pos.getW());

                int[] lightState = new int[]{
                        1,
                        1
                };
                GLES30.glUniform1iv(mLightStateHandle, 2, lightState, 0);

            }
        }
    };

    GLESObjectListener mLightListener = new GLESObjectListener() {

        @Override
        public void update(GLESObject object) {
        }

        @Override
        public void apply(GLESObject object) {
            GLESShader shader = object.getShader();
            GLESTransform transform = object.getWorldTransform();
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

            if (mVersion == Version.GLES_20) {
                float[] lightPos = new float[]{
                        mLight1Pos.getX(),
                        mLight1Pos.getY(),
                        mLight1Pos.getZ(),
                        mLight1Pos.getW(),
                        mLight2Pos.getX(),
                        mLight2Pos.getY(),
                        mLight2Pos.getZ(),
                        mLight2Pos.getW(),
                };
                GLES20.glUniform4fv(mLightPosHandle, 2, lightPos, 0);

                int[] lightState = new int[]{
                        0,
                        1,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0
                };
                GLES20.glUniform1iv(mLightStateHandle, 8, lightState, 0);
            } else {
                // GLES30.glUniform4f(mLightPos1Handle, mLightPos.getX(),
                // mLightPos.getY(), mLightPos.getZ(), mLightPos.getW());
                GLES30.glUniform4f(mLightPos2Handle, mLight2Pos.getX(),
                        mLight2Pos.getY(), mLight2Pos.getZ(), mLight2Pos.getW());

                int[] lightState = new int[]{
                        0,
                        1
                };
                GLES30.glUniform1iv(mLightStateHandle, 2, lightState, 0);
            }
        }
    };

    GLESObjectListener mLight2Listener = new GLESObjectListener() {

        @Override
        public void update(GLESObject object) {
            GLESTransform transform = object.getTransform();

            transform.setIdentity();
            transform.setPreTranslate(mLight2Radius, 0f, 0f);
            transform.setRotate(mDegree, 0f, 0f, 1f);

        }

        @Override
        public void apply(GLESObject object) {
            GLESShader shader = object.getShader();
            GLESTransform transform = object.getWorldTransform();
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

            if (mVersion == Version.GLES_20) {
                float[] lightPos = new float[]{
                        mLight1Pos.getX(),
                        mLight1Pos.getY(),
                        mLight1Pos.getZ(),
                        mLight1Pos.getW(),
                        mLight2Pos.getX(),
                        mLight2Pos.getY(),
                        mLight2Pos.getZ(),
                        mLight2Pos.getW()
                };
                GLES20.glUniform4fv(mLightPosHandle, 2, lightPos, 0);

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

                GLES20.glUniform1iv(mLightStateHandle, 8, lightState, 0);
            } else {
                GLES30.glUniform4f(mLightPos1Handle, mLight1Pos.getX(),
                        mLight1Pos.getY(), mLight1Pos.getZ(), mLight1Pos.getW());
                // GLES30.glUniform4f(mLightPos2Handle, mLight2Pos.getX(),
                // mLight2Pos.getY(), mLight2Pos.getZ(), mLight2Pos.getW());

                int[] lightState = new int[]{
                        1,
                        0
                };
                GLES30.glUniform1iv(mLightStateHandle, 2, lightState, 0);
            }
        }
    };

    private GLESNodeListener mNodeListener = new GLESNodeListener() {

        @Override
        public void update(GLESNode node) {
            GLESTransform transform = node.getTransform();

            transform.setIdentity();
            transform.setRotate(mDegree, 0f, 0f, 1f);
            transform.setPreTranslate(mLight1Radius, 0f, 0f);
        }
    };
}