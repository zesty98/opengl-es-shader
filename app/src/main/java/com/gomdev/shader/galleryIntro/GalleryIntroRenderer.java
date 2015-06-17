package com.gomdev.shader.galleryIntro;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.gomdev.gles.GLESAnimator;
import com.gomdev.gles.GLESAnimatorCallback;
import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESConfig.Version;
import com.gomdev.gles.GLESContext;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESObject;
import com.gomdev.gles.GLESRect;
import com.gomdev.gles.GLESRenderer;
import com.gomdev.gles.GLESSceneManager;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESShaderConstant;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESVector3;
import com.gomdev.gles.GLESVertexInfo;
import com.gomdev.shader.R;
import com.gomdev.shader.SampleRenderer;
import com.gomdev.shader.ShaderUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Random;

public class GalleryIntroRenderer extends SampleRenderer {
    private static final String CLASS = "GalleryIntroRenderer";
    private static final String TAG = GalleryIntroConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = GalleryIntroConfig.DEBUG;

    private static final int NUM_OF_POINT_IN_GOM_WIDTH = 100;
    private static final int NUM_ELEMENT_OF_POSITION = 3;
    private static final int NUM_ELEMENT_OF_TEXCOORD = 2;

    private class Particle {
        float mX = 0f;
        float mY = 0f;
        float mZ = 0f;

        float mInitX = 0f;
        float mInitY = 0f;
        float mInitZ = 0f;

        float mVelocity = 1f;

        Particle(float x, float y, float z) {
            mX = x;
            mY = y;
            mZ = z;
        }
    }

    private class ParticleSet {
        ArrayList<Particle> mParticles = new ArrayList<>();

        GLESShader mShader = null;

        int mNumOfPointsInWidth = 0;
        int mNumOfPointsInHeight = 0;
        int mNumOfPoints = 0;

        float mX = 0f;
        float mY = 0f;

        float mWidth = 0f;
        float mHeight = 0f;

        float mPointSize = 0f;
    }

    private GLESRenderer mRenderer = null;
    private final GLESSceneManager mSM;
    private final GLESNode mRoot;
    private final GLESObject mIntroObject;

    private Version mVersion;

    private Random mRandom = new Random();
    private Interpolator mInterpolator = new AccelerateDecelerateInterpolator();

    private GLESShader mShader = null;

    private GLESAnimator mAnimator = null;

    private int mWidth = 0;
    private int mHeight = 0;

    private ParticleSet mParticleSet = new ParticleSet();

    private boolean mIsOnAnimation = false;

    public GalleryIntroRenderer(Context context) {
        super(context);

        if (DEBUG) {
            Log.d(TAG, "IntroRenderer()");
        }

        mVersion = GLESContext.getInstance().getVersion();

        mRenderer = GLESRenderer.createRenderer();
        mSM = GLESSceneManager.createSceneManager();
        mRoot = mSM.createRootNode("root");

        GLESGLState state = new GLESGLState();
        state.setCullFaceState(true);
        state.setCullFace(GLES20.GL_BACK);
        state.setDepthState(false);
        state.setBlendState(true);
        state.setBlendFuncSeperate(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA,
                GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        mIntroObject = mSM.createObject("Intro");
        mIntroObject.setGLState(state);
        mRoot.addChild(mIntroObject);

        mAnimator = new GLESAnimator(mAnimatorCB);
        mAnimator.setValues(0f, 1f);
        mAnimator.setDuration(0L, GalleryIntroConfig.INTRO_ANIMATION_DURATION);
    }

    public void destroy() {
    }

    @Override
    protected void onDrawFrame() {
        super.updateFPS();

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (mAnimator.doAnimation() == true) {
            mView.requestRender();
        }

        mRenderer.updateScene(mSM);
        mRenderer.drawScene(mSM);
    }

    @Override
    protected void onSurfaceChanged(int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceChanged() width=" + width + " height=" + height);
        }

        mWidth = width;
        mHeight = height;

        mRenderer.reset();

        GLESCamera camera = setupCamera(width, height);
        mIntroObject.setCamera(camera);

        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.intro);
        float imgWidth = bitmap.getWidth();
        float imgHeight = bitmap.getHeight();

