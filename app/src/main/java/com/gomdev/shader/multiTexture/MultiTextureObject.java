package com.gomdev.shader.multiTexture;

import com.gomdev.gles.GLESObject;

/**
 * Created by gomdev on 15. 4. 21..
 */
public class MultiTextureObject extends GLESObject {
    private static final String CLASS = "MultiTextureObject";
    private static final String TAG = MultiTextureConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = MultiTextureConfig.DEBUG;

    private int mIndex = 0;
    private boolean mIsChecked = false;

    public MultiTextureObject() {
        super();
    }

    public MultiTextureObject(String name) {
        super(name);
    }

    void setIndex(int index) {
        mIndex = index;
    }

    int getIndex() {
        return mIndex;
    }

    void setCheck(boolean isChecked) {
        mIsChecked = isChecked;
    }

    boolean isChecked() {
        return mIsChecked;
    }
}
