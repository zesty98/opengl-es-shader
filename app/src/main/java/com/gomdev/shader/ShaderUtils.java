package com.gomdev.shader;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.gomdev.gles.GLESFileUtils;
import com.gomdev.gles.GLESUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ShaderUtils {
    static final String CLASS = "ShaderUtils";
    static final String TAG = ShaderConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = ShaderConfig.DEBUG;

    private static final String CPU_FILE = "/proc/cpuinfo";

    public static String getSavedFilePath(Context context, String prefix,
                                          String shaderTitle) {
        File file = context.getExternalFilesDir(null);
        StringBuilder builder = new StringBuilder(file.getAbsolutePath());
        builder.append(File.separatorChar);
        builder.append(prefix);
        builder.append("_");
        builder.append(shaderTitle);
        builder.append(".dat");

        return builder.toString();
    }

    public static String getShaderSource(Context context) {
        ShaderContext shaderContext = ShaderContext.getInstance();
        ShaderInfo savedShaderInfo = shaderContext.getSavedShaderInfo();

        String savedFileName = savedShaderInfo.mFilePath;
        String shaderSource = null;
        File file = new File(savedFileName);
        if (file.exists() == true) {
            shaderSource = GLESFileUtils.read(savedFileName);
        } else {
            int shaderResID = savedShaderInfo.mResID;
            shaderSource = GLESUtils
                    .getStringFromReosurce(context, shaderResID);
        }

        return shaderSource;
    }

    public static String getShaderSource(Context context, int i) {
        ShaderContext shaderContext = ShaderContext.getInstance();

        ArrayList<ShaderInfo> shaderInfos = shaderContext.getShaderInfoList();
        ShaderInfo shaderInfo = shaderInfos.get(i);

        String savedFileName = shaderInfo.mFilePath;
        String shaderSource = null;
        File file = new File(savedFileName);
        if (file.exists() == true) {
            shaderSource = GLESFileUtils.read(savedFileName);
        } else {
            int shaderResID = shaderInfo.mResID;
            shaderSource = GLESUtils
                    .getStringFromReosurce(context, shaderResID);
        }

        return shaderSource;
    }

    public static String getFragmentShaderSource(Context context, int i) {
        ShaderContext shaderContext = ShaderContext.getInstance();

        ArrayList<ShaderInfo> shaderInfos = shaderContext.getShaderInfoList();
        ShaderInfo shaderInfo = shaderInfos.get(i);

        String savedFileName = shaderInfo.mFilePath;
        String shaderSource = null;
        File file = new File(savedFileName);
        if (file.exists() == true) {
            shaderSource = GLESFileUtils.read(savedFileName);
        } else {
            int shaderResID = shaderInfo.mResID;
            shaderSource = GLESUtils
                    .getStringFromReosurce(context, shaderResID);
        }

        return shaderSource;
    }

    public static void restoreShaderContext(Bundle icicle) {
        ShaderContext context = ShaderContext.getInstance();
        if (context == null) {
            context = ShaderContext.newInstance();
        }

        ArrayList<ShaderInfo> prefShaderInfoList = context.getShaderInfoList();
        ArrayList<ShaderInfo> shaderInfoList = new ArrayList<ShaderInfo>();

        ArrayList<ShaderInfo> savedShaderInfoList = icicle
                .getParcelableArrayList(ShaderConfig.STATE_SHADER_INFO);

        int size = savedShaderInfoList.size();
        for (int i = 0; i < size; i++) {
            shaderInfoList.add(savedShaderInfoList.get(i));
        }
        context.setShaderInfoList(shaderInfoList);
        prefShaderInfoList.clear();

        context.setSampleName(icicle.getString(ShaderConfig.STATE_SAMPLE_NAME));
        context.setNumOfShaders(icicle.getInt(ShaderConfig.STATE_NUM_OF_SHADER));
        context.setSavedShaderInfo((ShaderInfo) icicle
                .getParcelable(ShaderConfig.STATE_SAVED_SHADER_INFO));
        context.setShowInfo(icicle.getBoolean(ShaderConfig.STATE_SHOW_INFO));
        context.setShowFPS(icicle.getBoolean(ShaderConfig.STATE_SHOW_FPS));
        context.setUseGLES30(icicle.getBoolean(ShaderConfig.STATE_USE_GLES30));
        context.setExtensions(icicle.getString(ShaderConfig.STATE_EXTENSIONS));
    }

    public static void saveShaderContext(Bundle outState) {
        ShaderContext context = ShaderContext.getInstance();
        outState.putParcelableArrayList(ShaderConfig.STATE_SHADER_INFO,
                context.getShaderInfoList());
        outState.putString(ShaderConfig.STATE_SAMPLE_NAME,
                context.getSampleName());
        outState.putInt(ShaderConfig.STATE_NUM_OF_SHADER,
                context.getNumOfShaders());
        outState.putParcelable(ShaderConfig.STATE_SAVED_SHADER_INFO,
                context.getSavedShaderInfo());
        outState.putBoolean(ShaderConfig.STATE_SHOW_INFO, context.showInfo());
        outState.putBoolean(ShaderConfig.STATE_SHOW_FPS, context.showFPS());
        outState.putBoolean(ShaderConfig.STATE_USE_GLES30, context.useGLES30());
        outState.putString(ShaderConfig.STATE_EXTENSIONS,
                context.getExtensions());
    }

    public static Map<String, String> getCPUInfo(String[] infos) {
        Map<String, String> cpuInfos = new HashMap<String, String>();
        FileReader fstream;

        try {
            fstream = new FileReader(CPU_FILE);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Could not read " + CPU_FILE);
            return null;
        }

        BufferedReader in = new BufferedReader(fstream, 500);
        String line;
        try {
            while ((line = in.readLine()) != null) {
                for (String str : infos) {
                    if (line.indexOf(str) >= 0) {
                        int index = line.indexOf(":");
                        cpuInfos.put(str, line.substring(index + 2));
                    }
                }
            }
        } catch (IOException e) {
            Log.e("readMem", e.toString());
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return cpuInfos;
    }
}
