package com.example.sickar.image;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
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

import java.util.Map;
import java.util.Objects;

public class ImageSystemPageFragment extends Fragment {
    private static final String TAG = "app_" + ImageSystemPageFragment.class.getSimpleName();

    private int[] viewXY;
    private float[] initXY;
    private float[] imageInitXY;
    private Map<String, Bitmap> mImages;

    /**
     * Takes in a map of Bitmap images to orientation
     * keys: TOP, BOT, RF, RB, LF, LB
     *
     * @param images images map
     */
    public ImageSystemPageFragment(Map<String, Bitmap> images) {
        viewXY = new int[]{0, 0};
        initXY = new float[]{0, 0};
        imageInitXY = new float[]{0, 0};
        this.mImages = images;
    }

    /**
     * Initializes a fragment with no images
     */
    public ImageSystemPageFragment() {
        this(null);
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
        View view = inflater.inflate(R.layout.fragment_image_system_page, container, false);
        ImageView image = view.findViewById(R.id.main_imageView);
        image.post(() -> {
            imageInitXY[0] = image.getX();
            imageInitXY[1] = image.getY();
        });

        RadioGroup pictureSelectors = view.findViewById(R.id.image_selectors);
        pictureSelectors.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case (R.id.radioButtonTop):
//                    image.setImageDrawable(getResources().getDrawable(R.drawable.mclaren1, null));
                    setImageBitmap(image, "Top");
                    break;
                case (R.id.radioButtonBot):
//                    image.setImageDrawable(getResources().getDrawable(R.drawable.mclaren2, null));
                    setImageBitmap(image, "Bot");
                    break;
                case (R.id.radioButtonRF):
                    setImageBitmap(image, "RF");
                    break;
                case (R.id.radioButtonRB):
                    setImageBitmap(image, "RB");
                    break;
                case (R.id.radioButtonLF):
                    setImageBitmap(image, "LF");
                    break;
                case (R.id.radioButtonLB):
                    setImageBitmap(image, "LB");
                    break;
                case (-1):
                    // check cleared
                    break;
            }
            image.setX((view.getWidth() - image.getWidth()) / 2f);
            image.setY((view.getHeight() - image.getHeight()) / 2f);
        });

        ScaleGestureListener scaleGestureListener = new ScaleGestureListener(image);
        ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(this.getContext(),
                scaleGestureListener);

        image.setOnTouchListener((vw, ev) -> {
            scaleGestureDetector.onTouchEvent(ev);
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initXY[0] = ev.getRawX() - vw.getX() - viewXY[0];
                    initXY[1] = ev.getRawY() - vw.getY() - viewXY[1];
                    break;
                case MotionEvent.ACTION_MOVE:
                    vw.setX(ev.getRawX() - viewXY[0] - initXY[0]);
                    vw.setY(ev.getRawY() - viewXY[1] - initXY[1]);
                    break;
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
//        Log.i(TAG, Arrays.toString(viewXY));
    }

    private void setImageBitmap(ImageView image, String key) {
        if (mImages != null && mImages.containsKey(key)) {
            image.setImageBitmap(mImages.get(key));
            image.invalidate();
        }
    }

}
