package com.gomdev.shader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.gomdev.gles.GLESConfig;
import com.gomdev.gles.GLESConfig.Version;
import com.gomdev.gles.GLESContext;
import com.gomdev.shader.coloredPointAdv.ColoredPointAdvActivity;
import com.gomdev.shader.coloredPointAdv.ColoredPointAdvConfig;
import com.gomdev.shader.coloredPointBasic.ColoredPointBasicConfig;
import com.gomdev.shader.coloredPointBlending.ColoredPointBlendingConfig;
import com.gomdev.shader.coloredRectangle.ColoredRectangleConfig;
import com.gomdev.shader.coloredTriangle.ColoredTriangleConfig;
import com.gomdev.shader.compressedTexture.CompressedTextureConfig;
import com.gomdev.shader.cubemapAdv.CubemapAdvConfig;
import com.gomdev.shader.cubemapBasic.CubemapBasicConfig;
import com.gomdev.shader.galleryIcon.GalleryIconActivity;
import com.gomdev.shader.galleryIcon.GalleryIconConfig;
import com.gomdev.shader.galleryIntro.GalleryIntroActivity;
import com.gomdev.shader.galleryIntro.GalleryIntroConfig;
import com.gomdev.shader.shaderIcon.ShaderIconConfig;
import com.gomdev.shader.instancedRendering.IRConfig;
import com.gomdev.shader.instancedRendering2.IR2Config;
import com.gomdev.shader.mipmap.MipmapConfig;
import com.gomdev.shader.multiLighting.MultiLightingConfig;
import com.gomdev.shader.perFragmentLighting.PFLConfig;
import com.gomdev.shader.perVertexLighting.PVLConfig;
import com.gomdev.shader.shaderIcon.ShaderIconActivity;
import com.gomdev.shader.textRendering.TextRenderingActivity;
import com.gomdev.shader.textRendering.TextRenderingConfig;
import com.gomdev.shader.texturedCube.TexturedCubeConfig;
import com.gomdev.shader.texturedPointAdv.TexturedPointAdvConfig;
import com.gomdev.shader.texturedPointBasic.TexturedPointBasicConfig;
import com.gomdev.shader.texturedRectangle.TexturedRectangleConfig;
import com.gomdev.shader.transformFeedback.TransformFeedbackActivity;
import com.gomdev.shader.transformFeedback.TransformFeedbackConfig;

import java.util.ArrayList;

public class SampleList {
    static final String CLASS = "SampleList";
    static final String TAG = ShaderConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = ShaderConfig.DEBUG;

    private Context mContext = null;
    private ArrayList<SampleInfo> mSamples = new ArrayList<SampleInfo>();

    public SampleList(Context context) {
        mContext = context;
    }

    void setupSampleInfos() {
        mSamples.clear();

        SharedPreferences pref = mContext.getSharedPreferences(
                ShaderConfig.PREF_NAME, 0);
        boolean useGLES30 = pref.getBoolean(ShaderConfig.PREF_USE_GLES_30,
                GLESConfig.DEFAULT_GLES_VERSION == Version.GLES_30);

        Version version = Version.GLES_20;
        if (useGLES30 == true) {
            GLESContext.getInstance().setVersion(Version.GLES_30);
            version = Version.GLES_30;
        } else {
            GLESContext.getInstance().setVersion(Version.GLES_20);
            version = Version.GLES_20;
        }

        setupColoredTriangle(version);
        setupColoredPlane(version);
        setupTexturePlane(version);
        setupTextureCube(version);
        setupShaderIcon(version);
        setupGalleryIcon(version);
        setupGalleryIntro(version);
        setupMipmap(version);
        setupPVL(version);
        setupPFL(version);
        setupMultiLighting(version);
        setupColoredPointBasic(version);
        setupColoredPointAdv(version);
        setupTexturedPointBasic(version);
        setupTexturedPointAdv(version);
        setupColoredPointBlending(version);
        setupCubemap(version);
        setupCubemapAdv(version);
        // setupOQ(version);
        setupIR(version);
        setupIR2(version);
        setupCompressedTexture(version);
        setupTransformFeedback(version);
//        setupTextRendering(version);
        // setupWhitehole(version);

        if (DEBUG) {
            Log.d(TAG, "onCreate() Samples");
            for (SampleInfo sampleInfo : mSamples) {
                Log.d(TAG, "\t " + sampleInfo.mSampleName);
            }
        }
    }

