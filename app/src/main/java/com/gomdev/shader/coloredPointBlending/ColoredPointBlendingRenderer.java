package com.gomdev.shader.coloredPointBlending;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.opengl.GLES20;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESConfig;
import com.gomdev.gles.GLESConfig.Version;
import com.gomdev.gles.GLESContext;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESMeshUtils;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESObject;
import com.gomdev.gles.GLESParticle;
import com.gomdev.gles.GLESRect;
import com.gomdev.gles.GLESRenderer;
import com.gomdev.gles.GLESRendererListener;
import com.gomdev.gles.GLESSceneManager;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESShaderConstant;
import com.gomdev.gles.GLESTransform;
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVertexInfo;
import com.gomdev.gles.GLESVertexInfo.PrimitiveMode;
import com.gomdev.gles.GLESVertexInfo.RenderType;
import com.gomdev.shader.R;
import com.gomdev.shader.SampleRenderer;
import com.gomdev.shader.ShaderUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

public class ColoredPointBlendingRenderer extends SampleRenderer implements
        GLESRendererListener {
    private static final String CLASS = "ColoredPointBlendingRenderer";
    private static final String TAG = ColoredPointBlendingConfig.TAG + "_"
            + CLASS;
    private static final boolean DEBUG = ColoredPointBlendingConfig.DEBUG;

    private static final int NUM_OF_PARTICLES = 20;

    private static final int NUM_ELEMENT_OF_POSITION = 3;
    private static final int NUM_ELEMENT_OF_COLOR = 4;
    private static final int NUM_ELEMENT_OF_POINTSIZE = 1;

    private GLESRenderer mRenderer = null;
    private GLESSceneManager mSM = null;
    private GLESObject mParticleObject;
    private GLESShader mParticleShader;
    private GLESObject mCubeObject;
    private GLESShader mCubeShader;
    private Version mVersion;
    private GLESVertexInfo mVertexInfo = null;
    private Random mRandom = new Random();

    private GLESGLState mState = null;

    private int mPointSizeLocation = -1;

    private float mScreenRatio = 0f;

    private FloatBuffer mPointSizeBuffer = null;

    private LinearLayout mLayout = null;
    private TextView mBlendingInfoView = null;

    private boolean mIsParticlesSorted = true;

    private ArrayList<GLESParticle> mParticles = new ArrayList<GLESParticle>();
    private LinkedList<GLESParticle> mSortedParticles = new LinkedList<GLESParticle>();

    public ColoredPointBlendingRenderer(Context context) {
        super(context);

        mVersion = GLESContext.getInstance().getVersion();

        mRenderer = GLESRenderer.createRenderer();
        mSM = GLESSceneManager.createSceneManager();
        GLESNode root = mSM.createRootNode("Root");

        mParticleObject = mSM.createObject("Particle");
        mCubeObject = mSM.createObject("Cube");

        mState = new GLESGLState();
        mState.setCullFaceState(true);
        mState.setCullFace(GLES20.GL_BACK);
        mState.setDepthState(true);
        mState.setBlendState(false);
        mState.setBlendFunc(
                GLES20.GL_SRC_ALPHA,
                GLES20.GL_ONE_MINUS_SRC_ALPHA);
        mParticleObject.setGLState(mState);

        GLESGLState state = new GLESGLState();
        state.setCullFaceState(true);
        state.setCullFace(GLES20.GL_BACK);
        state.setDepthState(true);
        state.setDepthFunc(GLES20.GL_LEQUAL);
        state.setBlendState(true);
        state.setBlendFunc(
                GLES20.GL_SRC_ALPHA,
                GLES20.GL_ONE_MINUS_SRC_ALPHA);
        mCubeObject.setGLState(state);

        mIsParticlesSorted = false;

        root.addChild(mCubeObject);
        root.addChild(mParticleObject);

        mRenderer.setListener(this);

        setupBlendingInfoView();
    }

    private void setupBlendingInfoView() {
        mLayout = (LinearLayout) ((Activity) mContext)
                .findViewById(R.id.layout_info);
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);

        mBlendingInfoView = new TextView(mContext);
        mLayout.addView(mBlendingInfoView, params);

        StringBuilder builder = new StringBuilder();
        builder.append("DepthTest : enable\n");
        builder.append("Sorting : false\n");
        builder.append("Blending : disable\n");

        int margin = mContext.getResources().getDimensionPixelSize(R.dimen.textview_margin);
        mBlendingInfoView.setPadding(margin, margin, margin, margin);
        mBlendingInfoView.setText(builder.toString());
        mBlendingInfoView.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        mBlendingInfoView.setTextColor(Color.WHITE);
        mBlendingInfoView.setVisibility(View.VISIBLE);
    }

    public void destroy() {
        mParticleObject = null;
        mCubeObject = null;
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

        int positionIndex = mParticleShader.getPositionAttribIndex();
        FloatBuffer positionBuffer = (FloatBuffer) mVertexInfo
                .getBuffer(positionIndex);
        FloatBuffer colorBuffer = (FloatBuffer) mVertexInfo
                .getBuffer(mParticleShader.getColorAttribIndex());

        if (mIsParticlesSorted == true) {
            for (int i = 0; i < NUM_OF_PARTICLES; i++) {
                GLESParticle particle = mSortedParticles.get(i);
                positionBuffer
                        .put(i * NUM_ELEMENT_OF_POSITION + 0, particle.mX);
                positionBuffer
                        .put(i * NUM_ELEMENT_OF_POSITION + 1, particle.mY);
                positionBuffer
                        .put(i * NUM_ELEMENT_OF_POSITION + 2, particle.mZ);

                colorBuffer.put(i * NUM_ELEMENT_OF_COLOR + 0, particle.mR);
                colorBuffer.put(i * NUM_ELEMENT_OF_COLOR + 1, particle.mG);
                colorBuffer.put(i * NUM_ELEMENT_OF_COLOR + 2, particle.mB);
                colorBuffer.put(i * NUM_ELEMENT_OF_COLOR + 3, particle.mA);

                mPointSizeBuffer.put(i, particle.mSize);
            }
        } else {
            for (int i = 0; i < NUM_OF_PARTICLES; i++) {
                GLESParticle particle = mParticles.get(i);
                positionBuffer
                        .put(i * NUM_ELEMENT_OF_POSITION + 0, particle.mX);
                positionBuffer
                        .put(i * NUM_ELEMENT_OF_POSITION + 1, particle.mY);
                positionBuffer
                        .put(i * NUM_ELEMENT_OF_POSITION + 2, particle.mZ);

                colorBuffer.put(i * NUM_ELEMENT_OF_COLOR + 0, particle.mR);
                colorBuffer.put(i * NUM_ELEMENT_OF_COLOR + 1, particle.mG);
                colorBuffer.put(i * NUM_ELEMENT_OF_COLOR + 2, particle.mB);
                colorBuffer.put(i * NUM_ELEMENT_OF_COLOR + 3, particle.mA);

                mPointSizeBuffer.put(i, particle.mSize);
            }
        }

        GLESTransform transform = mCubeObject.getTransform();
        transform.setIdentity();
        transform.setRotate(30f, 1f, 0f, 0f);
        transform.rotate(45f, 0f, 1f, 0f);
    }

    @Override
    protected void onSurfaceChanged(int width, int height) {
        mRenderer.reset();

        mScreenRatio = (float) width / height;

        GLESCamera camera = setupCamera(width, height);

        mParticleObject.setCamera(camera);
        mCubeObject.setCamera(camera);

        mVertexInfo = createParticles(width, height);
        mParticleObject.setVertexInfo(mVertexInfo, false, false);

        GLESVertexInfo vertexInfo = GLESMeshUtils.createCube(mCubeShader,
                mScreenRatio * 0.5f, false, false, true);
        mCubeObject.setVertexInfo(vertexInfo, false, false);
    }

    private GLESCamera setupCamera(int width, int height) {
        GLESCamera camera = new GLESCamera();

        float fovy = 30f;
        float eyeZ = 1f / (float) Math.tan(Math.toRadians(fovy * 0.5));

        camera.setLookAt(0f, 0f, eyeZ, 0f, 0f, 0f, 0f, 1f, 0f);

        camera.setFrustum(fovy, mScreenRatio, eyeZ * 0.1f, 400f);

        camera.setViewport(new GLESRect(0, 0, width, height));

        return camera;
    }

    private GLESVertexInfo createParticles(float width, float height) {
        mVertexInfo = new GLESVertexInfo();

        float[] position = new float[NUM_OF_PARTICLES * NUM_ELEMENT_OF_POSITION];
        float[] color = new float[NUM_OF_PARTICLES * NUM_ELEMENT_OF_COLOR];
        float[] size = new float[NUM_OF_PARTICLES * NUM_ELEMENT_OF_POINTSIZE];

        float posX = 0f;
        float posY = 0f;
        float posZ = 0f;

        float r = 1f;
        float g = 1f;
        float b = 1f;
        float a = 1f;

        float basePointSize = GLESUtils.getPixelFromDpi(mContext, 50);
        float particleSize = 0f;

        mParticles.clear();
        mSortedParticles.clear();
        for (int i = 0; i < NUM_OF_PARTICLES; i++) {
            posX = (mRandom.nextFloat() * 2f - 1f) * mScreenRatio;
            posY = mRandom.nextFloat() * 2f - 1f;
            posZ = -mRandom.nextFloat() * 0.01f;

            GLESParticle particle = new GLESParticle(posX, posY, posZ);

            position[i * NUM_ELEMENT_OF_POSITION + 0] = posX;
            position[i * NUM_ELEMENT_OF_POSITION + 1] = posY;
            position[i * NUM_ELEMENT_OF_POSITION + 2] = posZ;

            r = mRandom.nextFloat();
            g = mRandom.nextFloat();
            b = mRandom.nextFloat();
            a = mRandom.nextFloat() * 0.5f + 0.3f;

            color[i * NUM_ELEMENT_OF_COLOR + 0] = r;
            color[i * NUM_ELEMENT_OF_COLOR + 1] = g;
            color[i * NUM_ELEMENT_OF_COLOR + 2] = b;
            color[i * NUM_ELEMENT_OF_COLOR + 3] = a;

            particle.mR = r;
            particle.mG = g;
            particle.mB = b;
            particle.mA = a;

            particleSize = basePointSize + mRandom.nextFloat() * basePointSize
                    * 2f;
            size[i * NUM_ELEMENT_OF_POINTSIZE] = particleSize;

            particle.mSize = particleSize;

            mParticles.add(particle);
            mSortedParticles.add(particle);
        }

        int positionIndex = mParticleShader.getPositionAttribIndex();
        mVertexInfo.setBuffer(positionIndex, position,
                NUM_ELEMENT_OF_POSITION);

        mVertexInfo.setBuffer(mParticleShader.getColorAttribIndex(), color,
                NUM_ELEMENT_OF_COLOR);

        mPointSizeBuffer = GLESUtils.makeFloatBuffer(size);

        mVertexInfo.setPrimitiveMode(PrimitiveMode.POINTS);
        mVertexInfo.setRenderType(RenderType.DRAW_ARRAYS);

        quicksort(mSortedParticles, 0, mSortedParticles.size() - 1);

        return mVertexInfo;
    }

    private void quicksort(LinkedList<GLESParticle> particles, int left,
                           int right) {
        if (left < right) {
            int p = partition(particles, left, right);
            quicksort(particles, left, p - 1);
            quicksort(particles, p + 1, right);
        }
    }

    private int partition(LinkedList<GLESParticle> particles, int left,
                          int right) {
        int pivotIndex = right;
        GLESParticle pivotParticle = particles.get(pivotIndex);
        float pivotValue = pivotParticle.mZ;
        swap(particles, pivotIndex, right);
        int storeIndex = left;

        GLESParticle particle = null;
        for (int i = left; i < right; i++) {
            particle = particles.get(i);
            if (particle.mZ < pivotValue) {
                swap(particles, i, storeIndex);
                storeIndex++;
            }
        }
        swap(particles, storeIndex, right);
        return storeIndex;
    }

    private void swap(LinkedList<GLESParticle> particles, int indexA, int indexB) {
        if (indexA == indexB) {
            return;
        }

        if (indexA > indexB) {
            int temp = indexA;
            indexA = indexB;
            indexB = temp;
        }

        GLESParticle particleA = particles.get(indexA);
        GLESParticle particleB = particles.get(indexB);
        particles.remove(indexA);
        particles.remove(indexB - 1);
        particles.add(indexA, particleB);
        particles.add(indexB, particleA);
    }

    @Override
    protected void onSurfaceCreated() {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCreated()");
        }

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        mParticleObject.setShader(mParticleShader);
        mCubeObject.setShader(mCubeShader);
    }

    @Override
    protected boolean createShader() {
        if (DEBUG) {
            Log.d(TAG, "createShader()");
        }

        mParticleShader = new GLESShader(mContext);

        String vsSource = ShaderUtils.getShaderSource(mContext, 0);
        String fsSource = ShaderUtils.getShaderSource(mContext, 1);

        mParticleShader.setShaderSource(vsSource, fsSource);
        if (mParticleShader.load() == false) {
            return false;
        }

        if (mVersion == Version.GLES_20) {
            String attribName = GLESShaderConstant.ATTRIB_POSITION;
            mParticleShader.setPositionAttribIndex(attribName);

            attribName = GLESShaderConstant.ATTRIB_COLOR;
            mParticleShader.setColorAttribIndex(attribName);

            mPointSizeLocation = GLES20.glGetAttribLocation(
                    mParticleShader.getProgram(),
                    "aPointSize");
        }

        mCubeShader = new GLESShader(mContext);

        vsSource = ShaderUtils.getShaderSource(mContext, 2);
        fsSource = ShaderUtils.getShaderSource(mContext, 3);

        mCubeShader.setShaderSource(vsSource, fsSource);
        if (mCubeShader.load() == false) {
            return false;
        }

        if (mVersion == Version.GLES_20) {
            String attribName = GLESShaderConstant.ATTRIB_POSITION;
            mCubeShader.setPositionAttribIndex(attribName);

            attribName = GLESShaderConstant.ATTRIB_COLOR;
            mCubeShader.setColorAttribIndex(attribName);
        }

        return true;
    }

    public void onResume() {
        mBlendingInfoView.setVisibility(View.VISIBLE);
    }

    public void onPause() {
        mBlendingInfoView.setVisibility(View.GONE);
    }

    private int mIndex = 0;

    public void touchDown(float x, float y) {
        if (DEBUG) {
            Log.d(TAG, "touchDown() x=" + x + " y=" + y);
        }

        StringBuilder builder = null;

        int index = mIndex % 4;

        switch (index) {
            case 0:
                mState.setDepthState(true);
                mState.setBlendState(true);
                mState.setBlendFunc(
                        GLES20.GL_SRC_ALPHA,
                        GLES20.GL_ONE_MINUS_SRC_ALPHA);
                mIsParticlesSorted = false;

                builder = new StringBuilder();
                builder.append("DepthTest : enable\n");
                builder.append("Sorting : false\n");
                builder.append("Blending : enable\n");
                builder.append("BlendFunc : \n");
                builder.append("\tSrcFactor : GL_SRC_ALPHA\n");
                builder.append("\tDstFactor : GL_ONE_MINUS_SRC_ALPHA\n");

                mBlendingInfoView.setText(builder.toString());
                break;
            case 1:
                mState.setDepthState(false);
                mState.setBlendState(true);
                mState.setBlendFunc(
                        GLES20.GL_SRC_ALPHA,
                        GLES20.GL_ONE_MINUS_SRC_ALPHA);
                mIsParticlesSorted = false;

                builder = new StringBuilder();
                builder.append("DepthTest : disable\n");
                builder.append("Sorting : false\n");
                builder.append("Blending : enable\n");
                builder.append("BlendFunc : \n");
                builder.append("\tSrcFactor : GL_SRC_ALPHA\n");
                builder.append("\tDstFactor : GL_ONE_MINUS_SRC_ALPHA\n");

                mBlendingInfoView.setText(builder.toString());
                break;
            case 2:
                mState.setDepthState(true);
                mState.setBlendState(true);
                mState.setBlendFunc(
                        GLES20.GL_SRC_ALPHA,
                        GLES20.GL_ONE_MINUS_SRC_ALPHA);
                mIsParticlesSorted = true;

                builder = new StringBuilder();
                builder.append("DepthTest : enable\n");
                builder.append("Sorting : true\n");
                builder.append("Blending : enable\n");
                builder.append("BlendFunc : \n");
                builder.append("\tSrcFactor : GL_SRC_ALPHA\n");
                builder.append("\tDstFactor : GL_ONE_MINUS_SRC_ALPHA\n");

                mBlendingInfoView.setText(builder.toString());
                break;
            case 3:
                mState.setDepthState(true);
                mState.setBlendState(false);
                mIsParticlesSorted = false;

                builder = new StringBuilder();
                builder.append("DepthTest : enable\n");
                builder.append("Sorting : false\n");
                builder.append("Blending : disable\n");

                mBlendingInfoView.setText(builder.toString());
                break;
            default:

        }

        mIndex++;

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
    public void setupVAO(GLESShader shader, GLESVertexInfo vertexInfo) {

    }

    @Override
    public void enableVertexAttribute(GLESObject object) {
        if (object == mCubeObject) {
            return;
        }

        int userAttribLocation = -1;

        if (mVersion == Version.GLES_20) {
            userAttribLocation = mPointSizeLocation;
        } else {
            userAttribLocation = GLESConfig.POINTSIZE_LOCATION;
        }

        GLES20.glVertexAttribPointer(userAttribLocation,
                NUM_ELEMENT_OF_POINTSIZE, GLES20.GL_FLOAT, false,
                NUM_ELEMENT_OF_POINTSIZE * GLESConfig.FLOAT_SIZE_BYTES,
                mPointSizeBuffer);
        GLES20.glEnableVertexAttribArray(userAttribLocation);
    }

    @Override
    public void disableVertexAttribute(GLESObject object) {
        if (object == mCubeObject) {
            return;
        }

        int userAttribLocation = -1;

        if (mVersion == Version.GLES_20) {
            userAttribLocation = mPointSizeLocation;
        } else {
            userAttribLocation = GLESConfig.POINTSIZE_LOCATION;
        }

        GLES20.glDisableVertexAttribArray(userAttribLocation);
    }
}
