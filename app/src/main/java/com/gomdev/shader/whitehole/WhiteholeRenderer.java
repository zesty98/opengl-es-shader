package com.gomdev.shader.whitehole;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.util.Log;

import com.gomdev.gles.GLESAnimator;
import com.gomdev.gles.GLESAnimatorCallback;
import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESConfig.Version;
import com.gomdev.gles.GLESContext;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESMeshUtils;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESRect;
import com.gomdev.gles.GLESRenderer;
import com.gomdev.gles.GLESSceneManager;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESShaderConstant;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVector3;
import com.gomdev.gles.GLESVertexInfo;
import com.gomdev.shader.R;
import com.gomdev.shader.SampleRenderer;
import com.gomdev.shader.ShaderUtils;

import java.util.ArrayList;

public class WhiteholeRenderer extends SampleRenderer {
    private static final String CLASS = "WhiteholeRenderer";
    private static final String TAG = WhiteholeConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = WhiteholeConfig.DEBUG;

    private GLESRenderer mRenderer = null;
    private GLESSceneManager mSM = null;

    private WhiteholeObject mWhiteholeObject = null;
    private GLESTexture mWhiteholeTexture = null;

    private Version mVersion;

    private int mWidth;
    private int mHeight;
    private GLESShader mShaderWhitehole;

    private boolean mIsTouchDown = false;
    private float mDownX = 0f;
    private float mDownY = 0f;

    private GLESAnimatorCallback mCallback = null;

    ArrayList<GLESAnimator> mAnimatorList = new ArrayList<GLESAnimator>();
    private GLESAnimator mAnimator = null;
    private float mRadius = 0f;
    private float mMinRingSize = 0f;
    private float mMaxRingSize = 0.0f;
    private float mBoundaryRingSize = 0f;

    public WhiteholeRenderer(Context context) {
        super(context);

        mVersion = GLESContext.getInstance().getVersion();

        mRenderer = GLESRenderer.createRenderer();
        mSM = GLESSceneManager.createSceneManager();
        GLESNode root = mSM.createRootNode("Root");

        mWhiteholeObject = new WhiteholeObject();

        GLESGLState state = new GLESGLState();
        state.setCullFaceState(true);
        state.setCullFace(GLES20.GL_BACK);
        state.setDepthState(true);
        state.setDepthFunc(GLES20.GL_LEQUAL);
        mWhiteholeObject.setGLState(state);

        root.addChild(mWhiteholeObject);

    }

    public void destroy() {
        mWhiteholeObject = null;
    }

    @Override
    protected void onDrawFrame() {
        super.updateFPS();

        int count = 0;
        boolean needToRequestRender = false;

        for (GLESAnimator animator : mAnimatorList) {
            if (animator != null) {
                needToRequestRender = animator.doAnimation();

                if (needToRequestRender == true) {
                    count++;
                }
            }
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        mRenderer.updateScene(mSM);
        mRenderer.drawScene(mSM);

        if (count > 0) {
            mView.requestRender();
        }
    }

    @Override
    protected void onSurfaceChanged(int width, int height) {
        mWidth = width;
        mHeight = height;

        mRenderer.reset();

        GLESCamera camera = setupCamera(width, height);

        mWhiteholeObject.setCamera(camera);
        mWhiteholeObject.setScreenSize(width, height);

        GLESVertexInfo vertexInfo = GLESMeshUtils.createPlaneMesh(
                mShaderWhitehole,
                mWidth, mHeight, WhiteholeConfig.MESH_RESOLUTION, true, false);
        mWhiteholeObject.setVertexInfo(vertexInfo, true, true);
    }

    private GLESCamera setupCamera(int width, int height) {
        GLESCamera camera = new GLESCamera();
        camera.setLookAt(0f, 0f, 64f, 0f, 0f, 0f, 0f, 1f, 0f);

        float right = width * 0.5f / 4f;
        float left = -right;
        float top = height * 0.5f / 4f;
        float bottom = -top;
        float near = 16f;
        float far = 256f;

        camera.setFrustum(left, right, bottom, top, near, far);

        camera.setViewport(new GLESRect(0, 0, width, height));

        return camera;
    }

    @Override
    protected void onSurfaceCreated() {
        GLES20.glClearColor(0.0f, 0.0f, 1.0f, 0.0f);

        createAnimation();

        mWhiteholeObject.setShader(mShaderWhitehole);
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.galaxy);
        GLESTexture.Builder builder = new GLESTexture.Builder(
                GLES20.GL_TEXTURE_2D, bitmap.getWidth(), bitmap.getHeight());
        mWhiteholeTexture = builder.setWrapMode(GLES20.GL_REPEAT)
                .load(bitmap);

        bitmap.recycle();
        mWhiteholeObject.setTexture(mWhiteholeTexture);

        mMinRingSize = GLESUtils.getPixelFromDpi(mContext,
                WhiteholeConfig.MIN_RING_SIZE);
        mMaxRingSize = (float) Math.hypot(GLESUtils.getWidthPixels(mContext),
                GLESUtils.getHeightPixels(mContext));
        mBoundaryRingSize = GLESUtils.getPixelFromDpi(mContext,
                WhiteholeConfig.BOUNDARY_RING_SIZE);
    }

