
package com.example.com.aoitek.fadeinvideodemo;

import android.content.Context;
import android.os.Build;
import android.text.format.Time;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.vision.face.FaceDetector;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

class MakeFadeInVideoTask extends TrackableAsyncTask<Void, Void, String> implements FadeInVideoEncoder.Callback {
    final private static String TAG = "MakeVideoTask";

    final private static int VIDEO_WIDTH = 800;
    final private static int VIDEO_HEIGHT = 450;
    final private static String VIDEO_PATH = "sdcard/Download/video_encoded.mp4";

    private long mStartTime;
    private long mEndTime;
    final private List<String> mImagePathList;
    final private String mAudioPath;
    final private Callback mCallback;
    final private Context mContext;
    private ProgressBar mProgress;

    public MakeFadeInVideoTask(Context context, List<String> imagePathList, String audioPath, Tracker tracker, Callback callback) {
        super(tracker);

        mContext = context;
        mImagePathList = imagePathList;
        mAudioPath = audioPath;
        mCallback = callback;
    }

    public MakeFadeInVideoTask(Context context, List<String> imagePathList, String audioPath, Tracker tracker, Callback callback,
            ProgressBar progress) {
        super(tracker);

        mContext = context;
        mImagePathList = imagePathList;
        mAudioPath = audioPath;
        mCallback = callback;
        mProgress = progress;

        if (mProgress != null) {
            mProgress.setProgress(0);
        }
    }

    @Override
    protected String doInBackground(Void... params) {
        Time time = new Time();
        time.setToNow();
        mStartTime = time.toMillis(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            FadeInVideoEncoder avc = new FadeInVideoEncoder(mContext, VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_PATH, getFaceDetector(), this);

            try {
                avc.encodeToMp4(mImagePathList, mAudioPath);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        time.setToNow();
        mEndTime = time.toMillis(false);

        return VIDEO_PATH;
    }

    @Override
    protected void onSuccess(String videoPath) {
        float costTimeSec = (mEndTime - mStartTime) / 1000;
        Log.d("TAG", "Make video success!!! cost " + costTimeSec + "seconds");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Toast.makeText(mContext, "Video maker not support for this android version.(lower then JELLY_BEAN_MR2)",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (mCallback != null) {
            mCallback.onVideoMakeCompleted(videoPath, mImagePathList.get(0));
        }
    }

    public interface Callback {
        void onVideoMakeCompleted(String resultPath, String previewUrl);

        void onProgress(int progress);
    }

    // VideoEncoder.Callback
    @Override
    public void onProgressUpdate(int progress) {
        if (mProgress != null) {
            mProgress.setProgress(progress);
        }
        if (mCallback != null) {
            mCallback.onProgress(progress);
        }
    }

    private FaceDetector getFaceDetector() {

        FaceDetector faceDetector = new FaceDetector.Builder(mContext)
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.FAST_MODE)
                .setProminentFaceOnly(true)
                .build();

        if (!faceDetector.isOperational()) {
            Log.d(TAG, "Face detector dependencies are not yet available.");
        }

        return faceDetector;
    }
}
