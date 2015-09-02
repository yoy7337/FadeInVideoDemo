
package com.example.com.aoitek.fadeinvideodemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageSize;

public class GmsAlpahFadeInView extends View {

    private static final String TAG = "GmsAlpahFadeInView";
    private static final String DEBUG_TAG = "debug";
    private static final boolean DEBUG = false;

    private static final int IMAGE_FADE_IN_TIME = (int) (1f * 1000);
    private static final int VIEW_REFRESH_INTERVAL = 50;
    private static final int MAX_ALPHA_VAL = 255;
    private static final int MIN_ALPHA_VAL = 0;

    private static final double DEFAULT_IMAGE_SIZE_FOR_VIEW_RATIO = 0.7;
    private static final double DEFAULT_EYE_FOR_VIEW_WIDTH_RATIO = 0.1;
    private static final double DEFAULT_EYE_CENTER_X_FOR_VIEW_WIDTH_RATIO = 0.5;
    private static final double DEFAULT_EYE_CENTER_Y_FOR_VIEW_HEIGHT_RATIO = 0.36;

    private static final String IMAGE_LABEL = "  Made With Lollipop  ";
    private static final double DEFAULT_TEXT_SIZE_FOR_HEIGHT_RATIO = 0.05;

    private int mDefaultViewWidth = 800;
    private int mDefaultViewHeight = 450;

    private int mDefaultImageWidth = 640;
    private int mDefaultImageHeight = 360;

    private float mDefaultEyeDistance = (float) (mDefaultViewWidth * DEFAULT_EYE_FOR_VIEW_WIDTH_RATIO);
    private int mDefaultEyeCenterX = (int) (mDefaultViewWidth * DEFAULT_EYE_CENTER_X_FOR_VIEW_WIDTH_RATIO);
    private int mDefaultEyeCenterY = (int) (mDefaultViewHeight * DEFAULT_EYE_CENTER_Y_FOR_VIEW_HEIGHT_RATIO);

    private String mImagePath = null;
    private BitmapFactory.Options mBitmapOptions = new BitmapFactory.Options();

    private Bitmap mBaseBitmap = null;

    private ImageLoader mImageLoader;

    private int mFadeinAlpthVal = MAX_ALPHA_VAL;

    FaceDetector mFaceDetector;

    Face mCurrentFace;
    PointF mCurrentEyeCenter;
    float mCurrentEyeDistance;
    PointF mCurrentLEye;
    PointF mCurrentREye;

    Face mNextFace;
    PointF mNextEyeCenter;
    float mNextEyeDistance;
    PointF mNextLEye;
    PointF mNextREye;

    private int mCurrentImageIndex = -1;

    Bitmap mCurrentBitmap = null;
    float mCurrentBitmapScale = 1;
    Bitmap mNextBitmap = null;
    float mNextBitmapScale = 1;

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
        mImagePath = "file://" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/";
        mBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        mBitmapOptions.inMutable = true;

        mImageLoader = ImageLoader.getInstance();
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        super.onSizeChanged(xNew, yNew, xOld, yOld);

