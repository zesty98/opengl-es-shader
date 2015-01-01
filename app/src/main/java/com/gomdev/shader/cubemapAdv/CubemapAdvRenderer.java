package com.gomdev.shader.cubemapAdv;

import com.gomdev.gles.*;
import com.gomdev.gles.GLESConfig.Version;
import com.gomdev.shader.R;
import com.gomdev.shader.SampleRenderer;
import com.gomdev.shader.ShaderUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

public class CubemapAdvRenderer extends SampleRenderer {
    private static final String CLASS = "CubemapAdvRenderer";
    private static final String TAG = CubemapAdvConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = CubemapAdvConfig.DEBUG;

    private GLESSceneManager mSM = null;

    private GLESObject mObject = null;
    private GLESShader mShader = null;

    private int mNormalMatrixHandle = -1;

    private Version mVersion;

    private boolean mIsTouchDown = false;

    private float mDownX = 0f;
    private float mDownY = 0f;

    private float mMoveX = 0f;
    private float mMoveY = 0f;

    private float mScreenRatio = 0f;

    private int[] mResIDs = new int[] {
            R.drawable.x_pos,
            R.drawable.x_neg,
            R.drawable.y_pos,
            R.drawable.y_neg,
            R.drawable.z_pos,
            R.drawable.z_neg
    };

    public CubemapAdvRenderer(Context context) {
        super(context);

        mVersion = GLESContext.getInstance().getVersion();

        mSM = GLESSceneManager.createSceneManager();
        GLESNode root = mSM.createRootNode("Root");

        mObject = mSM.createObject("TextureObject");

        GLESGLState state = new GLESGLState();
        state.setCullFaceState(true);
        state.setCullFace(GLES20.GL_BACK);
        state.setDepthState(true);
        state.setDepthFunc(GLES20.GL_LEQUAL);
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

        GLESCamera camera = mObject.getCamera();

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

        GLES20.glUniformMatrix3fv(mNormalMatrixHandle, 1, false,
                normalMatrix, 0);
    }

    @Override
    protected void onSurfaceChanged(int width, int height) {
        mRenderer.reset();

        mScreenRatio = (float) width / height;

        GLESCamera camera = setupCamera(width, height);

        mObject.setCamera(camera);

        float size = mScreenRatio;
        GLESVertexInfo vertexInfo = GLESMeshUtils.createPlane(mShader,
                size, size, true, false, false, false);

        mObject.setVertexInfo(vertexInfo, true, true);

        Bitmap[] bitmaps = new Bitmap[mResIDs.length];
        for (int i = 0; i < mResIDs.length; i++) {
            bitmaps[i] = BitmapFactory.decodeResource(mContext.getResources(),
                    mResIDs[i]);
        }
        int imgWidth = bitmaps[0].getWidth();
        int imgHeight = bitmaps[0].getHeight();
        GLESTexture.Builder builder = new GLESTexture.Builder(
                GLES20.GL_TEXTURE_CUBE_MAP, imgWidth, imgHeight);
        GLESTexture texture = builder.load(bitmaps);
        mObject.setTexture(texture);
    }

    private GLESCamera setupCamera(int width, int height) {
        GLESCamera camera = new GLESCamera();

        float fovy = 60f;
        float eyeZ = 1f / (float) Math.tan(Math.toRadians(fovy * 0.5));

        camera.setLookAt(0f, 0f, eyeZ, 0f, 0f, 0f, 0f, 1f, 0f);

        camera.setFrustum(fovy, mScreenRatio, 1f, 400f);

        camera.setViewport(new GLESRect(0, 0, width, height));

        int location = GLES20.glGetUniformLocation(mShader.getProgram(),
                "uEyePos");
        GLES20.glUniform4f(location, 0f, 0f, eyeZ, 1f);

        return camera;
    }

    @Override
    protected void onSurfaceCreated() {
        GLES20.glClearColor(0.7f, 0.7f, 0.7f, 0.0f);

        mObject.setShader(mShader);

        mNormalMatrixHandle = GLES20.glGetUniformLocation(mShader.getProgram(),
                "uNormalMatrix");
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
}
