package com.gomdev.shader.transformFeedback;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.os.Build;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESConfig;
import com.gomdev.gles.GLESContext;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESObject;
import com.gomdev.gles.GLESRect;
import com.gomdev.gles.GLESRendererListener;
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

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Random;

public class TransformFeedbackRenderer extends SampleRenderer implements GLESRendererListener {
    private static final String CLASS = "TransformFeedbackRenderer";
    private static final String TAG = TransformFeedbackConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = TransformFeedbackConfig.DEBUG;

    private static final int NUM_OF_POINT_IN_WIDTH = 200;
    private static final int NUM_ELEMENT_OF_POSITION = 4;
    private static final int NUM_ELEMENT_OF_TEXCOORD = 2;

    private static final int DURATION_ANIMATION = 1500;
    private static final float INVERSE_DURATION_ANIMATION = 1f / DURATION_ANIMATION;

    private static final String[] sVaryings = new String[]{
            "vPosition"
    };

    private Context mContext = null;

    private Random mRandom = new Random();

    private GLESConfig.Version mVersion;

    private GLESSceneManager mSM = null;

    private GLESObject mObject;
    private GLESShader mShader;
    private GLESVertexInfo mVertexInfo = null;

    private float mWidth = 0f;
    private float mHeight = 0f;

    private float mScreenRatio = 0f;

    private int mNumOfPoints = 0;
    private float mPointSize = 0f;

    private FloatBuffer mPosBuffer = null;
    private FloatBuffer mOrigPosBuffer = null;
    private FloatBuffer mTexBuffer = null;
    private FloatBuffer mVelocityFactorBuffer = null;

    private FloatBuffer mUserDataBuffer = null; // velocity, normalizedDuration, dirX, dirY

    private int mSrcIndex = 0;

    private int[] mVBOs = new int[5];
    private int[] mTFs = new int[2];

    // touch and animation
    private ArrayList<Particle> mParticles = new ArrayList<>();

    private Interpolator mInterpolator = new AccelerateDecelerateInterpolator();

    private long mPrevTick = 0L;
    private float mNormalizedTime = 0f;

    private boolean mIsTouchDown = false;
    private boolean mIsDownAnimation = false;
    private boolean mIsUpAnimation = false;

    private int mSurfaceTop = 0;

    private float mDownX = 0f;
    private float mDownY = 0f;

    public TransformFeedbackRenderer(Context context) {
        super(context);

        mContext = context;

        mVersion = GLESContext.getInstance().getVersion();

        mSM = GLESSceneManager.createSceneManager();
        GLESNode root = mSM.createRootNode("Root");

        mObject = mSM.createObject("Object");

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

        mRenderer.setListener(this);
    }

    public void destroy() {
        mObject = null;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onDrawFrame() {
        super.updateFPS();

        mShader.useProgram();

        update();

        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT | GLES30.GL_COLOR_BUFFER_BIT);

        mRenderer.updateScene(mSM);
        mRenderer.drawScene(mSM);


        mSrcIndex = (mSrcIndex + 1) % 2;
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
            normalizedElapsedTime = -normalizedElapsedTime;
        }

        if (mNormalizedTime > 1f) {
            mNormalizedTime = 1f;
            mIsDownAnimation = false;
            normalizedElapsedTime = 0f;
        }

        if (mNormalizedTime < 0f) {
            mNormalizedTime = 0f;
            mIsUpAnimation = false;
            normalizedElapsedTime = 0f;
        }

        float x = 0f;
        float y = 0f;

        if (mVersion == GLESConfig.Version.GLES_20) {

            synchronized (this) {
                int size = mParticles.size();
                for (int i = 0; i < size; i++) {
                    Particle particle = mParticles.get(i);
                    GLESVector3 dir = particle.getDirection();

                    float elapsedTime = normalizedElapsedTime;

                    x = mPosBuffer.get(i * NUM_ELEMENT_OF_POSITION + 0);
                    y = mPosBuffer.get(i * NUM_ELEMENT_OF_POSITION + 1);

                    if (mNormalizedTime > particle.getNormalizedDuration()) {
                        elapsedTime = 0f;
                    }

                    x = x + dir.mX * elapsedTime * particle.getVelocityX();

                    if (mNormalizedTime == 0f) {
                        mPosBuffer.put(i * NUM_ELEMENT_OF_POSITION + 0, particle.mX);
                    } else {
                        mPosBuffer.put(i * NUM_ELEMENT_OF_POSITION + 0, x);
                    }

                    y = y + dir.mY * elapsedTime * particle.getVelocityX();

                    if (mNormalizedTime == 0f) {
                        mPosBuffer.put(i * NUM_ELEMENT_OF_POSITION + 1, particle.mY);
                    } else {
                        mPosBuffer.put(i * NUM_ELEMENT_OF_POSITION + 1, y);
                    }
                }
            }
            mPosBuffer.position(0);

            mVertexInfo.setBuffer(mShader.getPositionAttribIndex(), mPosBuffer);
        } else {
            mShader.useProgram();
            int location = mShader.getUniformLocation("uUserData");
            GLES20.glUniform4f(location, mDownX, mDownY, normalizedElapsedTime, mNormalizedTime);
        }

