package com.gomdev.shader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;

public class ShaderListDialog extends DialogFragment {
    static final String CLASS = "ShaderListDialog";
    static final String TAG = ShaderConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = ShaderConfig.DEBUG;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ShaderContext context = ShaderContext.getInstance();
        int numOfShaders = context.getNumOfShaders();
        String[] list = new String[numOfShaders];

        ArrayList<ShaderInfo> mShaderInfos = context.getShaderInfoList();

        for (int i = 0; i < numOfShaders; i++) {
            list[i] = mShaderInfos.get(i).mTitle;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.sample_shader_list)
                .setItems(list, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveSelectedShaderInfo(which);

                        Intent intent = new Intent(getActivity(),
                                com.gomdev.shader.ShaderViewActivity.class);
                        startActivity(intent);
                    }
                });
        return builder.create();
    }

    private void saveSelectedShaderInfo(int which) {
        ShaderContext context = ShaderContext.getInstance();
        ArrayList<ShaderInfo> mShaderInfos = context.getShaderInfoList();

        ShaderInfo info = mShaderInfos.get(which);

        context.setSavedShaderInfo(info);
    }
}