    private void setupIR(Version version) {
        SampleInfo info = new SampleInfo();
        info.mSampleName = IRConfig.EFFECT_NAME;
        info.mIntent = new Intent(mContext,
                com.gomdev.shader.instancedRendering.IRActivity.class);

        if (version == Version.GLES_20) {
            info.mShaderResIDs = new int[]{
                    R.raw.pfl_color_20_vs,
                    R.raw.pfl_color_20_fs,
            };

            info.mShaderTitle = new String[]{
                    "IR 20 VS",
                    "IR 20 FS",
            };
        } else {
            info.mShaderResIDs = new int[]{
                    R.raw.ir_30_vs,
                    R.raw.pfl_color_30_fs
            };

            info.mShaderTitle = new String[]{
                    "IR 30 VS",
                    "IR 30 FS",
            };
        }

        mSamples.add(info);
    }

    private void setupIR2(Version version) {
        SampleInfo info = new SampleInfo();
        info.mSampleName = IR2Config.EFFECT_NAME;
        info.mIntent = new Intent(mContext,
                com.gomdev.shader.instancedRendering2.IR2Activity.class);

        if (version == Version.GLES_20) {
            info.mShaderResIDs = new int[]{
                    R.raw.ir2_20_vs,
                    R.raw.pfl_color_20_fs,
            };

            info.mShaderTitle = new String[]{
                    "IR2 20 VS",
                    "IR2 20 FS",
            };
        } else {
            info.mShaderResIDs = new int[]{
                    R.raw.ir2_30_vs,
                    R.raw.pfl_color_30_fs
            };

            info.mShaderTitle = new String[]{
                    "IR2 30 VS",
                    "IR2 30 FS",
            };
        }

        mSamples.add(info);
    }

    private void setupTexturePlane(Version version) {
        SampleInfo info = new SampleInfo();
        info.mSampleName = TexturedRectangleConfig.EFFECT_NAME;
        info.mIntent = new Intent(
                mContext,
                com.gomdev.shader.texturedRectangle.TexturedRectangleActivity.class);

        if (version == Version.GLES_20) {
            info.mShaderResIDs = new int[]{
                    R.raw.texture_20_vs,
                    R.raw.texture_20_fs,
            };

            info.mShaderTitle = new String[]{
                    "Texture Rectangle 20 VS",
                    "Texture Rectangle 20 FS",
            };
        } else {
            info.mShaderResIDs = new int[]{
                    R.raw.texture_30_vs,
                    R.raw.texture_30_fs,
            };

            info.mShaderTitle = new String[]{
                    "Texture Rectangle 30 VS",
                    "Texture Rectangle 30 FS",
            };
        }

        mSamples.add(info);
    }

    private void setupTextureCube(Version version) {
        SampleInfo info = new SampleInfo();
        info.mSampleName = TexturedCubeConfig.EFFECT_NAME;
        info.mIntent = new Intent(
                mContext,
                com.gomdev.shader.texturedCube.TexturedCubeActivity.class);

        if (version == Version.GLES_20) {
            info.mShaderResIDs = new int[]{
                    R.raw.texture_20_vs,
                    R.raw.texture_20_fs,
            };

            info.mShaderTitle = new String[]{
                    "Texture Cube 20 VS",
                    "Texture Cube 20 FS",
            };
        } else {
            info.mShaderResIDs = new int[]{
                    R.raw.texture_30_vs,
                    R.raw.texture_30_fs,
            };

            info.mShaderTitle = new String[]{
                    "Texture Cube 30 VS",
                    "Texture Cube 30 FS",
            };
        }

        mSamples.add(info);
    }

    private void setupMipmap(Version version) {
        SampleInfo info = new SampleInfo();
        info.mSampleName = MipmapConfig.EFFECT_NAME;
        info.mIntent = new Intent(mContext,
                com.gomdev.shader.mipmap.MipmapActivity.class);

        if (version == Version.GLES_20) {
            info.mShaderResIDs = new int[]{
                    R.raw.texture_20_vs,
                    R.raw.texture_20_fs,
            };

            info.mShaderTitle = new String[]{
                    "Mipmapping 20 VS",
                    "Mipmapping 20 FS",
            };
        } else {
            info.mShaderResIDs = new int[]{
                    R.raw.texture_30_vs,
                    R.raw.texture_30_fs,
            };

            info.mShaderTitle = new String[]{
                    "Mipmaping 30 VS",
                    "Mipampping 30 FS",
            };
        }

        mSamples.add(info);
    }

