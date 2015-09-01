
package com.example.com.aoitek.fadeinvideodemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;


public class AlpahFadeInView extends View {

    private static final int IMAGE_FADE_IN_TIME = (int) (3f * 1000);
    private static final int VIEW_REFRESH_INTERVAL = 100;
    private static final int MAX_ALPHA_VAL = 255;
    private static final int MIN_ALPHA_VAL = 0;

    private static final int MAX_FACE_DETECT = 1;

    private String mImagePath = null;
    private BitmapFactory.Options mBitmapOptions = new BitmapFactory.Options();

    private int mFadeinAlpthVal = MAX_ALPHA_VAL;

    FaceDetector.Face currentFace;
    FaceDetector.Face nextFace;

    private int mCurrentImageIndex = -1;

    Bitmap currentBitmap = null;
    Bitmap nextBitmap = null;

    Paint mBackGroundPaint = null;
    Paint mAlphaPaint = null;

    int viewWidth = 0;
    int viewHeight = 0;

    int defaultImageWidth = 640;
    int defaultImageHeight = 480;

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

    public AlpahFadeInView(Context context) {
        super(context);

        init();
    }

    public AlpahFadeInView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public AlpahFadeInView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    public void init() {
        mImagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/";
        mBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        mBackGroundPaint = new Paint();
        mAlphaPaint = new Paint();
    }

    private FaceDetector.Face detectFace(Bitmap bitmap, String tag) {
        FaceDetector faceDetect = new FaceDetector(bitmap.getWidth(), bitmap.getHeight(), MAX_FACE_DETECT);
        FaceDetector.Face[] faces = null;
        faces = new FaceDetector.Face[MAX_FACE_DETECT];
        int numFace = faceDetect.findFaces(bitmap, faces);
        if (numFace > 0) {
            Log.d("yoy", "detect face..., faces[0].confidence(): " + faces[0].confidence());
            Log.d("yoy", "EULERX: " + faces[0].pose(FaceDetector.Face.EULER_X) + ", EULERY: " + faces[0].pose(FaceDetector.Face.EULER_Y)
                    + ", EULERZ: " + faces[0].pose(FaceDetector.Face.EULER_Z));
            PointF p = new PointF();
            faces[0].getMidPoint(p);
            Log.d("yoy", "faces[0].eyesDistance(): " + faces[0].eyesDistance() + ", PointF.x: " + p.x + ", PointF.y: " + p.y);

            return faces[0];
        } else {
            Log.w("yoy", "not detect face...: " + tag);

            return null;
        }
    }

    int defaultEyesDistance = 100;

    private Bitmap scaleBitMap(Bitmap bitmap, FaceDetector.Face face) {
        Bitmap scaledBitmap = bitmap;

        if (face != null) {
            int width = (int) (bitmap.getWidth() * defaultEyesDistance / face.eyesDistance());
            int height = (int) (bitmap.getHeight() * defaultEyesDistance / face.eyesDistance());

            Log.d("yoy", "origin bitmap.getWidth()=" + bitmap.getWidth());
            Log.d("yoy", "origin bitmap.getHeight()=" + bitmap.getHeight());
            Log.d("yoy", "scale to " + (defaultEyesDistance / face.eyesDistance()));
            scaledBitmap = bitmap.createScaledBitmap(bitmap, width, height, true);
            Log.d("yoy", "scaled bitmap.getWidth()=" + bitmap.getWidth());
            Log.d("yoy", "scaled bitmap.getHeight()=" + bitmap.getHeight());
        } else {
            Log.d("yoy", "scale to 1");
            scaledBitmap = bitmap.createScaledBitmap(bitmap, defaultImageWidth, defaultImageHeight, true);
        }

        bitmap.recycle();

        return scaledBitmap;
    }

    private void drawBitmap(Bitmap bitmap, FaceDetector.Face face, Canvas canvas, Paint paint) {
        PointF p = new PointF(0, 0);
        float left = ((defaultImageWidth - bitmap.getWidth()) / 2);
        float top = ((defaultImageHeight - bitmap.getHeight()) / 2);

        if (face != null) {
            face.getMidPoint(p);

            left = defaultImageWidth / 2 - p.x * (defaultEyesDistance / face.eyesDistance());
            top = defaultImageHeight / 2 - p.y * (defaultEyesDistance / face.eyesDistance());
        }

        Log.d("yoy", "@drawBitmap left=" + left + ", top=" + top);
        Log.d("yoy", "@drawBitmap bitmap.getWidth()=" + bitmap.getWidth() + ", bitmap.getHeight()=" + bitmap.getHeight());

        canvas.drawBitmap(bitmap, left, top, paint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        viewWidth = getWidth();
        viewHeight = getHeight();

        // Log.d("yoy", "@onDraw mFadeinAlpthVal=" + mFadeinAlpthVal + ", mCurrentImageIndex=" +
        // mCurrentImageIndex);

        // 1. need switch bitmap?
        if (mFadeinAlpthVal >= MAX_ALPHA_VAL) {

            if (mCurrentImageIndex > IMAGES_FILE_NAME.length - 2) {
                drawBitmap(nextBitmap, nextFace, canvas, mBackGroundPaint);
                return;
            }

            if (currentBitmap != null) {
                currentBitmap.recycle();
                currentBitmap = null;
            }

            currentBitmap = nextBitmap;
            currentFace = nextFace;

            nextBitmap = BitmapFactory.decodeFile(mImagePath + IMAGES_FILE_NAME[mCurrentImageIndex + 1], mBitmapOptions);
            nextFace = detectFace(nextBitmap, IMAGES_FILE_NAME[mCurrentImageIndex + 1]);
            nextBitmap = scaleBitMap(nextBitmap, nextFace);
            mFadeinAlpthVal = MIN_ALPHA_VAL;

            mCurrentImageIndex++;
        } else {
            mFadeinAlpthVal += MAX_ALPHA_VAL * VIEW_REFRESH_INTERVAL / IMAGE_FADE_IN_TIME;
            if (mFadeinAlpthVal > MAX_ALPHA_VAL) {
                mFadeinAlpthVal = MAX_ALPHA_VAL;
            }
        }

        if (currentBitmap != null) {
            drawBitmap(currentBitmap, currentFace, canvas, mBackGroundPaint);
            // canvas.drawBitmap(currentBitmap, 0, 0, mBackGroundPaint);
        }

        if (nextBitmap != null) {
            mAlphaPaint.setAlpha(mFadeinAlpthVal);
            drawBitmap(nextBitmap, nextFace, canvas, mAlphaPaint);
            // canvas.drawBitmap(nextBitmap, 0, 0, mAlphaPaint);
        }

        postInvalidateDelayed(VIEW_REFRESH_INTERVAL);
    }
}
