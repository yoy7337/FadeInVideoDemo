
package com.example.com.aoitek.fadeinvideodemo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.DisplayMetrics;

import com.nostra13.universalimageloader.core.ImageLoader;

public class Utils {

    private static final int VIDEO_WIDTH_RATIOS = 16;
    private static final int VIDEO_HEIGHT_RATIOS = 9;

    public static double distance(PointF p1, PointF p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }

    public static PointF centerPoint(PointF p1, PointF p2) {
        PointF p = new PointF();
        p.x = (float) ((p1.x + p2.x) / 2.0);
        p.y = (float) ((p1.y + p2.y) / 2.0);
        return p;
    }

    public static int getVideoWidth(Activity activity) {
        return Math.min(getScreenHeight(activity) * VIDEO_WIDTH_RATIOS / VIDEO_HEIGHT_RATIOS, getScreenWidth(activity));
    }

    public static int getVideoHeight(Activity activity) {
        return Math.min(getScreenWidth(activity) * VIDEO_HEIGHT_RATIOS / VIDEO_WIDTH_RATIOS, getScreenHeight(activity));
    }

    public static int getScreenWidth(Activity activity) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        return displaymetrics.widthPixels;
    }

    public static int getScreenHeight(Activity activity) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        return displaymetrics.heightPixels;
    }
    
    public static Bitmap getBitmapWithSize(String imageUrl, int reqWidth, int reqHeight) {
        Bitmap scaledBitmap = null;
        ImageLoader imageLoader = ImageLoader.getInstance();

        Bitmap bitmap = imageLoader.loadImageSync(imageUrl);
        if (bitmap.getWidth() != reqWidth || bitmap.getHeight() != reqHeight) {
            scaledBitmap = Bitmap.createScaledBitmap(bitmap, reqWidth, reqHeight, true);
        } else {
            scaledBitmap = bitmap;
        }

        return scaledBitmap;
    }
}
