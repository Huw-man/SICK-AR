package com.example.sickar.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GestureDetectorCompat;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sickar.Constants;
import com.example.sickar.R;
import com.example.sickar.Utils;
import com.example.sickar.image.ImageActivity;
import com.example.sickar.libs.OnSwipeListener;
import com.example.sickar.main.adapters.ItemRecyclerViewAdapter;
import com.example.sickar.main.helpers.ARScene;
import com.example.sickar.main.helpers.BarcodeData;
import com.example.sickar.main.helpers.BarcodeGraphicOverlay;
import com.example.sickar.main.helpers.BarcodeProcessor;
import com.example.sickar.main.helpers.Item;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.DeadlineExceededException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.ResourceExhaustedException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "app_" + MainActivity.class.getSimpleName();

    private ArFragment arFragment;
    private ArSceneView mArSceneView;
    private BarcodeProcessor mBarcodeProcessor;
    private ConstraintLayout mRootView;
    private ProgressBar mProgressBar;
    private DataViewModel mDataModel;
    private RecyclerView mBarcodeInfo;
    private ItemRecyclerViewAdapter mAdapter;
    private GestureDetectorCompat mDetector;
    private BarcodeGraphicOverlay mOverlay;
    private ARScene mArScene;
    private Vibrator mVibrator;

    // handlers to process messages issued from other threads
    private Handler mMainHandler;
    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;

    public static void animateRecyclerViewVisible(RecyclerView view) {
        view.setVisibility(RecyclerView.VISIBLE);
        TranslateAnimation animator = new TranslateAnimation(view.getWidth(), 0, 0, 0);
        animator.setDuration(500);
        view.startAnimation(animator);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) mBarcodeInfo.getLayoutParams();
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            params.matchConstraintPercentWidth = (float) 0.50;
        } else {
            params.matchConstraintPercentWidth = (float) 0.3;

        }
        mBarcodeInfo.setLayoutParams(params);
        configureDisplaySize(newConfig.orientation);
    }

    public static void animateRecyclerViewGone(RecyclerView view) {
        TranslateAnimation animator = new TranslateAnimation(0, view.getWidth(), 0, 0);
        animator.setDuration(500);
        view.startAnimation(animator);
        view.setVisibility(RecyclerView.GONE);
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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mRootView = findViewById(R.id.main_constraint_layout);

        // start gesture listener
        mDetector = new GestureDetectorCompat(this, new MainGestureListener());

        // start ar arFragment
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        assert arFragment != null;
        mArSceneView = arFragment.getArSceneView();
        mArSceneView.getScene().addOnUpdateListener(frameTime -> {
//            arFragment.onUpdate(frameTime);
            this.onUpdate();
        });
        mArSceneView.setOnTouchListener((v, event) -> {
            mDetector.onTouchEvent(event);
            return false;
        });

        // hide the plane discovery animation
        arFragment.getPlaneDiscoveryController().hide();
        arFragment.getPlaneDiscoveryController().setInstructionView(null);
        mArSceneView.getPlaneRenderer().setVisible(false);

        // mProgressBar
        mProgressBar = findViewById(R.id.progressBar);
        mProgressBar.setVisibility(ProgressBar.GONE);

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

        // initialize the handlers
        mMainHandler = setupMainHandler();
        mBackgroundHandler = setupBackgroundHandler();

        // create ARScene instance for ARCore functionality
        mArScene = new ARScene(this, mArSceneView);

        // Vibrator
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // initialize graphic overlay
        mOverlay = new BarcodeGraphicOverlay(this);
        //noinspection ConstantConditions
        ((FrameLayout) arFragment.getView()).addView(mOverlay);

        // initialize the barcode processor
        mBarcodeProcessor = BarcodeProcessor.getInstance();
//        mBarcodeProcessor.setGraphicOverlay(mOverlay);
        mBarcodeProcessor.setMainHandler(mMainHandler);
        mBarcodeProcessor.setBackgroundHandler(mBackgroundHandler);

        FloatingActionButton launch_img = findViewById(R.id.launch_image_activity);
        launch_img.setOnClickListener(v -> {
            Intent imageIntent = new Intent(this, ImageActivity.class);
            this.startActivity(imageIntent);
        });
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            int visibility = savedInstanceState.getInt("recyclerViewVisibility");
            if (visibility == RecyclerView.VISIBLE) {
                animateRecyclerViewVisible(mBarcodeInfo);
            } else { //Gone and invisible
                animateRecyclerViewGone(mBarcodeInfo);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mArSceneView != null) {
            mArSceneView.destroy();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mArSceneView != null) {
            mArSceneView.pause();
        }
        mBarcodeProcessor.stop();
//        mBackgroundHandlerThread.quitSafely();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mArSceneView == null) {
            return;
        }

        if (mArSceneView.getSession() == null) {
            try {
                setupArSession(new Session(this));
            } catch (Exception e) {
                Utils.displayErrorSnackbar(mRootView, "arSession failed to create", e);
            }
        }
        try {
            mArSceneView.resume();
        } catch (CameraNotAvailableException e) {
            Utils.displayErrorSnackbar(mRootView,
                    "Unable to get Camera", e);
            finish();
            return;
        }
        mBarcodeProcessor.start();
        configureDisplaySize(this.getResources().getConfiguration().orientation);
