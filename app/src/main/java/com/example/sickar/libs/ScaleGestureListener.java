package com.example.sickar.libs;

import android.view.ScaleGestureDetector;
import android.view.View;

/**
 * Custom listener for ScaleGestureDetector which handles the result of a scaling gesture on a view.
 */
public class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
    private static final String TAG = "app_" + ScaleGestureListener.class.getSimpleName();

    /**
     * Maximum Scale allowed
     */
    private static final float MAX_SCALE = 10f;

    /**
     * Minimum Scale allowed
     */
    private static final float MIN_SCALE = 0.1f;

    /**
     * Scaling factor that is altered by the scaling gesture
     */
    private float scaleFactor = 1f;

    /**
     * View that receives the scaling event.
     */
    private View view;

    /**
     * Construct a ScaleGestureListener with the view to be scaled.
     *
     * @param view the view to receive scaling events
     */
    public ScaleGestureListener(View view) {
        this.view = view;
    }

    /**
     * Responds to scaling events for a gesture in progress. Reported by pointer motion.
     *
     * @param detector ScaleGestureDetector: The detector reporting the event - use this to retrieve
     *                 extended info about event state.
     * @return Whether or not the detector should consider this event as handled. If an event was
     * not handled, the detector will continue to accumulate movement until an event is handled. his
     * can be useful if an application, for example, only wants to update scaling factors if the
     * change is greater than 0.01.
     */
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