        GLESTexture gomTexture = new GLESTexture.Builder(GLES20.GL_TEXTURE_2D, (int) imgWidth, (int) imgHeight)
                .load(bitmap);
        mIntroObject.setTexture(gomTexture);
        bitmap.recycle();

        {
            float startX = -imgWidth * 0.5f;
            float startY = imgHeight * 0.7f;

            float pointSize = Math.round(imgWidth / NUM_OF_POINT_IN_GOM_WIDTH);
            int numOfPointsInWidth = (int) Math.ceil(imgWidth / pointSize);
            float temp = numOfPointsInWidth * imgHeight / imgWidth;
            int numOfPointsInHeight = (int) Math.ceil(temp);
            int numOfPoints = numOfPointsInWidth * numOfPointsInHeight;

            mParticleSet.mX = startX;
            mParticleSet.mY = startY;
            mParticleSet.mWidth = imgWidth;
            mParticleSet.mHeight = imgHeight;
            mParticleSet.mNumOfPointsInWidth = numOfPointsInWidth;
            mParticleSet.mNumOfPointsInHeight = numOfPointsInHeight;
            mParticleSet.mNumOfPoints = numOfPoints;
            mParticleSet.mPointSize = pointSize;
            mParticleSet.mShader = mShader;

            GLESVertexInfo gomVertexInfo = createParticles(mParticleSet);
            mIntroObject.setVertexInfo(gomVertexInfo, false, false);

            setUniforms(mParticleSet);
        }

        mAnimator.start();

