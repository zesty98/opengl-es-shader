package com.gomdev.shader;

import android.os.Parcel;
import android.os.Parcelable;

public class ShaderInfo implements Parcelable {

    String mSampleName;
    String mTitle;
    int mResID;
    String mFilePath;

    public static final Parcelable.Creator<ShaderInfo> CREATOR = new
            Parcelable.Creator<ShaderInfo>() {
                public ShaderInfo createFromParcel(Parcel in) {
                    return new ShaderInfo(in);
                }

                public ShaderInfo[] newArray(int size) {
                    return new ShaderInfo[size];
                }
            };

    public ShaderInfo() {

    }

    private ShaderInfo(Parcel in) {
        mSampleName = in.readString();
        mTitle = in.readString();
        mResID = in.readInt();
        mFilePath = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mSampleName);
        dest.writeString(mTitle);
        dest.writeInt(mResID);
        dest.writeString(mFilePath);
    }
}
