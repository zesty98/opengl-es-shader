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

package com.gomdev.shader;

import java.util.ArrayList;

import com.gomdev.gles.GLESContext;
import com.gomdev.gles.GLESFileUtils;
import com.gomdev.gles.GLESConfig.Version;
import com.gomdev.shader.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

@SuppressLint("Registered")
public class SampleActivity extends Activity implements Ad {
    static final String CLASS = "SampleActivity";
    static final String TAG = ShaderConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = ShaderConfig.DEBUG;

    protected GLSurfaceView mView;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (icicle != null) {
            ShaderUtils.restoreShaderContext(icicle);
        }

        setContentView(R.layout.common_main);

        hideInfomation();
    }

    private void hideInfomation() {
        LinearLayout layout = (LinearLayout) findViewById(R.id.layout_info);
        layout.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        ShaderUtils.saveShaderContext(outState);
    }

    @Override
    public int getLayoutID() {
        return R.layout.fragment_sample;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sample_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.shader_list:
            showShaderListDialog();
            return true;
        case R.id.restore:
            showRestoreDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showRestoreDialog() {
        if (isEmptySavedFile() == true) {
            Toast.makeText(this, "No saved shader file", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        hideInfomation();

        ShaderRestoreDialog dialog = new ShaderRestoreDialog();
        dialog.show(getFragmentManager(), "restore");
    }

    private boolean isEmptySavedFile() {
        ShaderContext context = ShaderContext.getInstance();

        ArrayList<ShaderInfo> shaders = context.getShaderInfoList();
        for (ShaderInfo shaderInfo : shaders) {
            if (GLESFileUtils.isExist(shaderInfo.mFilePath)) {
                return false;
            }
        }

        return true;
    }

    private void showShaderListDialog() {
        hideInfomation();

        ShaderListDialog dialog = new ShaderListDialog();
        dialog.show(getFragmentManager(), "shaderlist");
    }

    protected void setGLESVersion() {
        Version version = GLESContext.getInstance().getVersion();
        switch (version) {
        case GLES_20:
            mView.setEGLContextClientVersion(2);
            break;
        case GLES_30:
            mView.setEGLContextClientVersion(3);
            break;
        default:
            mView.setEGLContextClientVersion(2);
        }
    }
}