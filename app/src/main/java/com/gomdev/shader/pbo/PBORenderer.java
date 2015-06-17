package com.gomdev.shader.pbo;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.os.Build;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gomdev.gles.GLESAnimator;
import com.gomdev.gles.GLESAnimatorCallback;
import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESConfig.Version;
import com.gomdev.gles.GLESContext;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESNodeListener;
import com.gomdev.gles.GLESObject;
import com.gomdev.gles.GLESObjectListener;
import com.gomdev.gles.GLESRect;
import com.gomdev.gles.GLESRenderer;
import com.gomdev.gles.GLESRendererListener;
import com.gomdev.gles.GLESSceneManager;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESShaderConstant;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESTransform;
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVector3;
import com.gomdev.gles.GLESVertexInfo;
import com.gomdev.shader.R;
import com.gomdev.shader.SampleRenderer;
import com.gomdev.shader.ShaderUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class PBORenderer extends SampleRenderer implements GLESRendererListener {
    private static final String CLASS = "PBORenderer";
    private static final String TAG = PBOConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = PBOConfig.DEBUG;

    private static final long SCALE_ANIMATION_DURATION = 500L;
    private static final int GRID_SIZE = 256;
    private static final float LINE_WIDTH_IN_DP = 5f;   // dp

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    private GLESRenderer mRenderer = null;
    private GLESSceneManager mSM = null;

    private GLESNode mScreenNode = null;
    private GLESNode mImageNode = null;
    private PBOObject[] mObjects = null;
    private GLESObject mLineObject = null;
    private GLESObject mScreenObject = null;

    private GLESShader mTextureShader = null;
    private GLESShader mColorShader = null;
    private Version mVersion;

    private float mScreenRatio = 0f;
    private int mWidth = 0;
    private int mHeight = 0;

    private BitmapRegionDecoder mDecoder = null;

    private float mBitmapRatio = 0f;
    private int mBitmapWidth = 0;
    private int mBitmapHeight = 0;

    private int mNumOfObjects = 0;
    private int mNumOfObjectInWidth = 0;
    private int mNumOfObjectInHeight = 0;

    private float mLineWidth = 0f;

    private int[] mPBOIDs = null;

    private TextView mInfoView = null;

    // touch event
    private GestureDetector mGestureDetector = null;
    private GLESAnimator mAnimator = null;

    private boolean mIsDown = false;

    private float mPrevX = 0f;
    private float mPrevY = 0f;

    private float mTranslateX = 0f;
    private float mTranslateY = 0f;

    private float mCurrentTranslateX = 0f;
    private float mCurrentTranslateY = 0f;

    private boolean mIsOnAnimation = false;
    private boolean mIsDownScale = false;
    private float mDownScale = 1f;
    private float mCurrentScale = 1f;

    private float mSmallScreenX = 0f;
    private float mSmallScreenY = 0f;

    private float mCurrentScreenX = 0f;
    private float mCurrentScreenY = 0f;

    private ThreadPoolExecutor mExecutor = null;
    private CompletionService<BlockInfo> mCompletionService = null;

    class BlockInfo {
        final int mIndex;
        final Bitmap mBitmap;

        BlockInfo(int index, Bitmap bitmap) {
            mIndex = index;
            mBitmap = bitmap;
        }
    }

    public PBORenderer(Context context) {
        super(context);

        if (DEBUG) {
            Log.d(TAG, "PBORenderer() cpu count=" + CPU_COUNT);
        }

        mVersion = GLESContext.getInstance().getVersion();

        InputStream is = mContext.getResources().openRawResource(R.raw.large_image_2340x4160);

        try {
            mDecoder = BitmapRegionDecoder.newInstance(is, false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mBitmapWidth = mDecoder.getWidth();
        mBitmapHeight = mDecoder.getHeight();
        mNumOfObjectInWidth = (int) Math.ceil((float) mBitmapWidth / GRID_SIZE);
        mNumOfObjectInHeight = (int) Math.ceil((float) mBitmapHeight / GRID_SIZE);
        mNumOfObjects = mNumOfObjectInWidth * mNumOfObjectInHeight;

        mLineWidth = GLESUtils.getPixelFromDpi(mContext, LINE_WIDTH_IN_DP);

        mRenderer = GLESRenderer.createRenderer();

        createScene();
        setupInfoView();

        mGestureDetector = new GestureDetector(mContext, mGestureListener);
        mAnimator = new GLESAnimator(mAnimatorCB);
        mAnimator.setValues(0f, 1f);
        mAnimator.setDuration(0L, SCALE_ANIMATION_DURATION);

        mExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(CPU_COUNT);
    }

    private void createScene() {
        mSM = GLESSceneManager.createSceneManager();

        mRenderer.setListener(this);

        GLESNode root = mSM.createRootNode("Root");

        mImageNode = mSM.createNode("ImageNode");
        root.addChild(mImageNode);
        mImageNode.setListener(mImageNodeListener);

        mScreenNode = mSM.createNode("Node");
        root.addChild(mScreenNode);
        mScreenNode.setListener(mScreenNodeListener);

        mObjects = new PBOObject[mNumOfObjects];

        GLESGLState state = new GLESGLState();
        state.setCullFaceState(true);
        state.setCullFace(GLES20.GL_BACK);
        state.setDepthState(false);

        for (int i = 0; i < mNumOfObjects; i++) {
            mObjects[i] = new PBOObject();
            mObjects[i].setGLState(state);
            mImageNode.addChild(mObjects[i]);

            mObjects[i].setTextureMapped(false);
        }

        mLineObject = mSM.createObject("Line");
        mLineObject.setGLState(state);
        mLineObject.setListener(mLineObjectListener);
        mImageNode.addChild(mLineObject);

        mScreenObject = mSM.createObject("Screen");
        mScreenObject.setGLState(state);
        mScreenObject.setListener(mScreenObjectListener);
        mScreenNode.addChild(mScreenObject);
    }

    private void setupInfoView() {
        LinearLayout mLayout = (LinearLayout) ((Activity) mContext)
                .findViewById(R.id.layout_info);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        mInfoView = new TextView(mContext);
        mLayout.addView(mInfoView, params);

        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;

        StringBuilder builder = new StringBuilder();
        builder.append("Red grid line : one object\n");
        builder.append("Red thick line : screen\n");
        builder.append("Image size : " + mBitmapWidth + "x" + mBitmapHeight + "\n");
        builder.append("Screen size : " + width + "x" + height + "\n\n");
        builder.append("Double tap : scale up or down");

        mInfoView.setText(builder.toString());

        int margin = mContext.getResources().getDimensionPixelSize(R.dimen.textview_margin);
        mInfoView.setPadding(margin, margin, margin, margin);
        mInfoView.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        mInfoView.setTextColor(Color.WHITE);
        mInfoView.setVisibility(View.VISIBLE);
    }

    public void destroy() {
        mObjects = null;
        mLineObject = null;
        mScreenObject = null;
    }

    @Override
    protected void onDrawFrame() {
        super.updateFPS();

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (mAnimator.doAnimation() == true) {
            mView.requestRender();
        }

        if (mVersion == Version.GLES_20) {
            updateTexture();
        } else {
            updatePBO();
        }

        mRenderer.updateScene(mSM);
        mRenderer.drawScene(mSM);
    }

    private void updateTexture() {
        int numOfUpdatedTexture = 0;

        mCompletionService = new ExecutorCompletionService<>(mExecutor);

        for (int i = 0; i < mNumOfObjects; i++) {
            PBOObject object = mObjects[i];

            int width = object.getWidth();
            int height = object.getHeight();

            float left = object.getX() + mTranslateX;
            float right = left + width;
            float top = object.getY() + mTranslateY;
            float bottom = top - height;

            boolean isInScreen = isInScreen(left, right, bottom, top);

            if (isInScreen == true && object.isTextureMapped() == false) {
                int imageX = object.getImageX();
                int imageY = object.getmImageY();
                numOfUpdatedTexture++;

                final Rect rect = new Rect(imageX, imageY, imageX + width, imageY + height);
                final int index = i;

                mCompletionService.submit(new Callable<BlockInfo>() {
                    @Override
                    public BlockInfo call() throws Exception {
                        return decodeRegion(index, rect);
                    }
                });
            } else if (isInScreen == false && object.isTextureMapped() == true) {
                GLESTexture texture = object.getTexture();
                if (texture != null) {
                    texture.destroy();
                }

                object.setTexture(null);
                object.setTextureMapped(false);
            }
        }

        for (int i = 0; i < numOfUpdatedTexture; i++) {
            Future<BlockInfo> f = null;
            try {
                f = mCompletionService.take();
                BlockInfo blockInfo = f.get();

                Bitmap bitmap = blockInfo.mBitmap;
                int index = blockInfo.mIndex;

                GLESTexture.Builder builder = new GLESTexture.Builder(
                        GLES20.GL_TEXTURE_2D, bitmap.getWidth(), bitmap.getHeight())
                        .setWrapMode(GLES20.GL_CLAMP_TO_EDGE)
                        .setFilter(GLES20.GL_NEAREST, GLES20.GL_NEAREST);
                GLESTexture texture = builder.load(bitmap);
                bitmap.recycle();
                bitmap = null;

                mObjects[index].setTexture(texture);
                mObjects[index].setTextureMapped(true);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
//                throw launderThrowable(e.getCause());
                e.printStackTrace();
            }

        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void updatePBO() {
        int numOfUpdatedTexture = 0;

        mCompletionService = new ExecutorCompletionService<>(mExecutor);

        for (int i = 0; i < mNumOfObjects; i++) {
            PBOObject object = mObjects[i];

            int width = object.getWidth();
            int height = object.getHeight();

            float left = object.getX() + mTranslateX;
            float right = left + width;
            float top = object.getY() + mTranslateY;
            float bottom = top - height;

            boolean isInScreen = isInScreen(left, right, bottom, top);

            if (isInScreen == true && object.isTextureMapped() == false) {
                numOfUpdatedTexture++;

                int imageX = object.getImageX();
                int imageY = object.getmImageY();

                final Rect rect = new Rect(imageX, imageY, imageX + width, imageY + height);
                final int index = i;

                mCompletionService.submit(new Callable<BlockInfo>() {
                    @Override
                    public BlockInfo call() throws Exception {
                        return decodeRegion(index, rect);
                    }
                });
            } else if (isInScreen == false && object.isTextureMapped() == true) {
                PBOUtils.destroyTexture(object);
                object.setTextureMapped(false);
            }
        }

        for (int i = 0; i < numOfUpdatedTexture; i++) {
            Future<BlockInfo> f = null;
            try {
                f = mCompletionService.take();
                BlockInfo blockInfo = f.get();

                Bitmap bitmap = blockInfo.mBitmap;
                int index = blockInfo.mIndex;

                int width = bitmap.getWidth();
                int height = bitmap.getHeight();

                PBOObject object = mObjects[index];

                PBOUtils.createTexture(object);

                GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, mPBOIDs[index]);

                int byteSize = width * height * 4;
                mTextureShader.mapBufferRange(byteSize);
                mTextureShader.uploadBuffer(bitmap, width, 0, 0, width, height);
                mTextureShader.unmapBuffer();
                bitmap.recycle();

                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, object.getTextureID());
                mTextureShader.texSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, 0);

                object.setTextureMapped(true);

                GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, 0);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
//                throw launderThrowable(e.getCause());
                e.printStackTrace();
            }
        }
    }

    private BlockInfo decodeRegion(int index, Rect rect) {
        Bitmap bitmap = mDecoder.decodeRegion(rect, null);
        BlockInfo blockInfo = new BlockInfo(index, bitmap);

        return blockInfo;
    }

    @Override
    protected void onSurfaceChanged(int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceChanged() width=" + width + " height=" + height);
        }

        mWidth = width;
        mHeight = height;
        mScreenRatio = (float) width / height;

        mBitmapRatio = (float) mBitmapWidth / mBitmapHeight;

        mRenderer.reset();

        GLESCamera camera = setupCamera(width, height);

        int halfWidth = mBitmapWidth / 2;
        int halfHeight = mBitmapHeight / 2;

        int objX = 0;
        int objY = 0;
        int objWidth = 0;
        int objHeight = 0;

        int imageX = 0;
        int imageY = 0;
        Rect rect = new Rect();
        for (int i = 0; i < mNumOfObjectInHeight; i++) {
            objY = halfHeight - GRID_SIZE * i;

            for (int j = 0; j < mNumOfObjectInWidth; j++) {
                int index = i * mNumOfObjectInWidth + j;

                mObjects[index].setCamera(camera);

                objX = -halfWidth + GRID_SIZE * j;

                imageX = halfWidth + objX;
                objWidth = GRID_SIZE;
                if (imageX + objWidth > mBitmapWidth) {
                    objWidth = mBitmapWidth - imageX;
                }

                imageY = halfHeight - objY;
                objHeight = GRID_SIZE;
                if (imageY + objHeight > mBitmapHeight) {
                    objHeight = mBitmapHeight - imageY;
                }

                mObjects[index].setPosition(objX, objY);
                mObjects[index].setImagePosition(imageX, imageY);
                mObjects[index].setWidth(objWidth);
                mObjects[index].setHeight(objHeight);

                GLESVertexInfo vertexInfo = ShaderUtils.createPlane(mTextureShader,
                        objX, objY,
                        objWidth, objHeight,
                        false, true, false, false,
                        1f, 1f, 1f);
                mObjects[index].setVertexInfo(vertexInfo, true, false);

                if (mVersion == Version.GLES_20) {
                    if (isInScreen(mObjects[index]) == true) {
                        rect.set(imageX, imageY, imageX + objWidth, imageY + objHeight);
                        Bitmap bitmap = mDecoder.decodeRegion(rect, null);
                        GLESTexture.Builder builder = new GLESTexture.Builder(
                                GLES20.GL_TEXTURE_2D, bitmap.getWidth(), bitmap.getHeight())
                                .setWrapMode(GLES20.GL_CLAMP_TO_EDGE)
                                .setFilter(GLES20.GL_NEAREST, GLES20.GL_NEAREST);
                        GLESTexture texture = builder.load(bitmap);
                        bitmap.recycle();
                        bitmap = null;

                        mObjects[index].setTexture(texture);
                        mObjects[index].setTextureMapped(true);
                    } else {
                        mObjects[index].setTexture(null);
                        mObjects[index].setTextureMapped(false);
                    }
                }
            }
        }

        mLineObject.setCamera(camera);
        GLESVertexInfo vertexInfo = PBOUtils.createLineVertexInfo(mColorShader,
                -mBitmapWidth * 0.5f, mBitmapHeight * 0.5f,
                mBitmapWidth, mBitmapHeight,
                mNumOfObjectInWidth, mNumOfObjectInHeight, GRID_SIZE);
        mLineObject.setVertexInfo(vertexInfo, true, false);

        mScreenObject.setCamera(camera);
        vertexInfo = PBOUtils.createScreenVertexInfo(mColorShader,
                -mWidth * 0.5f + mLineWidth * 0.5f, mHeight * 0.5f - mLineWidth * 0.5f,
                mWidth - mLineWidth, mHeight - mLineWidth);
        mScreenObject.setVertexInfo(vertexInfo, true, false);

        if (mVersion == Version.GLES_30) {
            setupBufferObject();
            updatePBO();
        }
    }

    private boolean isInScreen(PBOObject object) {
        boolean isInScreen = false;

        float left = object.getX();
        float right = left + object.getWidth();
        float top = object.getY();
        float bottom = top - object.getHeight();

        isInScreen(left, right, bottom, top);

        return isInScreen;
    }

    private boolean isInScreen(float left, float right, float bottom, float top) {
        boolean isInScreen = false;

        float screenLeft = -mWidth * 0.5f;
        float screenRight = mWidth * 0.5f;
        float screenTop = mHeight * 0.5f;
        float screenBottom = -mHeight * 0.5f;

        if (left < screenRight && right > screenLeft
                && top > screenBottom && bottom < screenTop) {
            isInScreen = true;
        }

        return isInScreen;
    }

    private GLESCamera setupCamera(int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "setupCamera() width=" + width + " hegiht=" + height);
        }

        GLESCamera camera = new GLESCamera();

        float fovy = 30f;
        float eyeZ = (height / 2f) / (float) Math.tan(Math.toRadians(fovy * 0.5));

        camera.setLookAt(0f, 0f, eyeZ, 0f, 0f, 0f, 0f, 1f, 0f);

        camera.setFrustum(fovy, mScreenRatio, eyeZ * 0.001f, eyeZ * 3f);

        camera.setViewport(new GLESRect(0, 0, width, height));

        return camera;
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void setupBufferObject() {
        mPBOIDs = new int[mNumOfObjects];
        GLES30.glGenBuffers(mNumOfObjects, mPBOIDs, 0);

        for (int i = 0; i < mNumOfObjects; i++) {
            int byteSize = mObjects[i].getWidth() * mObjects[i].getHeight() * 4;
            GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, mPBOIDs[i]);
            GLES30.glBufferData(GLES30.GL_PIXEL_UNPACK_BUFFER, byteSize, null, GLES30.GL_DYNAMIC_DRAW);
            GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, 0);
            mObjects[i].setTextureMapped(false);
        }
    }


    @Override
    protected void onSurfaceCreated() {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCreated()");
        }

        GLES20.glClearColor(0.7f, 0.7f, 0.7f, 0.0f);

        for (int i = 0; i < mNumOfObjects; i++) {
            mObjects[i].setShader(mTextureShader);
        }

        mLineObject.setShader(mColorShader);
        mScreenObject.setShader(mColorShader);
    }

    @Override
    protected boolean createShader() {
        if (DEBUG) {
            Log.d(TAG, "createShader()");
        }

        {
            mTextureShader = new GLESShader(mContext);

            String vsSource = ShaderUtils.getShaderSource(mContext, 0);
            String fsSource = ShaderUtils.getShaderSource(mContext, 1);

            mTextureShader.setShaderSource(vsSource, fsSource);
            if (mTextureShader.load() == false) {
                return false;
            }

            if (mVersion == Version.GLES_20) {
                String attribName = GLESShaderConstant.ATTRIB_POSITION;
                mTextureShader.setPositionAttribIndex(attribName);

                attribName = GLESShaderConstant.ATTRIB_TEXCOORD;
                mTextureShader.setTexCoordAttribIndex(attribName);
            }
        }

        {
            mColorShader = new GLESShader(mContext);

            String vsSource = ShaderUtils.getShaderSource(mContext, 2);
            String fsSource = ShaderUtils.getShaderSource(mContext, 3);

            mColorShader.setShaderSource(vsSource, fsSource);
            if (mColorShader.load() == false) {
                return false;
            }

            if (mVersion == Version.GLES_20) {
                String attribName = GLESShaderConstant.ATTRIB_POSITION;
                mColorShader.setPositionAttribIndex(attribName);
            }
        }

        return true;
    }

    public void onResume() {
        if (mInfoView != null) {
            mInfoView.setVisibility(View.VISIBLE);
        }
    }

    public void onPause() {
        if (mInfoView != null) {
            mInfoView.setVisibility(View.GONE);
        }
    }

    @Override
    protected boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();

        float x = event.getX();
        float y = event.getY();

        mGestureDetector.onTouchEvent(event);

        if (mIsDownScale == true) {
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mIsDown = true;

                touchDown(x, y);
                break;
            case MotionEvent.ACTION_UP:
                if (mIsDown == true) {
                    touchUp(x, y);
                }

                mIsDown = false;

                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsDown == true) {
                    touchMove(x, y);
                }
                break;
        }

        return true;
    }


    public void touchDown(float x, float y) {
        mPrevX = x;
        mPrevY = y;
    }

    public void touchUp(float x, float y) {

    }

    public void touchMove(float x, float y) {
        float translateX = 0f;
        float translateY = 0f;

        float maxTranslateX = 0f;
        if (mBitmapWidth > mWidth) {
            maxTranslateX = (mBitmapWidth - mWidth) * 0.5f;
        }

        float maxTranslateY = 0f;
        if (mBitmapHeight > mHeight) {
            maxTranslateY = (mBitmapHeight - mHeight) * 0.5f;
        }

        translateX = mTranslateX;
        translateX += (x - mPrevX);

        if (maxTranslateX < Math.abs(translateX)) {
            if (translateX < 0) {
                translateX = -maxTranslateX;
            } else {
                translateX = maxTranslateX;
            }
        }

        mTranslateX = translateX;

        translateY = mTranslateY;
        translateY += (mPrevY - y);
        if (maxTranslateY < Math.abs(translateY)) {
            if (translateY < 0) {
                translateY = -maxTranslateY;
            } else {
                translateY = maxTranslateY;
            }
        }

        mTranslateY = translateY;

        mCurrentTranslateX = mTranslateX;
        mCurrentTranslateY = mTranslateY;

        mPrevX = x;
        mPrevY = y;
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
        if (mVersion == Version.GLES_30) {
            if (object instanceof PBOObject) {
                PBOObject pboObject = (PBOObject) object;
                if (pboObject.isTextureMapped() == true) {
                    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, pboObject.getTextureID());
                }
            }
        }
    }

    @Override
    public void disableVertexAttribute(GLESObject object) {

    }

    private GLESNodeListener mScreenNodeListener = new GLESNodeListener() {
        @Override
        public void update(GLESNode node) {

            GLESTransform transform = node.getTransform();
            transform.setIdentity();
            if (mIsOnAnimation == true || mIsDownScale == true) {
                transform.setScale(mCurrentScale);
                transform.setTranslate(mCurrentScreenX, mCurrentScreenY, 0);
            }
        }
    };

    private GLESNodeListener mImageNodeListener = new GLESNodeListener() {
        @Override
        public void update(GLESNode node) {
            GLESTransform transform = node.getTransform();
            transform.setIdentity();
            transform.setScale(mCurrentScale);
            transform.setTranslate(mCurrentTranslateX, mCurrentTranslateY, 0);
        }
    };

    private GLESObjectListener mLineObjectListener = new GLESObjectListener() {
        @Override
        public void update(GLESObject object) {

        }

        @Override
        public void apply(GLESObject object) {
            GLES20.glLineWidth(1f);
        }
    };

    private GLESObjectListener mScreenObjectListener = new GLESObjectListener() {
        @Override
        public void update(GLESObject object) {

        }

        @Override
        public void apply(GLESObject object) {
            GLES20.glLineWidth(mLineWidth);
        }
    };


    private GestureDetector.SimpleOnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            boolean result = false;
            int action = e.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    break;
                case MotionEvent.ACTION_UP:
                    if (mScreenRatio < mBitmapRatio) {
                        mDownScale = (float) mWidth / mBitmapWidth;
                    } else {
                        mDownScale = (float) mHeight / mBitmapHeight;
                    }

                    mSmallScreenX = -mTranslateX * mDownScale;
                    mSmallScreenY = -mTranslateY * mDownScale;

                    if (mIsDownScale == true) {
                        mIsDownScale = false;
                    } else {
                        mIsDownScale = true;
                    }

                    mAnimator.start();
                    break;
                case MotionEvent.ACTION_MOVE:
                    break;
            }
            return true;
        }
    };

    private float mNormalizedValue = 0f;
    private GLESAnimatorCallback mAnimatorCB = new GLESAnimatorCallback() {
        @Override
        public void onAnimation(GLESVector3 current) {
            mIsOnAnimation = true;

            float normalizedValue = current.getX();
            mNormalizedValue = normalizedValue;
            if (mIsDownScale == true) {
                mCurrentScale = 1f + (mDownScale - 1f) * normalizedValue;

                mCurrentTranslateX = mTranslateX * (1f - normalizedValue);
                mCurrentTranslateY = mTranslateY * (1f - normalizedValue);

                mCurrentScreenX = mSmallScreenX * normalizedValue;
                mCurrentScreenY = mSmallScreenY * normalizedValue;
            } else {
                mCurrentScale = mDownScale + (1f - mDownScale) * normalizedValue;
                mCurrentTranslateX = mTranslateX * normalizedValue;
                mCurrentTranslateY = mTranslateY * normalizedValue;

                mCurrentScreenX = mSmallScreenX * (1f - normalizedValue);
                mCurrentScreenY = mSmallScreenY * (1f - normalizedValue);
            }
        }

        @Override
        public void onCancel() {
            if (mIsDownScale == true) {
                mCurrentScale = mDownScale;
                mIsDownScale = false;
            } else {
                mCurrentScale = 1f;
                mIsDownScale = true;
            }

            mIsOnAnimation = false;
        }

        @Override
        public void onFinished() {
            if (mIsDownScale == true) {
                mCurrentScale = mDownScale;
            } else {
                mCurrentScale = 1f;
            }

            mIsOnAnimation = false;
        }
    };
}