    private void setupColoredTriangle(Version version) {
        SampleInfo info = new SampleInfo();
        info.mSampleName = ColoredTriangleConfig.EFFECT_NAME;
        info.mIntent = new Intent(mContext,
                com.gomdev.shader.coloredTriangle.ColoredTriangleActivity.class);
        if (version == Version.GLES_20) {
            info.mShaderResIDs = new int[]{
                    R.raw.color_20_vs,
                    R.raw.color_20_fs,
            };

            info.mShaderTitle = new String[]{
                    "Colored Triangle 20 VS",
                    "Colored Triangle 20 FS",
            };
        } else {
            info.mShaderResIDs = new int[]{
                    R.raw.color_30_vs,
                    R.raw.color_30_fs
            };

            info.mShaderTitle = new String[]{
                    "Colored Triangle 30 VS",
                    "Colored Triangle 30 FS",
            };
        }

        mSamples.add(info);
    }

    private void setupColoredPlane(Version version) {
        SampleInfo info = new SampleInfo();
        info.mSampleName = ColoredRectangleConfig.EFFECT_NAME;
        info.mIntent = new Intent(
                mContext,
                com.gomdev.shader.coloredRectangle.ColoredRectangleActivity.class);
        if (version == Version.GLES_20) {
            info.mShaderResIDs = new int[]{
                    R.raw.color_20_vs,
                    R.raw.color_20_fs,
            };

            info.mShaderTitle = new String[]{
                    "Colored Rectangle 20 VS",
                    "Colored Rectangle 20 FS",
            };
        } else {
            info.mShaderResIDs = new int[]{
                    R.raw.color_30_vs,
                    R.raw.color_30_fs
            };

            info.mShaderTitle = new String[]{
                    "Colored Rectangle 30 VS",
                    "Colored Rectangle 30 FS",
            };
        }

        mSamples.add(info);
    }

    private void setupColoredPointBasic(Version version) {
        SampleInfo info = new SampleInfo();
        info.mSampleName = ColoredPointBasicConfig.EFFECT_NAME;
        info.mIntent = new Intent(
                mContext,
                com.gomdev.shader.coloredPointBasic.ColoredPointBasicActivity.class);
        if (version == Version.GLES_20) {
            info.mShaderResIDs = new int[]{
                    R.raw.point_color_20_vs,
                    R.raw.color_20_fs,
            };

            info.mShaderTitle = new String[]{
                    "Colored Point 20 VS",
                    "Colored Point 20 FS",
            };
        } else {
            info.mShaderResIDs = new int[]{
                    R.raw.point_color_30_vs,
                    R.raw.color_30_fs
            };

            info.mShaderTitle = new String[]{
                    "Colored Point 30 VS",
                    "Colored Point 30 FS",
            };
        }

        mSamples.add(info);
    }

    private void setupColoredPointAdv(Version version) {
        SampleInfo info = new SampleInfo();
        info.mSampleName = ColoredPointAdvConfig.EFFECT_NAME;
        info.mIntent = new Intent(
                mContext,
                ColoredPointAdvActivity.class);
        if (version == Version.GLES_20) {
            info.mShaderResIDs = new int[]{
                    R.raw.point2_color_20_vs,
                    R.raw.point2_color_20_fs,
            };

            info.mShaderTitle = new String[]{
                    "Colored Point 20 VS",
                    "Colored Point 20 FS",
            };
        } else {
            info.mShaderResIDs = new int[]{
                    R.raw.point2_color_30_vs,
                    R.raw.point2_color_30_fs
            };

            info.mShaderTitle = new String[]{
                    "Colored Point 30 VS",
                    "Colored Point 30 FS",
            };
        }

        mSamples.add(info);
    }

