package com.gomdev.shader.compressedTexture;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.opengl.GLES20;
import android.support.v4.view.GestureDetectorCompat;
import android.text.InputType;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESCompressedTextureInfo;
import com.gomdev.gles.GLESConfig;
import com.gomdev.gles.GLESConfig.Version;
import com.gomdev.gles.GLESContext;
import com.gomdev.gles.GLESDDSDecoder;
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
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVertexInfo;
import com.gomdev.shader.R;
import com.gomdev.shader.SampleRenderer;
import com.gomdev.shader.ShaderUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class CompressedTextureRenderer extends SampleRenderer {
    private static final String CLASS = "CompressedTextureRenderer";
    private static final String TAG = CompressedTextureConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = CompressedTextureConfig.DEBUG;

    enum TextureType {
        NONE,
        ORIGINAL,
        ETC1,
        ATC_RGB,
        ATC_RGBA_EXPLICIT,
        ATC_RGBA_INTERPOLATED,
        ETC2_RGB,
        ETC2_RGB_PUNCHTHROUGHT_ALPHA,
        ETC2_RGBA,
        S3TC_DXT1_RGBA,
        S3TC_DXT3_RGBA,
        S3TC_DXT5_RGBA
    }

    class TextureInfo {
        private final TextureType mType;
        private final String mInfo;
        private GLESTexture mTexture;
        private int mResID;
        private int mInternalFormat;

        TextureInfo(TextureType type, String info) {
            mType = type;
            mInfo = info;
        }

        TextureType getType() {
            return mType;
        }

        String getInfo() {
            return mInfo;
        }

        void setTexture(GLESTexture texture) {
            mTexture = texture;
        }

        GLESTexture getTexture() {
            return mTexture;
        }

        void setResID(int resID) {
            mResID = resID;
        }

        int getResID() {
            return mResID;
        }

        void setInternalFormat(int internalFormat) {
            mInternalFormat = internalFormat;
        }

        int getInternalFormat() {
            return mInternalFormat;
        }
    }

    private GLESRenderer mRenderer = null;
    private GLESSceneManager mSM = null;

    private GLESNode mRoot = null;
    private GLESObject mObject = null;
    private GLESObject mBGObject = null;
    private GLESShader mShader = null;

    private GLESTexture mBGTexture = null;
    private GLESTexture mOriginalTexture = null;

    private GestureDetectorCompat mGestureDetector = null;

    private Version mVersion;

    private float mScreenRatio = 0f;

    private int mIndex = 0;

    private Object mLockObject = new Object();

    private LinearLayout mLayout = null;
    private TextView mInfoView = null;

    private Map<TextureType, TextureInfo> mTextureInfos = new LinkedHashMap<>();
    private TextureType[] mTextureTypes = null;

    public CompressedTextureRenderer(Context context) {
        super(context);

        mVersion = GLESContext.getInstance().getVersion();

        mRenderer = GLESRenderer.createRenderer();

        setupInfoView();

        createScene();

        mGestureDetector = new GestureDetectorCompat(context, mGestureListener);
    }

    private void setupTextureInfos() {
        mTextureInfos.clear();

        TextureInfo info = new TextureInfo(TextureType.NONE, "BG");
        mTextureInfos.put(TextureType.NONE, info);

        info = new TextureInfo(TextureType.ORIGINAL, "BG + Original");
        mTextureInfos.put(TextureType.ORIGINAL, info);

        boolean isSupport = GLESUtils.checkGLESExtension("GL_OES_compressed_ETC1_RGB8_texture");
        if (isSupport == true) {
            info = new TextureInfo(TextureType.ETC1, "BG + ETC1");
            info.setResID(R.raw.transparent_etc1_rgb);
            info.setInternalFormat(GLESConfig.GL_ETC1_RGB8_OES);

            mTextureInfos.put(TextureType.ETC1, info);
        }

        isSupport = GLESUtils.checkGLESExtension("GL_AMD_compressed_ATC_texture");
        if (isSupport == true) {
            info = new TextureInfo(TextureType.ATC_RGB, "BG + ATC_RGB");
            info.setResID(R.raw.transparent_atc_rgb);
            info.setInternalFormat(GLESConfig.GL_ATC_RGB_AMD);
            mTextureInfos.put(TextureType.ATC_RGB, info);

            info = new TextureInfo(TextureType.ATC_RGBA_EXPLICIT, "BG + ATC_RGBA_EXPLICIT");
            info.setResID(R.raw.transparent_atc_rgba_e);
            info.setInternalFormat(GLESConfig.GL_ATC_RGBA_EXPLICIT_ALPHA_AMD);
            mTextureInfos.put(TextureType.ATC_RGBA_EXPLICIT, info);

            info = new TextureInfo(TextureType.ATC_RGBA_INTERPOLATED, "BG + ATC_RGBA_INTERPOLATED");
            info.setResID(R.raw.transparent_atc_rgba_i);
            info.setInternalFormat(GLESConfig.GL_ATC_RGBA_INTERPOLATED_ALPHA_AMD);
            mTextureInfos.put(TextureType.ATC_RGBA_INTERPOLATED, info);
        }

        isSupport = GLESUtils.checkGLESExtension("GL_EXT_texture_compression_dxt1");
        if (isSupport == true) {
            info = new TextureInfo(TextureType.S3TC_DXT1_RGBA, "BG + S3TC_DXT1_RGBA");
            info.setResID(R.raw.transparent_s3tc_dxt1_rgba);
            info.setInternalFormat(GLESConfig.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT);
            mTextureInfos.put(TextureType.S3TC_DXT1_RGBA, info);
        }

        isSupport = GLESUtils.checkGLESExtension("GL_EXT_texture_compression_s3tc");
        if (isSupport == true) {
            info = new TextureInfo(TextureType.S3TC_DXT3_RGBA, "BG + S3TC_DXT3_RGBA");
            info.setResID(R.raw.transparent_s3tc_dxt3_rgba);
            info.setInternalFormat(GLESConfig.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT);
            mTextureInfos.put(TextureType.S3TC_DXT3_RGBA, info);

            info = new TextureInfo(TextureType.S3TC_DXT5_RGBA, "BG + S3TC_DXT5_RGBA");
            info.setResID(R.raw.transparent_s3tc_dxt5_rgba);
            info.setInternalFormat(GLESConfig.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT);
            mTextureInfos.put(TextureType.S3TC_DXT5_RGBA, info);
        }

        if (mVersion == Version.GLES_30) {
            info = new TextureInfo(TextureType.ETC2_RGB, "BG + ETC2_RGB");
            info.setResID(R.raw.transparent_etc2_rgb8);
            info.setInternalFormat(GLESConfig.GL_COMPRESSED_RGB8_ETC2);
            mTextureInfos.put(TextureType.ETC2_RGB, info);

            info = new TextureInfo(TextureType.ETC2_RGB_PUNCHTHROUGHT_ALPHA, "BG + ETC2_RGB8_PUNCHTHROUGH_ALPHA1");
            info.setResID(R.raw.transparent_etc2_rgb8_a1);
            info.setInternalFormat(GLESConfig.GL_COMPRESSED_RGB8_PUNCHTHROUGH_ALPHA1_ETC2);
            mTextureInfos.put(TextureType.ETC2_RGB_PUNCHTHROUGHT_ALPHA, info);

            info = new TextureInfo(TextureType.ETC2_RGBA, "BG + ETC2_RGBA");
            info.setResID(R.raw.transparent_etc2_rgba);
            info.setInternalFormat(GLESConfig.GL_COMPRESSED_RGBA8_ETC2_EAC);
            mTextureInfos.put(TextureType.ETC2_RGBA, info);
        }

        int size = mTextureInfos.size();
        mTextureTypes = new TextureType[size];
        Set<TextureType> types = mTextureInfos.keySet();
        mTextureTypes = types.toArray(mTextureTypes);
    }

    private void setupInfoView() {
        mLayout = (LinearLayout) ((Activity) mContext)
                .findViewById(R.id.layout_info);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        mInfoView = new TextView(mContext);
        mLayout.addView(mInfoView, params);
        mInfoView.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        mInfoView.setTextColor(Color.WHITE);
        mInfoView.setVisibility(View.VISIBLE);
        int margin = mContext.getResources().getDimensionPixelSize(R.dimen.textview_margin);
        mInfoView.setPadding(margin, margin, margin, margin);
        mInfoView.setText("BG");

        mIndex = 0;
    }

    private void createScene() {
        mSM = GLESSceneManager.createSceneManager();
        mRoot = mSM.createRootNode("Root");

        mBGObject = mSM.createObject("BG");
        mObject = mSM.createObject("Object");

        {
            GLESGLState state = new GLESGLState();
            state.setCullFaceState(true);
            state.setCullFace(GLES20.GL_BACK);
            state.setDepthState(true);
            state.setDepthFunc(GLES20.GL_LEQUAL);
            state.setBlendState(true);
            state.setBlendFunc(
                    GLES20.GL_SRC_ALPHA,
                    GLES20.GL_ONE_MINUS_SRC_ALPHA);

            mObject.setGLState(state);
        }

        {
            GLESGLState state = new GLESGLState();
            state.setCullFaceState(true);
            state.setCullFace(GLES20.GL_BACK);
            state.setDepthState(true);
            state.setDepthFunc(GLES20.GL_LEQUAL);
            state.setBlendState(true);
            state.setBlendFunc(
                    GLES20.GL_SRC_ALPHA,
                    GLES20.GL_ONE_MINUS_SRC_ALPHA);

            mBGObject.setGLState(state);
        }

        mRoot.addChild(mBGObject);
        mRoot.addChild(mObject);
    }

    public void destroy() {
        mObject = null;
        mBGObject = null;
    }

    @Override
    protected void onDrawFrame() {
        synchronized (mLockObject) {
            super.updateFPS();

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            mRenderer.updateScene(mSM);
            mRenderer.drawScene(mSM);
        }
    }

    @Override
    protected void onSurfaceChanged(int width, int height) {
        mRenderer.reset();

        mScreenRatio = (float) width / height;

        GLESCamera camera = setupCamera(width, height);

        mObject.setCamera(camera);
        mBGObject.setCamera(camera);

        GLESVertexInfo vertexInfo = GLESMeshUtils.createPlane(mShader,
                width, height, false, true, false, false);

        mObject.setVertexInfo(vertexInfo, true, true);
        mObject.hide();

        mBGObject.setVertexInfo(vertexInfo, true, true);
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

    @Override
    protected void onSurfaceCreated() {
        GLES20.glClearColor(0.7f, 0.7f, 0.7f, 0.0f);

        setupTextureInfos();

        createTextures();

        mObject.setShader(mShader);
        mObject.setTexture(mOriginalTexture);

        mBGObject.setShader(mShader);
        mBGObject.setTexture(mBGTexture);
    }

    private void createTextures() {
        {
            Bitmap bitmap = GLESUtils.makeBitmap(512, 512, Bitmap.Config.ARGB_8888, 0xFFFFFF00);
            GLESTexture.Builder builder = new GLESTexture.Builder(
                    GLES20.GL_TEXTURE_2D, bitmap.getWidth(), bitmap.getHeight());
            mBGTexture = builder.load(bitmap);
        }

        {
            Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.transparent);
            GLESTexture.Builder builder = new GLESTexture.Builder(
                    GLES20.GL_TEXTURE_2D, bitmap.getWidth(), bitmap.getHeight());
            mOriginalTexture = builder.load(bitmap);
            TextureInfo textureInfo = mTextureInfos.get(TextureType.ORIGINAL);
            textureInfo.setTexture(mOriginalTexture);
        }

        for (int i = 2; i < mTextureTypes.length; i++) {
            TextureType type = mTextureTypes[i];
            TextureInfo info = mTextureInfos.get(type);

            InputStream inputStream = mContext.getResources().openRawResource(info.getResID());
            GLESCompressedTextureInfo compressedTextureInfo = null;
            try {
                compressedTextureInfo = GLESDDSDecoder.decode(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            GLESTexture.Builder builder = new GLESTexture.Builder(
                    GLES20.GL_TEXTURE_2D, compressedTextureInfo.getWidth(), compressedTextureInfo.getHeight())
                    .setInternalFormat(info.getInternalFormat());
            GLESTexture texture = builder.load(compressedTextureInfo.getData());

            TextureInfo textureInfo = mTextureInfos.get(type);
            textureInfo.setTexture(texture);
        }
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

    @Override
    protected boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    public GestureDetector.SimpleOnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            mIndex++;

            int size = mTextureInfos.size();
            if (mIndex > (size - 1)) {
                mIndex = 0;
            }

            synchronized (mLockObject) {

                TextureType type = mTextureTypes[mIndex];
                TextureInfo info = mTextureInfos.get(type);

                if (type == TextureType.NONE) {
                    mObject.hide();
                } else {
                    mObject.show();
                }
                mObject.setTexture(info.getTexture());
                mInfoView.setText(info.getInfo());

                mView.requestRender();
            }
            return super.onSingleTapUp(e);
        }
    };
}
