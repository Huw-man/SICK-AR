package com.example.sickar3;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {
    private static final String LOGTAG = "app_" + MainActivity.class.getSimpleName();

    private ArFragment arFragment;
    private ArSceneView arSceneView;
    private View rootView;
    private ProgressBar progressBar;
    private DataViewModel mDataModel;
    // indicates whether or not a barcode scan is currently happening so the next frame to process is not issued until on barcode scan is done.
    // 0 no process running, 1 process running
    private AtomicInteger live;
    private RecyclerView mBarcodeInfo;
    private InfoListAdapter mAdapter;
    private GestureDetectorCompat mDetector;
    private BarcodeGraphicOverlay mOverlay;
    private ARScene mArScene;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rootView = findViewById(R.id.main_constraint_layout);

        // start gesture listener
        mDetector = new GestureDetectorCompat(this, new MainGestureListener());

        // start ar arFragment
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        arSceneView = arFragment.getArSceneView();
        live = new AtomicInteger(0);
        arSceneView.getScene().addOnUpdateListener(frameTime -> {
            arFragment.onUpdate(frameTime);
            this.onUpdate();
        });
        arSceneView.setOnTouchListener((v, event) -> {
            mDetector.onTouchEvent(event);
            return true;
        });

        mOverlay = new BarcodeGraphicOverlay(this);
        //noinspection ConstantConditions
        ((FrameLayout) arFragment.getView()).addView(mOverlay);

        // hide the plane discovery animation
        arFragment.getPlaneDiscoveryController().hide();
        arFragment.getPlaneDiscoveryController().setInstructionView(null);

        // progressBar
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(ProgressBar.GONE);

        // start ViewModel and attach observers to liveData and errorLiveData
        mDataModel = ViewModelProviders.of(this).get(DataViewModel.class);
        mDataModel.getLiveData().observe(this, this::dataObserver);
        mDataModel.getErrorLiveData().observe(this, this::errorObserver);

        // Initialize RecyclerView
        mBarcodeInfo = findViewById(R.id.recyclerView);
        mBarcodeInfo.setLayoutManager(new LinearLayoutManager(this));
        // set barcodeInfo TextView to maintain information across configuration changes
        mAdapter = setInfoListAdapter(mDataModel, savedInstanceState);
        mBarcodeInfo.setAdapter(mAdapter);

        // attach the itemTouchHelper to recyclerView
        setupItemTouchHelper().attachToRecyclerView(mBarcodeInfo);

        // create ARScene instance for ARCore functionality
        mArScene = new ARScene(this.getApplicationContext(), arSceneView);
    }



    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            int visibility = savedInstanceState.getInt("recyclerViewVisibility");
            if (visibility == RecyclerView.VISIBLE) {
                Utils.animateRecyclerViewVisible(mBarcodeInfo);
            } else { //Gone and invisible
                Utils.animateRecyclerViewGone(mBarcodeInfo);
            }
        }
    }

    /**
     * Save UI state for configuration changes
     *
     * @param outState Bundle for outstate
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("recyclerViewVisibility", mBarcodeInfo.getVisibility());
        outState.putStringArrayList("adapter contents", mAdapter.getItemDataStrings());
    }

    @Override
    protected void onResume() {
        super.onResume();
        live.set(0);
        if (arSceneView == null) {
            return;
        }

        if (arSceneView.getSession() == null) {
            try {
                setupAutoFocus(new Session(this));
                return;
            } catch (Exception e) {
                Utils.displayErrorSnackbar(rootView, "arSession failed to create", e);
            }
        }
        try {
            arSceneView.resume();
        } catch (CameraNotAvailableException e) {
            Utils.displayErrorSnackbar(rootView,
                    "Unable to get Camera", e);
            finish();
            return;
        }
        if (arSceneView.getSession() != null) {
            Utils.displayErrorSnackbar(rootView,
                    "searching for surfaces...", null);
        }
    }

    // TODO: make progressBar continue on configuration change
    @Override
    protected void onPause() {
        super.onPause();
        // pause the background scanning and don't issue requests
        live.set(1);
        if (arSceneView != null) {
            arSceneView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (arSceneView != null) {
            arSceneView.destroy();
        }
    }

    /**
     * Sets the ArFragment to use autofocus with camera.
     *
     * @param arSession ArCore Session for this application
     */
    private void setupAutoFocus(Session arSession) {
        Config arConfig = new Config(arSession);

        if (arConfig.getFocusMode() == Config.FocusMode.FIXED) {
            arConfig.setFocusMode(Config.FocusMode.AUTO);
        }

        arConfig.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);

        arSession.configure(arConfig);

        arSceneView.setupSession(arSession);

        Log.i(LOGTAG, "The camera is current in focus mode " + arConfig.getFocusMode().name());
    }

    /**
     * Retrieves barcode data from viewModel if there is data stored
     * or returns a new InfoListAdapter if there is no data
     *
     * @param dvm DataViewModel
     * @return InfoListAdapter
     */
    private InfoListAdapter setInfoListAdapter(DataViewModel dvm, Bundle savedInstanceState) {
        if (dvm.getLiveData().getValue() != null &&
                !dvm.getLiveData().getValue().isEmpty() &&
                savedInstanceState != null) {
            return new InfoListAdapter(this,
                    getItemListFromStringList(dvm.getLiveData().getValue(),
                            Objects.requireNonNull(
                                    savedInstanceState.getStringArrayList("adapter contents"))));
        } else {
            // initialize data list if no data
            return new InfoListAdapter(this, new ArrayList<>());
        }
    }

    /**
     * Retrieves a list of Item objects from barcodeData given a list of barcode Strings.
     * Helper method for setInfoListAdapter()
     * @param b_data
     * @param barcodes
     * @return
     */
    private ArrayList<Item> getItemListFromStringList(BarcodeData b_data, ArrayList<String> barcodes) {
        ArrayList<Item> itemList = new ArrayList<>();
        for (String bc : barcodes) {
            itemList.add(b_data.get(bc));
        }
        return itemList;
    }

    /**
     * run live barcode detection on frames
     * attached to Scene as OnUpdateListener
     */
    private void onUpdate() {
        if (live.get() == 0) { // no process running so we can issue another one
            live.set(1);
            ArSceneView view = arSceneView;
            // check for valid view (I think problems happen during app startup and the view is not ready yet)
            if (view.getWidth() > 0 && view.getHeight() > 0) {
                //Create bitmap of the sceneview
                final Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                        Bitmap.Config.ARGB_8888);

                final HandlerThread handlerThread = new HandlerThread("PixelCopier");
                handlerThread.start();
                if (view.getHolder().getSurface().isValid()) {
                    PixelCopy.request(view, bitmap, (copyResult -> {
                        if (copyResult == PixelCopy.SUCCESS) {
                            runBarcodeScanner(bitmap);
                        } else {
                            Log.i(LOGTAG, "frame failed to process");
                            live.set(0);
                        }
                        handlerThread.quitSafely();
                    }), new Handler(handlerThread.getLooper()));
                }
                updateOverlay();
            } else {
                live.set(0);
            }
        }
    }

    /**
     * handles the displaying of barcode data upon observing a data update
     *
     * @param barcodeData, data object
     */
    private void dataObserver(BarcodeData barcodeData) {
        Log.i(LOGTAG, "data model changed");
        // check if item card already exists in view, only update for a new item.
//        Log.i(LOGTAG, mAdapter.getItemData().toString());
        if (!barcodeData.isEmpty()) {
//            Log.i(LOGTAG, "inserted new item to recyclerview");
            updateRecyclerView(barcodeData.getLatest());
        }
        progressBar.setVisibility(ProgressBar.GONE);
        live.set(0);
    }

    /**
     * updates the recycler view with an item
     *
     * @param item
     */
    private void updateRecyclerView(Item item) {
        // add latest item to the top of recyclerView
        if (!mAdapter.getItemData().contains(item)) {
            mAdapter.addItem(item);
            mBarcodeInfo.scrollToPosition(0);
        }
    }

    /**
     * Observes an error that is passed through the LiveData model in
     * ViewModel dedicated to errors.
     */
    private void errorObserver(String error) {
        Utils.displayErrorSnackbar(rootView, error, null);
        progressBar.setVisibility(ProgressBar.GONE);
        live.set(0);
    }

    /**
     * Setup for the ItemTouchHelper of recyclerView. Handles the drag, drop,
     * and swipe functionality of the cards.
     *
     * @return
     */
    private ItemTouchHelper setupItemTouchHelper() {
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT |
                        ItemTouchHelper.DOWN | ItemTouchHelper.UP,
                ItemTouchHelper.LEFT) {

            /**
             * Defines the drag and drop functionality.
             *
             * @param recyclerView The RecyclerView that contains the list items
             * @param viewHolder The SportsViewHolder that is being moved
             * @param target The SportsViewHolder that you are switching the
             *               original one with.
             * @return true if the item was moved, false otherwise
             */
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                // get the from and to positions
                int from = viewHolder.getAdapterPosition();
                int to = target.getAdapterPosition();

                // Swap the items and notify the adapter
                Collections.swap(mAdapter.getItemData(), from, to);
                mAdapter.notifyItemMoved(from, to);
                return true;
            }

            /**
             * Defines the swipe to dismiss functionality.
             *
             * @param viewHolder The viewholder being swiped.
             * @param direction The direction it is swiped in.
             */
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int index = viewHolder.getAdapterPosition();
                // detach from AR anchors
                Item item = mAdapter.getItemData().get(index);
                if (item.isPlaced()) {
                    Log.i(LOGTAG, arSceneView.getScene().getChildren().toString());
                    item.detachFromAnchors();
                    try {
                        Log.i(LOGTAG, "item remove update");
                        Log.i(LOGTAG, arSceneView.getScene().getChildren().toString());
                        arSceneView.getSession().update();
                    } catch (CameraNotAvailableException e) {
                        Log.i(LOGTAG, "camera not available on removal of ar item");
                    }
                }

                // Remove the item from the dataset.
                mAdapter.getItemData().remove(index);
                // Notify the adapter.
                mAdapter.notifyItemRemoved(index);
            }
        });
        return helper;
    }

    /**
     * Takes a bitmap image and reads the barcode with FirebaseVisionBarcodeDetector
     * Todo: make this part of ViewModel?
     *
     * @param bitmap bitmap image passed from takePhoto()
     */
    private void runBarcodeScanner(Bitmap bitmap) {
        // configure Barcode scanner
        FirebaseVisionBarcodeDetectorOptions options = new FirebaseVisionBarcodeDetectorOptions.Builder().setBarcodeFormats(
                // detect all barcode formats. improve performance by specifying which barcode formats to detect
                FirebaseVisionBarcode.FORMAT_ALL_FORMATS
//                FirebaseVisionBarcode.FORMAT_CODE_128
        ).build();
        // create FirebaseVisionImage
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        // get an instance of detector
        FirebaseVisionBarcodeDetector detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options);
        // detect barcodes
        Task<List<FirebaseVisionBarcode>> result = detector.detectInImage(image)
                .addOnSuccessListener(barcodes -> {
                    if (barcodes.isEmpty()) {// no barcodes read
//                        progressBar.setVisibility(ProgressBar.GONE);
//                        live.set(0);
                    } else {
                        progressBar.setVisibility(ProgressBar.VISIBLE);
                        // Task completed successfully
                        for (FirebaseVisionBarcode barcode : barcodes) {
                            pushNewBoundingBox(barcode.getBoundingBox());
                            String value = barcode.getDisplayValue();

                            Toast.makeText(getApplicationContext(), value, Toast.LENGTH_SHORT).show();
                            Log.i(LOGTAG, "detected: " + value);

                            Item item = mDataModel.getBarcodeItem(value);
                            if (item != null) {
                                // item was already fetched and cached in barcodeData
                                Log.i(LOGTAG, "item already entered");
                                updateRecyclerView(item);

                                Point[] corners = barcode.getCornerPoints();
                                Point topCenter = Utils.midPoint(corners[0], corners[1]);
                                boolean success = mArScene.tryPlaceARCard(topCenter.x, topCenter.y, item);
                                if (!success) {
                                    errorObserver("unable to attach Anchor or anchor already attached");
                                }
                                progressBar.setVisibility(ProgressBar.GONE);
                            }
                        }
                    }
                    live.set(0);
                })
                .addOnFailureListener(e -> {
                    // Task failed with an exception
                    progressBar.setVisibility(ProgressBar.GONE);
                    Toast.makeText(getApplicationContext(), "Sorry, something went wrong!", Toast.LENGTH_SHORT).show();
                    Log.i(LOGTAG, "barcode not read with exception " + e.getMessage());
                });
    }

    private void updateOverlay() {
        mOverlay.invalidate();
    }

    private void pushNewBoundingBox(Rect rect) {
        mOverlay.drawBoundingBox(rect);
    }

    private class MainGestureListener extends OnSwipeListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        /**
         * The Direction enum will tell you how the user swiped.
         *
         * @param direction direction that user swiped
         */
        @Override
        public boolean onSwipe(Direction direction) {
            switch (direction) {
                case right:
                    if (mBarcodeInfo.getVisibility() == RecyclerView.VISIBLE) {
                        Utils.animateRecyclerViewGone(mBarcodeInfo);
                    }
                    break;
                case left:
                    if (mBarcodeInfo.getVisibility() == RecyclerView.GONE) {
                        Utils.animateRecyclerViewVisible(mBarcodeInfo);
                    }
                    break;
            }
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return super.onSingleTapUp(e);
        }
    }
}
