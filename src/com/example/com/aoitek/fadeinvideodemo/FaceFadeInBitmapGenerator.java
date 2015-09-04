
package com.example.com.aoitek.fadeinvideodemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PointF;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageSize;

import java.util.List;

public class FaceFadeInBitmapGenerator {

    private static final String TAG = "FaceFadeInBitmapGenerator";
    private static final String DEBUG_TAG = "debug";
    private static final boolean DEBUG = false;

    // define ratio for fade in image and eyes align point
    private static final double DEFAULT_IMAGE_SIZE_FOR_VIEW_RATIO = 0.5;
    private static final double DEFAULT_EYES_FOR_VIEW_WIDTH_RATIO = 0.1;
    private static final double DEFAULT_EYES_CENTER_X_FOR_VIEW_WIDTH_RATIO = 0.5;
    private static final double DEFAULT_EYES_CENTER_Y_FOR_VIEW_HEIGHT_RATIO = 0.36;

    // define for image label
    private static final String DEFAULT_IMAGE_LABEL = "  Made With Lollipop  ";
    private static final double DEFAULT_TEXT_SIZE_FOR_HEIGHT_RATIO = 0.03;
    private static final int DEFAULT_LABEL_BACKGROUND_COLOR = Color.argb(255, 0, 0, 0);
    private static final int DEFAULT_LABEL_TEXT_COLOR = Color.argb(128, 255, 255, 255);

    // define for boarder
    private static final int DEFAULT_BOARDER_COLOR = Color.argb(128, 255, 255, 255);
    private static final int DEFAULT_BOARDER_WIDTH = 8;
    private static final float DEFAULT_BOARDER_BLUR_RADUIS = 2;

    private final Context mContext;

    private int mWidth = 800;
    private int mHeight = 450;

    private int mDefaultImageWidth = 640;
    private int mDefaultImageHeight = 360;

    // eyes distance and eyes center point
    private float mEyesDistance = (float) (mWidth * DEFAULT_EYES_FOR_VIEW_WIDTH_RATIO);
    private int mEyesCenterX = (int) (mWidth * DEFAULT_EYES_CENTER_X_FOR_VIEW_WIDTH_RATIO);
    private int mEyesCenterY = (int) (mHeight * DEFAULT_EYES_CENTER_Y_FOR_VIEW_HEIGHT_RATIO);

    private Bitmap mBaseBitmap = null;
    private final BitmapFactory.Options mBitmapOptions = new BitmapFactory.Options();
    private final ImageLoader mImageLoader = ImageLoader.getInstance();

    Bitmap mFadeInBitmap = null;
    IdentifiedFace mFadeInFace;
    float mFadeBitmapScale = 1;

    private final FaceDetector mFaceDetector;

