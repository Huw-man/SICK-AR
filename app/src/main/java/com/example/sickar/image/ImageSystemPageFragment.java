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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This fragment displays the images associated with a particular system of an item.
 */
public class ImageSystemPageFragment extends Fragment {
    /**
     * debugging TAG
     */
    private static final String TAG = "app_" + ImageSystemPageFragment.class.getSimpleName();

    /**
     * Maps the name of the camera device to the ids of the radio button group. Used for setting the
     * first image in the viewer.
     */
    private static Map<String, Integer> radioGroupNamesToIds;

    static {
        radioGroupNamesToIds = new LinkedHashMap<>();
        radioGroupNamesToIds.put("Top", R.id.radioButtonTop);
        radioGroupNamesToIds.put("Bot", R.id.radioButtonBot);
        radioGroupNamesToIds.put("LF", R.id.radioButtonLF);
        radioGroupNamesToIds.put("LB", R.id.radioButtonLB);
        radioGroupNamesToIds.put("RF", R.id.radioButtonRF);
        radioGroupNamesToIds.put("RB", R.id.radioButtonRB);
    }

    /**
     * Pixel offset of this fragment view. This is used to offset the presence of an AppBar or
     * Navigation bar which changes the absolute position of this fragment.
     */
    private int[] viewXY;

    /**
     * Holds the initial position of the touch from the top left corner of the image. This is used
     * to keep the relative position of the image to the touch point consistent while dragging or
     * scaling.
     */
    private float[] initXY;

    /**
     * Holds all the images relevant to this system. The mappings use the device names (TOP, BOT,
     * RF, RB, LF, LB)
     */
    private Map<String, Bitmap> images; // contains the images

    /**
     * Construct an ImageSystemPageFragment with a map of camera device names to Bitmaps. It should
     * contain images according to keys: TOP, BOT, RF, RB, LF, LB
     *
     * @param images images map
     */
    ImageSystemPageFragment(Map<String, Bitmap> images) {
        viewXY = new int[]{0, 0};
        initXY = new float[]{0, 0};
        this.images = images;
    }

    /**
     * Construct a fragment with no images
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

        RadioGroup imageSelectors = view.findViewById(R.id.image_selectors);
        imageSelectors.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case (R.id.radioButtonTop):
                    setImageBitmap(image, "Top");
                    break;
                case (R.id.radioButtonBot):
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
        // select the first available image on start
        boolean anyImage = false;
        for (String deviceName : radioGroupNamesToIds.keySet()) {
            if (images.get(deviceName) != null) {
                //noinspection ConstantConditions
                imageSelectors.check(radioGroupNamesToIds.get(deviceName));
                anyImage = true;
                break;
            }
        }
        if (!anyImage) setDefaultImage(image);

        ScaleGestureListener scaleGestureListener = new ScaleGestureListener(image);
        ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(this.getContext(),
                scaleGestureListener);

        // implements the drag behavior
        view.setOnTouchListener((vw, ev) -> {
            scaleGestureDetector.onTouchEvent(ev);
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initXY[0] = ev.getRawX() - image.getX() - viewXY[0];
                    initXY[1] = ev.getRawY() - image.getY() - viewXY[1];
                    break;
                case MotionEvent.ACTION_MOVE:
                    image.setX(ev.getRawX() - viewXY[0] - initXY[0]);
                    image.setY(ev.getRawY() - viewXY[1] - initXY[1]);
                    break;
            }
            return true;
        });

        return view;
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally tied to {@link ImageActivity} onResume() of the containing
     * Activity's lifecycle.
     *
     * This fragment's offset is also calculated in onResume().
     */
    @Override
    public void onResume() {
        super.onResume();
        // set the offset required since the fragment is embedded in a viewpager
        Objects.requireNonNull(this.getView()).getLocationOnScreen(viewXY);
    }

    /**
     * Sets the bitmap of an ImageView to be the from the specified camera device
     *
     * @param image ImageView
     * @param key   (TOP, BOT, RF, RB, LF, LB)
     */
    private void setImageBitmap(ImageView image, String key) {
        if (images == null) return;
        if (images.containsKey(key)) {
            image.setImageBitmap(images.get(key));
        } else {
            setDefaultImage(image);
        }
        // resize image back to base scale
        image.setScaleX(1);
        image.setScaleY(1);
        image.invalidate();
    }

    /**
     * Sets the default image to an ImageView
     *
     * @param imageView ImageView
     */
    private void setDefaultImage(ImageView imageView) {
        imageView.setImageDrawable(getResources().getDrawable(R.drawable.no_images_icon, null));
    }

}
