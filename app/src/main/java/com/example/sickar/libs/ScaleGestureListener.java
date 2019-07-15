package com.example.sickar.libs;

import android.view.ScaleGestureDetector;
import android.view.View;

/**
 * Listener for ScaleGestureDetector which scales a view
 */
public class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
    private static final String TAG = "app_" + ScaleGestureListener.class.getSimpleName();

    private static final float MAX_SCALE = 10f;
    private static final float MIN_SCALE = 0.1f;
    private float mScaleFactor = 1f;
    private View mView;

    public ScaleGestureListener(View view) {
        mView = view;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
//        Log.i(TAG, "scale factor" + detector.getScaleFactor());
        mScaleFactor += (detector.getScaleFactor() - 1);
        mScaleFactor = Math.max(MIN_SCALE, Math.min(MAX_SCALE, mScaleFactor));
        mView.setScaleX(mScaleFactor);
        mView.setScaleY(mScaleFactor);
        return super.onScale(detector);
    }
}