    private void setupTexturedPointBasic(Version version) {
        SampleInfo info = new SampleInfo();
        info.mSampleName = TexturedPointBasicConfig.EFFECT_NAME;
        info.mIntent = new Intent(
                mContext,
                com.gomdev.shader.texturedPointBasic.TexturedPointBasicActivity.class);
        if (version == Version.GLES_20) {
            info.mShaderResIDs = new int[]{
                    R.raw.point_texture_20_vs,
                    R.raw.point_texture_20_fs,
            };

            info.mShaderTitle = new String[]{
                    "Textured Point 20 VS",
                    "Textured Point 20 FS",
            };
        } else {
            info.mShaderResIDs = new int[]{
                    R.raw.point_texture_30_vs,
                    R.raw.point_texture_30_fs
            };

            info.mShaderTitle = new String[]{
                    "Textured Point 30 VS",
                    "Textured Point 30 FS",
            };
        }

        mSamples.add(info);
    }

    private void setupTexturedPointAdv(Version version) {
        SampleInfo info = new SampleInfo();
        info.mSampleName = TexturedPointAdvConfig.EFFECT_NAME;
        info.mIntent = new Intent(
                mContext,
                com.gomdev.shader.texturedPointAdv.TexturedPointAdvActivity.class);
        if (version == Version.GLES_20) {
            info.mShaderResIDs = new int[]{
                    R.raw.point2_texture_20_vs,
                    R.raw.point2_texture_20_fs,
            };

            info.mShaderTitle = new String[]{
                    "Textured Point 20 VS",
                    "Textured Point 20 FS",
            };
        } else {
            info.mShaderResIDs = new int[]{
                    R.raw.point2_texture_30_vs,
                    R.raw.point2_texture_30_fs
            };

            info.mShaderTitle = new String[]{
                    "Textured Point 30 VS",
                    "Textured Point 30 FS",
            };
        }

        mSamples.add(info);
    }

    private void setupColoredPointBlending(Version version) {
        SampleInfo info = new SampleInfo();
        info.mSampleName = ColoredPointBlendingConfig.EFFECT_NAME;
        info.mIntent = new Intent(
                mContext,
                com.gomdev.shader.coloredPointBlending.ColoredPointBlendingActivity.class);
        if (version == Version.GLES_20) {
            info.mShaderResIDs = new int[]{
                    R.raw.point_cs_20_vs,
                    R.raw.custom_point_c_20_fs,
                    R.raw.color_20_vs,
                    R.raw.color_20_fs
            };

            info.mShaderTitle = new String[]{
                    "Colored Point 20 VS",
                    "Colored Point 20 FS",
                    "Cube 20 VS",
                    "Cube 20 FS"
            };
        } else {
            info.mShaderResIDs = new int[]{
                    R.raw.point_cs_30_vs,
                    R.raw.custom_point_c_30_fs,
                    R.raw.color_30_vs,
                    R.raw.color_30_fs
            };

            info.mShaderTitle = new String[]{
                    "Colored Point 30 VS",
                    "Colored Point 30 FS",
                    "Cube 30 VS",
                    "Cube 30 FS"
            };
        }

        mSamples.add(info);
    }

    private void setupShaderIcon(Version version) {
        SampleInfo info = new SampleInfo();
        info.mSampleName = ShaderIconConfig.EFFECT_NAME;
        info.mIntent = new Intent(mContext,
                ShaderIconActivity.class);
        if (version == Version.GLES_20) {
            info.mShaderResIDs = new int[]{
                    R.raw.color_20_vs,
                    R.raw.color_20_fs,
                    R.raw.texture_20_vs,
                    R.raw.texture_20_fs
            };

            info.mShaderTitle = new String[]{
                    "Object 20 VS",
                    "Object 20 FS",
                    "BG 20 VS",
                    "BG 20 FS",
            };
        } else {
            info.mShaderResIDs = new int[]{
                    R.raw.color_30_vs,
                    R.raw.color_30_fs,
                    R.raw.texture_30_vs,
                    R.raw.texture_30_fs
            };

            info.mShaderTitle = new String[]{
                    "Object 30 VS",
                    "Object 30 FS",
                    "BG 30 VS",
                    "BG 30 FS",
            };
        }

        mSamples.add(info);
    }

