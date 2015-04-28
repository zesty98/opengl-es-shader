package com.gomdev.shader.pbo;

import com.gomdev.gles.GLESObject;
import com.gomdev.shader.pbo.PBORenderer.BlockInfo;

import java.lang.ref.WeakReference;

/**
 * Created by gomdev on 15. 4. 5..
 */
public class PBOObject extends GLESObject {
    private static final String CLASS = "PBOObject";
    private static final String TAG = PBOConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = PBOConfig.DEBUG;

    enum TextureState {
        NONE,
        DECODING,
        QUEUED,
        MAPPING
    }

    private int mX = 0;
    private int mY = 0;

    private int mImageX = 0;
    private int mImageY = 0;

    private int mWidth = 0;
    private int mHeight = 0;

    private int mTextureID = -1;

    private boolean mIsTextureMapped = false;

    public PBOObject() {
        super();
    }

    public PBOObject(String name) {
        super(name);
    }

    int getX() {
        return mX;
    }

    int getY() {
        return mY;
    }

    void setPosition(int x, int y) {
        mX = x;
        mY = y;
    }

    int getImageX() {
        return mImageX;
    }

    int getmImageY() {
        return mImageY;
    }

    void setImagePosition(int x, int y) {
        mImageX = x;
        mImageY = y;
    }

    void setWidth(int width) {
        mWidth = width;
    }

    int getWidth() {
        return mWidth;
    }

    void setHeight(int height) {
        mHeight = height;
    }

    int getHeight() {
        return mHeight;
    }

    void setTextureMapped(boolean isTextureMapped) {
        mIsTextureMapped = isTextureMapped;
    }

    void setTextureID(int textureID) {
        mTextureID = textureID;
    }

    int getTextureID() {
        return mTextureID;
    }

    boolean isTextureMapped() {
        return mIsTextureMapped;
    }
}
