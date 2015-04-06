package com.gomdev.shader.textRendering;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.os.Build;
import android.util.Log;

import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESConfig.Version;
import com.gomdev.gles.GLESContext;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESMeshUtils;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESObject;
import com.gomdev.gles.GLESRect;
import com.gomdev.gles.GLESRendererListener;
import com.gomdev.gles.GLESSceneManager;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESShaderConstant;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVertexInfo;
import com.gomdev.shader.SampleRenderer;
import com.gomdev.shader.ShaderUtils;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;

public class TextRenderingRenderer extends SampleRenderer implements GLESRendererListener {
    private static final String CLASS = "TextRenderingRenderer";
    private static final String TAG = TextRenderingConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = true;//TextRenderingConfig.DEBUG;

    private final float TEXT_SIZE = 20f;        // dpi
    private final float TEXT_SHADOW_RADIUS = 0.7f;  // dpi
    private final float TEXT_SHADOW_DX = 0.3f;  // dpi
    private final float TEXT_SHADOW_DY = 0.3f;  // dpi
    private final int TEXT_SHADOW_COLOR = 0x88444444;
    private final int TEXT_COLOR = 0xFF222222;
    private final int TEXT_MARGIN = 3;

    private GLESSceneManager mSM = null;

    private GLESObject mObject = null;
    private GLESTexture mTexture = null;

    private GLESShader mShader = null;
    private Version mVersion;

    private float mScreenRatio = 0f;
    private int mWidth = 0;
    private int mHeight = 0;

    private int mMargin = 0;

    private Bitmap mBitmap = null;

    private int mNumOfObject = 0;
    private int mTextWidth = 0;
    private int mTextHeight = 0;

    public TextRenderingRenderer(Context context) {
        super(context);

        if (DEBUG) {
            Log.d(TAG, "TextRenderingRenderer()");
        }

        mVersion = GLESContext.getInstance().getVersion();

        createScene();

        mMargin = GLESUtils.getPixelFromDpi(mContext, TEXT_MARGIN);
    }

    private void createScene() {
        mSM = GLESSceneManager.createSceneManager();

        mRenderer.setListener(this);

        GLESNode root = mSM.createRootNode("Root");

        mObject = mSM.createObject("Object");

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

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        mRenderer.updateScene(mSM);
        mRenderer.drawScene(mSM);

        updateBitmap();

        if (mVersion == Version.GLES_30) {
            updateTextUsingPBO();
        }
    }

    void updateBitmap() {
        Calendar calendar = Calendar.getInstance();

        mBitmap.eraseColor(0xFFFFFFFF);
        for (int i = 0; i < mNumOfObject; i++) {
            String text = calendar.getTime().toString();
            drawText(i, text);
        }
    }

    @Override
    protected void onSurfaceChanged(int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceChanged() width=" + width + " height=" + height);
        }

        mWidth = width;
        mHeight = height;
        mScreenRatio = (float) width / height;

        mBitmap = createBitmap();
        updateBitmap();

        mRenderer.reset();

        GLESCamera camera = setupCamera(width, height);

        mObject.setCamera(camera);

        GLESVertexInfo vertexInfo = GLESMeshUtils.createPlane(mShader,
                mBitmap.getWidth(), mBitmap.getHeight(),
                false, true, false, false);
        mObject.setVertexInfo(vertexInfo, true, false);

        if (mVersion == Version.GLES_30) {
            setupBufferObject();
        } else {
            GLESTexture.Builder builder = new GLESTexture.Builder(
                    GLES20.GL_TEXTURE_2D, mBitmap.getWidth(), mBitmap.getHeight());
            mTexture = builder.load(mBitmap);
            mTextureID = mTexture.getTextureID();

            mObject.setTexture(mTexture);
        }

        Log.d(TAG, "onSurfaceChanged() bitmap width=" + mBitmap.getWidth() + " height=" + mBitmap.getHeight());
        Log.d(TAG, "\t bitmap width * height=" + (mBitmap.getWidth() * mBitmap.getHeight() * 4));
        Log.d(TAG, "\t config=" + mBitmap.getConfig());
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

