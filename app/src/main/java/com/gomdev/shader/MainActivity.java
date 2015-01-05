package com.gomdev.shader;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Map;

public class MainActivity extends FragmentActivity {
    static final String CLASS = "MainActivity";
    static final String TAG = ShaderConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = ShaderConfig.DEBUG;

    static final int REMOVE_DUMMY_GL_SURFACE = 100;

    static final int TAB_SAMPLELIST_POSITION = 0;
    static final int TAB_OPTIONS_POSITION = 1;
    static final int TAB_DEVICEINFO_POSITION = 2;

    class TabInfo {
        int mPosition;
        String mTitle;
        Class<?> mClass;
        Bundle mArgs;
        Fragment mFragment;

        public TabInfo(int position, String title, Class<?> clss, Bundle args) {
            mPosition = position;
            mTitle = title;
            mClass = clss;
            mArgs = args;
        }
    }

    private ArrayList<TabInfo> mTabInfos = new ArrayList<TabInfo>();

    private TabsAdapter mTabsAdapter;
    private ViewPager mViewPager;
    private SlidingTabLayout mSlidingTabLayout;

    private GLSurfaceView mView;
    private DummyRenderer mRenderer;

    private boolean mIsGPUInfoUpdated = false;

    public MainActivity() {
        if (DEBUG) {
            Log.d(TAG, "MainActivity()");
        }

        createTabInfos();
    }

    private void createTabInfos() {
        mTabInfos.clear();

        TabInfo tab = new TabInfo(TAB_SAMPLELIST_POSITION, "Samples",
                SampleListFragment.class, null);
        mTabInfos.add(tab);
        tab = new TabInfo(TAB_OPTIONS_POSITION, "Options",
                OptionsFragment.class, null);
        mTabInfos.add(tab);
        tab = new TabInfo(TAB_DEVICEINFO_POSITION, "DeviceInfo",
                DeviceInfoFragment.class, null);
        mTabInfos.add(tab);
    }

    @SuppressLint("NewApi")
    public void onCreate(Bundle savedInstanceState) {
        if (DEBUG) {
            Log.d(TAG, "onCreate()");
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getActionBar();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            actionBar.setElevation(0);
        }

        ShaderContext.newInstance();

        setupHardwareInfo();

        setupTabs();
    }

    private void setupHardwareInfo() {
        SharedPreferences pref = getSharedPreferences(
                ShaderConfig.PREF_NAME, 0);
        String extensions = pref
                .getString(ShaderConfig.PREF_GLES_EXTENSION, "");

        String renderer = pref
                .getString(ShaderConfig.PREF_GLES_RENDERER, "");

        String vendor = pref
                .getString(ShaderConfig.PREF_GLES_VENDOR, "");

        String version = pref
                .getString(ShaderConfig.PREF_GLES_VERSION, "");

        String hardware = pref
                .getString(ShaderConfig.PREF_CPU_HARDWARE, "");

        String architecture = pref
                .getString(ShaderConfig.PREF_CPU_ARCHITECTURE, "");

        String feature = pref
                .getString(ShaderConfig.PREF_CPU_FEATURE, "");

        if (extensions.compareTo("") != 0 &&
                renderer.compareTo("") != 0 &&
                vendor.compareTo("") != 0 &&
                version.compareTo("") != 0 &&
                hardware.compareTo("") != 0 &&
                architecture.compareTo("") != 0 &&
                feature.compareTo("") != 0) {
            ShaderContext.getInstance().setExtensions(extensions);
            ShaderContext.getInstance().setRenderer(renderer);
            ShaderContext.getInstance().setVendor(vendor);
            ShaderContext.getInstance().setVersionStr(version);

            ShaderContext.getInstance().setHardware(hardware);
            ShaderContext.getInstance().setArchitecture(architecture);
            ShaderContext.getInstance().setFeature(feature);

            mView = (GLSurfaceView) findViewById(
                    R.id.glsurfaceview);
            mView.setVisibility(View.GONE);
        } else {
            setupGLRendererForGPUInfo();
            getCPUInfo();
        }
    }

