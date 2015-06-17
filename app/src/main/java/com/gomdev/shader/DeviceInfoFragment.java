package com.gomdev.shader;

import android.annotation.SuppressLint;
import android.app.Service;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gomdev.gles.GLESConfig;

@SuppressLint("InflateParams")
public class DeviceInfoFragment extends MainFragment {
    static final String CLASS = "DeviceInfoFragment";
    static final String TAG = ShaderConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = ShaderConfig.DEBUG;

    private FrameLayout mLayout = null;
    private ProgressBar mProgressBar = null;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (DEBUG) {
            Log.d(TAG, "onCreateView() " + this);
        }

        FrameLayout layout = (FrameLayout) inflater.inflate(
                R.layout.fragment_device_info, null);
        mLayout = layout;

        mProgressBar = (ProgressBar) mLayout.findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.VISIBLE);

        setDeviceInfo();

        return layout;
    }

    private void setDeviceInfo() {
        if (DEBUG) {
            Log.d(TAG, "setDeviceInfo()");
        }

        ShaderContext shaderContext = ShaderContext.getInstance();

        // Hardware
        TextView hardwareView = (TextView) mLayout.findViewById(R.id.hardware);

        String hardware = shaderContext.getHardware();
        hardwareView.setText(hardware);

        // Architecture
        TextView architectureView = (TextView) mLayout
                .findViewById(R.id.architecture);

        String architecture = shaderContext.getArchitecture();
        architectureView.setText(architecture);

        // Feature
        TextView featureView = (TextView) mLayout.findViewById(R.id.feature);

        String feature = shaderContext.getFeature();
        featureView.setText(feature);

        // Vendor
        TextView vendorView = (TextView) mLayout.findViewById(R.id.vendor);

        String vendor = shaderContext.getVendor();
        vendorView.setText(vendor);

        // Renderer
        TextView rendererView = (TextView) mLayout.findViewById(R.id.renderer);

        String renderer = shaderContext.getRenderer();
        rendererView.setText(renderer);

        // set GLESVersion
        TextView versionView = (TextView) mLayout.findViewById(R.id.version);

        String versionStr = getGLESVersionStr();
        versionView.setText(versionStr);

        TextView extensionView = (TextView) mLayout
                .findViewById(R.id.extensions);
        String extensions = shaderContext.getExtensions();
        extensionView.setText(extensions);

        if (extensions != null) {
            mProgressBar.setVisibility(View.GONE);
        }
    }

    private String getGLESVersionStr() {
        ShaderContext shaderContext = ShaderContext.getInstance();
        String versionStr = shaderContext.getVersionStr();
        if (versionStr != null) {
            return versionStr;
        } else {
            GLESConfig.Version version = shaderContext.getSupportedGLESVersion();
            switch (version) {
                case GLES_31:
                    versionStr = "OpenGL ES 3.1";
                    break;
                case GLES_30:
                    versionStr = "OpenGL ES 3.0";
                    break;
                case GLES_20:
                    versionStr = "OpenGL ES 2.0";
                    break;
                case GLES_10:
                    versionStr = "OpenGL ES 1.0";
                    break;
                default:
                    versionStr = "OpenGL ES 1.0";
            }
        }

        return versionStr;
    }

    void updateDeviceInfo() {
        if (DEBUG) {
            Log.d(TAG, "updateDeviceInfo()");
        }

        if (mLayout == null) {
            LayoutInflater inflater = (LayoutInflater) getActivity()
                    .getSystemService(Service.LAYOUT_INFLATER_SERVICE);
            mLayout = (FrameLayout) inflater.inflate(
                    R.layout.fragment_device_info, null);

            if (mLayout == null) {
                Log.e(TAG, "updateDeviceInfo() mLayout is null");
                return;
            }
        }

        if (mProgressBar == null) {
            mProgressBar = (ProgressBar) mLayout.findViewById(R.id.progressBar);
            if (mProgressBar == null) {
                Log.e(TAG, "updateDeviceInfo() mProgressBar is null");
                return;
            }
        }

        setDeviceInfo();
    }

    @Override
    int getFragmentPosition() {
        return MainActivity.TAB_DEVICEINFO_POSITION;
    }
}