    private int mPBOID = -1;
    private int mTextureID = -1;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void setupBufferObject() {
        int[] ids = new int[1];

        GLES30.glGenTextures(1, ids, 0);
        mTextureID = ids[0];
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureID);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, mBitmap.getWidth(), mBitmap.getHeight(), 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);

        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);

        GLES30.glGenBuffers(1, ids, 0);
        mPBOID = ids[0];

        GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, mPBOID);
        GLES30.glBufferData(GLES30.GL_PIXEL_UNPACK_BUFFER, mBitmap.getByteCount(), null, GLES30.GL_DYNAMIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, 0);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void updateTextUsingPBO() {
        GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, mPBOID);

        Buffer buffer = GLES30.glMapBufferRange(GLES30.GL_PIXEL_UNPACK_BUFFER, 0, mBitmap.getByteCount(), GLES30.GL_MAP_WRITE_BIT);

        mBitmap.copyPixelsToBuffer(buffer);
    }

    @Override
    protected void onSurfaceCreated() {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCreated()");
        }

        GLES20.glClearColor(0.7f, 0.7f, 0.7f, 0.0f);
//        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);

        mObject.setShader(mShader);
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

    private Bitmap createBitmap() {
        Calendar calendar = Calendar.getInstance();
        String text = calendar.getTime().toString();

        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setShadowLayer(
                GLESUtils.getPixelFromDpi(mContext, TEXT_SHADOW_RADIUS),
                GLESUtils.getPixelFromDpi(mContext, TEXT_SHADOW_DX),
                GLESUtils.getPixelFromDpi(mContext, TEXT_SHADOW_DY),
                TEXT_SHADOW_COLOR);
        textPaint.setTextSize(GLESUtils.getPixelFromDpi(mContext, TEXT_SIZE));
        textPaint.setARGB(0xFF, 0x00, 0x00, 0x00);
        textPaint.setColor(TEXT_COLOR);

        Rect rect = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), rect);
        float textWidth = textPaint.measureText(text, 0, text.length());
        float textHeight = rect.height();

        rect.left -= mMargin;
        rect.top -= mMargin;
        rect.right += mMargin;
        rect.bottom += mMargin;

        mTextWidth = (int) (textWidth + mMargin * 2f);
        mTextHeight = (int) (textHeight + mMargin * 2f);

        mNumOfObject = (int) Math.ceil((float) mHeight / mTextHeight);
        Bitmap bitmap = Bitmap.createBitmap(mTextWidth, mNumOfObject * mTextHeight, Bitmap.Config.ARGB_8888);

        return bitmap;
    }

    private void drawText(int index, String text) {
        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setShadowLayer(
                GLESUtils.getPixelFromDpi(mContext, TEXT_SHADOW_RADIUS),
                GLESUtils.getPixelFromDpi(mContext, TEXT_SHADOW_DX),
                GLESUtils.getPixelFromDpi(mContext, TEXT_SHADOW_DY),
                TEXT_SHADOW_COLOR);
        textPaint.setTextSize(GLESUtils.getPixelFromDpi(mContext, TEXT_SIZE));
        textPaint.setARGB(0xFF, 0x00, 0x00, 0x00);
        textPaint.setColor(TEXT_COLOR);

        Rect rect = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), rect);
        float textWidth = textPaint.measureText(text, 0, text.length());
        float textHeight = rect.height();

        rect.left -= mMargin;
        rect.top -= mMargin;
        rect.right += mMargin;
        rect.bottom += mMargin;

        float width = textWidth + mMargin * 2f;
        float height = textHeight + mMargin * 2f;

        float x = mMargin;
        float y = (height - textHeight) * 0.5f + textHeight + index * mTextHeight;

        GLESUtils.drawTextToBitmap((int) x, (int) y, text, textPaint, mBitmap, false);
    }

    public void touchDown(float x, float y) {
    }

    public void touchUp(float x, float y) {
    }

    public void touchMove(float x, float y) {
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
            unmap();
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureID);
            mShader.texSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, 0);
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID);
            Buffer buffer = ByteBuffer
                    .allocateDirect(mBitmap.getByteCount())
                    .order(ByteOrder.nativeOrder()).asIntBuffer();

            mBitmap.copyPixelsToBuffer(buffer);
            buffer.position(0);

            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void unmap() {
        GLES30.glUnmapBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER);
    }

    @Override
    public void disableVertexAttribute(GLESObject object) {
        GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, 0);
    }
}
