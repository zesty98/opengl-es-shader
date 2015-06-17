package com.gomdev.shader.cubemapBasic;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.util.Log;

import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESConfig.Version;
import com.gomdev.gles.GLESContext;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESMeshUtils;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESObject;
import com.gomdev.gles.GLESRect;
import com.gomdev.gles.GLESRenderer;
import com.gomdev.gles.GLESSceneManager;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESShaderConstant;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESTransform;
import com.gomdev.gles.GLESVertexInfo;
import com.gomdev.shader.R;
import com.gomdev.shader.SampleRenderer;
import com.gomdev.shader.ShaderUtils;

public class CubemapBasicRenderer extends SampleRenderer {
    private static final String CLASS = "CubemapBasicRenderer";
    private static final String TAG = CubemapBasicConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = CubemapBasicConfig.DEBUG;

    private GLESRenderer mRenderer = null;
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

    private int[] mResIDs = new int[]{
            R.drawable.x_pos,
            R.drawable.x_neg,
            R.drawable.y_pos,
            R.drawable.y_neg,
            R.drawable.z_pos,
            R.drawable.z_neg
    };

    public CubemapBasicRenderer(Context context) {
        super(context);

        mVersion = GLESContext.getInstance().getVersion();

        mRenderer = GLESRenderer.createRenderer();
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
    }

    private GLESVertexInfo mSphereVertexInfo = null;
    private GLESVertexInfo mCubeVertexInfo = null;

    @Override
    protected void onSurfaceChanged(int width, int height) {
        mRenderer.reset();

        mScreenRatio = (float) width / height;

        GLESCamera camera = setupCamera(width, height);

        mObject.setCamera(camera);

        float size = mScreenRatio * 0.35f;
        mSphereVertexInfo = GLESMeshUtils.createSphere(mShader,
                size, 20, 20, false, true, false);

        float cubeSize = mScreenRatio * 0.7f;
        mCubeVertexInfo = GLESMeshUtils.createCube(mShader,
                cubeSize, true, false, false);

        mObject.setVertexInfo(mSphereVertexInfo, false, false);

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

            attribName = GLESShaderConstant.ATTRIB_NORMAL;
            mShader.setNormalAttribIndex(attribName);
        }

        return true;
    }

    private int mObjectIndex = 0;

    public void touchDown(float x, float y) {
        if (DEBUG) {
            Log.d(TAG, "touchDown() x=" + x + " y=" + y);
        }

        mIsTouchDown = true;

        mDownX = x;
        mDownY = y;

        if (mObjectIndex == 0) {
            mObjectIndex = 1;
            mObject.setVertexInfo(mSphereVertexInfo, false, false);
        } else {
            mObjectIndex = 0;
            mObject.setVertexInfo(mCubeVertexInfo, false, false);
        }

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