    private void setupGalleryIcon(Version version) {
        SampleInfo info = new SampleInfo();
        info.mSampleName = GalleryIconConfig.EFFECT_NAME;
        info.mIntent = new Intent(mContext,
                GalleryIconActivity.class);
        if (version == Version.GLES_20) {
            info.mShaderResIDs = new int[]{
                    R.raw.color_20_vs,
                    R.raw.color_20_fs,
                    R.raw.texture_20_vs,
                    R.raw.texture_20_fs
            };

            info.mShaderTitle = new String[]{
                    "Object 20 VS",
                    "Object 20 FS",
                    "BG 20 VS",
                    "BG 20 FS",
            };
        } else {
            info.mShaderResIDs = new int[]{
                    R.raw.color_30_vs,
                    R.raw.color_30_fs,
                    R.raw.texture_30_vs,
                    R.raw.texture_30_fs
            };

            info.mShaderTitle = new String[]{
                    "Object 30 VS",
                    "Object 30 FS",
                    "BG 30 VS",
                    "BG 30 FS",
            };
        }

        mSamples.add(info);
    }

    private void setupGalleryIntro(Version version) {
        SampleInfo info = new SampleInfo();
        info.mSampleName = GalleryIntroConfig.EFFECT_NAME;
        info.mIntent = new Intent(mContext,
                GalleryIntroActivity.class);
        if (version == Version.GLES_20) {
            info.mShaderResIDs = new int[]{
                    R.raw.gallery_intro_20_vs,
                    R.raw.gallery_intro_20_fs
            };

            info.mShaderTitle = new String[]{
                    "Gallery Intro 20 VS",
                    "Gallery Intro 20 FS"
            };
        } else {
            info.mShaderResIDs = new int[]{
                    R.raw.gallery_intro_20_vs,
                    R.raw.gallery_intro_20_fs
            };

            info.mShaderTitle = new String[]{
                    "Gallery Intro 20 VS",
                    "Gallery Intro 20 FS"
            };
        }

        mSamples.add(info);
    }

    private void setupPVL(Version version) {
        SampleInfo info = new SampleInfo();
        info.mSampleName = PVLConfig.EFFECT_NAME;
        info.mIntent = new Intent(mContext,
                com.gomdev.shader.perVertexLighting.PVLActivity.class);
        if (version == Version.GLES_20) {
            info.mShaderResIDs = new int[]{
                    R.raw.pvl_color_20_vs,
                    R.raw.pvl_color_20_fs
            };

            info.mShaderTitle = new String[]{
                    "Per Vertex Lighting 20 VS",
                    "Per Vertex Lighting 20 FS"
            };
        } else {
            info.mShaderResIDs = new int[]{
                    R.raw.pvl_color_30_vs,
                    R.raw.pvl_color_30_fs
            };

            info.mShaderTitle = new String[]{
                    "Per Vertex Lighting 30 VS",
                    "Per Vertex Lighting 30 FS"
            };
        }

        mSamples.add(info);
    }

    private void setupCubemap(Version version) {
        SampleInfo info = new SampleInfo();
        info.mSampleName = CubemapBasicConfig.EFFECT_NAME;
        info.mIntent = new Intent(mContext,
                com.gomdev.shader.cubemapBasic.CubemapBasicActivity.class);

        if (version == Version.GLES_20) {
            info.mShaderResIDs = new int[]{
                    R.raw.cubemap_texture_20_vs,
                    R.raw.cubemap_texture_20_fs,
            };

            info.mShaderTitle = new String[]{
                    "Cubemap 20 VS",
                    "Cubemap 20 FS",
            };
        } else {
            info.mShaderResIDs = new int[]{
                    R.raw.cubemap_texture_30_vs,
                    R.raw.cubemap_texture_30_fs,
            };

            info.mShaderTitle = new String[]{
                    "Cubemap 30 VS",
                    "Cubemap 30 FS",
            };
        }

        mSamples.add(info);
    }

    private void setupCubemapAdv(Version version) {
        SampleInfo info = new SampleInfo();
        info.mSampleName = CubemapAdvConfig.EFFECT_NAME;
        info.mIntent = new Intent(mContext,
                com.gomdev.shader.cubemapAdv.CubemapAdvActivity.class);

        if (version == Version.GLES_20) {
            info.mShaderResIDs = new int[]{
                    R.raw.cubemap2_texture_20_vs,
                    R.raw.cubemap2_texture_20_fs,
            };

            info.mShaderTitle = new String[]{
                    "Cubemap 20 VS",
                    "Cubemap 20 FS",
            };
        } else {
            info.mShaderResIDs = new int[]{
                    R.raw.cubemap2_texture_30_vs,
                    R.raw.cubemap2_texture_30_fs,
            };

            info.mShaderTitle = new String[]{
                    "Cubemap 30 VS",
                    "Cubemap 30 FS",
            };
        }

        mSamples.add(info);
    }

