package com.gomdev.shader;

import java.util.ArrayList;

import com.gomdev.gles.GLESConfig;
import com.gomdev.gles.GLESConfig.Version;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class OptionsFragment extends MainFragment {
    static final String CLASS = "ShaderOptionsDialog";
    static final String TAG = ShaderConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = ShaderConfig.DEBUG;

    public enum Options {
        SHOW_INFO(0, "Show informations"),
        SHOW_FPS(1, "Show FPS"),
        USE_GLES30(2, "Use OpenGL ES 3.0");

        private final int mIndex;
        private final String mTitle;

        Options(int index, String optionName) {
            mIndex = index;
            mTitle = optionName;
        }

        public int getIndex() {
            return mIndex;
        }

        public String getTitle() {
            return mTitle;
        }
    }

    public static final Options[] OPTIONS = new Options[] {
            Options.SHOW_INFO,
            Options.USE_GLES30,
            Options.SHOW_FPS
    };

    private ArrayList<String> mOptions = new ArrayList<String>();
    private ArrayAdapter<String> mAdapter = null;
    private int mNumOfOptions = OPTIONS.length;

    private SharedPreferences mPref = null;
    private SharedPreferences.Editor mPrefEditor = null;

    @Override
    public View onCreateView(LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (DEBUG) {
            Log.d(TAG, "onCreateView() " + this);
        }

        mPref = getActivity().getSharedPreferences(ShaderConfig.PREF_NAME, 0);
        mPrefEditor = mPref.edit();

        getOptionsFromPref();

        final ListView listView = makeOptionList(mNumOfOptions);

        return listView;
    }

    private void getOptionsFromPref() {
        final ShaderContext context = ShaderContext.getInstance();

        boolean showInfo = mPref.getBoolean(ShaderConfig.PREF_SHOW_INFO, true);
        context.setShowInfo(showInfo);
        boolean showFPS = mPref.getBoolean(ShaderConfig.PREF_SHOW_FPS, true);
        context.setShowFPS(showFPS);
        boolean useGLES30 = mPref.getBoolean(ShaderConfig.PREF_USE_GLES_30,
                GLESConfig.GLES_VERSION == Version.GLES_30);
        context.setUseGLES30(useGLES30);
    }

    private ListView makeOptionList(final int numOfOptions) {
        final ShaderContext context = ShaderContext.getInstance();

        for (int i = 0; i < mNumOfOptions; i++) {
            mOptions.add(OPTIONS[i].getTitle());
        }

        mAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_multiple_choice, mOptions);

        Activity activity = getActivity();
        final ListView listView = new ListView(activity);
        listView.setAdapter(mAdapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                boolean isChecked = ((CheckedTextView) view).isChecked();
                Options option = OPTIONS[position];
                switch (option) {
                case SHOW_INFO:
                    context.setShowInfo(isChecked);
                    mPrefEditor.putBoolean(
                            ShaderConfig.PREF_SHOW_INFO,
                            isChecked);
                    break;
                case USE_GLES30:
                    context.setUseGLES30(isChecked);
                    mPrefEditor.putBoolean(
                            ShaderConfig.PREF_USE_GLES_30,
                            isChecked);
                    break;
                case SHOW_FPS:
                    context.setShowFPS(isChecked);
                    mPrefEditor.putBoolean(
                            ShaderConfig.PREF_SHOW_FPS,
                            isChecked);
                    break;
                default:
                }

                mPrefEditor.commit();
            }

        });

        for (int i = 0; i < mNumOfOptions; i++) {
            Options option = OPTIONS[i];
            switch (option) {
            case SHOW_INFO:
                listView.setItemChecked(i, context.showInfo());
                break;
            case USE_GLES30:
                listView.setItemChecked(i, context.useGLES30());
                break;
            case SHOW_FPS:
                listView.setItemChecked(i, context.showFPS());
                break;
            }
        }

        return listView;
    }

    @Override
    int getFragmentPosition() {
        return MainActivity.TAB_OPTIONS_POSITION;
    }
}
