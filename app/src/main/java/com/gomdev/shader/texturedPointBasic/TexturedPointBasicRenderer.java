package com.gomdev.shader.texturedPointBasic;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.util.Log;

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
import com.gomdev.gles.GLESVector3;
import com.gomdev.gles.GLESVertexInfo;
import com.gomdev.gles.GLESVertexInfo.PrimitiveMode;
import com.gomdev.gles.GLESVertexInfo.RenderType;
import com.gomdev.shader.R;
import com.gomdev.shader.SampleRenderer;
import com.gomdev.shader.ShaderUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Random;

public class TexturedPointBasicRenderer extends SampleRenderer implements
        GLESRendererListener {
    private static final String CLASS = "TexturedPointBasicRenderer";
    private static final String TAG = TexturedPointBasicConfig.TAG + "_"
            + CLASS;
    private static final boolean DEBUG = TexturedPointBasicConfig.DEBUG;

    private static final int NUM_OF_PARTICLES = 100;

    private static final int NUM_ELEMENT_OF_POSITION = 3;
    private static final int NUM_ELEMENT_OF_SIZE = 1;

    private static final int USER_ATTRIB_LOCATION = 4;

    private static final int DURATION_ANIMATION = 3000;
    private static final float INVERSE_DURATION_ANIMATION = 1f / DURATION_ANIMATION;

    private GLESSceneManager mSM = null;
    private GLESObject mObject;
    private GLESShader mShader;
    private Version mVersion;
    private GLESVertexInfo mVertexInfo = null;
    private Random mRandom = new Random();

    private int mSizeLocation = -1;

    private float mScreenRatio = 0f;

    private FloatBuffer mSizeBuffer = null;

    private FloatBuffer mPosBuffer = null;

    // animation
    private ArrayList<GLESParticle> mParticles = new ArrayList<GLESParticle>();
    private long mPrevTick = 0L;

    public TexturedPointBasicRenderer(Context context) {
        super(context);

        mVersion = GLESContext.getInstance().getVersion();

        mSM = GLESSceneManager.createSceneManager();
        GLESNode root = mSM.createRootNode("Root");

        mObject = mSM.createObject("BasicObject");

        GLESGLState state = new GLESGLState();
        state.setCullFaceState(true);
        state.setCullFace(GLES20.GL_BACK);
        state.setDepthState(false);
        // state.setDepthFunc(GLES20.GL_LEQUAL);
        state.setBlendState(true);
        state.setBlendFuncSeperate(
                GLES20.GL_ONE,
                GLES20.GL_ONE_MINUS_SRC_ALPHA,
                GLES20.GL_ONE,
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
        long tick = System.currentTimeMillis();

        if (mPrevTick == 0L) {
            mPrevTick = tick;
        }

        float normalizedElapsedTime = ((float) (tick - mPrevTick))
                * INVERSE_DURATION_ANIMATION;
        mPrevTick = tick;

        float x = 0f;
        float y = 0f;

        int size = mParticles.size();
        for (int i = 0; i < size; i++) {
            GLESParticle particle = mParticles.get(i);
            GLESVector3 velocity = particle.getVelocity();

            x = particle.mX + normalizedElapsedTime * velocity.mX;
            if (x > mScreenRatio) {
                x -= mScreenRatio * 2f;
            } else if (x < -mScreenRatio) {
                x += mScreenRatio * 2f;
            }
            particle.mX = x;

            y = particle.mY - normalizedElapsedTime * velocity.mY;
            if (y < -1f) {
                y += 2f;
            }
            particle.mY = y;

            mPosBuffer.put(i * NUM_ELEMENT_OF_POSITION + 0, x);
            mPosBuffer.put(i * NUM_ELEMENT_OF_POSITION + 1, y);
        }
        mPosBuffer.position(0);
        mVertexInfo.setBuffer(mShader.getPositionAttribIndex(), mPosBuffer);

        mView.requestRender();
    }

    @Override
    protected void onSurfaceChanged(int width, int height) {
        mRenderer.reset();

        mScreenRatio = (float) width / height;

        GLESCamera camera = setupCamera(width, height);

        mObject.setCamera(camera);

        mVertexInfo = createParticles(width, height);
        mObject.setVertexInfo(mVertexInfo, false, false);

        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.snow);
        GLESTexture.Builder builder = new GLESTexture.Builder(
                GLES20.GL_TEXTURE_2D, bitmap.getWidth(), bitmap.getHeight());
        GLESTexture texture = builder.load(bitmap);
        mObject.setTexture(texture);

        mPrevTick = 0L;
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

        float[] position = new float[NUM_OF_PARTICLES * NUM_ELEMENT_OF_POSITION];
        float[] size = new float[NUM_OF_PARTICLES * NUM_ELEMENT_OF_SIZE];

        float posX = 0f;
        float posY = 0f;
        float posZ = 0f;

        float basePointSize = GLESUtils.getPixelFromDpi(mContext, 50);

        float velocityX = 0f;
        float velocityY = 0f;

        mParticles.clear();
        for (int i = 0; i < NUM_OF_PARTICLES; i++) {
            posX = (mRandom.nextFloat() * 2f - 1f) * mScreenRatio;
            posY = mRandom.nextFloat() * 2f - 1f;
            posZ = -NUM_OF_PARTICLES * 0.001f + 0.001f * i;

            position[i * NUM_ELEMENT_OF_POSITION + 0] = posX;
            position[i * NUM_ELEMENT_OF_POSITION + 1] = posY;
            position[i * NUM_ELEMENT_OF_POSITION + 2] = posZ;

            velocityX = (mRandom.nextFloat() - 0.5f) * 0.4f;
            velocityY = mRandom.nextFloat() * 0.7f + 1f;

            GLESParticle particle = new GLESParticle(posX, posY, posZ);
            particle.setVelocity(velocityX, velocityY, 0f);

            mParticles.add(particle);

            size[i * NUM_ELEMENT_OF_SIZE] = basePointSize +
                    mRandom.nextFloat() * basePointSize * 2f;
        }
        int positionIndex = mShader.getPositionAttribIndex();
        vertexInfo.setBuffer(positionIndex, position, NUM_ELEMENT_OF_POSITION);
        mPosBuffer = (FloatBuffer) vertexInfo.getBuffer(positionIndex);

        mSizeBuffer = GLESUtils.makeFloatBuffer(size);

        vertexInfo.setPrimitiveMode(PrimitiveMode.POINTS);
        vertexInfo.setRenderType(RenderType.DRAW_ARRAYS);

        return vertexInfo;
    }

    @Override
    protected void onSurfaceCreated() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

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

            mSizeLocation = GLES20.glGetAttribLocation(mShader.getProgram(),
                    "aSize");
        }

        return true;
    }

    public void touchDown(float x, float y) {
        if (DEBUG) {
            Log.d(TAG, "touchDown() x=" + x + " y=" + y);
        }

        mView.requestRender();
    }

    public void touchUp(float x, float y) {
        mView.requestRender();
    }

    public void touchMove(float x, float y) {
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

    @Override
    public void enableVertexAttribute(GLESObject object) {
        int userAttribLocation = -1;

        if (mVersion == Version.GLES_20) {
            userAttribLocation = mSizeLocation;
        } else {
            userAttribLocation = USER_ATTRIB_LOCATION;
        }

        GLES20.glVertexAttribPointer(userAttribLocation,
                NUM_ELEMENT_OF_SIZE, GLES20.GL_FLOAT, false,
                NUM_ELEMENT_OF_SIZE * GLESConfig.FLOAT_SIZE_BYTES,
                mSizeBuffer);
        GLES20.glEnableVertexAttribArray(userAttribLocation);
    }

    @Override
    public void disableVertexAttribute(GLESObject object) {
        int userAttribLocation = -1;

        if (mVersion == Version.GLES_20) {
            userAttribLocation = mSizeLocation;
        } else {
            userAttribLocation = USER_ATTRIB_LOCATION;
        }

        GLES20.glDisableVertexAttribArray(userAttribLocation);
    }
}
