package com.example.sickar.image;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sickar.R;
import com.example.sickar.libs.ScaleGestureListener;

import java.util.Objects;

public class ImageSystemPageFragment extends Fragment {
    private static final String TAG = "app_" + ImageSystemPageFragment.class.getSimpleName();

    private int[] viewXY;

    public ImageSystemPageFragment() {
        viewXY = new int[]{0, 0};
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

        ImageView image = view.findViewById(R.id.main_imageView);

        RadioGroup pictureSelectors = view.findViewById(R.id.image_selectors);
        pictureSelectors.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case (R.id.radioButtonTop):
                    image.setImageDrawable(getResources().getDrawable(R.drawable.mclaren1, null));
                    break;
                case (R.id.radioButtonBot):
                    image.setImageDrawable(getResources().getDrawable(R.drawable.mclaren2, null));
                    break;
                case (R.id.radioButtonRF):
                    break;
                case (R.id.radioButtonRB):
                    break;
                case (R.id.radioButtonLF):
                    break;
                case (R.id.radioButtonLB):
                    break;
                case (-1):
                    // check cleared
                    break;
            }
        });

        ScaleGestureListener scaleGestureListener = new ScaleGestureListener(image);
        ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(this.getContext(),
                scaleGestureListener);

        image.setOnTouchListener((vw, ev) -> {
            view.getLocationOnScreen(viewXY);
            scaleGestureDetector.onTouchEvent(ev);
            if (ev.getAction() == MotionEvent.ACTION_MOVE) {
                vw.setX(ev.getRawX() - viewXY[0] - (float) vw.getWidth() / 2);
                vw.setY(ev.getRawY() - viewXY[1] - (float) vw.getHeight() / 2);
            }
            return true;
        });

        return view;
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally
     * tied to {@link ImageActivity} onResume() of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onResume() {
        super.onResume();
        // set the offset required since the fragment is embedded in a viewpager
        Objects.requireNonNull(this.getView()).getLocationOnScreen(viewXY);
    }

}