        mIsOnAnimation = true;
    }

    private void setUniforms(ParticleSet particleSet) {
        particleSet.mShader.useProgram();

        int location = GLES20.glGetUniformLocation(particleSet.mShader.getProgram(),
                "uPointSize");
        GLES20.glUniform1f(location, particleSet.mPointSize);

        location = GLES20.glGetUniformLocation(particleSet.mShader.getProgram(),
                "uPointSizeInTexCoord");
        float texCoordFactorX = particleSet.mPointSize / particleSet.mWidth;
        float texCoordFactorY = particleSet.mPointSize / particleSet.mHeight;
        GLES20.glUniform2f(location, texCoordFactorX, texCoordFactorY);
    }


    private GLESVertexInfo createParticles(ParticleSet particleSet) {
        particleSet.mParticles.clear();

        float[] position = new float[particleSet.mNumOfPoints * NUM_ELEMENT_OF_POSITION];
        float[] texCoord = new float[particleSet.mNumOfPoints * NUM_ELEMENT_OF_TEXCOORD];

        float posX = 0f;
        float posY = 0f;
        float posZ = 0f;

        int posIndexOffsetX = 0;
        int posIndexOffsetY = 0;
        int texIndexOffsetX = 0;
        int texIndexOffsetY = 0;

        float halfPointSize = particleSet.mPointSize * 0.5f;

        for (int i = 0; i < particleSet.mNumOfPointsInHeight; i++) {
            posIndexOffsetY = i * NUM_ELEMENT_OF_POSITION * particleSet.mNumOfPointsInWidth;
            texIndexOffsetY = i * NUM_ELEMENT_OF_TEXCOORD * particleSet.mNumOfPointsInWidth;

            posX = particleSet.mX + halfPointSize;
            posY = particleSet.mY - i * particleSet.mPointSize - halfPointSize;

            for (int j = 0; j < particleSet.mNumOfPointsInWidth; j++) {
                posIndexOffsetX = j * NUM_ELEMENT_OF_POSITION;
                texIndexOffsetX = j * NUM_ELEMENT_OF_TEXCOORD;

                posX += particleSet.mPointSize;

                position[posIndexOffsetY + posIndexOffsetX + 0] = posX;
                position[posIndexOffsetY + posIndexOffsetX + 1] = posY;
                position[posIndexOffsetY + posIndexOffsetX + 2] = posZ;

                Particle particle = new Particle(posX, posY, posZ);
                particleSet.mParticles.add(particle);

                boolean rightSide = mRandom.nextBoolean();
                if (rightSide == true) {
                    particle.mInitX = -mWidth - mRandom.nextFloat() * 0.2f * mWidth;
                } else {
                    particle.mInitX = mWidth + mRandom.nextFloat() * 0.2f * mWidth;
                }

                particle.mInitY = (mRandom.nextFloat() - 0.5f) * mHeight;
                particle.mInitZ = 0f;

                particle.mVelocity = 1f + mRandom.nextFloat() * 0.3f;

                texCoord[texIndexOffsetY + texIndexOffsetX + 0] = (j * particleSet.mPointSize + halfPointSize) / particleSet.mWidth;
                texCoord[texIndexOffsetY + texIndexOffsetX + 1] = (i * particleSet.mPointSize + halfPointSize) / particleSet.mHeight;
            }
        }

        GLESVertexInfo vertexInfo = new GLESVertexInfo();

        vertexInfo.setBuffer(particleSet.mShader.getPositionAttribIndex(), position, NUM_ELEMENT_OF_POSITION);
        vertexInfo.setBuffer(particleSet.mShader.getTexCoordAttribIndex(), texCoord, NUM_ELEMENT_OF_TEXCOORD);

        vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.POINTS);
        vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);

        return vertexInfo;
    }

    private GLESCamera setupCamera(int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "setupCamera() width=" + width + " hegiht=" + height);
        }

        GLESCamera camera = new GLESCamera();

        float eyeZ = 0.1f;

        camera.setLookAt(0f, 0f, eyeZ, 0f, 0f, 0f, 0f, 1f, 0f);
        camera.setOrtho(-mWidth * 0.5f, mWidth * 0.5f, -mHeight * 0.5f, mHeight * 0.5f, -1f, 1f);

        camera.setViewport(new GLESRect(0, 0, width, height));

        return camera;
    }

    @Override
    protected void onSurfaceCreated() {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCreated()");
        }

        GLES20.glClearColor(1f, 1f, 1f, 1f);

        mIntroObject.setShader(mShader);
    }

    @Override
    protected boolean createShader() {
        if (DEBUG) {
            Log.d(TAG, "createShader()");
        }

        {
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
        }

        return true;
    }

    public void touchDown(float x, float y) {
        if (DEBUG) {
            Log.d(TAG, "touchDown() x=" + x + " y=" + y);
        }

        if (mIsOnAnimation == false) {
            mAnimator.start();
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

    private final GLESAnimatorCallback mAnimatorCB = new GLESAnimatorCallback() {
        @Override
        public void onAnimation(GLESVector3 current) {
            float normalizedValue = mInterpolator.getInterpolation(current.getX());

            {
                GLESShader shader = mParticleSet.mShader;
                ArrayList<Particle> particles = mParticleSet.mParticles;

                GLESVertexInfo vertexInfo = mIntroObject.getVertexInfo();
                FloatBuffer position = (FloatBuffer) vertexInfo.getBuffer(shader.getPositionAttribIndex());

                float x = 0f;
                float y = 0f;

                float scaledNormalizedValue = 0f;

                int size = particles.size();
                for (int i = 0; i < size; i++) {
                    Particle particle = particles.get(i);

                    scaledNormalizedValue = normalizedValue * particle.mVelocity;

                    if (scaledNormalizedValue > 1f) {
                        scaledNormalizedValue = 1f;
                    }

                    x = particle.mInitX + (particle.mX - particle.mInitX) * scaledNormalizedValue;
                    y = particle.mInitY + (particle.mY - particle.mInitY) * scaledNormalizedValue;

                    position.put(i * NUM_ELEMENT_OF_POSITION + 0, x);
                    position.put(i * NUM_ELEMENT_OF_POSITION + 1, y);
                }
            }
        }

        @Override
        public void onCancel() {

        }

        @Override
        public void onFinished() {
            mIsOnAnimation = false;
        }
    };
}
