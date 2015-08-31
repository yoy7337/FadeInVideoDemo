
package com.example.com.aoitek.fadeinvideodemo;

import android.content.Context;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class AlpahFadeInSurfaceView extends SurfaceView implements
        SurfaceHolder.Callback {

    final private Handler mHandler = new Handler();
    SurfaceHolder mHolder;
    Paint mPaint = new Paint();

    public AlpahFadeInSurfaceView(Context context) {
        super(context);
        init();
    }

    public AlpahFadeInSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AlpahFadeInSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mHolder = this.getHolder();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

}
