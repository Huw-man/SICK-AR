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
    private float scaleFactor = 1f;
    private View view;

    public ScaleGestureListener(View view) {
        this.view = view;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
//        Log.i(TAG, "scale factor" + detector.getScaleFactor());
        scaleFactor += (detector.getScaleFactor() - 1);
        scaleFactor = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scaleFactor));
        view.setScaleX(scaleFactor);
        view.setScaleY(scaleFactor);
        return super.onScale(detector);
    }
}
