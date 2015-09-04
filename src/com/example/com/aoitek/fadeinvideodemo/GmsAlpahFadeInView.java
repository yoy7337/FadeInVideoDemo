
package com.example.com.aoitek.fadeinvideodemo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.vision.face.FaceDetector;

public class GmsAlpahFadeInView extends View {

    private static final String TAG = "GmsAlpahFadeInView";
    // define image change interval and frame update interval
    private static final int IMAGE_FADE_IN_TIME = (int) (0.5f * 1000);
    private static final int VIEW_REFRESH_INTERVAL = 20;

    private static final int MAX_ALPHA_VAL = 255;
    private static final int MIN_ALPHA_VAL = 0;

    private String mImagePath = null;

    private int mFadeinAlpthVal = MAX_ALPHA_VAL;

    private int mCurrentImageIndex = -1;

    private FaceFadeInBitmapGenerator mFaceFadeInBitmapGenerator;

    private static final String IMAGES_FILE_NAME[] = {
            "1.jpg",
            "2.jpg",
            "3.jpg",
            "4.jpg",
            "5.jpg",
            "6.jpg",
            "7.jpg",
            "8.jpg",
            "9.jpg",
            "10.jpg",
    };

    public GmsAlpahFadeInView(Context context) {
        super(context);

        init();
    }

    public GmsAlpahFadeInView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public GmsAlpahFadeInView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    public void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        mImagePath = "file://" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/";
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        super.onSizeChanged(xNew, yNew, xOld, yOld);

        if (mFaceFadeInBitmapGenerator == null) {
            mFaceFadeInBitmapGenerator = new FaceFadeInBitmapGenerator(getContext(), xNew, yNew, getFaceDetector());
        }
    }

    private FaceDetector getFaceDetector() {

        FaceDetector faceDetector = new FaceDetector.Builder(getContext())
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.FAST_MODE)
                .setProminentFaceOnly(true)
                .build();

        if (!faceDetector.isOperational()) {
            Toast.makeText(getContext(), "Face detector dependencies are not yet available.", Toast.LENGTH_SHORT)
                    .show();
        }

        return faceDetector;
    }

    private void processNextImg() {
        mCurrentImageIndex++;
        mFadeinAlpthVal = MIN_ALPHA_VAL;

        if (mFaceFadeInBitmapGenerator != null) {
            mFaceFadeInBitmapGenerator.setFadeImage(mImagePath + IMAGES_FILE_NAME[mCurrentImageIndex]);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mFaceFadeInBitmapGenerator == null) {
            return;
        }

        // 1. need switch bitmap?
        if (mFadeinAlpthVal >= MAX_ALPHA_VAL) {

            if (mCurrentImageIndex > IMAGES_FILE_NAME.length - 2) {
                mFaceFadeInBitmapGenerator.release(false);

                canvas.drawBitmap(mFaceFadeInBitmapGenerator.genFadeInImage(MAX_ALPHA_VAL), 0, 0, new Paint());
                return;
            }

            processNextImg();

        } else {
            mFadeinAlpthVal += MAX_ALPHA_VAL * VIEW_REFRESH_INTERVAL / IMAGE_FADE_IN_TIME;
            if (mFadeinAlpthVal > MAX_ALPHA_VAL) {
                mFadeinAlpthVal = MAX_ALPHA_VAL;
            }
        }

        canvas.drawBitmap(mFaceFadeInBitmapGenerator.genFadeInImage(mFadeinAlpthVal), 0, 0, new Paint());
        postInvalidateDelayed(VIEW_REFRESH_INTERVAL);
    }
}