//        if (!mBackgroundHandlerThread.isAlive()) mBackgroundHandlerThread.start();
    }

    /**
     * Sets the ArFragment to use autofocus with camera.
     *
     * @param arSession ArCore Session for this application
     */
    private void setupArSession(Session arSession) {
        Config arConfig = new Config(arSession);

        if (arConfig.getFocusMode() == Config.FocusMode.FIXED) {
            arConfig.setFocusMode(Config.FocusMode.AUTO);
        }
        arConfig.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);

        // set CameraConfig to use the highest resolution available (it is currently 1440x1080)
        // there are three resolutions available from lowest to highest (640x480, 1280x720, 1440x1080)
        arSession.setCameraConfig(arSession.getSupportedCameraConfigs().get(2));
        arSession.configure(arConfig);
        mArSceneView.setupSession(arSession);
        Log.i(TAG, "The camera is current in focus mode " + arConfig.getFocusMode().name());
    }

    /**
     * resets the display parameters for BarcodeProcessor and mOverlay
     */
    private void configureDisplaySize(int orientation) {
        try {
            //noinspection ConstantConditions
            mBarcodeProcessor.setRotation(BarcodeProcessor.getRotationCompensation(
                    mArSceneView.getSession().getCameraConfig().getCameraId(),
                    this,
                    this
            ));
            mOverlay.setCameraSize(mArSceneView.getSession().getCameraConfig().getImageSize(),
                    orientation);
        } catch (CameraAccessException | NullPointerException e) {
            Utils.displayErrorSnackbar(mRootView, "error on configuration change", e);
        }
    }

    /**
     * Configures the main Handler to receive messages from barcode processes to update
     * Views.
     *
     * @return Handler
     */
    private Handler setupMainHandler() {
        return new Handler(Looper.getMainLooper()) {
            /**
             * Handle messages sent from the BarcodeProcessRunnable upon completion
             * of barcode detection on a single frame.
             *
             * @param msg new message to process
             */
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case Constants.BARCODE_READ_SUCCESS:
                        Bundle data = msg.getData();
                        Rect box = data.getParcelable("boundingBox");
                        String value = data.getString("value");

                        mOverlay.drawBoundingBox(box);
                        mOverlay.invalidate();

                        // get item if it is already in the cache
                        Item item = mDataModel.getBarcodeItem(value);
                        if (item != null && (!item.isScanned() || !item.isPlaced())) {
                            // item was already fetched and cached in barcodeData
                            Point[] corners = (Point[]) data.getParcelableArray("cornerPoints");
                            if (corners != null) {
                                Point topCenter = Utils.midPoint(corners[0], corners[1]);
                                boolean success = mArScene.tryPlaceARCard(topCenter.x, topCenter.y, item);
                                if (success) {
                                    Utils.vibrate2(mVibrator);
                                } else {
                                    Utils.displayErrorSnackbar(mRootView, "unable to attach Anchor", null);
                                }
                            }
                        } else if (item == null && !mDataModel.requestPending(value)) {
                            // first time requesting this item so network request is issued
                            mDataModel.fetchBarcodeData(value);
                            mProgressBar.setVisibility(ProgressBar.VISIBLE);
                            Log.i(TAG, "network request issued for " + value);
                            Utils.vibrate(mVibrator, 300);
                        }
                        break;
                    case Constants.BARCODE_READ_EMPTY:
                        mOverlay.clear();
                        mOverlay.invalidate();
                        break;
                    case Constants.BARCODE_READ_FAILURE:
                        String errMsg = msg.getData().getString("error reading barcodes");
                        Utils.displayErrorSnackbar(mOverlay.getRootView(), errMsg, null);
                        break;
                    case Constants.REQUEST_ISSUED:
                        mProgressBar.setVisibility(ProgressBar.VISIBLE);
                        break;
                    case Constants.REQUEST_PENDING:
                        mProgressBar.setVisibility(ProgressBar.VISIBLE);
                        break;
                }
            }
        };
    }

    /**
     * Configures the background handler and background thread to process barcodes
     *
     * @return Handler
     */
    private Handler setupBackgroundHandler() {
        HandlerThread backgroundHandlerThread = new HandlerThread("background");
        mBackgroundHandlerThread = backgroundHandlerThread;
        backgroundHandlerThread.start();
        return new Handler(backgroundHandlerThread.getLooper()) {
            /**
             * Receive messages from Barcode Processor to lookup barcodes,
             * issue network requests, and place AR elements
             *
             * @param msg message to handle
             */
            @Override
            public void handleMessage(Message msg) {
                // TODO: do barcode filtering here
                if (msg.what == Constants.BARCODE_READ_SUCCESS) {
                    Bundle data = msg.getData();
                    String value = data.getString("value");
                    Item item = mDataModel.getBarcodeItem(value);
                    if (item != null && (!item.isScanned() || !item.isPlaced()) && item.getName() != null) {
                        // item was already fetched and cached in barcodeData
                        Point[] corners = (Point[]) data.getParcelableArray("cornerPoints");
                        if (corners != null) {
                            Point topCenter = Utils.midPoint(corners[0], corners[1]);
                            boolean success = mArScene.tryPlaceARCard(topCenter.x, topCenter.y, item);
                            if (success) {
                                Utils.vibrate2(mVibrator);
                            } else {
                                Utils.displayErrorSnackbar(mRootView, "unable to attach Anchor", null);
                            }
                        }
                    } else if (item != null && item.getName() == null) {
                        // there is a current request for this item that is pending
                        Message msgOut = mMainHandler.obtainMessage(Constants.REQUEST_PENDING);
                        msgOut.sendToTarget();

//                        Log.i(TAG, "network request pending" + value);
                    } else if (item == null) {
                        // first time requesting this item and a network request was issued
                        Message msgOut = mMainHandler.obtainMessage(Constants.REQUEST_ISSUED);
                        msgOut.sendToTarget();

                        Log.i(TAG, "network request issued for " + value);
                        Utils.vibrate(mVibrator, 300);
                    }
                }
            }
        };
    }

    /**
     * Retrieves barcode data from viewModel if there is data stored
     * or returns a new ItemRecyclerViewAdapter if there is no data
     *
     * @param dvm DataViewModel
     * @return ItemRecyclerViewAdapter
     */
    private ItemRecyclerViewAdapter setInfoListAdapter(DataViewModel dvm, Bundle savedInstanceState) {
        if (dvm.getLiveData().getValue() != null &&
                !dvm.getLiveData().getValue().isEmpty() &&
                savedInstanceState != null) {
            return new ItemRecyclerViewAdapter(this,
                    getItemListFromStringList(dvm.getLiveData().getValue(),
                            Objects.requireNonNull(
                                    savedInstanceState.getStringArrayList("adapter contents"))));
        } else {
            // initialize data list if no data
            return new ItemRecyclerViewAdapter(this, new ArrayList<>());
        }
    }

    /**
     * Retrieves a list of Item objects from barcodeData given a list of barcode Strings.
     * Helper method for setInfoListAdapter()
     *
     * @param b_data BarcodeData object
     * @param barcodes List of barcodes to get from the data object
     * @return List of Items corresponding to the barcodes passed in
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
        try {
            //noinspection ConstantConditions
            Image frameImage = mArSceneView.getArFrame().acquireCameraImage();
            mBarcodeProcessor.pushFrame(frameImage);
        } catch (NullPointerException | DeadlineExceededException |
                ResourceExhaustedException | NotYetAvailableException e) {
            if (e instanceof NotYetAvailableException) {
                Utils.displayErrorSnackbar(mRootView, "Waiting for Camera", null);
            } else {
                Utils.displayErrorSnackbar(mRootView, "On retrieving frame", e);
            }
        }
    }

    /**
     * handles the displaying of barcode data upon observing a data update
     *
     * @param barcodeData, data object
     */
    private void dataObserver(BarcodeData barcodeData) {
        // check if item card already exists in view, only update for a new item.
        if (!barcodeData.isEmpty()) {
            updateRecyclerView(barcodeData.getLatest());
        }
        mProgressBar.setVisibility(ProgressBar.GONE);
    }

    /**
     * updates the recycler view with an item and
     * checks if the item is already in the recyclerView before adding it
     *
     * @param item item to update the recyclerView with
     */
    private void updateRecyclerView(Item item) {
        // add latest item to the top of recyclerView
        if (!mAdapter.getItemData().contains(item)) {
            Log.i(TAG, "inserted new item to recyclerView");
            mAdapter.addItem(item);
            mBarcodeInfo.scrollToPosition(0);
        }
    }

    /**
     * Observes an error that is passed through the LiveData model in
     * ViewModel dedicated to errors.
     */
    private void errorObserver(String error) {
        Utils.displayErrorSnackbar(mRootView, error, null);
        mProgressBar.setVisibility(ProgressBar.GONE);
    }

    /**
     * Setup for the ItemTouchHelper of recyclerView. Handles the drag, drop,
     * and swipe functionality of the cards.
     *
     * @return ItemTouchHelper helper for touch gestures on recyclerView
     */
    private ItemTouchHelper setupItemTouchHelper() {
        return new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT |
                        ItemTouchHelper.DOWN | ItemTouchHelper.UP,
                0) {

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

                Item item = mAdapter.getItemData().get(index);
                item.setScanned(false); // removed from display
                if (item.isPlaced()) {
                    // clear the AR display
                    // detach from AR anchors
//                    Log.i(TAG, mArSceneView.getScene().getChildren().toString());
                    item.detachFromAnchors();
                    try {
//                        Log.i(TAG, "item remove update");
//                        Log.i(TAG, mArSceneView.getScene().getChildren().toString());
                        Objects.requireNonNull(mArSceneView.getSession()).update();
                    } catch (CameraNotAvailableException e) {
                        Log.i(TAG, "camera not available on removal of ar item");
                    }
                }
                // clear the switch reference
                item.setVisibleToggleReference(null);

                // Remove the item from the recyclerView adapter.
                // Note the item remains in the barcodeData cache
                mAdapter.getItemData().remove(index);
                // Notify the adapter.
                mAdapter.notifyItemRemoved(index);
            }
        });
    }

    public DataViewModel getViewModel() {
        return mDataModel;
    }

    public View getRootView() {
        return mRootView;
    }

    private class MainGestureListener extends OnSwipeListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return false;
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
                        animateRecyclerViewGone(mBarcodeInfo);
                    }
                    break;
                case left:
                    if (mBarcodeInfo.getVisibility() == RecyclerView.GONE) {
                        animateRecyclerViewVisible(mBarcodeInfo);
                    }
                    break;
            }
            return false;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return super.onSingleTapUp(e);
        }
    }

}