        mView.requestRender();
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceChanged()");
        }

        mRenderer.reset();

        mObject.setShader(mShader);

        mWidth = width;
        mHeight = height;

        mScreenRatio = (float) width / height;

        GLES30.glViewport(0, 0, width, height);

        GLESCamera camera = setupCamera(width, height);
        mObject.setCamera(camera);

        mPointSize = ((float) width) / NUM_OF_POINT_IN_WIDTH;

        mVertexInfo = createParticles();
        mObject.setVertexInfo(mVertexInfo, false, false);

        if (mVersion == GLESConfig.Version.GLES_30) {
            createVBO();
        }

        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.lockscreen);
        GLESTexture.Builder builder = new GLESTexture.Builder(
                GLES20.GL_TEXTURE_2D, bitmap.getWidth(), bitmap.getHeight());
        GLESTexture texture = builder.load(bitmap);
        mObject.setTexture(texture);

        int location = mShader.getUniformLocation("uPointSize");
        GLES20.glUniform1f(location, mPointSize);

        int titleBarHeight = GLESUtils.getTitleBarHeight((Activity) mContext);
        int statusBarHeight = GLESUtils.getStatusBarHeight((Activity) mContext);
        mSurfaceTop = titleBarHeight + statusBarHeight;
    }

    private GLESCamera setupCamera(int width, int height) {
        GLESCamera camera = new GLESCamera();

        float fovy = 30f;
        float eyeZ = (height / 2f) / (float) Math.tan(Math.toRadians(fovy * 0.5));

        camera.setLookAt(0f, 0f, eyeZ, 0f, 0f, 0f, 0f, 1f, 0f);

        camera.setFrustum(fovy, mScreenRatio, eyeZ * 0.01f, eyeZ * 10f);

        camera.setViewport(new GLESRect(0, 0, width, height));

        return camera;
    }

    private GLESVertexInfo createParticles() {
        GLESVertexInfo vertexInfo = new GLESVertexInfo();

        float temp = NUM_OF_POINT_IN_WIDTH / mScreenRatio;
        int numOfPointInHeight = (int) Math.ceil(temp);
        mNumOfPoints = NUM_OF_POINT_IN_WIDTH * numOfPointInHeight;

        vertexInfo.setNumOfVertex(mNumOfPoints);

        float[] position = new float[mNumOfPoints * NUM_ELEMENT_OF_POSITION];
        float[] texCoord = new float[mNumOfPoints * NUM_ELEMENT_OF_TEXCOORD];

        float[] user = new float[mNumOfPoints];


        float halfPointSize = mPointSize * 0.5f;

        float startOffsetPosX = -mWidth * 0.5f + halfPointSize;
        float startOffsetPosY = -mHeight * 0.5f + halfPointSize;

        int yPosOffset = 0;
        int xPosOffset = 0;

        int yTexOffset = 0;
        int xTexOffset = 0;

        int yUserOffset = 0;
        int xUserOffset = 0;

        float posX = 0f;
        float posY = 0f;
        float posZ = 0f;

        float velocity = 0f;

        float s = 0f;
        float t = 0f;

        float startOffsetTexCoordX = halfPointSize / mWidth;
        float startOffsetTexCoordY = 1.0f - halfPointSize / mHeight;

        mParticles.clear();
        for (int i = 0; i < numOfPointInHeight; i++) {
            yPosOffset = i * NUM_OF_POINT_IN_WIDTH * NUM_ELEMENT_OF_POSITION;
            yTexOffset = i * NUM_OF_POINT_IN_WIDTH * NUM_ELEMENT_OF_TEXCOORD;
            yUserOffset = i * NUM_OF_POINT_IN_WIDTH;

            for (int j = 0; j < NUM_OF_POINT_IN_WIDTH; j++) {
                xPosOffset = j * NUM_ELEMENT_OF_POSITION;
                xTexOffset = j * NUM_ELEMENT_OF_TEXCOORD;
                xUserOffset = j;

                posX = startOffsetPosX + mPointSize * j;
                posY = startOffsetPosY + mPointSize * i;
                posZ = mRandom.nextFloat() * 0.01f;

                position[yPosOffset + xPosOffset + 0] = posX;
                position[yPosOffset + xPosOffset + 1] = posY;
                position[yPosOffset + xPosOffset + 2] = posZ;
                position[yPosOffset + xPosOffset + 3] = 1f;

                float velocityFactor = mRandom.nextFloat() * 0.5f + 1f;

                user[yUserOffset + xUserOffset] = velocityFactor;

                Particle particle = new Particle(posX, posY, posZ);
                particle.setVelocityFactor(velocityFactor);

                mParticles.add(particle);

                s = startOffsetTexCoordX +
                        (mPointSize * j) / mWidth;
                t = startOffsetTexCoordY - mPointSize * i / mHeight;

                texCoord[yTexOffset + xTexOffset + 0] = s;
                texCoord[yTexOffset + xTexOffset + 1] = t;
            }
        }

        int attribIndex = 0;

        if (GLESConfig.Version.GLES_20 == mVersion) {
            attribIndex = mShader.getPositionAttribIndex();
            vertexInfo.setBuffer(attribIndex, position, NUM_ELEMENT_OF_POSITION);
            mPosBuffer = (FloatBuffer) vertexInfo.getBuffer(attribIndex);

            attribIndex = mShader.getTexCoordAttribIndex();
            vertexInfo.setBuffer(attribIndex, texCoord, NUM_ELEMENT_OF_TEXCOORD);
            mTexBuffer = (FloatBuffer) vertexInfo.getBuffer(attribIndex);
        } else {
            mPosBuffer = GLESUtils.makeFloatBuffer(position);
            mOrigPosBuffer = GLESUtils.makeFloatBuffer(position);

            mTexBuffer = GLESUtils.makeFloatBuffer(texCoord);

            mVelocityFactorBuffer = GLESUtils.makeFloatBuffer(user);

            float[] userDatas = new float[mNumOfPoints * 4];   // velocity, normalizedDuration, dirX, dirY
            mUserDataBuffer = GLESUtils.makeFloatBuffer(userDatas);
        }

        vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.POINTS);
        vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);

        return vertexInfo;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void createVBO() {
        GLES30.glGenTransformFeedbacks(2, mTFs, 0);
        GLES30.glGenBuffers(5, mVBOs, 0);

        for (int i = 0; i < 2; i++) {
            GLES30.glBindTransformFeedback(GLES30.GL_TRANSFORM_FEEDBACK, mTFs[i]);
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVBOs[i]);
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,
                    mPosBuffer.capacity() * GLESConfig.FLOAT_SIZE_BYTES,
                    mPosBuffer, GLES30.GL_DYNAMIC_COPY);
            GLES30.glBindBufferBase(GLES30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, mVBOs[i]);
        }

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVBOs[2]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,
                mOrigPosBuffer.capacity() * GLESConfig.FLOAT_SIZE_BYTES,
                mOrigPosBuffer, GLES30.GL_STATIC_DRAW);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVBOs[3]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,
                mTexBuffer.capacity() * GLESConfig.FLOAT_SIZE_BYTES,
                mTexBuffer, GLES30.GL_STATIC_DRAW);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVBOs[4]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,
                mVelocityFactorBuffer.capacity() * GLESConfig.FLOAT_SIZE_BYTES,
                mVelocityFactorBuffer, GLES30.GL_STATIC_DRAW);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
        GLES30.glBindTransformFeedback(GLES30.GL_TRANSFORM_FEEDBACK, 0);
    }

    @Override
    public void onSurfaceCreated() {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCreated()");
        }

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);


    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
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

        if (mVersion == GLESConfig.Version.GLES_30) {
            GLES30.glTransformFeedbackVaryings(mShader.getProgram(), sVaryings,
                    GLES30.GL_INTERLEAVED_ATTRIBS);

            mShader.linkProgram();
        } else {
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

        mDownX = x - mWidth * 0.5f;
        mDownY = mHeight * 0.5f - (y - mSurfaceTop);


        mIsDownAnimation = true;

        synchronized (this) {
            GLESVector3 dir = new GLESVector3();
            int size = mParticles.size();
            for (int i = 0; i < size; i++) {
                Particle particle = mParticles.get(i);

                dir.set(mDownX - particle.mX, mDownY - particle.mY, 0f);
                float length = dir.length();
                dir.normalize();
                particle.setDirection(dir.mX, dir.mY, dir.mZ);
                particle.setDistance(length);

                float velocity = length * particle.getVelocityFactor();
                particle.setVelocityX(velocity);

                if (mVersion == GLESConfig.Version.GLES_20) {
                    mPosBuffer.put(i * NUM_ELEMENT_OF_POSITION + 0, particle.mX);
                    mPosBuffer.put(i * NUM_ELEMENT_OF_POSITION + 1, particle.mY);
                    mPosBuffer.put(i * NUM_ELEMENT_OF_POSITION + 2, 0f);
                    mPosBuffer.put(i * NUM_ELEMENT_OF_POSITION + 3, 1f);
                } else {
                    float particleNormalizedDuration = particle.getNormalizedDuration();

                    mUserDataBuffer.put(i * 4 + 0, velocity);
                    mUserDataBuffer.put(i * 4 + 1, particleNormalizedDuration);
                    mUserDataBuffer.put(i * 4 + 2, dir.mX);
                    mUserDataBuffer.put(i * 4 + 3, dir.mY);
                }
            }
        }

        mPrevTick = System.currentTimeMillis();
        mNormalizedTime = 0;

        mView.requestRender();
    }

    public void touchUp(float x, float y) {
        if (mIsTouchDown == false) {
            return;
        }

        mIsDownAnimation = false;
        mIsUpAnimation = true;

        mPrevTick = System.currentTimeMillis();

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

    }

    @Override
    public void setupVAO(GLESObject object) {

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void enableVertexAttribute(GLESObject object) {

        if (mVersion == GLESConfig.Version.GLES_30) {
            mShader.useProgram();

            bindUserDatas();

            bindVertexBuffers(mVBOs[mSrcIndex]);

            int dstIndex = (mSrcIndex + 1) % 2;
            GLES30.glBindTransformFeedback(GLES30.GL_TRANSFORM_FEEDBACK, mTFs[dstIndex]);

            GLES30.glBeginTransformFeedback(GLES30.GL_POINTS);

            bindOriginPositionBuffers(mVBOs[2]);

            bindTexCoordBuffer(mVBOs[3]);

            bindUserBuffer(mVBOs[4]);
        }
    }

    private void bindUserDatas() {
        int index = mShader.getAttribLocation("aUserData");
        GLES30.glVertexAttribPointer(index, 4, GLES30.GL_FLOAT, false,
                4 * GLESConfig.FLOAT_SIZE_BYTES,
                mUserDataBuffer);
        GLES30.glEnableVertexAttribArray(index);
    }

    private void bindVertexBuffers(int vbo) {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);

        int index = mShader.getPositionAttribIndex();
        GLES30.glVertexAttribPointer(index, 4, GLES30.GL_FLOAT, false,
                4 * GLESConfig.FLOAT_SIZE_BYTES,
                0);
        GLES30.glEnableVertexAttribArray(index);
    }

    private void bindOriginPositionBuffers(int vbo) {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);

        int index = mShader.getAttribLocation("aOriginPosition");
        GLES30.glVertexAttribPointer(index, NUM_ELEMENT_OF_POSITION, GLES30.GL_FLOAT, false,
                NUM_ELEMENT_OF_POSITION * GLESConfig.FLOAT_SIZE_BYTES,
                0);
        GLES30.glEnableVertexAttribArray(index);
    }

    private void bindTexCoordBuffer(int vbo) {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);

        int index = mShader.getTexCoordAttribIndex();
        GLES30.glVertexAttribPointer(index, NUM_ELEMENT_OF_TEXCOORD, GLES30.GL_FLOAT, false,
                NUM_ELEMENT_OF_TEXCOORD * GLESConfig.FLOAT_SIZE_BYTES,
                0);
        GLES30.glEnableVertexAttribArray(index);
    }

    private void bindUserBuffer(int vbo) {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);

        int index = mShader.getAttribLocation("aVelocityFactor");
        GLES30.glVertexAttribPointer(index, 1, GLES30.GL_FLOAT, false,
                GLESConfig.FLOAT_SIZE_BYTES,
                0);
        GLES30.glEnableVertexAttribArray(index);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void disableVertexAttribute(GLESObject object) {
        if (mVersion == GLESConfig.Version.GLES_30) {
            GLES30.glEndTransformFeedback();
            GLES30.glBindTransformFeedback(GLES30.GL_TRANSFORM_FEEDBACK, 0);

            unbindVertexBuffer();

            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
        }
    }

    private void unbindVertexBuffer() {
        mShader.useProgram();

        int index = mShader.getPositionAttribIndex();
        GLES30.glDisableVertexAttribArray(index);

        index = mShader.getAttribLocation("aOriginPosition");
        GLES30.glDisableVertexAttribArray(index);

        index = mShader.getTexCoordAttribIndex();
        GLES30.glDisableVertexAttribArray(index);

        index = mShader.getAttribLocation("aVelocityFactor");
        GLES30.glDisableVertexAttribArray(index);

        index = mShader.getAttribLocation("aUserData");
        GLES30.glDisableVertexAttribArray(index);

        index = mShader.getAttribLocation("aDirection");
        GLES30.glDisableVertexAttribArray(index);
    }
}