    private void setupPFL(Version version) {
        SampleInfo info = new SampleInfo();
        info.mSampleName = PFLConfig.EFFECT_NAME;
        info.mIntent = new Intent(mContext,
                com.gomdev.shader.perFragmentLighting.PFLActivity.class);
        if (version == Version.GLES_20) {
            info.mShaderResIDs = new int[]{
                    R.raw.pfl_color_20_vs,
                    R.raw.pfl_color_20_fs
            };

            info.mShaderTitle = new String[]{
                    "Per Fragment Lighting 20 VS",
                    "Per Fragment Lighting 20 FS"
            };
        } else {
            info.mShaderResIDs = new int[]{
                    R.raw.pfl_color_30_vs,
                    R.raw.pfl_color_30_fs
            };

            info.mShaderTitle = new String[]{
                    "Per Fragment Lighting 30 VS",
                    "Per Fragment Lighting 30 FS"
            };
        }

        mSamples.add(info);
    }

    // private void setupOQ(Version version) {
    // SampleInfo info = new SampleInfo();
    // info.mSampleName = OQConfig.EFFECT_NAME;
    // info.mIntent = new Intent(mContext,
    // com.gomdev.shader.occlusionQuery.OQActivity.class);
    // if (version == Version.GLES_20) {
    // info.mShaderResIDs = new int[] {
    // R.raw.oq_20_vs,
    // R.raw.oq_20_fs,
    // };
    //
    // info.mShaderTitle = new String[] {
    // "Occlusion Query 20 VS",
    // "Occlusion Query 20 FS",
    // };
    // } else {
    // info.mShaderResIDs = new int[] {
    // R.raw.oq_30_vs,
    // R.raw.oq_30_fs
    // };
    //
    // info.mShaderTitle = new String[] {
    // "Occlusion Query 30 VS",
    // "Occlusion Query 30 FS",
    // };
    // }
    //
    // mSamples.add(info);
    // }

    private void setupMultiLighting(Version version) {
        SampleInfo info = new SampleInfo();
        info.mSampleName = MultiLightingConfig.EFFECT_NAME;
        info.mIntent = new Intent(mContext,
                com.gomdev.shader.multiLighting.MultiLightingActivity.class);
        if (version == Version.GLES_20) {
            info.mShaderResIDs = new int[]{
                    R.raw.pfl_color_20_vs,
                    R.raw.pfl_color_20_fs,
            };

            info.mShaderTitle = new String[]{
                    "MultiLighting 20 VS",
                    "MultiLighting 20 FS",
            };
        } else {
            info.mShaderResIDs = new int[]{
                    R.raw.pfl_color_30_vs,
                    R.raw.pfl_color_30_fs
            };

            info.mShaderTitle = new String[]{
                    "MultiLighting 30 VS",
                    "MultiLighting 30 FS",
            };
        }

        mSamples.add(info);
    }

    private void setupCompressedTexture(Version version) {
        SampleInfo info = new SampleInfo();
        info.mSampleName = CompressedTextureConfig.EFFECT_NAME;
        info.mIntent = new Intent(mContext,
                com.gomdev.shader.compressedTexture.CompressedTextureActivity.class);
        if (version == Version.GLES_20) {
            info.mShaderResIDs = new int[]{
                    R.raw.texture_20_vs,
                    R.raw.texture_20_fs,
            };

            info.mShaderTitle = new String[]{
                    "ComressedTexture 20 VS",
                    "ComressedTexture 20 FS",
            };
        } else {
            info.mShaderResIDs = new int[]{
                    R.raw.texture_30_vs,
                    R.raw.texture_30_fs
            };

            info.mShaderTitle = new String[]{
                    "ComressedTexture 30 VS",
                    "ComressedTexture 30 FS",
            };
        }

        mSamples.add(info);
    }

    private void setupTransformFeedback(Version version) {
        SampleInfo info = new SampleInfo();
        info.mSampleName = TransformFeedbackConfig.EFFECT_NAME;
        info.mIntent = new Intent(
                mContext,
                TransformFeedbackActivity.class);
        if (version == Version.GLES_20) {
            info.mShaderResIDs = new int[]{
                    R.raw.point2_color_20_vs,
                    R.raw.transformfeedback_20_fs,
            };

            info.mShaderTitle = new String[]{
                    "Transform feedback 20 VS",
                    "Transform feedback 20 FS",
            };
        } else {
            info.mShaderResIDs = new int[]{
                    R.raw.transformfeedback_30_vs,
                    R.raw.transformfeedback_30_fs
            };

            info.mShaderTitle = new String[]{
                    "Transform feedback 30 VS",
                    "Transform feedback 30 FS",
            };
        }

        mSamples.add(info);
    }

