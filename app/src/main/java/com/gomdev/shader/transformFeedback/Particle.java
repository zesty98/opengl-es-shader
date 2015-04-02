package com.gomdev.shader.transformFeedback;

import com.gomdev.gles.GLESParticle;
import com.gomdev.gles.GLESVector3;

/**
 * Created by gomdev on 15. 2. 3..
 */
class Particle extends GLESParticle {
    private static final String CLASS = "Particle";
    private static final String TAG = TransformFeedbackConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = TransformFeedbackConfig.DEBUG;

    private GLESVector3 mVelocity = new GLESVector3(1f, 1f, 1f);
    private GLESVector3 mDirection = new GLESVector3();
    private float mNormalizedDuration = 1f;
    private float mDistance = 0f;
    private float mVelocityFactor = 1f;

    Particle(float x, float y, float z) {
        super(x, y, z);
    }

    void setDirection(float x, float y, float z) {
        mDirection.set(x, y, z);
    }

    GLESVector3 getDirection() {
        return mDirection;
    }

    void setDistance(float distance) {
        mDistance = distance;
    }

    float getDistance() {
        return mDistance;
    }

    void setVelocityFactor(float factor) {
        mVelocityFactor = factor;
    }

    float getVelocityFactor() {
        return mVelocityFactor;
    }

    @Override
    public void setVelocityX(float vel) {
        mVelocity.setX(vel);

        if (mVelocity.getX() != 1.0f) {
            mNormalizedDuration = mDistance / mVelocity.getX();
        }
    }

    @Override
    public float getVelocityX() {
        return mVelocity.getX();
    }

    @Override
    public float getNormalizedDuration() {
        return mNormalizedDuration;
    }
}