    @Override
    protected boolean createShader() {
        if (DEBUG) {
            Log.d(TAG, "createShader()");
        }

        mShaderWhitehole = new GLESShader(mContext);

        String vsSource = ShaderUtils.getShaderSource(mContext, 0);
        String fsSource = ShaderUtils.getShaderSource(mContext, 1);

        mShaderWhitehole.setShaderSource(vsSource, fsSource);
        if (mShaderWhitehole.load() == false) {
            return false;
        }

        if (mVersion == Version.GLES_20) {
            String attribName = GLESShaderConstant.ATTRIB_POSITION;
            mShaderWhitehole.setPositionAttribIndex(attribName);

            attribName = GLESShaderConstant.ATTRIB_TEXCOORD;
            mShaderWhitehole.setTexCoordAttribIndex(attribName);
        }

        return true;
    }

    public void touchDown(float x, float y) {
        if (DEBUG) {
            Log.d(TAG, "touchDown() x=" + x + " y=" + y);
        }

        mIsTouchDown = true;

        for (GLESAnimator animator : mAnimatorList) {
            if (animator != null) {
                animator.cancel();
            }
        }

        mDownX = x;
        mDownY = y;

        mWhiteholeObject.setPosition(x, y);
        mWhiteholeObject.setRadius(mMinRingSize);

        mView.requestRender();
    }

    public void touchUp(float x, float y) {
        if (mIsTouchDown == false) {
            return;
        }

        mRadius = (float) Math.hypot((x - mDownX), (y - mDownY)) + mMinRingSize;

        if (mRadius > mBoundaryRingSize) {
            mAnimator.setDuration(0L, 500L);
            mAnimator.start(mRadius, mMaxRingSize);
        } else {
            mAnimator.setDuration(0L, 500L);
            mAnimator.start(mRadius, 0f);
        }

        mView.requestRender();

        mIsTouchDown = false;
    }

    public void touchMove(float x, float y) {
        if (mIsTouchDown == false) {
            return;
        }

        mRadius = (float) Math.hypot((x - mDownX), (y - mDownY)) + mMinRingSize;
        mWhiteholeObject.setRadius(mRadius);

        mView.requestRender();
    }

    public void touchCancel(float x, float y) {

    }

    private void createAnimation() {
        mCallback = new GLESAnimatorCallback() {

            @Override
            public void onAnimation(GLESVector3 currentValue) {
                mRadius = currentValue.getX();
                mWhiteholeObject.setRadius(mRadius);
            }

            @Override
            public void onFinished() {
            }

            @Override
            public void onCancel() {
                // TODO Auto-generated method stub

            }
        };

        mAnimator = new GLESAnimator(mCallback);
        mAnimatorList.add(mAnimator);
    }
}