        if (mBaseBitmap == null) {

            mDefaultViewWidth = xNew;
            mDefaultViewHeight = yNew;
            mDefaultImageWidth = (int) (mDefaultViewWidth * DEFAULT_IMAGE_SIZE_FOR_VIEW_RATIO);
            mDefaultImageHeight = (int) (mDefaultViewHeight * DEFAULT_IMAGE_SIZE_FOR_VIEW_RATIO);

            mDefaultEyeDistance = (float) (mDefaultViewWidth * DEFAULT_EYE_FOR_VIEW_WIDTH_RATIO);
            mDefaultEyeCenterX = (int) (mDefaultViewWidth * DEFAULT_EYE_CENTER_X_FOR_VIEW_WIDTH_RATIO);
            mDefaultEyeCenterY = (int) (mDefaultViewHeight * DEFAULT_EYE_CENTER_Y_FOR_VIEW_HEIGHT_RATIO);

            Log.d(TAG, "@onSizeChanged: mDefaultViewWidth=" + mDefaultViewWidth + ", mDefaultViewHeight=" + mDefaultViewHeight);
            Log.d(TAG, "@onSizeChanged: mDefaultImageWidth=" + mDefaultImageWidth + ", mDefaultImageHeight=" + mDefaultImageHeight);
            Log.d(TAG, "@onSizeChanged: mDefaultEyeDistance=" + mDefaultEyeDistance);
            Log.d(TAG, "@onSizeChanged: mDefaultEyeCenterX=" + mDefaultEyeCenterX + ", mDefaultEyeCenterY=" + mDefaultEyeCenterY);

            mBaseBitmap = Bitmap.createBitmap(mDefaultViewWidth, mDefaultViewHeight, Bitmap.Config.RGB_565);
        }
    }

    private void getFaceDetector() {
        if (mFaceDetector == null) {
            mFaceDetector = new FaceDetector.Builder(getContext())
                    .setTrackingEnabled(false)
                    .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                    .setMode(FaceDetector.FAST_MODE)
                    .setProminentFaceOnly(true)
                    .build();

        }
        if (!mFaceDetector.isOperational()) {
            Toast.makeText(getContext(), "Face detector dependencies are not yet available.", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private void releaseFaceDetector() {
        if (mFaceDetector != null) {
            mFaceDetector.release();
            mFaceDetector = null;
        }
    }

    private SparseArray<Face> detectFace(Frame frame, String tag) {
        getFaceDetector();

        SparseArray<Face> faces = mFaceDetector.detect(frame);

        return faces;
    }

    private Bitmap scaleBitMap(float scale, Bitmap bitmap, String bitmapPath) {
        Bitmap scaledBitmap = bitmap;

        if (scale > 0) {
            float scaledW = (float) bitmap.getWidth() * scale;
            float scaledH = (float) bitmap.getHeight() * scale;
            scaledBitmap = mImageLoader.loadImageSync(bitmapPath,
                    new ImageSize((int) scaledW, (int) scaledH));
        }

        return scaledBitmap;
    }

    private void getNextEyesPosition(Face face) {
        if (face != null) {
            for (Landmark landmark : face.getLandmarks()) {
                switch (landmark.getType()) {
                    case Landmark.LEFT_EYE:
                        mNextLEye = landmark.getPosition();
                        break;
                    case Landmark.RIGHT_EYE:
                        mNextREye = landmark.getPosition();
                        break;
                }
            }

            if (mNextLEye != null && mNextREye != null) {
                mNextEyeCenter = Utils.centerPoint(mNextLEye, mNextREye);
                mNextEyeDistance = (float) Utils.distance(mNextLEye, mNextREye);
                mNextBitmapScale = mDefaultEyeDistance / mNextEyeDistance;
            }
        }
    }

    private void processNextImg() {
        mCurrentBitmap = mNextBitmap;
        mCurrentFace = mNextFace;
        mCurrentBitmapScale = mNextBitmapScale;
        mCurrentLEye = mNextLEye;
        mCurrentREye = mNextREye;
        mCurrentEyeCenter = mNextEyeCenter;
        mCurrentEyeDistance = mNextEyeDistance;

        mNextBitmap = null;
        mNextFace = null;
        mNextBitmapScale = 1;
        mNextLEye = null;
        mNextREye = null;
        mNextEyeCenter = null;
        mNextEyeDistance = 0;

        mNextBitmap = mImageLoader.loadImageSync(mImagePath + IMAGES_FILE_NAME[mCurrentImageIndex + 1], new ImageSize(
                mDefaultImageWidth,
                mDefaultImageHeight));
        Frame frame = new Frame.Builder().setBitmap(mNextBitmap).build();
        SparseArray<Face> faces = detectFace(frame, IMAGES_FILE_NAME[mCurrentImageIndex + 1]);
        if (faces.size() > 0) {
            mNextFace = faces.valueAt(0);
            getNextEyesPosition(mNextFace);
            mNextBitmap = scaleBitMap(mNextBitmapScale, mNextBitmap, mImagePath + IMAGES_FILE_NAME[mCurrentImageIndex + 1]);
        }

        mFadeinAlpthVal = MIN_ALPHA_VAL;

        mCurrentImageIndex++;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Canvas myCanvas = new Canvas(mBaseBitmap);

        // 1. need switch bitmap?
        if (mFadeinAlpthVal >= MAX_ALPHA_VAL) {

            if (mCurrentImageIndex > IMAGES_FILE_NAME.length - 2) {
                // drawBitmap(mNextBitmap, mNextFace, canvas, mBackGroundPaint);
                drawBitmap(mNextBitmap, mNextFace, mNextBitmapScale, mNextEyeCenter, myCanvas, new Paint(), MAX_ALPHA_VAL);
                releaseFaceDetector();

                canvas.drawBitmap(mBaseBitmap, 0, 0, new Paint());
                return;
            }

            processNextImg();
        } else {
            mFadeinAlpthVal += MAX_ALPHA_VAL * VIEW_REFRESH_INTERVAL / IMAGE_FADE_IN_TIME;
            if (mFadeinAlpthVal > MAX_ALPHA_VAL) {
                mFadeinAlpthVal = MAX_ALPHA_VAL;
            }
        }

        if (mCurrentBitmap != null) {
            drawBitmap(mCurrentBitmap, mCurrentFace, mCurrentBitmapScale, mCurrentEyeCenter, myCanvas, new Paint(), MAX_ALPHA_VAL);
        }

        if (mNextBitmap != null) {
            drawBitmap(mNextBitmap, mNextFace, mNextBitmapScale, mNextEyeCenter, myCanvas, new Paint(), mFadeinAlpthVal);
        }

        postInvalidateDelayed(VIEW_REFRESH_INTERVAL);

        canvas.drawBitmap(mBaseBitmap, 0, 0, new Paint());
    }

    private void drawDebugLine(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        canvas.drawLine(mDefaultEyeCenterX - mDefaultEyeDistance / 2, 0, mDefaultEyeCenterX - mDefaultEyeDistance / 2, mDefaultViewHeight,
                paint);
        canvas.drawLine(mDefaultEyeCenterX + mDefaultEyeDistance / 2, 0, mDefaultEyeCenterX + mDefaultEyeDistance / 2, mDefaultViewHeight,
                paint);
        canvas.drawLine(0, mDefaultEyeCenterY, mDefaultViewWidth, mDefaultEyeCenterY, paint);
    }

    private void drawLabel(Canvas canvas) {
        float textSize = (float) (mDefaultViewHeight * DEFAULT_TEXT_SIZE_FOR_HEIGHT_RATIO);

        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.argb(128, 255, 255, 255));
        textPaint.setTextSize(textSize);

        Paint backgroundPaint = new Paint();
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setColor(Color.argb(128, 0, 0, 0));
        canvas.drawRect(0, mDefaultViewHeight - 2 * textSize, textPaint.measureText(IMAGE_LABEL), mDefaultViewHeight, backgroundPaint);

        canvas.drawText(IMAGE_LABEL, 0, mDefaultViewHeight - textSize / 2, textPaint);
    }

    private void drawBitmap(Bitmap bitmap, Face face, float scale, PointF eyeCenter, Canvas canvas, Paint paint, int alpha) {
        float left = ((mDefaultViewWidth - bitmap.getWidth()) / 2);
        float top = ((mDefaultViewHeight - bitmap.getHeight()) / 2);

        canvas.save();
        paint.setAntiAlias(true);
        paint.setAlpha(alpha);

        if (face != null) {
            if (DEBUG) {
                Log.d(DEBUG_TAG, "bitmap.getWidth() = " + bitmap.getWidth() + ", bitmap.getHeight()=" + bitmap.getHeight());
                Log.d(DEBUG_TAG, "left = " + left + ", top=" + top);
            }
            left = ((float) mDefaultEyeCenterX - eyeCenter.x * scale);
            top = ((float) mDefaultEyeCenterY - eyeCenter.y * scale);
            if (DEBUG) {
                Log.d(DEBUG_TAG, "eyeCenter.x = " + eyeCenter.x + ", eyeCenter.y=" + eyeCenter.y +
                        ", scale =" + scale);
            }

            canvas.rotate(face.getEulerZ(), mDefaultEyeCenterX, mDefaultEyeCenterY);
        }

        paint.setColor(Color.argb(alpha, 255, 255, 255));
        paint.setStyle(Paint.Style.STROKE);
        int strokeWidth = 4;
        paint.setStrokeWidth(strokeWidth * 2);

        canvas.drawRect(left - strokeWidth, top - strokeWidth, left + bitmap.getWidth() + strokeWidth, top + bitmap.getHeight()
                + strokeWidth, paint);
        canvas.drawBitmap(bitmap, left, top, paint);
        canvas.restore();

        // drawDebugLine(canvas);
        drawLabel(canvas);
    }
}