    private void setupGLRendererForGPUInfo() {
        mRenderer = new DummyRenderer(this);
        mRenderer.setHandler(mHandler);
        mView = (GLSurfaceView) findViewById(R.id.glsurfaceview);
        mView.setEGLContextClientVersion(2);
        mView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mView.setRenderer(mRenderer);
        mView.setZOrderOnTop(true);
        mView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        mView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    private void getCPUInfo() {
        String[] infos = new String[]{
                ShaderConfig.PREF_CPU_HARDWARE,
                ShaderConfig.PREF_CPU_ARCHITECTURE,
                ShaderConfig.PREF_CPU_FEATURE
        };
        Map<String, String> cpuInfos = ShaderUtils.getCPUInfo(infos);

        SharedPreferences pref = getSharedPreferences(
                ShaderConfig.PREF_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();

        // cpu hardware
        String hardware = cpuInfos.get(ShaderConfig.PREF_CPU_HARDWARE);
        ShaderContext.getInstance().setHardware(hardware);

        editor.putString(ShaderConfig.PREF_CPU_HARDWARE, hardware);

        // cpu architecture
        String architecture = cpuInfos.get(ShaderConfig.PREF_CPU_ARCHITECTURE);
        ShaderContext.getInstance().setArchitecture(architecture);

        editor.putString(ShaderConfig.PREF_CPU_FEATURE, architecture);

        // cpu feature
        String feature = cpuInfos.get(ShaderConfig.PREF_CPU_FEATURE);
        ShaderContext.getInstance().setFeature(feature);

        editor.putString(ShaderConfig.PREF_CPU_FEATURE, feature);

        editor.commit();
    }

    private void setupTabs() {
        mTabsAdapter = new TabsAdapter(
                getSupportFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mTabsAdapter);

        mSlidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setViewPager(mViewPager);
        mSlidingTabLayout.setOnPageChangeListener(new MyPageChangeListener());

        int color = getResources().getColor(R.color.selectedIndicatorColor);
        mSlidingTabLayout.setSelectedIndicatorColors(color);

        int bgColor = getResources().getColor(R.color.colorPrimary);
        mSlidingTabLayout.setBackgroundColor(bgColor);
    }

    void setCurrentFragment(int index, Fragment fragment) {
        if (DEBUG) {
            Log.d(TAG, "setCurrentFragment()");
        }

        mTabInfos.get(index).mFragment = fragment;
    }

    class MyPageChangeListener extends ViewPager.SimpleOnPageChangeListener {

        @Override
        public void onPageSelected(int position) {
            TabInfo tabInfo = mTabInfos.get(position);
            Fragment fragment = tabInfo.mFragment;

            switch (position) {
                case TAB_SAMPLELIST_POSITION:
                    if (fragment != null) {
                        ((SampleListFragment) fragment)
                                .resetSampleList();
                    }
                    break;
                case TAB_DEVICEINFO_POSITION:
                    if (mIsGPUInfoUpdated == true
                            && fragment != null) {
                        ((DeviceInfoFragment) fragment)
                                .updateDeviceInfo();
                        mView.setVisibility(View.GONE);
                        mIsGPUInfoUpdated = false;
                    }
                    break;
                default:
            }
        }
    }

    public class TabsAdapter extends FragmentPagerAdapter {

        public TabsAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            if (DEBUG) {
                Log.d(TAG + "_TabsAdapter", "getItem() i=" + i);
            }

            Fragment fragment = null;
            switch (i) {
                case TAB_SAMPLELIST_POSITION:
                    fragment = new SampleListFragment();
                    break;
                case TAB_OPTIONS_POSITION:
                    fragment = new OptionsFragment();
                    break;
                case TAB_DEVICEINFO_POSITION:
                    fragment = new DeviceInfoFragment();
                    break;
                default:
                    fragment = new SampleListFragment();
            }

            return fragment;
        }

        @Override
        public int getCount() {
            return mTabInfos.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (DEBUG) {
                Log.d(TAG + "_TabsAdapter", "getPageTitle() position="
                        + position);
            }

            TabInfo tabInfo = mTabInfos.get(position);

            return tabInfo.mTitle;
        }
    }

    private Handler mHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REMOVE_DUMMY_GL_SURFACE:
                    if (DEBUG) {
                        Log.d(TAG, "handleMessage() REMOVE_DUMMY_GL_SURFACE");
                    }

                    TabInfo tabInfo = mTabInfos.get(TAB_DEVICEINFO_POSITION);
                    DeviceInfoFragment fragment = (DeviceInfoFragment) tabInfo.mFragment;
                    if (fragment != null) {
                        fragment.updateDeviceInfo();
                        mView.setVisibility(View.GONE);
                    } else {
                        mIsGPUInfoUpdated = true;
                    }
                    break;
                default:
            }
        }
    };
}
