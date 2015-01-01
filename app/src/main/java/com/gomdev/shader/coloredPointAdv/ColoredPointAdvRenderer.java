package com.gomdev.shader.coloredPointAdv;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Random;

import com.gomdev.gles.*;
import com.gomdev.gles.GLESConfig.Version;
import com.gomdev.gles.GLESVertexInfo.PrimitiveMode;
import com.gomdev.gles.GLESVertexInfo.RenderType;
import com.gomdev.shader.SampleRenderer;
import com.gomdev.shader.R;
import com.gomdev.shader.ShaderUtils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.animation.Interpolator;
import android.opengl.GLES20;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;

public class ColoredPointAdvRenderer extends SampleRenderer {
    private static final String CLASS = "ColoredPointAdbRenderer";
    private static final String TAG = ColoredPointAdvConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = ColoredPointAdvConfig.DEBUG;

    private static final int NUM_OF_POINT_IN_WIDTH = 100;
    private static final int NUM_ELEMENT_OF_POSITION = 3;
    private static final int NUM_ELEMENT_OF_TEXCOORD = 2;

    private static final int DURATION_ANIMATION = 1500;
    private static final float INVERSE_DURATION_ANIMATION = 1f / DURATION_ANIMATION;

    private GLESSceneManager mSM = null;
    private GLESObject mObject;
    private GLESShader mShader;
    private Version mVersion;
    private GLESVertexInfo mVertexInfo = null;
    private Random mRandom = new Random();
    private Interpolator mInterpolator = new AccelerateDecelerateInterpolator();

    private boolean mIsTouchDown = false;

    private int mWidth = 0;
    private int mHeight = 0;

    private float mDownXInSpace = 0f;
    private float mDownYInSpace = 0f;

    private int mSurfaceTop = 0;

    private float mScreenRatio = 0f;

    private float mPointSize = 0f;
    private float mPointSizeInSpace = 0f;

    private FloatBuffer mPosBuffer = null;

    // animation
    private ArrayList<GLESParticle> mParticles = new ArrayList<GLESParticle>();
    private long mPrevTick = 0L;

    private boolean mIsDownAnimation = false;
    private boolean mIsUpAnimation = false;
    private float mNormalizedTime = 0f;

    public ColoredPointAdvRenderer(Context context) {
        super(context);

        mVersion = GLESContext.getInstance().getVersion();

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
        if (mIsDownAnimation == false && mIsUpAnimation == false) {
            return;
        }

        long tick = System.currentTimeMillis();

        float normalizedElapsedTime = ((float) (tick - mPrevTick))
                * INVERSE_DURATION_ANIMATION;
        mPrevTick = tick;

        if (mIsDownAnimation == true) {
            mNormalizedTime += normalizedElapsedTime;
        } else if (mIsUpAnimation == true) {
            mNormalizedTime -= normalizedElapsedTime;
        }

        if (mNormalizedTime >= 1f) {
            mNormalizedTime = 1f;
            mIsDownAnimation = false;
        }

        if (mNormalizedTime <= 0f) {
            mNormalizedTime = 0f;
            mIsUpAnimation = false;
        }

        float normalizedTime =
                mInterpolator.getInterpolation(mNormalizedTime);

        float x = 0f;
        float y = 0f;

        float particleTime = 0f;
        int size = mParticles.size();
        for (int i = 0; i < size; i++) {
            GLESParticle particle = mParticles.get(i);
            if (normalizedTime > particle.getNormalizedDuration()) {
                particleTime = particle.getNormalizedDuration();
            } else {
                particleTime = normalizedTime;
            }
            x = particle.mX + (mDownXInSpace - particle.mX) * particleTime
                    * particle.getVelocityX();
            mPosBuffer.put(i * NUM_ELEMENT_OF_POSITION + 0, x);

            y = particle.mY + (mDownYInSpace - particle.mY) * particleTime
                    * particle.getVelocityX();
            mPosBuffer.put(i * NUM_ELEMENT_OF_POSITION + 1, y);
        }
        mPosBuffer.position(0);
        mVertexInfo.setBuffer(mShader.getPositionAttribIndex(), mPosBuffer);

        mView.requestRender();
    }

    @Override
    protected void onSurfaceChanged(int width, int height) {
        mRenderer.reset();

        mWidth = width;
        mHeight = height;

        mScreenRatio = (float) width / height;

        GLESCamera camera = setupCamera(width, height);

        mObject.setCamera(camera);

        mPointSize = ((float) width) / NUM_OF_POINT_IN_WIDTH;
        mPointSizeInSpace = 2f * mScreenRatio * mPointSize / width;

        mVertexInfo = createParticles(width, height);
        mObject.setVertexInfo(mVertexInfo, false, false);

        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.lockscreen);
        GLESTexture.Builder builder = new GLESTexture.Builder(
                GLES20.GL_TEXTURE_2D, bitmap.getWidth(), bitmap.getHeight());
        GLESTexture texture = builder.load(bitmap);
        mObject.setTexture(texture);

