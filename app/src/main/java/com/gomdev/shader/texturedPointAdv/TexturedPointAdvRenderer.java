package com.gomdev.shader.texturedPointAdv;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESConfig;
import com.gomdev.gles.GLESConfig.Version;
import com.gomdev.gles.GLESContext;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESObject;
import com.gomdev.gles.GLESParticle;
import com.gomdev.gles.GLESRect;
import com.gomdev.gles.GLESRendererListener;
import com.gomdev.gles.GLESSceneManager;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESShaderConstant;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVertexInfo;
import com.gomdev.gles.GLESVertexInfo.PrimitiveMode;
import com.gomdev.gles.GLESVertexInfo.RenderType;
import com.gomdev.shader.R;
import com.gomdev.shader.SampleRenderer;
import com.gomdev.shader.ShaderUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;

public class TexturedPointAdvRenderer extends SampleRenderer implements
        GLESRendererListener {
    private static final String CLASS = "TexturedPointAdvRenderer";
    private static final String TAG = TexturedPointAdvConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = TexturedPointAdvConfig.DEBUG;

    private static final int NUM_OF_POINT_IN_WIDTH = 40;
    private static final int NUM_ELEMENT_OF_POSITION = 3;
    private static final int NUM_ELEMENT_OF_TEXCOORD = 2;
    private static final int NUM_ELEMENT_OF_USER_ATTRIB = 1;

    private static final int USER_ATTRIB_LOCATION = 4;

    private static final int DURATION_ANIMATION = 500;
    private static final float INVERSE_DURATION_ANIMATION = 1f / DURATION_ANIMATION;

    private static final float ROUND_FACTOR = 0.5f;

    private GLESSceneManager mSM = null;
    private GLESObject mObject;
    private GLESShader mShader;
    private Version mVersion;
    private GLESVertexInfo mVertexInfo = null;
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
    private FloatBuffer mUserBuffer = null;

    private int mDistFromTouchLocation = -1;

    // animation
    private ArrayList<GLESParticle> mParticles = new ArrayList<GLESParticle>();
    private long mPrevTick = 0L;

    private boolean mIsDownAnimation = false;
    private boolean mIsUpAnimation = false;

    private float mMaxDepth = 0f;
    private float mCurrentDepth = 0f;
    private float mMaxRadius = 0f;

    public TexturedPointAdvRenderer(Context context) {
        super(context);

        mVersion = GLESContext.getInstance().getVersion();

        mSM = GLESSceneManager.createSceneManager();
        GLESNode root = mSM.createRootNode("Root");

        mObject = mSM.createObject("BasicObject");

        GLESGLState state = new GLESGLState();
        state.setCullFaceState(true);
        state.setCullFace(GLES20.GL_BACK);
        state.setDepthState(false);
        state.setBlendState(true);
        state.setBlendFuncSeperate(
                GLES20.GL_SRC_ALPHA,
                GLES20.GL_ONE_MINUS_SRC_ALPHA,
                GLES20.GL_SRC_ALPHA,
                GLES20.GL_ONE_MINUS_SRC_ALPHA);
        mObject.setGLState(state);

        root.addChild(mObject);

        mRenderer.setListener(this);
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
            mCurrentDepth += normalizedElapsedTime * mMaxDepth;

            if (mCurrentDepth > mMaxDepth) {
                mCurrentDepth = mMaxDepth;
                mIsDownAnimation = false;
            }
        } else if (mIsUpAnimation == true) {
            mCurrentDepth -= normalizedElapsedTime * mMaxDepth;
            if (mCurrentDepth < 0f) {
                mCurrentDepth = 0f;
                mIsUpAnimation = false;
            }
        }

        float normalizedTime = mCurrentDepth / mMaxDepth;

        float z = 0f;

        float distFromTouch = 0f;
        float normalizedDist = 1f;
        float userData = 1f;

        int size = mParticles.size();
        for (int i = 0; i < size; i++) {
            GLESParticle particle = mParticles.get(i);

            distFromTouch = (float) Math.hypot(particle.mX - mDownXInSpace,
                    particle.mY - mDownYInSpace);
            if (distFromTouch <= mMaxRadius) {
                normalizedDist = mInterpolator
                        .getInterpolation(distFromTouch / mMaxRadius);
                z = -mCurrentDepth * (1f - normalizedDist);
                // ROUND_FACTOR affect point is seen as a circle.
                normalizedDist *= ROUND_FACTOR;
                userData = normalizedDist + (1.0f - normalizedTime);
                userData = Math.min(userData, 1.0f);
            } else {
                normalizedDist = 1f;
                z = 0f;
                userData = 1f;
            }
            mPosBuffer.put(i * NUM_ELEMENT_OF_POSITION + 2, z);
            mUserBuffer.put(i * NUM_ELEMENT_OF_USER_ATTRIB, userData);
        }
        mPosBuffer.position(0);
        int positionIndex = mShader.getPositionAttribIndex();
        mVertexInfo.setBuffer(positionIndex, mPosBuffer);

        mUserBuffer.position(0);

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

        mMaxDepth = mScreenRatio * 0.05f;
        mMaxRadius = mScreenRatio * 0.5f;

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

        location = GLES20.glGetUniformLocation(mShader.getProgram(),
                "uPointSizeInTexCoord");
        float texCoordFactorX = mPointSizeInSpace / (mScreenRatio * 2f);
        float texCoordFactorY = mPointSizeInSpace / 2f;
        GLES20.glUniform2f(location, texCoordFactorX, texCoordFactorY);

        int titleBarHeight = GLESUtils.getTitleBarHeight((Activity) mContext);
        int statusBarHeight = GLESUtils.getStatusBarHeight((Activity) mContext);
        mSurfaceTop = titleBarHeight + statusBarHeight;
    }

    private GLESCamera setupCamera(int width, int height) {
        GLESCamera camera = new GLESCamera();

        float fovy = 120f;
        float eyeZ = 1f / (float) Math.tan(Math.toRadians(fovy * 0.5));

        camera.setLookAt(0f, 0f, eyeZ, 0f, 0f, 0f, 0f, 1f, 0f);

        camera.setFrustum(fovy, mScreenRatio, eyeZ * 0.1f, 400f);

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
        float[] normalizedDist = new float[numOfPoints
                * NUM_ELEMENT_OF_USER_ATTRIB];

        float halfPointSizeInSpace = mPointSizeInSpace * 0.5f;

        int yPosOffset = 0;
        int xPosOffset = 0;
        int yTexOffset = 0;
        int xTexOffset = 0;
        int yDistOffset = 0;
        int xDistOffset = 0;

        float posX = 0f;
        float posY = 0f;
        float posZ = 0f;

        float startOffsetPosX = -mScreenRatio + halfPointSizeInSpace;
        float startOffstPosY = -1f + halfPointSizeInSpace;

        float s = 0f;
        float t = 0f;

        float startOffsetTexCoordX = halfPointSizeInSpace / (mScreenRatio * 2f);
        float startOffsetTexCoordY = 1.0f - halfPointSizeInSpace / 2f;

        mParticles.clear();
        for (int i = 0; i < numOfPointInHeight; i++) {
            yPosOffset = i * NUM_OF_POINT_IN_WIDTH * NUM_ELEMENT_OF_POSITION;
            yTexOffset = i * NUM_OF_POINT_IN_WIDTH * NUM_ELEMENT_OF_TEXCOORD;
            yDistOffset = i * NUM_OF_POINT_IN_WIDTH
                    * NUM_ELEMENT_OF_USER_ATTRIB;

            for (int j = 0; j < NUM_OF_POINT_IN_WIDTH; j++) {
                xPosOffset = j * NUM_ELEMENT_OF_POSITION;
                xTexOffset = j * NUM_ELEMENT_OF_TEXCOORD;
                xDistOffset = j * NUM_ELEMENT_OF_USER_ATTRIB;

                posX = startOffsetPosX + mPointSizeInSpace * j;
                posY = startOffstPosY + mPointSizeInSpace * i;
                posZ = 0f;

                position[yPosOffset + xPosOffset + 0] = posX;
                position[yPosOffset + xPosOffset + 1] = posY;
                position[yPosOffset + xPosOffset + 2] = posZ;

                GLESParticle particle = new GLESParticle(posX, posY, posZ);

                mParticles.add(particle);

                s = startOffsetTexCoordX +
                        (mPointSizeInSpace * j) / (mScreenRatio * 2f);
                t = startOffsetTexCoordY - mPointSizeInSpace * i / 2f;

                texCoord[yTexOffset + xTexOffset + 0] = s;
                texCoord[yTexOffset + xTexOffset + 1] = t;

                normalizedDist[yDistOffset + xDistOffset] = 1f;
            }
        }

        int attribIndex = mShader.getPositionAttribIndex();
        vertexInfo.setBuffer(attribIndex, position, NUM_ELEMENT_OF_POSITION);
        mPosBuffer = (FloatBuffer) vertexInfo.getBuffer(attribIndex);

        attribIndex = mShader.getTexCoordAttribIndex();
        vertexInfo.setBuffer(attribIndex, texCoord, NUM_ELEMENT_OF_TEXCOORD);

        mUserBuffer = GLESUtils.makeFloatBuffer(normalizedDist);

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

            mDistFromTouchLocation = GLES20.glGetAttribLocation(
                    mShader.getProgram(), "aNormalizedDistFromTouch");
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

        mCurrentDepth = 0f;

        mView.requestRender();
    }

    public void touchUp(float x, float y) {
        if (mIsTouchDown == false) {
            return;
        }

        mPrevTick = System.currentTimeMillis();
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

    @Override
    public void setupVBO(GLESShader shader, GLESVertexInfo vertexInfo) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setupVAO(GLESShader shader, GLESVertexInfo vertexInfo) {
        // TODO Auto-generated method stub

    }

    @Override
    public void enableVertexAttribute(GLESObject object) {
        int userAttribLocation = -1;

        if (mVersion == Version.GLES_20) {
            userAttribLocation = mDistFromTouchLocation;
        } else {
            userAttribLocation = USER_ATTRIB_LOCATION;
        }

        GLES20.glVertexAttribPointer(userAttribLocation,
                NUM_ELEMENT_OF_USER_ATTRIB, GLES20.GL_FLOAT, false,
                NUM_ELEMENT_OF_USER_ATTRIB * GLESConfig.FLOAT_SIZE_BYTES,
                mUserBuffer);
        GLES20.glEnableVertexAttribArray(userAttribLocation);
    }

    @Override
    public void disableVertexAttribute(GLESObject object) {
        int userAttribLocation = -1;

        if (mVersion == Version.GLES_20) {
            userAttribLocation = mDistFromTouchLocation;
        } else {
            userAttribLocation = USER_ATTRIB_LOCATION;
        }

        GLES20.glDisableVertexAttribArray(userAttribLocation);
    }
}