    public FaceFadeInBitmapGenerator(Context context, int width, int height, FaceDetector faceDetector) {
        mContext = context;
        mFaceDetector = faceDetector;

        mBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        mBitmapOptions.inMutable = true;

        mWidth = width;
        mHeight = height;
        mDefaultImageWidth = (int) (mWidth * DEFAULT_IMAGE_SIZE_FOR_VIEW_RATIO);
        mDefaultImageHeight = (int) (mHeight * DEFAULT_IMAGE_SIZE_FOR_VIEW_RATIO);

        mEyesDistance = (float) (mWidth * DEFAULT_EYES_FOR_VIEW_WIDTH_RATIO);
        mEyesCenterX = (int) (mWidth * DEFAULT_EYES_CENTER_X_FOR_VIEW_WIDTH_RATIO);
        mEyesCenterY = (int) (mHeight * DEFAULT_EYES_CENTER_Y_FOR_VIEW_HEIGHT_RATIO);

        Log.d(TAG, "@init: mWidth=" + mWidth + ", mHeight=" + mHeight);
        Log.d(TAG, "@init: mDefaultImageWidth=" + mDefaultImageWidth + ", mDefaultImageHeight=" + mDefaultImageHeight);
        Log.d(TAG, "@init: mEyesDistance=" + mEyesDistance);
        Log.d(TAG, "@init: mEyesCenterX=" + mEyesCenterX + ", mEyesCenterY=" + mEyesCenterY);

        mBaseBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.RGB_565);
    }

    public void release(boolean releaseBaseBitmap) {
        if (mBaseBitmap != null && releaseBaseBitmap) {
            mBaseBitmap.recycle();
        }

        if (mFaceDetector != null) {
            mFaceDetector.release();
        }
    }

    public boolean setFadeImage(String imagePath) {
        mFadeInBitmap = mImageLoader.loadImageSync(imagePath, new ImageSize(mDefaultImageWidth, mDefaultImageHeight));
        if (mFadeInBitmap == null) {
            Log.e(TAG, "Can not load image from imagePath");
            return false;
        }

        Frame frame = new Frame.Builder().setBitmap(mFadeInBitmap).build();
        mFadeInFace = detectFace(mFaceDetector, frame, imagePath);

        if (mFadeInFace != null) {
            mFadeBitmapScale = calculateEyesDistanceScale(mEyesDistance, mFadeInFace);
            mFadeInBitmap = scaleBitMapByEyesScale(mFadeBitmapScale, mFadeInBitmap, imagePath);
        } else {
            mFadeBitmapScale = 1;
        }

        return true;
    }

    public Bitmap genFadeInImage(int alpha) {
        drawFadeInImage(alpha, DEFAULT_IMAGE_LABEL, true);
        return mBaseBitmap;
    }

    private void drawFadeInImage(int alpha, String label, boolean withBoard) {
        if (mFadeInBitmap == null) {
            return;
        }

        Canvas canvas = new Canvas(mBaseBitmap);
        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG));
        canvas.save();

        float left = ((mWidth - mFadeInBitmap.getWidth()) / 2);
        float top = ((mHeight - mFadeInBitmap.getHeight()) / 2);

        if (mFadeInFace != null) {
            left = ((float) mEyesCenterX - mFadeInFace.mEyeCenterPos.x * mFadeBitmapScale);
            top = ((float) mEyesCenterY - mFadeInFace.mEyeCenterPos.y * mFadeBitmapScale);
            if (DEBUG) {
                Log.d(DEBUG_TAG, "bitmap.getWidth() = " + mFadeInBitmap.getWidth() + ", bitmap.getHeight()=" + mFadeInBitmap.getHeight());
                Log.d(DEBUG_TAG, "left = " + left + ", top=" + top);
                Log.d(DEBUG_TAG, "eyesCenter.x = " + mFadeInFace.mEyeCenterPos.x + ", eyesCenter.y=" + mFadeInFace.mEyeCenterPos.y +
                        ", mFadeBitmapScale =" + mFadeBitmapScale);
            }

            canvas.rotate(mFadeInFace.mFace.getEulerZ(), mEyesCenterX, mEyesCenterY);
        }

        // 1. draw imgage
        Paint imagePaint = new Paint();
        imagePaint.setAlpha(alpha);
        canvas.drawBitmap(mFadeInBitmap, left, top, imagePaint);

        if (withBoard) {
            drawColorBoard(canvas, left, top, alpha, Color.argb(alpha, 255, 255, 255), DEFAULT_BOARDER_WIDTH, DEFAULT_BOARDER_BLUR_RADUIS);
        }

        canvas.restore();

        if (!TextUtils.isEmpty(label)) {
            drawLabel(canvas, label);
        }
    }

    private void drawColorBoard(Canvas canvas, float left, float top, int alpha, int color, int boardWidth, float blurRadius) {
        Paint boardPaint = new Paint();
        boardPaint.setMaskFilter(new BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL));
        boardPaint.setAlpha(alpha);
        boardPaint.setColor(color);
        boardPaint.setStyle(Paint.Style.STROKE);
        boardPaint.setStrokeWidth(boardWidth);
        canvas.drawRect(left - boardWidth / 2,
                top - boardWidth / 2,
                left + mFadeInBitmap.getWidth() + boardWidth / 2,
                top + mFadeInBitmap.getHeight() + boardWidth / 2,
                boardPaint);
    }

    private void drawLabel(Canvas canvas, String label) {
        float textSize = (float) (mHeight * DEFAULT_TEXT_SIZE_FOR_HEIGHT_RATIO);

        Paint textPaint = new Paint();
        textPaint.setColor(DEFAULT_LABEL_TEXT_COLOR);
        textPaint.setTextSize(textSize);
        Rect textBounds = new Rect();
        textPaint.getTextBounds(label, 0, label.length(), textBounds);

        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(DEFAULT_LABEL_BACKGROUND_COLOR);

        canvas.drawRect(0, mHeight - (int) (2.5 * textBounds.height()), textPaint.measureText(DEFAULT_IMAGE_LABEL), mHeight,
                backgroundPaint);

        canvas.drawText(label, 0, mHeight - textBounds.height(), textPaint);
    }

    private IdentifiedFace detectFace(FaceDetector faceDetector, Frame frame, String tag) {
        SparseArray<Face> faces = faceDetector.detect(frame);
        if (faces.size() > 0) {
            return new IdentifiedFace(faces.valueAt(0));
        } else {
            return null;
        }
    }

    private float calculateEyesDistanceScale(float targetEyesDistance, IdentifiedFace face) {
        return face.available() ? (targetEyesDistance / face.mEyeDistance) : 1;
    }

    private Bitmap scaleBitMapByEyesScale(float scale, Bitmap bitmap, String bitmapPath) {
        Bitmap scaledBitmap = bitmap;

        if (scale > 0) {
            float scaledW = (float) bitmap.getWidth() * scale;
            float scaledH = (float) bitmap.getHeight() * scale;
            scaledBitmap = mImageLoader.loadImageSync(bitmapPath,
                    new ImageSize((int) scaledW, (int) scaledH));
        }

        return scaledBitmap;
    }
}
