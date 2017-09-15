/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.samples.comparison;

import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.logging.FLog;
import com.facebook.samples.comparison.adapters.FrescoAdapter;
import com.facebook.samples.comparison.adapters.GlideAdapter;
import com.facebook.samples.comparison.adapters.ImageListAdapter;
import com.facebook.samples.comparison.adapters.PicassoAdapter;
import com.facebook.samples.comparison.adapters.UilAdapter;
import com.facebook.samples.comparison.adapters.VolleyAdapter;
import com.facebook.samples.comparison.adapters.VolleyDraweeAdapter;
import com.facebook.samples.comparison.configs.imagepipeline.ImagePipelineConfigFactory;
import com.facebook.samples.comparison.instrumentation.PerfListener;
import com.facebook.samples.comparison.urlsfetcher.ImageFormat;
import com.facebook.samples.comparison.urlsfetcher.ImageSize;
import com.facebook.samples.comparison.urlsfetcher.ImageUrlsFetcher;
import com.facebook.samples.comparison.urlsfetcher.ImageUrlsRequestBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends ActionBarActivity {

    private static final String TAG = "FrescoSample";

    // These need to be in sync with {@link R.array.image_loaders}
    public static final int FRESCO_INDEX = 1;
    public static final int FRESCO_OKHTTP_INDEX = 2;
    public static final int GLIDE_INDEX = 3;
    public static final int PICASSO_INDEX = 4;
    public static final int UIL_INDEX = 5;
    public static final int VOLLEY_INDEX = 6;

    private static final long STATS_CLOCK_INTERVAL_MS = 1000;
    private static final int DEFAULT_MESSAGE_SIZE = 1024;
    private static final int BYTES_IN_MEGABYTE = 1024 * 1024;

    private static final String EXTRA_ALLOW_ANIMATIONS = "allow_animations";
    private static final String EXTRA_USE_DRAWEE = "use_drawee";
    private static final String EXTRA_CURRENT_ADAPTER_INDEX = "current_adapter_index";
    private static final long SCROLL_CLOCK_INTERVAL_MS = 500;

    private Handler mHandler;
    private Runnable mStatsClockTickRunnable;

    private TextView mStatsDisplay;
    private Spinner mLoaderSelect;
    private ListView mImageList;

    static public boolean mUseDrawee;
    static public boolean mAllowAnimations;
    static public boolean mAutoScroll;
    static public boolean mStapleUrls;
    private int mCurrentAdapterIndex;

    private ImageListAdapter mCurrentAdapter;
    private PerfListener mPerfListener;

    List<String> mImageUrls = new ArrayList<>();
    static public boolean mUsePNG;
    static public boolean mUseJPEG;
    private Runnable mScrollDownClockTickRunnable;
    private int one_minute = 0;
    private Runnable mScrollUpClockTickRunnable;


    public void setStapleUrls(boolean stapleUrls) {
        mStapleUrls = stapleUrls;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        FLog.setMinimumLoggingLevel(FLog.WARN);//坑比..也放在Application
//        Drawables.init(getResources());//放在Application

        mAllowAnimations = false;
        mUseDrawee = true;
        mUseJPEG = true;
        mUsePNG = true;
        mAutoScroll = true;
        mStapleUrls = true;

        mCurrentAdapterIndex = 0;
        if (savedInstanceState != null) {
            mAllowAnimations = savedInstanceState.getBoolean(EXTRA_ALLOW_ANIMATIONS);
            mUseDrawee = savedInstanceState.getBoolean(EXTRA_USE_DRAWEE);
            mCurrentAdapterIndex = savedInstanceState.getInt(EXTRA_CURRENT_ADAPTER_INDEX);
        }

        mHandler = new Handler(Looper.getMainLooper());
        mStatsClockTickRunnable = new Runnable() {//1s一次的数据统计
            @Override
            public void run() {
                updateStats();
                scheduleNextStatsClockTick();
            }
        };
        mScrollDownClockTickRunnable = new Runnable() {//1s自动滑动
            @Override
            public void run() {
                updateListViewDownPosition();
            }
        };
        mScrollUpClockTickRunnable = new Runnable() {//1s自动滑动
            @Override
            public void run() {
                updateListViewUpPosition();
            }
        };

        mCurrentAdapter = null;
        mPerfListener = new PerfListener();
        initUrls();
        mStatsDisplay = (TextView) findViewById(R.id.stats_display);
        mImageList = (ListView) findViewById(R.id.image_list);
        mLoaderSelect = (Spinner) findViewById(R.id.loader_select);
        mLoaderSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setAdapter(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        mLoaderSelect.setSelection(mCurrentAdapterIndex);
    }

    private void initUrls() {
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/cdde94078aebd040ea38d536afb11a6dbdcb6402?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/79f93e19fd2cb1382e6d381ed50c0c1291fd1edd?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/9b798f4279b5888a0219e5a111d2403bc4e2ee9a?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/d959bc29fb4edbe1934c9a7aeb6bc40377d07d1f?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/087fdf99e700785dddcc676a125cb8b44db5e4a4?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/82bf9ffb90b595ceb3fa078d08ad07a748893e89?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/198ab93a1cbb66f8d8e172f74cc9a401f1fd0f48?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/022d74f478e47335a8b5798a255796d4b30c35b5?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/07c53b4f74be11a42c69633e9128bbfe9795069a?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/a27b2ac3d88fe97b3261837a138a971fcf79393b?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/9aa3c2503532aaf95dabad295774b171590c1155?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/58e98753920b3c83afa9f84f2a9a99c9db3f4d0c?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/f41f91e1d3f54bc1474bb171aa8be64e0b9f2990?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/32a1e836f1fd7d9ee64df4c53e6929bb75dcfd8f?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/dff26297ba018190da6be448f3d86afc559b8b58?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/5326d309fd16cd5a81953de828d6eaf7af828d9a?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/5e20c04c77a111e9f7a09bed0cc9da7fc382cdea?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/d3ffa82cf2fa4df058b519383d1126ed8dc2c531?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/f1086e1c267eeae2c6e4e32f98c2e46acaad8f2e?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/6624da9dd95d331ad816a99fdbb4bc7a26d665b4?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/aff09cbcf1ba4c0d1eef5aafb7e1adb62728cc3a?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/2e7ae5d0393958dc8b7025e633d286459708c866?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/626f3df1d51beabfa18435f2cd690802fce45fb0?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/40728a6b326f56bf7643c114ba30fdca01a264b6?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/39ab2cd3e90b62ae3dd141bd2804cbcc3abae417?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/bd3dce63a73454502aebaa6abeb958d56ec551f5?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/3f0061d3e3b0933efbbf3f6f98e5ebb52224426e?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/ab89876166a7aafe5d8982bbde20d4b6ada268bd?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/4e370dab527d12ed82aba06a0c0f00c1763c5c5a?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        /*----30---*/mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/afa40b7b5a62d89ce8255e4e6654c2b558ad94ad?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/476f3bd5c01c03f676c4303ed1b681f3350b7a47?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/f4c4a98401c53cda47863e193b012073cc5f7bcc?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/b8bcd31870c7dbce78ce6d8b42b356f685311ca4?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/701cd7333e088f5e00320815cb981a862d85b3cd?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/8f0cb035d6dc49460c6aebae7bb9f3e96ab5eede?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/97a9d61cd3ca35894f1e7692ccbb7afa7501d073?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/989509f462f7dfa3326937d99021a058c6c6d6e6?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/ce34c0fd3f4500857a84ffb8ebd7b33118410ae2?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/98ddffd70ea4271e80ead38487460fa61d5463bf?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/fce6c0835a923347006154976e9294d4c62e5348?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/0c6145211ed1a3baec0cb5cbe22ffb34905787da?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/f530d841c134764f1e96480844b19c21666ceae3?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/3d00cab35d1506ab9d6dc3952d77df41e5875604?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/5dd6dbaaf1c3815648eaa9341c1d63550c34b013?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/242cec8450e5ca8dfc2cfc304f1e7d2b49ca69fc?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/b203119acf22b3caba90a955134dea30ca393402?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/ebc9f35145279103e68974ca4f3ff8cee770ec32?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/7d5a39dd3cc866cbf669fc889fb03d86e59035e1?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/2429c8255a5274dab283c02461cf9f1557eafde3?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/89e0f94f7f896653fe0fc891e4f4605347f32e26?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/7963060d8eaf3a2c0501986f1c704ee17913588c?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/b86dcb6807f98da997749f8a2263a0aaa41a7f70?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/71ab711aafe9e7a21cd6464d39f2cf8c0334b8fa?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/1868f2053e8c59277d87ba9226a0362034f971ce?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/dd7f6912787afd589c4d6943e9d88cddaf0a51ba?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/dd7f6912787afd589c4d6943e9d88cddaf0a51ba?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/1868f2053e8c59277d87ba9226a0362034f971ce?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/be8863a9899a494bcb83a6ce3d0c682fc21d2f39?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/71ab711aafe9e7a21cd6464d39f2cf8c0334b8fa?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        /*---60---*/mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/7963060d8eaf3a2c0501986f1c704ee17913588c?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/b86dcb6807f98da997749f8a2263a0aaa41a7f70?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/2429c8255a5274dab283c02461cf9f1557eafde3?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/ebc9f35145279103e68974ca4f3ff8cee770ec32?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/7d5a39dd3cc866cbf669fc889fb03d86e59035e1?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/5dd6dbaaf1c3815648eaa9341c1d63550c34b013?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/36e735af84ff41df377f08941d804af461f8a949?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/3628a9f5c9e7635ec15ae5b8e6703f3ac5b41d97?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/31f5cf51a69169150f54c4620f7afb397b880b9b?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/3149aa4eed3a49ec042927488065af6cc36df800?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/d33a2235d84e0a2dcd9651dd4eb6f819b587e6e8?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/71dc3d30dcf5c8159d3e87eb8c5e9b49827626d7?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/d52bfb429308d81904210b4783f40617ec0cd580?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/31a35a68e343283b351bafa3d18a9194b92d119b?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/ed658a8de46b68f4cfcd42928b1148340935c113?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/1b7295f87029bcd69e8ec0942271e5e45f80d8b9?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/364cf2b54a36a7b41ad9229f6431e2d660d97d3a?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/7478a63249d0057a05758f7c205f04b1d0b5c509?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/601f6a7323ad470dd560e0ea262d6d836e949d9d?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/06f724f1dc5962c215b45706d537540349bc48d1?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/9cb81b108945a415c4ce28d91bd5b17f1778975c?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/67abb488f00effa658ce5d935a7c23d12b0ba9c5?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/d9876c62ed23f59429317f08ed9261addcfafd8b?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/c106b776143f4f2efd591594bae452e143f966ff?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/1bd162c0d95ba6116d9b99c2b0134ce5387fd420?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/c6e54ab4f7f6385655feae090f8ef0e7b154239a?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/e4302bc0c202b02b2381038e9fee66229e62cc3e?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/bca8ad36d8b8b2e44f052ecadffb7c91f073eb00?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/48d9297e7ac5971fc89719c8ddfec62d21bcd84e?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/1cccb7869a0c35c2eb0dee22835dc6b56e722dca?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/efdd36ae2a9ec26e6b46b5d89bc66f9431316d34?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/b5d65aa17e9089ec3a77a745dff95e29caf1bbc1?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/2053ed3fa645750cf4c6be50191bfef8b37aefd9?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/b9c3bb5659d01d714278d18a5252e7dfb8fdfdbd?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/67b478990dba62d06bb20c60b5decfb36e478d0a?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/1a20db6325d887cd6a07684e9310a9d6879666e0?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/ec0b63601388f95038879d3bab41012f60db718e?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/e90d655d13392d3b953dfa6679721e5088ccc41b?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/a38a4f5f08fdf215220c9c2a8b41c812d9ad7f0e?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/830c652775b5c4a5f86b27cdc2de6f98540e708d?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        /*---100---*/mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/97c3352f7adbacd8a1ca01a5102cd939e96e5279?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/f7db7354ce639ba2901fea01900823bc4ec197a7?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/25e03a9e2afe28424f4d1c8a9a2de33feaa754cf?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/3e99fc13fabf7e44f57f9e83e541b7ebda6a183e?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/cd1912bbf8bbbcbbf82efef2e9706351450275ad?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/17950b147d6b0633d0c5b713c4bad0b6d4aac003?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/91b99e0d50e85d80a09161c7e3f6d1b66d1931cb?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/85c72309cb27caadd2c2d92cfc483470b2a845fd?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/f26d99a8ecb84d262f8734eec8e2b7d52c012f68?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/23262cd02d1c4ea9e8f88896e5cd0f70f6fc2206?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/a401e74f74b282f91ddb4883424e143b545b2774?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/a846fa6757c6fd42c24086013e755dcf6ce5b663?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/b5a97f87bb1d5d00ff2598a035c680ffcc10457e?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/1db7ca90d9d3d35ecb98493df92e4c860b647543?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/b9510706bf9d1ae49f2754ac8a2eac53e047ed58?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/a87a0a7d22bef6e22ac5f697fe3c380df364a7fd?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/524bff650295f266278602573f8209f0814f0357?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/91877eb153f9ad7e10871eeef832a912ed998601?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/be30f403a47b6bd5be44266b0626d906e8856689?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/128908be7ee699e1dfe82a48e04123764ad4325d?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/904933eec868328ad739285cf800a5398afa412a?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/26b4e6314c875a203fa1fac8200bdfc15244e998?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/6521328965cc7a86e733a4d084dcf882614fc567?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/e756cc173903e3932edf6fb411467bf3cff0099e?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/87568c08bf4d7cb17a5f2285dc6a0e39969b2d2e?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/d83eec00bcb453fa5fe88e5f5250250ad02da3ff?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/4d840b17af84fd3128b770326cf43fae8a264483?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/80b22410a13fca51fe86f7224bd0a988e87e8d09?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/50fec4a76b7eb68dd3523e051df5d46a062d55c1?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/47b4b280eb26847e06e568962b65b4e0c2a7922b?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/d2c0495c368ce7371e4d6520000866fb89d6774b?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/61f3f76862b08623500dc6a7ca35845d77cfdc29?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/61775e680a7e3ff63d756c60610f07ef77764914?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/61035fdea7d18040cf707931324e820fd1164541?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/1f71d43b8971a37396c101993623f4a3c9259136?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/5d1ec0a96d0ea94ddd3373469658e0f858523c4e?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/4688c6003493fc47de4ef4d4acf53d78147c0c23?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/3d1a85fac8a557a716eaa8005a5bc6f26634563e?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/bf8510f010162af3d0c6fa51ee5646976c01a680?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/ad7999afba6363e009b11774a51ddda0cf9069b8?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/bc457d9ccfbe6ec7e47650d2e32fe658f5bc64e5?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/fb1db8947a3098965407f0eb5c06144880ef3f7f?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/097ad4ff7a0bc4a68ab0cc8d82ff90a0e66f2ae6?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/9098566b20076108f19bf063f56ef3475ad2a9e0?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/39e35e2a715570a5a818de4f9d47b11273f9bee1?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/c98c1895684a945118c78733de6266b6f027a38a?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/4c58893d1a270870e05b348c34ea0acf83fbdb5c?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/e5ea696f2c6746eee18418d9f88753b2eb17c63d?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/e4bc88a8489b03ebc48d4942a899a5105ab15752?imageview/0/w/338/h/190/blur/1");
        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/0afe9e4ec6130a4d27c122a96504f9020576ae21?imageview/0/w/338/h/190/blur/1");
        /*---150---*/mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/1d22765b09204b57c843a7ba82bc0ff4c6f7a6ff?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/5469093bd6b6f8604eb74ebefc3932f261b5ccd8?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/d8d17d42e94155437623dbf91e0f7038a1759459?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/8f8abc007a9239f67c6f93ddea801bd43d40dd3d?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/23cebd699389c9344d9866f313320fa363e7ce40?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/78058745fb12d6a21efca3c33c3d7266ea6a6378?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/29d2da6d1e4ae505a407bdee653619777935e365?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/9ed9dd0e9dbda1ddd458b28c6ee54cd71dffc4a2?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/ba4c6e55f1e7790085dd1b8685f3029a1040584e?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/fd3d394d280ecb6189ee3ac46ae5dd8f7e0142c1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/d3e99f18629dcd4259f9013480278795099bbfd1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/95b2e8f192e20ce45a7e502d428485c62a92eb03?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/53578f450a243640878cfea8a2566a6c203bd00f?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/60a737d2f8bae94fe5e1a536ddb36d52c2be22fc?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/ef5e10f19db2455c21b394f01988d723703352db?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/822f63c85b6b0d0bee8b920201e62210df83ebeb?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/a5619ab060d77f4ba2363347569c630d643a0b2c?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/d0f4169a113bc91d2f56758ec51a562507460a25?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/5ea9b057b1ecf142651ba69da98678f804372c06?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/9efeca86325e2710e1951cd65d911ccd0fb1caa3?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/9b8906b2050ae2fcbfff41767088d8f0a2a10527?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/a26087e96c530483f40f00bdaf2979f19c2183c9?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/a3c345cc9b08e0b48a4978312805128c00b837c7?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/cd845194040295c16de01b6b493255c255298e01?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/f29673db25c2fdbfeb8c11db3305b3e872c135c6?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/87a6c3439c019850dd13f7ebe90c5e4c15e6d4c6?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/cb39730ded20d2d9a259c2845e474a870db99925?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/76e645cc1a5600e8927a0bc43ec70360f766b797?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/3b069d40715000e7b8507e0fd02417bfffaf2a2f?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/1aa1abc3c7e48f8b6c5da924a0db500902ca3ad3?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/5bd93386bf6c37f5eac37a4cc7b9abea7fa93074?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/37683e90414dcfeec9642b153d9ef959f538fdf8?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/f8b320ee953ba92f14760e9041bc24efd8151091?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/268e43b740b5e4e42372497330eb01d302de1764?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/3c66ac32a93a84d4cd8591aa755bfc69c9f730dc?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/78bd0b4b1e93c99e9dc95dad9148446725ced2b9?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/2cb0b6871a5200f8ab374f139f6d1c4c2ef58885?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/44ee5f98bed63d549d68193c5e0c173576a138b6?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/db52dfe237ce790d83d26e4aee630cbd06bda037?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/b9fafb08dce5d48aaaa42a938442201cd5246159?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/a1f44e95d91e752524c72a7fce504f662ed51257?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/ba9c1409edd4b1944a0ba07ae6d03376610f37ef?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/5071dd2946f64dc19c3aa81a72ace5af21fc068c?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/a1ffc88937db66238c7755ff10adb06d2f72fdef?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/8913e470048886f448ef69c990a4f3b520576f88?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/32122650915a948bab0ad501d15ce6a310762c5f?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/7f3fe229b4186c35eb3ab3ebb2f896b6b5d002b3?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/b0b8779ec48ff9949192894b29e2417f1def0e2f?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/a3ea2d796ffe12620057d8eb47968cb42a198477?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/9e89c662ca985cf44b5c7c084e26e1e1f98ef542?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/9cb022f0be2b22244830664580bf3ca7512cacfc?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/e3c77fea7e39a1780229117f14b245b8c9513957?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/ff4c1539f6e902d8d883cc2b8b92294d6f961602?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/366002dc3ebd8c3e6f9e3d27da3a3a3640f67860?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/46e28e56bfbc4f64fc2ec0920a10b011e578391c?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/c0a803f0b49012ba44d5d59af4dd3a276b8aa30b?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/c8de680d261007ba4a02183f7656749067e2319a?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/a9eaf3904021dc6dfb8f849f85b0aa8b051acd97?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/4a374a365bd42bb9bf6f8db01b0979a7c3fc5067?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/5559dee178aab8396c115832399f302a8f74978e?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");
//        /*---210---*/mImageUrls.add("http://screenshot.msstatic.com/yysnapshot/f8d749cfc682a2f5c8c7ae963b63c4308448f20e?imageview/0/w/338/h/190/blur/1?imageview/0/w/338/h/190/blur/1");


    }

    private void updateListViewUpPosition() {
        int currIndex = one_minute--;
        if (currIndex == 1) {
            scheduleNextScrollClockTickIfCan(mScrollDownClockTickRunnable);
        } else {
            scheduleNextScrollClockTickIfCan(mScrollUpClockTickRunnable);
        }
        mImageList.setSelection(currIndex);

    }

    private void updateListViewDownPosition() {
        int currIndex = one_minute++;
        if (currIndex == mImageUrls.size()-2) {
            scheduleNextScrollClockTickIfCan(mScrollUpClockTickRunnable);
        } else {
            scheduleNextScrollClockTickIfCan(mScrollDownClockTickRunnable);
        }
        mImageList.setSelection(currIndex);
    }

    private void scheduleNextScrollClockTickIfCan(Runnable nextRunnable) {
        if (mAutoScroll) {
            mHandler.postDelayed(nextRunnable, STATS_CLOCK_INTERVAL_MS);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_ALLOW_ANIMATIONS, mAllowAnimations);
        outState.putBoolean(EXTRA_USE_DRAWEE, mUseDrawee);
        outState.putInt(EXTRA_CURRENT_ADAPTER_INDEX, mCurrentAdapterIndex);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.allow_animations).setChecked(mAllowAnimations);
        menu.findItem(R.id.use_drawee).setChecked(mUseDrawee);
        menu.findItem(R.id.use_jpeg).setChecked(mUseJPEG);
        menu.findItem(R.id.use_png).setChecked(mUsePNG);
        menu.findItem(R.id.auto_scroll).setChecked(mAutoScroll);
        menu.findItem(R.id.staple_urls).setChecked(mStapleUrls);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.allow_animations) {
            setAllowAnimations(!item.isChecked());
            return true;
        }

        if (id == R.id.use_drawee) {
            setUseDrawee(!item.isChecked());
            return true;
        }
        if (id == R.id.use_jpeg) {
            setUseJPEG(!item.isChecked());
            return true;
        }
        if (id == R.id.use_png) {
            setUsePNG(!item.isChecked());
            return true;
        }


//        if (id == R.id.clear_file_cache) {
//            clearAppCache();
//            return true;
//        }
//
//        if (id == R.id.init) {
//            initImageLoader();
//            return true;
//        }
        if (id == R.id.auto_scroll) {
            setAutoScroll(!item.isChecked());
            return true;
        }
        if (id == R.id.staple_urls) {
            setStapleUrls(!item.isChecked());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setAutoScroll(boolean autoScroll) {
        mAutoScroll = autoScroll;
    }


    private void clearAppCache() {
        File cacheDir = this.getApplicationContext().getExternalCacheDir();
        File frescoCacheDir = new File(cacheDir, ImagePipelineConfigFactory.IMAGE_PIPELINE_CACHE_DIR);
        long cacheSize = deleteByDirectory(frescoCacheDir);
        Toast.makeText(this, String.format("清除了%d KB缓存", cacheSize / 1024), Toast.LENGTH_SHORT).show();
    }

    private long deleteByDirectory(File targetDir) {
        long totalSize = 0;
        if (targetDir != null && targetDir.exists() && targetDir.isDirectory()) {
            File[] files = targetDir.listFiles();
            if (files != null) {//目标缓存有三级目录,递归删除
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    totalSize += file.length();
                    boolean isSuccessful = file.delete();
                    if (!isSuccessful) {
                        totalSize += deleteByDirectory(file);
                        boolean ignored = file.delete();
                    }
                }
            }
        }
        return totalSize;
    }


    @Override
    protected void onStart() {
        super.onStart();
        updateStats();
        scheduleNextStatsClockTick();
    }

    protected void onStop() {
        super.onStop();
        cancelNextStatsClockTick();
    }

    @VisibleForTesting
    public void setAllowAnimations(boolean allowAnimations) {
        mAllowAnimations = allowAnimations;
        supportInvalidateOptionsMenu();
        updateAdapter(null);
        reloadUrls();
    }

    public void setUseJPEG(boolean useJPEG) {
        mUseJPEG = useJPEG;
        supportInvalidateOptionsMenu();
        updateAdapter(null);
        reloadUrls();
    }

    public void setUsePNG(boolean usePNG) {
        mUsePNG = usePNG;
        supportInvalidateOptionsMenu();
        updateAdapter(null);
        reloadUrls();
    }

    @VisibleForTesting
    public void setUseDrawee(boolean useDrawee) {
        mUseDrawee = useDrawee;
        supportInvalidateOptionsMenu();
        setAdapter(mCurrentAdapterIndex);
    }

    private void setAdapter(int index) {
        FLog.w(TAG, "onImageLoaderSelect: %d", index);
//        if (mCurrentAdapter != null) {
//            mCurrentAdapter.shutDown();
//            mCurrentAdapter = null;
//            System.gc();
//        }//性能测试时一次性使用，不调用shutdown和GC

        mCurrentAdapterIndex = index;
        mPerfListener = new PerfListener();
        switch (index) {
            case FRESCO_INDEX:
            case FRESCO_OKHTTP_INDEX:
                mCurrentAdapter = new FrescoAdapter(
                        this,
                        R.id.image_list,
                        mPerfListener,
                        index == FRESCO_INDEX ?
                                ImagePipelineConfigFactory.getImagePipelineConfig(this) :
                                ImagePipelineConfigFactory.getOkHttpImagePipelineConfig(this));//性能测试不包含OKhttp,不到达
                break;
            case GLIDE_INDEX:
                mCurrentAdapter = new GlideAdapter(this, R.id.image_list, mPerfListener);
                break;
            case PICASSO_INDEX:
                mCurrentAdapter = new PicassoAdapter(this, R.id.image_list, mPerfListener);
                break;
            case UIL_INDEX:
                mCurrentAdapter = new UilAdapter(this, R.id.image_list, mPerfListener);
                break;
            case VOLLEY_INDEX:
                mCurrentAdapter = mUseDrawee ?//这个变量只在这里使用
                        new VolleyDraweeAdapter(this, R.id.image_list, mPerfListener) ://不包括Volley，不到达
                        new VolleyAdapter(this, R.id.image_list, mPerfListener);
                break;
            default:
                mCurrentAdapter = null;
                return;
        }

        mImageList.setAdapter(mCurrentAdapter);

        if (mImageUrls != null && !mImageUrls.isEmpty() && mStapleUrls) {
            updateAdapter(mImageUrls);
        } else {//非稳定URL才会重加载
            reloadUrls();
        }

        updateStats();
    }

    private void scheduleNextStatsClockTick() {
        mHandler.postDelayed(mStatsClockTickRunnable, SCROLL_CLOCK_INTERVAL_MS);
    }

    private void cancelNextStatsClockTick() {
        mHandler.removeCallbacks(mStatsClockTickRunnable);
    }

    private void reloadUrls() {
        String url = "http://api.imgur.com/3/gallery/hot/viral/0";//2DO HTTP 改成 HTTP
        ImageUrlsRequestBuilder builder = new ImageUrlsRequestBuilder(url);
        if (mUseJPEG) {
            builder.addImageFormat(
                    ImageFormat.JPEG,
                    ImageSize.LARGE_THUMBNAIL);
        }
        if (mUsePNG) {
            builder.addImageFormat(
                    ImageFormat.PNG,
                    ImageSize.LARGE_THUMBNAIL);
        }

        if (mAllowAnimations) {
            builder.addImageFormat(
                    ImageFormat.GIF,
                    ImageSize.ORIGINAL_IMAGE);
        }

        ImageUrlsFetcher.getImageUrls(
                builder.build(),
                new ImageUrlsFetcher.Callback() {
                    @Override
                    public void onFinish(List<String> result) {
                        mImageUrls = result;
                        updateAdapter(mImageUrls);
                    }
                });
    }

    private void updateAdapter(List<String> urls) {
        if (mCurrentAdapter != null) {
            mCurrentAdapter.clear();
            if (urls != null) {
                for (String url : urls) {
                    mCurrentAdapter.add(url);
                }
            }
            mCurrentAdapter.notifyDataSetChanged();
            if (mAutoScroll) {
                mHandler.postDelayed(mScrollDownClockTickRunnable,2000);//防止图片被略过
            }
        }
    }

    private void updateStats() {
        final Runtime runtime = Runtime.getRuntime();

        long totalMemory = runtime.totalMemory();
        long maxMemory = runtime.maxMemory();
        final long heapMemory = totalMemory - runtime.freeMemory();
        final StringBuilder sb = new StringBuilder(DEFAULT_MESSAGE_SIZE);
        // When changing format of output below, make sure to sync "scripts/test_runner.py" as well.
        appendSize(sb, "Java total heap size:    ", maxMemory, "\n");
        appendSize(sb, "Memory cache size:      ", maxMemory, "\n");
        appendSize(sb, "Java heap size:          ", heapMemory, "\n");
        appendSize(sb, "Native heap size:        ", Debug.getNativeHeapSize(), "\n");
        appendTime(sb, "Average photo wait time: ", mPerfListener.getAverageWaitTime(), "\n");
        appendNumber(sb, "Outstanding requests:    ", mPerfListener.getOutstandingRequests(), "\n");
        appendNumber(sb, "Cancelled requests:      ", mPerfListener.getCancelledRequests(), "\n");
        appendNumber(sb, "Image numbers:          ", mImageUrls == null ? 0 : mImageUrls.size(), "\n");
        appendNumber(sb, "Current Image index:     ", one_minute, "\n");
        final String message = sb.toString();
        mStatsDisplay.setText(message);
//        FLog.i(TAG, message);
    }

    private static void appendSize(StringBuilder sb, String prefix, long bytes, String suffix) {
        String value = String.format(Locale.getDefault(), "%.2f", (float) bytes / BYTES_IN_MEGABYTE);
        appendValue(sb, prefix, value + " MB", suffix);
    }

    private static void appendTime(StringBuilder sb, String prefix, long timeMs, String suffix) {
        appendValue(sb, prefix, timeMs + " ms", suffix);
    }

    private static void appendNumber(StringBuilder sb, String prefix, long number, String suffix) {
        appendValue(sb, prefix, number + "", suffix);
    }

    private static void appendValue(StringBuilder sb, String prefix, String value, String suffix) {
        sb.append(prefix).append(value).append(suffix);
    }
}
