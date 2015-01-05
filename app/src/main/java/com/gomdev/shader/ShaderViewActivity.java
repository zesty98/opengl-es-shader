package com.gomdev.shader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class ShaderViewActivity extends Activity implements Ad {
    static final String CLASS = "ShaderViewActivity";
    static final String TAG = ShaderConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = ShaderConfig.DEBUG;

    private TextView mTextView = null;
    private String mShaderSource = null;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (icicle != null) {
            ShaderUtils.restoreShaderContext(icicle);
        }

        setContentView(R.layout.common_main);

        mTextView = (TextView) findViewById(R.id.shader_view);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        ShaderUtils.saveShaderContext(outState);
    }

    @Override
    public int getLayoutID() {
        return R.layout.fragment_shader_view;
    }

    @Override
    protected void onStart() {
        super.onStart();

        mShaderSource = ShaderUtils.getShaderSource(this);

        mTextView.setText(mShaderSource);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.shader_view_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent(this,
                com.gomdev.shader.ShaderEditActivity.class);
        switch (item.getItemId()) {
            case R.id.edit:
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