    private void setupTextRendering(Version version) {
        SampleInfo info = new SampleInfo();
        info.mSampleName = TextRenderingConfig.EFFECT_NAME;
        info.mIntent = new Intent(mContext,
                TextRenderingActivity.class);

        if (version == Version.GLES_20) {
            info.mShaderResIDs = new int[]{
                    R.raw.texture_20_vs,
                    R.raw.texture_20_fs,
            };

            info.mShaderTitle = new String[]{
                    "Texture Rectangle 20 VS",
                    "Texture Rectangle 20 FS",
            };
        } else {
            info.mShaderResIDs = new int[]{
                    R.raw.texture_30_vs,
                    R.raw.texture_30_fs,
            };

            info.mShaderTitle = new String[]{
                    "Texture Rectangle 30 VS",
                    "Texture Rectangle 30 FS",
            };
        }

        mSamples.add(info);
    }

    // private void setupWhitehole(Version version) {
    // SampleInfo info = new SampleInfo();
    // info.mSampleName = WhiteholeConfig.EFFECT_NAME;
    // info.mIntent = new Intent(mContext,
    // com.gomdev.shader.whitehole.WhiteholeActivity.class);
    // if (version == Version.GLES_20) {
    // info.mShaderResIDs = new int[] {
    // R.raw.whitehole_20_vs,
    // R.raw.whitehole_20_fs,
    // };
    //
    // info.mShaderTitle = new String[] {
    // "Whitehole 20 VS",
    // "Whitehole 20 FS",
    // };
    // } else {
    // info.mShaderResIDs = new int[] {
    // R.raw.whitehole_30_vs,
    // R.raw.whitehole_30_fs
    // };
    //
    // info.mShaderTitle = new String[] {
    // "Whitehole 30 VS",
    // "Whitehole 30 FS",
    // };
    // }
    //
    // mSamples.add(info);
    // }

    void makeSampleList() {
        ArrayList<String> sampleList = new ArrayList<String>();
        for (int i = 0; i < mSamples.size(); i++) {
            SampleInfo info = mSamples.get(i);
            String sampleTitle = (i + 1) + ". " + info.mSampleName;
            sampleList.add(sampleTitle);
        }

        if (DEBUG) {
            Log.d(TAG, "onCreate() string list");

            for (String str : sampleList) {
                Log.d(TAG, "\t Item=" + str);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext,
                android.R.layout.simple_list_item_1, sampleList);

        ListView list = (ListView) ((Activity) mContext)
                .findViewById(R.id.list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(mItemClickListener);
    }

    ArrayList<String> getTitleList() {
        ArrayList<String> sampleList = new ArrayList<String>();
        for (int i = 0; i < mSamples.size(); i++) {
            SampleInfo info = mSamples.get(i);
            String sampleTitle = (i + 1) + ". " + info.mSampleName;
            sampleList.add(sampleTitle);
        }

        if (DEBUG) {
            Log.d(TAG, "onCreate() string list");

            for (String str : sampleList) {
                Log.d(TAG, "\t Item=" + str);
            }
        }

        return sampleList;
    }

    private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            String sampleName = parent.getItemAtPosition(position).toString();

            SampleInfo info = getSampleInfo(sampleName);
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
                        mContext, info.mSampleName, title);
                context.setShaderInfo(info.mSampleName, info.mShaderTitle[i],
                        info.mShaderResIDs[i], savedFileName);
            }

            if (DEBUG) {
                Log.d(TAG, "onItemClick() item=" + sampleName);
            }
            ((Activity) mContext).startActivity(info.mIntent);
        }
    };

    SampleInfo getSampleInfo(String sampleName) {
        for (SampleInfo sampleInfo : mSamples) {
            int index = sampleName.indexOf(' ');
            String name = sampleName.substring(index + 1);
            if (name.compareTo(sampleInfo.mSampleName) == 0) {
                return sampleInfo;
            }
        }

        return null;
    }
}