        int location = GLES20.glGetUniformLocation(mShader.getProgram(),
                "uPointSize");
        GLES20.glUniform1f(location, mPointSize);

        int titleBarHeight = GLESUtils.getTitleBarHeight((Activity) mContext);
        int statusBarHeight = GLESUtils.getStatusBarHeight((Activity) mContext);
        mSurfaceTop = titleBarHeight + statusBarHeight;
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

        float temp = NUM_OF_POINT_IN_WIDTH / mScreenRatio;
        int numOfPointInHeight = (int) Math.ceil(temp);
        int numOfPoints = NUM_OF_POINT_IN_WIDTH * numOfPointInHeight;
        float[] position = new float[numOfPoints * NUM_ELEMENT_OF_POSITION];
        float[] texCoord = new float[numOfPoints * NUM_ELEMENT_OF_TEXCOORD];

        float halfPointSizeInSpace = mPointSizeInSpace * 0.5f;

        float startOffsetPosX = -mScreenRatio + halfPointSizeInSpace;
        float startOffsetPosY = -1 + halfPointSizeInSpace;

        int yPosOffset = 0;
        int xPosOffset = 0;
        int yTexOffset = 0;
        int xTexOffset = 0;

        float posX = 0f;
        float posY = 0f;
        float posZ = 0f;

        float velocity = 0f;

        float s = 0f;
        float t = 0f;

        float startOffsetTexCoordX = halfPointSizeInSpace / (mScreenRatio * 2f);
        float startOffsetTexCoordY = 1.0f - halfPointSizeInSpace / 2f;

        mParticles.clear();
        for (int i = 0; i < numOfPointInHeight; i++) {
            yPosOffset = i * NUM_OF_POINT_IN_WIDTH * NUM_ELEMENT_OF_POSITION;
            yTexOffset = i * NUM_OF_POINT_IN_WIDTH * NUM_ELEMENT_OF_TEXCOORD;
            for (int j = 0; j < NUM_OF_POINT_IN_WIDTH; j++) {
                xPosOffset = j * NUM_ELEMENT_OF_POSITION;
                xTexOffset = j * NUM_ELEMENT_OF_TEXCOORD;

                posX = startOffsetPosX + mPointSizeInSpace * j;
                posY = startOffsetPosY + mPointSizeInSpace * i;
                posZ = mRandom.nextFloat() * 0.01f;

                position[yPosOffset + xPosOffset + 0] = posX;
                position[yPosOffset + xPosOffset + 1] = posY;
                position[yPosOffset + xPosOffset + 2] = posZ;

                velocity = mRandom.nextFloat() * 0.5f + 1f;

                GLESParticle particle = new GLESParticle(posX, posY, posZ);
                particle.setVelocityX(velocity);

                mParticles.add(particle);

                s = startOffsetTexCoordX +
                        (mPointSizeInSpace * j) / (mScreenRatio * 2f);
                t = startOffsetTexCoordY - mPointSizeInSpace * i / 2f;

                texCoord[yTexOffset + xTexOffset + 0] = s;
                texCoord[yTexOffset + xTexOffset + 1] = t;
            }
        }
        int attribIndex = mShader.getPositionAttribIndex();
        vertexInfo.setBuffer(attribIndex, position, NUM_ELEMENT_OF_POSITION);
        mPosBuffer = (FloatBuffer) vertexInfo.getBuffer(attribIndex);

        attribIndex = mShader.getTexCoordAttribIndex();
        vertexInfo.setBuffer(attribIndex, texCoord, NUM_ELEMENT_OF_TEXCOORD);

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

            attribName = GLESShaderConstant.ATTRIB_TEXCOORD;
            mShader.setTexCoordAttribIndex(attribName);
        }

        return true;
    }

    public void touchDown(float x, float y) {
        if (DEBUG) {
            Log.d(TAG, "touchDown() x=" + x + " y=" + y);
        }

        mIsTouchDown = true;

        mDownXInSpace = (x - mWidth * 0.5f) * mScreenRatio * 2f / mWidth;
        mDownYInSpace = (mHeight * 0.5f - (y - mSurfaceTop)) * 2f / mHeight;

        mPrevTick = System.currentTimeMillis();
        mIsDownAnimation = true;

        mView.requestRender();
    }

    public void touchUp(float x, float y) {
        if (mIsTouchDown == false) {
            return;
        }

        mIsDownAnimation = false;
        mIsUpAnimation = true;

        mView.requestRender();

        mIsTouchDown = false;
    }

    public void touchMove(float x, float y) {
        if (mIsTouchDown == false) {
            return;
        }

        mView.requestRender();
    }

    public void touchCancel(float x, float y) {
    }
}
