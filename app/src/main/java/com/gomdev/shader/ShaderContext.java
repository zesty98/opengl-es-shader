package com.gomdev.shader;

import com.gomdev.gles.GLESConfig;
import com.gomdev.gles.GLESConfig.Version;
import com.gomdev.gles.GLESContext;

import java.util.ArrayList;

public class ShaderContext {
    static final String CLASS = "ShaderContext";
    static final String TAG = ShaderConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = ShaderConfig.DEBUG;

    private static ShaderContext sShaderContext = null;

    private ArrayList<ShaderInfo> mShaderInfos = null;
    private String mSampleName = null;
    private int mNumOfShaders = 0;
    private ShaderInfo mSavedShaderInfo = null;

    private boolean mShowInfo = true;
    private boolean mShowFPS = true;
    private boolean mUseGLES30 = (GLESConfig.DEFAULT_GLES_VERSION == Version.GLES_30) ? true
            : false;

    private String mExtensions = null;
    private String mRenderer = null;
    private String mVendor = null;
    private String mVersion = null;

    private String mHardware = null;
    private String mArchitecture = null;
    private String mFeature = null;

    private Version mSupportedGLESVersion = Version.GLES_20;

    public static ShaderContext getInstance() {
        return sShaderContext;
    }

    public static ShaderContext newInstance() {
        sShaderContext = new ShaderContext();
        return sShaderContext;
    }

    private ShaderContext() {
        mShaderInfos = new ArrayList<ShaderInfo>();
    }

    public void setSampleName(String name) {
        mSampleName = name;
    }

    public String getSampleName() {
        return mSampleName;
    }

    public void setNumOfShaders(int num) {
        mNumOfShaders = num;
    }

    public int getNumOfShaders() {
        return mNumOfShaders;
    }

    public void setShaderInfo(String sampleName, String title,
                              int resID, String filePath) {
        ShaderInfo shader = new ShaderInfo();
        shader.mSampleName = sampleName;
        shader.mTitle = title;
        shader.mResID = resID;
        shader.mFilePath = filePath;

        mShaderInfos.add(shader);
    }

    public void setShaderInfoList(ArrayList<ShaderInfo> list) {
        mShaderInfos = list;
    }

    public ArrayList<ShaderInfo> getShaderInfoList() {
        return mShaderInfos;
    }

    public void clearShaderInfos() {
        mShaderInfos.clear();
    }

    public void setSavedShaderInfo(ShaderInfo info) {
        mSavedShaderInfo = info;
    }

    public ShaderInfo getSavedShaderInfo() {
        return mSavedShaderInfo;
    }

    public void setShowInfo(boolean showInfo) {
        mShowInfo = showInfo;
    }

    public boolean showInfo() {
        return mShowInfo;
    }

    public void setShowFPS(boolean showFPS) {
        mShowFPS = showFPS;
    }

    public boolean showFPS() {
        return mShowFPS;
    }

    public void setUseGLES30(boolean useGLES30) {
        Version supportedVersion = ShaderContext.getInstance().getSupportedGLESVersion();
        if (supportedVersion != Version.GLES_30 && supportedVersion != Version.GLES_31) {
            mUseGLES30 = false;
            GLESContext.getInstance().setVersion(Version.GLES_20);
            return;
        }
        mUseGLES30 = useGLES30;

        if (useGLES30 == true) {
            GLESContext.getInstance().setVersion(Version.GLES_30);
        } else {
            GLESContext.getInstance().setVersion(Version.GLES_20);
        }
    }

    public boolean useGLES30() {
        return mUseGLES30;
    }

    public void setExtensions(String extensions) {
        mExtensions = extensions;
    }

    public String getExtensions() {
        return mExtensions;
    }

    public void setRenderer(String renderer) {
        mRenderer = renderer;
    }

    public String getRenderer() {
        return mRenderer;
    }

    public void setVendor(String vendor) {
        mVendor = vendor;
    }

    public String getVendor() {
        return mVendor;
    }

    public void setVersionStr(String version) {
        mVersion = version;
    }

    public String getVersionStr() {
        return mVersion;
    }

    public void setHardware(String hardware) {
        mHardware = hardware;
    }

    public String getHardware() {
        return mHardware;
    }

    public void setArchitecture(String architecture) {
        mArchitecture = architecture;
    }

    public String getArchitecture() {
        return mArchitecture;
    }

    public void setFeature(String feature) {
        mFeature = feature;
    }

    public String getFeature() {
        return mFeature;
    }

    public void setSupportedGLESVersion(Version version) {
        mSupportedGLESVersion = version;
    }

    public Version getSupportedGLESVersion() {
        return mSupportedGLESVersion;
    }
}
