package com.gomdev.shader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.gomdev.gles.GLESFileUtils;

import java.util.ArrayList;

public class ShaderRestoreDialog extends DialogFragment {
    static final String CLASS = "ShaderRestoreDialog";
    static final String TAG = ShaderConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = ShaderConfig.DEBUG;

    private String mSampleName = null;
    private ArrayList<String> mSelectedShaders = new ArrayList<String>();
    private ArrayList<String> mSavedShaders = new ArrayList<String>();
    private ListView mListView = null;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        ShaderContext context = ShaderContext.getInstance();

        ArrayList<ShaderInfo> shaderInfos = context.getShaderInfoList();
        mSampleName = shaderInfos.get(0).mSampleName;

        for (ShaderInfo info : shaderInfos) {
            if (GLESFileUtils.isExist(info.mFilePath) == true) {
                mSavedShaders.add(info.mTitle);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity,
                android.R.layout.simple_list_item_multiple_choice,
                mSavedShaders);

        mListView = getMultiChoiceListView(adapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.sample_shader_list)
                .setView(mListView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteSelectedFiles();
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                mListView.clearChoices();
                            }
                        });
        return builder.create();
    }

    private ListView getMultiChoiceListView(ListAdapter adapter) {
        Activity activity = getActivity();
        ListView list = new ListView(activity);

        list.setAdapter(adapter);
        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        return list;
    }

    private void deleteSelectedFiles() {
        SparseBooleanArray sb = mListView.getCheckedItemPositions();
        if (sb.size() != 0) {
            for (int i = mListView.getCount() - 1; i >= 0; i--) {
                if (sb.get(i) == true) {
                    mSelectedShaders.add(mSavedShaders.get(i));
                }
            }
        }

        Activity activity = getActivity();
        String savedFileName = null;
        for (String title : mSelectedShaders) {
            savedFileName = ShaderUtils.getSavedFilePath(activity, mSampleName,
                    title);
            GLESFileUtils.delete(savedFileName);
            Toast.makeText(activity, savedFileName + " is deleted",
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        getActivity().recreate();
        super.onDismiss(dialog);
    }
}
