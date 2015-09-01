package com.example.com.aoitek.fadeinvideodemo;

import android.graphics.Bitmap;

import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;


public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Universal Image Loader init        
        ImageLoader imageLoader = ImageLoader.getInstance();
        if(!imageLoader.isInited()) {
            // universal image loader setup
            DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                //.showStubImage(R.drawable.exhib_default)
                //.showImageForEmptyUri(R.drawable.exhib_default)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .imageScaleType(ImageScaleType.EXACTLY_STRETCHED)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();

            ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
                 .defaultDisplayImageOptions(defaultOptions)
                 .memoryCache(new WeakMemoryCache())
                 //.writeDebugLogs()
                 .build();
            imageLoader.init(config); 
        }
    }
}
