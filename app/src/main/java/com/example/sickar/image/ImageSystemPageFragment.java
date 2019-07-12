package com.example.sickar.image;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sickar.R;

public class ImageSystemPageFragment extends Fragment {
    private static final String TAG = "app_" + ImageSystemPageFragment.class.getSimpleName();

    private ImageView top;
    private ScaleGestureDetector mScaleGestureDetector;

    public ImageSystemPageFragment() {
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is optional, and non-graphical fragments can return null (which
     * is the default implementation).  This will be called between
     * {@link #onCreate(Bundle)} and {@link #onActivityCreated(Bundle)}.
     *
     * <p>If you return a View from here, you will later be called in
     * {@link #onDestroyView} when the view is being released.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to.  The fragment should not add the view itself,
     *                           but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.image_system_page_fragment, container, false);
//        view.setOnTouchListener(new GestureListener(this.getContext()));
//        mScaleGestureDetector = new ScaleGestureDetector(this.getContext(), new ScaleListener());

        top = new ImageView(this.getContext());
        top.setImageDrawable(getResources().getDrawable(R.drawable.mclaren1, null));
//        top.setText("TEXT BOI!");
//        top.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.mclaren1, null),
//                null, null);
//        top.setOnTouchListener((v, event) -> {
//            Log.i(TAG, "scaling " + mScaleGestureDetector.isInProgress());
//            mScaleGestureDetector.onTouchEvent(event);
//            return false;
//        });

//        top.setOnTouchListener(new GestureListener(this.getContext()));


//        bottom.setOnTouchListener(new GestureListener(this.getContext()));
        LinearLayout ly = view.findViewById(R.id.image_linear_layout);
        ly.addView(top);

        ImageView bot = new ImageView(this.getContext());
        bot.setImageDrawable(getResources().getDrawable(R.drawable.mclaren2, null));
//
//        bot.setOnTouchListener(new GestureListener(this.getContext()));
////        bottom.setOnTouchListener(new GestureListener(this.getContext()));
        ly.addView(bot);
        return view;
    }

    private class GestureListener implements View.OnTouchListener,
            ScaleGestureDetector.OnScaleGestureListener {
        private static final float MAX_SCALE = 10f;
        private static final float MIN_SCALE = 0.1f;

        private ScaleGestureDetector mScaleDetector;
        private View mView;
        private float mScaleFactor = 1.0f;

        private GestureListener(Context context) {
            mScaleDetector = new ScaleGestureDetector(context, this);
        }

        /**
         * Called when a touch event is dispatched to a view. This allows listeners to
         * get a chance to respond before the target view.
         *
         * @param v     The view the touch event has been dispatched to.
         * @param event The MotionEvent object containing full information about
         *              the event.
         * @return True if the listener has consumed the event, false otherwise.
         */
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Log.i(TAG, "touch detected on " + v.toString());
            mView = v;
            Log.i(TAG, "scaling " + mScaleDetector.isInProgress());
//            mScaleFactor += 0.1f;
//            mView.setScaleX(mScaleFactor);
//            mView.setScaleY(mScaleFactor);
            mScaleDetector.onTouchEvent(event);
//            onScale(mScaleDetector);
            return false;
        }

        /**
         * Responds to scaling events for a gesture in progress.
         * Reported by pointer motion.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should consider this event
         * as handled. If an event was not handled, the detector
         * will continue to accumulate movement until an event is
         * handled. This can be useful if an application, for example,
         * only wants to update scaling factors if the change is
         * greater than 0.01.
         */
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            Log.i(TAG, "scale factor" + detector.getScaleFactor());
            mScaleFactor = detector.getScaleFactor();
            mScaleFactor = Math.max(MIN_SCALE, Math.min(MAX_SCALE, mScaleFactor));
            mView.setScaleX(mScaleFactor);
            mView.setScaleY(mScaleFactor);
            return false;
        }

        /**
         * Responds to the beginning of a scaling gesture. Reported by
         * new pointers going down.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should continue recognizing
         * this gesture. For example, if a gesture is beginning
         * with a focal point outside of a region where it makes
         * sense, onScaleBegin() may return false to ignore the
         * rest of the gesture.
         */
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return false;
        }

        /**
         * Responds to the end of a scale gesture. Reported by existing
         * pointers going up.
         * <p>
         * Once a scale has ended, {@link ScaleGestureDetector#getFocusX()}
         * and {@link ScaleGestureDetector#getFocusY()} will return focal point
         * of the pointers remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         */
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {

        }
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
            top.setScaleX(mScaleFactor);
            top.setScaleY(mScaleFactor);
            return super.onScale(detector);
        }
    }
}
