package com.example.sickar.image;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.TextView;

@SuppressLint("AppCompatCustomView")
public class ScalableImageView extends TextView {
    private static final String TAG = "app_" + ScalableImageView.class.getSimpleName();

    private ScaleGestureDetector mScaleGestureDetector;
    private TextView instance;

    public ScalableImageView(Context context) {
        super(context);
        mScaleGestureDetector = new ScaleGestureDetector(this.getContext(), new ScaleListener());
        instance = this;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.i(TAG, "touch");
        mScaleGestureDetector.onTouchEvent(event);
        return false;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private static final float MAX_SCALE = 10f;
        private static final float MIN_SCALE = 0.1f;
        private float mScaleFactor = 1f;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            Log.i(TAG, "scale factor" + detector.getScaleFactor());
            mScaleFactor = detector.getScaleFactor();
            mScaleFactor = Math.max(MIN_SCALE, Math.min(MAX_SCALE, mScaleFactor));
            instance.setScaleX(mScaleFactor);
            instance.setScaleY(mScaleFactor);
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }
    }
}
