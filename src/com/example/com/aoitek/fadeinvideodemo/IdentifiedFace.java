
package com.example.com.aoitek.fadeinvideodemo;

import android.graphics.PointF;

import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

public class IdentifiedFace {

    public PointF mEyeCenterPos = null;
    public PointF mLEyePos = null;
    public PointF mREyePos = null;
    public float mEyeDistance;
    public final Face mFace;

    public IdentifiedFace(Face face) {
        mFace = face;

        if (face != null) {
            for (Landmark landmark : face.getLandmarks()) {
                if (available()) {
                    break;
                }

                switch (landmark.getType()) {
                    case Landmark.LEFT_EYE:
                        mLEyePos = landmark.getPosition();
                        break;
                    case Landmark.RIGHT_EYE:
                        mREyePos = landmark.getPosition();
                        break;
                }

                if (mLEyePos != null && mREyePos != null) {
                    mEyeCenterPos = Utils.centerPoint(mLEyePos, mREyePos);
                    mEyeDistance = (float) Utils.distance(mLEyePos, mREyePos);
                }
            }
        }
    }

    public boolean available() {
        return mLEyePos != null && mREyePos != null;
    }
}
