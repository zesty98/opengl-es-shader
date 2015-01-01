/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gomdev.shader.occlusionQuery;

import com.gomdev.shader.SampleActivity;
import com.gomdev.shader.R;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;

public class OQActivity extends SampleActivity {
    private OQRenderer mRenderer;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mRenderer = new OQRenderer(this);
        mView = (GLSurfaceView) findViewById(R.id.sample_surfaceview);
        mRenderer.setSurfaceView(mView);

        setGLESVersion();

        mView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        mView.setRenderer(mRenderer);
        mView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mView.onPause();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (mRenderer == null) {
            return super.onTouchEvent(event);
        }

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            ((OQRenderer) mRenderer).touchDown(x, y);
            break;
        case MotionEvent.ACTION_UP:
            ((OQRenderer) mRenderer).touchUp(x, y);
            break;
        case MotionEvent.ACTION_MOVE:
            ((OQRenderer) mRenderer).touchMove(x, y);
            break;
        }

        return super.onTouchEvent(event);
    }
}