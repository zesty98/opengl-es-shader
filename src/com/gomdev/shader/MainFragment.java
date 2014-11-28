package com.gomdev.shader;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.util.Log;

public abstract class MainFragment extends Fragment {
    static final String CLASS = "MainFragment";
    static final String TAG = ShaderConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = ShaderConfig.DEBUG;

    private Activity mActivity = null;

    @Override
    public void onDestroy() {
        if (DEBUG) {
            Log.d(TAG, "onDestroy() " + this);
        }
        super.onDestroy();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (DEBUG) {
            Log.d(TAG, "onAttach() " + this);
        }

        mActivity = activity;
        ((MainActivity) activity).setCurrentFragment(getFragmentPosition(), this);
    }

    @Override
    public void onDetach() {
        if (DEBUG) {
            Log.d(TAG, "onDestroy() " + this);
        }

        if (mActivity != null) {
            ((MainActivity) mActivity).setCurrentFragment(getFragmentPosition(),
                    this);
        }

        super.onDetach();
    }

    abstract int getFragmentPosition();

}
