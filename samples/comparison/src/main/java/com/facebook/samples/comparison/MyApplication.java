package com.facebook.samples.comparison;

import android.app.Application;
import android.util.Log;

import com.facebook.common.logging.FLog;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.samples.comparison.configs.imagepipeline.ImagePipelineConfigFactory;
import com.facebook.samples.comparison.configs.picasso.SamplePicassoFactory;
import com.facebook.samples.comparison.configs.uil.SampleUilFactory;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.squareup.picasso.Picasso;

/**
 * Created by zsmallx on 2017/9/1.
 */

public class MyApplication extends Application {
public static Picasso sPicasso;
public static ImageLoader sImageLoader;
    @Override
    public void onCreate() {
        super.onCreate();
        Drawables.init(getResources());

        FLog.setMinimumLoggingLevel(FLog.VERBOSE);

        long curr=System.currentTimeMillis();
        long last=curr;
        frescoInit();//忽略Fresco+OKhttp
        curr=System.currentTimeMillis();
        Log.e("Fresco Init time----",""+(curr-last));

        glideInit();
        curr=System.currentTimeMillis();
        last=curr;
        picassoInit();
        curr=System.currentTimeMillis();
        Log.e("Picasso Init time----",""+(curr-last));

        curr=System.currentTimeMillis();
        last=curr;
        UILInit();
        curr=System.currentTimeMillis();
        Log.e("UIL Init time----",""+(curr-last));
    }

    private void UILInit() {
        sImageLoader=SampleUilFactory.getImageLoader(getApplicationContext());
    }

    private void picassoInit() {
        sPicasso=SamplePicassoFactory.getPicasso(getApplicationContext());
    }

    private void glideInit() {
        //Glide使用编译时注解进行类的生成
    }

    private void frescoInit() {
        ImagePipelineConfig imagePipelineConfig= ImagePipelineConfigFactory.getImagePipelineConfig(getApplicationContext());
////        ImagePipelineConfig imagePipelineConfig= ImagePipelineConfigFactory.getOkHttpImagePipelineConfig(this);
        Fresco.initialize(getApplicationContext(), imagePipelineConfig);
    }
}
