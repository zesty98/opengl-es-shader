package com.gomdev.shader;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class AdFragment extends Fragment {
    static final String CLASS = "AdFragment";
    static final String TAG = ShaderConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = ShaderConfig.DEBUG;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = null;

        if (ShaderConfig.ENABLE_AD) {
            view = inflater.inflate(R.layout.fragment_ad, container, false);
        } else {
            view = inflater.inflate(R.layout.fragment_dummy, container, false);
        }

        return view;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);

        if (ShaderConfig.ENABLE_AD) {
            AdView mAdView = (AdView) getView().findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder()
                    .build();
            mAdView.loadAd(adRequest);
        }
    }

}
