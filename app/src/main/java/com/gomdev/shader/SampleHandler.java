package com.gomdev.shader;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gomdev.gles.GLESConfig;
import com.gomdev.gles.GLESContext;

/**
 * Created by gomdev on 15. 6. 17..
 */
public class SampleHandler extends Handler {
    private static final String CLASS = "SampleHandler";
    private static final String TAG = ShaderConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = ShaderConfig.DEBUG;

    public static final int COMPILE_OR_LINK_ERROR = 1;
    public static final int COMPILE_AND_LINK_SUCCESS = 2;
    public static final int UPDATE_FPS = 3;

    private final Context mContext;

    private TextView mFPS = null;

    public SampleHandler(Context context) {
        super(Looper.getMainLooper());

        mContext = context;
    }



    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case COMPILE_OR_LINK_ERROR:
                Toast.makeText(mContext, "Compile or Link fails",
                        Toast.LENGTH_SHORT).show();

                showCompileLog();
                break;
            case COMPILE_AND_LINK_SUCCESS:
                setupInformation();
                break;
            case UPDATE_FPS:
                ShaderContext context = ShaderContext.getInstance();
                boolean showInfo = context.showInfo();
                boolean showFPS = context.showFPS();
                if (showInfo == true) {
                    LinearLayout layout = (LinearLayout) ((Activity) mContext)
                            .findViewById(R.id.layout_fps);
                    if (showFPS == false) {
                        layout.setVisibility(View.INVISIBLE);
                    } else {
                        layout.setVisibility(View.VISIBLE);
                    }
                }

                if (showFPS == false || showInfo == false) {
                    return;
                }

                if (mFPS == null) {
                    mFPS = (TextView) ((Activity) mContext)
                            .findViewById(R.id.fps);
                }
                mFPS.setText("" + msg.arg1);
                break;
        }
    }

    private void setupInformation() {
        LinearLayout layout = (LinearLayout) ((Activity) mContext)
                .findViewById(R.id.layout_info);
        boolean showInfo = ShaderContext.getInstance().showInfo();
        if (showInfo == true) {
            layout.setVisibility(View.VISIBLE);
            showGLESVersion();
        } else {
            layout.setVisibility(View.INVISIBLE);
        }
    }

    private void showGLESVersion() {
        TextView textView = (TextView) ((Activity) mContext)
                .findViewById(R.id.layout_version);

        GLESConfig.Version version = GLESContext.getInstance().getVersion();
        switch (version) {
            case GLES_20:
                textView.setText("OpenGL ES 2.0");
                break;
            case GLES_30:
                textView.setText("OpenGL ES 3.0");
                break;
            default:

        }
    }

    private void showCompileLog() {
        TextView view = (TextView) ((Activity) mContext)
                .findViewById(R.id.error_log);
        LinearLayout layout = (LinearLayout) ((Activity) mContext)
                .findViewById(R.id.layout_info);

        String compileLog = GLESContext.getInstance()
                .getShaderErrorLog();
        if (compileLog != null) {
            view.setText(compileLog);
            view.setVisibility(View.VISIBLE);
            layout.setVisibility(View.INVISIBLE);
        } else {
            view.setVisibility(View.INVISIBLE);
            layout.setVisibility(View.VISIBLE);
        }
    }
}
