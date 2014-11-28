package com.gomdev.shader;

import java.util.ArrayList;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class SampleListFragment extends MainFragment {
    private SampleList mSampleList = null;

    public SampleListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (DEBUG) {
            Log.d(TAG, "onCreateView() " + this);
        }

        View rootView = inflater.inflate(R.layout.fragment_list, container,
                false);

        mSampleList = new SampleList(getActivity());
        mSampleList.setupSampleInfos();

        ArrayList<String> mTitles = mSampleList.getTitleList();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, mTitles);

        ListView list = (ListView) rootView.findViewById(R.id.list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(mItemClickListener);

        return rootView;
    }

    void resetSampleList() {
        mSampleList.setupSampleInfos();
    }

    @Override
    int getFragmentPosition() {
        return MainActivity.TAB_SAMPLELIST_POSITION;
    }

    private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
            String sampleName = parent.getItemAtPosition(position).toString();

            SampleInfo info = mSampleList.getSampleInfo(sampleName);
            int numOfShader = info.mShaderResIDs.length;

            ShaderContext context = ShaderContext.getInstance();
            context.setSampleName(sampleName);
            context.setNumOfShaders(numOfShader);

            context.clearShaderInfos();

            String title = null;
            String savedFileName = null;
            for (int i = 0; i < numOfShader; i++) {
                title = info.mShaderTitle[i];
                savedFileName = ShaderUtils.getSavedFilePath(
                        getActivity(), info.mSampleName, title);
                context.setShaderInfo(info.mSampleName, info.mShaderTitle[i],
                        info.mShaderResIDs[i], savedFileName);
            }

            getActivity().startActivity(info.mIntent);
        }
    };
}
