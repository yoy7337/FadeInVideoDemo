
package com.example.com.aoitek.fadeinvideodemo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MakeVideoActivity extends Activity implements View.OnClickListener, MakeFadeInVideoTask.Callback {

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

    private static final String AUDIO_FILE_NAME = "1.raw";

    private TrackableAsyncTask.Tracker mTaskTracker = new TrackableAsyncTask.Tracker();

    TextView mProgressView;
    Button mMakeBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_video);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mProgressView = (TextView) findViewById(R.id.progress_text);
        mMakeBtn = (Button) findViewById(R.id.start_btn);
        mMakeBtn.setOnClickListener(this);
    }

    @Override
    public void onVideoMakeCompleted(String resultPath, String previewUrl) {
    }

    @Override
    public void onProgress(final int progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressView.setText("Progress: " + progress + " %");
            }
        });
    }

    @Override
    public void onClick(View v) {

        List<String> photoList = new ArrayList<String>();
        for (String fileName : IMAGES_FILE_NAME) {
            photoList.add("file://" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()
                    + "/" + fileName);
        }

        String audioPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()
                + "/" + AUDIO_FILE_NAME;
        InputStream audioInputStream = this.getResources().openRawResource(R.raw.gif_audio);

        MakeFadeInVideoTask makeVideoTask = new MakeFadeInVideoTask(this, photoList, audioInputStream, mTaskTracker, this, null);
        makeVideoTask.cancelPreviousAndExecuteParallel(null);
    }
}
