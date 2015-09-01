
package com.example.com.aoitek.fadeinvideodemo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        int viewWidth = Utils.getVideoWidth(this);
        int viewHeight = Utils.getVideoHeight(this);
        Log.d("yoy", "view W=" + viewWidth + ", H=" + viewHeight);

        View fadeInView = findViewById(R.id.fade_in_view);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) fadeInView.getLayoutParams();
        params.width = viewWidth;
        params.height = viewHeight;
    }
}
