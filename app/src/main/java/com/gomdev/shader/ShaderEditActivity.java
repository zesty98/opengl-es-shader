package com.gomdev.shader;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.gomdev.gles.GLESFileUtils;

public class ShaderEditActivity extends Activity implements Ad {
    static final String CLASS = "ShaderEditActivity";
    static final String TAG = ShaderConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = ShaderConfig.DEBUG;

    private EditText mEditView = null;

    private String mShaderSource = null;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (icicle != null) {
            ShaderUtils.restoreShaderContext(icicle);
        }

        setContentView(R.layout.common_main);

        mEditView = (EditText) findViewById(R.id.shader_edit);

        mShaderSource = ShaderUtils.getShaderSource(this);

        mEditView.setText(mShaderSource);

        ShaderInfo savedShaderInfo = ShaderContext.getInstance().getSavedShaderInfo();
        getActionBar().setTitle(savedShaderInfo.mTitle);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        ShaderUtils.saveShaderContext(outState);
    }

    @Override
    public int getLayoutID() {
        return R.layout.fragment_shader_edit;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.shader_edit_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                if (GLESFileUtils.isExternalStorageWriable() == false) {
                    Toast.makeText(this, "SDCard is not available",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }

                ShaderContext context = ShaderContext.getInstance();
                String savedFileName = context.getSavedShaderInfo().mFilePath;

                GLESFileUtils.write(savedFileName, mEditView.getText().toString());

                Toast.makeText(this, savedFileName + " Saved", Toast.LENGTH_SHORT)
                        .show();

                this.finish();

                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
