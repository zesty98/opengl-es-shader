package com.gomdev.shader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

@SuppressLint("InflateParams")
public class DeviceInfoFragment extends MainFragment {
    static final String CLASS = "DeviceInfoFragment";
    static final String TAG = ShaderConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = ShaderConfig.DEBUG;

    enum GLES_VERSION {
        GLES_10,
        GLES_20,
        GLES_30,
        GLES_31
    }

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

        // Hardware
        TextView hardwareView = (TextView) mLayout.findViewById(R.id.hardware);

        String hardware = ShaderContext.getInstance().getHardware();
        hardwareView.setText(hardware);

        // Architecture
        TextView architectureView = (TextView) mLayout
                .findViewById(R.id.architecture);

        String architecture = ShaderContext.getInstance().getArchitecture();
        architectureView.setText(architecture);

        // Feature
        TextView featureView = (TextView) mLayout.findViewById(R.id.feature);

        String feature = ShaderContext.getInstance().getFeature();
        featureView.setText(feature);

        // Vendor
        TextView vendorView = (TextView) mLayout.findViewById(R.id.vendor);

        String vendor = ShaderContext.getInstance().getVendor();
        vendorView.setText(vendor);

        // Renderer
        TextView rendererView = (TextView) mLayout.findViewById(R.id.renderer);

        String renderer = ShaderContext.getInstance().getRenderer();
        rendererView.setText(renderer);

        // set GLESVersion
        TextView versionView = (TextView) mLayout.findViewById(R.id.version);

        String versionStr = ShaderContext.getInstance().getVersion();
        if (versionStr != null) {
            versionView.setText(versionStr);
        } else {
            GLES_VERSION version = getGLESVersion();
            switch (version) {
            case GLES_31:
                versionView.setText("OpenGL ES 3.1");
                break;
            case GLES_30:
                versionView.setText("OpenGL ES 3.0");
                break;
            case GLES_20:
                versionView.setText("OpenGL ES 2.0");
                break;
            case GLES_10:
                versionView.setText("OpenGL ES 1.0");
                break;
            default:
            }
        }

        TextView extensionView = (TextView) mLayout
                .findViewById(R.id.extensions);
        String extensions = ShaderContext.getInstance().getExtensions();
        extensionView.setText(extensions);

        if (extensions != null) {
            mProgressBar.setVisibility(View.GONE);
        }
    }

    private GLES_VERSION getGLESVersion() {
        Activity activity = getActivity();
        ActivityManager am = (ActivityManager) activity
                .getSystemService(Context.ACTIVITY_SERVICE);

        ConfigurationInfo info = am.getDeviceConfigurationInfo();

        if (info.reqGlEsVersion >= 0x31000) {
            return GLES_VERSION.GLES_31;
        } else if (info.reqGlEsVersion >= 0x30000) {
            return GLES_VERSION.GLES_30;
        } else if (info.reqGlEsVersion >= 0x20000) {
            return GLES_VERSION.GLES_20;
        } else if (info.reqGlEsVersion >= 0x10000) {
            return GLES_VERSION.GLES_10;
        }
        return GLES_VERSION.GLES_10;
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
