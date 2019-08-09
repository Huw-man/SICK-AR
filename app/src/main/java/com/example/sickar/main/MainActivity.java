package com.example.sickar.main;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

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
import com.example.sickar.libs.OnSwipeListener;
import com.example.sickar.main.adapters.ItemRecyclerViewAdapter;
import com.example.sickar.main.helpers.ARScene;
import com.example.sickar.main.helpers.BarcodeDataCache;
import com.example.sickar.main.helpers.BarcodeProcessor;
import com.example.sickar.main.helpers.GraphicOverlay;
import com.example.sickar.main.helpers.Item;
import com.example.sickar.main.helpers.ItemTouchHelperCallback;
import com.example.sickar.tutorial.TutorialActivity;
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
import java.util.Arrays;
import java.util.Objects;

/**
 * Main Activity for this application.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "app_" + MainActivity.class.getSimpleName();
    private static final String TUTORIAL_KEY = "first_time";

    private ArSceneView arSceneView;
    private BarcodeProcessor barcodeProcessor;
    private ConstraintLayout rootView;
    private ProgressBar progressBar;
    private DataViewModel viewModel;
    private RecyclerView recyclerView;
    private ItemRecyclerViewAdapter recyclerViewAdapter;
    private ItemTouchHelper itemTouchHelper;
    private GestureDetectorCompat mainGestureDetector;
    private GraphicOverlay graphicOverlay;
    private ARScene arScene;
    private Vibrator vibrator;
    private AnimatorListenerAdapter reticleAnimateListener;
    private PointF center;
    private FloatingActionButton clicker;

    // handlers to process messages issued from other threads
    private Handler mainHandler;
    private HandlerThread backgroundHandlerThread;
    private Handler backgroundHandler;

    /**
     * Called on creation of this Activity
     *
     * @param savedInstanceState instance data saved from the precious instance if present
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        rootView = findViewById(R.id.main_constraint_layout);

        // start gesture listener
        mainGestureDetector = new GestureDetectorCompat(this, new MainGestureListener());

        // start ar arFragment
        ArFragment arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        assert arFragment != null;
        arSceneView = arFragment.getArSceneView();
        arSceneView.getScene().addOnUpdateListener(frameTime -> {
//            arFragment.onUpdate(frameTime);
            this.onUpdate();
        });
        arSceneView.setOnTouchListener((vw, ev) -> {
//            Log.i(TAG,", " + ev.getAction());
            mainGestureDetector.onTouchEvent(ev);
            return false;
        });

        // hide the plane discovery animation
        arFragment.getPlaneDiscoveryController().hide();
        arFragment.getPlaneDiscoveryController().setInstructionView(null);
        arSceneView.getPlaneRenderer().setVisible(false);

        // progressBar
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(ProgressBar.GONE);

        // start ViewModel and attach observers to liveData and errorLiveData
        viewModel = ViewModelProviders.of(this).get(DataViewModel.class);
        viewModel.getCacheData().observe(this, this::dataObserver);
        viewModel.getErrorLiveData().observe(this, this::errorObserver);
        viewModel.getCurrentRequestsData().observe(this, currentRequests -> {
            Log.i(TAG, "requests " + Arrays.toString(currentRequests.toArray()));
            if (currentRequests.isEmpty()) {
                progressBar.setVisibility(ProgressBar.GONE);
            }
        });

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        /*
        Initialize custom ItemTouchHelperCallback.
        The recyclerView adapter must be passed into ItemTouchHelperCallback so the appropriate
        items can be manipulated in the backing adapter for drag&drop and swipe-to-dismiss
        functionality to work properly.
        Call ItemTouchHelperCallback.setAdapter(itemRecyclerViewAdapter)
        */
        ItemTouchHelperCallback itemTouchHelperCallback = new ItemTouchHelperCallback();
        // ItemTouchHelper is passed into recyclerViewAdapter for custom swipe functionality
        // declare this before setRecyclerViewAdapter
        itemTouchHelper = new ItemTouchHelper(itemTouchHelperCallback);
        // set recyclerView to maintain information across configuration changes
        recyclerViewAdapter = setRecyclerViewAdapter(viewModel, savedInstanceState);
        recyclerView.setAdapter(recyclerViewAdapter);
        // attach the itemTouchHelper to recyclerView
        itemTouchHelperCallback.setAdapter(recyclerViewAdapter);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        // initialize the handlers
        mainHandler = setupMainHandler();
        backgroundHandler = setupBackgroundHandler();

        // create ARScene instance for ARCore functionality
        arScene = new ARScene(this, arFragment);

        // Vibrator
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // initialize graphic overlay
        graphicOverlay = new GraphicOverlay(this);
        //noinspection ConstantConditions
        ((FrameLayout) arFragment.getView()).addView(graphicOverlay);

        // initialize the barcode processor
        barcodeProcessor = BarcodeProcessor.getInstance();
        barcodeProcessor.setMainHandler(mainHandler);
        barcodeProcessor.setBackgroundHandler(backgroundHandler);

        // reticle setup
        center = new PointF();
        reticleAnimateListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                dispatchTouchEventToArSceneView(MotionEvent.ACTION_UP);
            }

            @Override
            public void onAnimationStart(Animator animation) {
                dispatchTouchEventToArSceneView(MotionEvent.ACTION_DOWN);
            }
        };
        graphicOverlay.setAnimatorListenerAdapter(reticleAnimateListener);

        clicker = findViewById(R.id.floatingActionButton);
        clicker.setOnClickListener(v -> graphicOverlay.startClickAnimation());
//        clicker.setOnTouchListener((v, ev)-> {
//            switch (ev.getAction()) {
//                case MotionEvent.ACTION_DOWN:
//                    dispatchTouchEventToArSceneView(MotionEvent.ACTION_DOWN);
//                case MotionEvent.ACTION_MOVE:
//                    dispatchTouchEventToArSceneView(MotionEvent.ACTION_MOVE);
//                case MotionEvent.ACTION_UP:
//                    dispatchTouchEventToArSceneView(MotionEvent.ACTION_UP);
//            }
//            Log.i(TAG, ev.getAction() +" ");
//            return false;
//        });
        clicker.hide();

        arSceneView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            center.x = (right - left) / 2f;
            center.y = (bottom - top) / 2f;
        });

        boolean firstTime = getPreferences(MODE_PRIVATE).getBoolean(TUTORIAL_KEY, true);
        if (firstTime) {
            launchTutorial();
            getPreferences(MODE_PRIVATE).edit().putBoolean(TUTORIAL_KEY, false).apply();
        }
    }

    /**
     * Called on configuration change. i.e when the screen rotates
     *
     * @param newConfig configuration
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) recyclerView.getLayoutParams();
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            params.matchConstraintPercentWidth = (float) 0.50;
        } else {
            params.matchConstraintPercentWidth = (float) 0.3;

        }
        recyclerView.setLayoutParams(params);
        configureDisplaySize(newConfig.orientation);
    }

    /**
     * Initialize the contents of the Activity's standard options menu.  You
     * should place your menu items in to <var>menu</var>.
     *
     * <p>This is only called once, the first time the options menu is
     * displayed.  To update the menu every time it is displayed, see
     * {@link #onPrepareOptionsMenu}.
     *
     * <p>The default implementation populates the menu with standard system
     * menu items.  These are placed in the {@link Menu#CATEGORY_SYSTEM} group so that
     * they will be correctly ordered with application-defined menu items.
     * Deriving classes should always call through to the base implementation.
     *
     * <p>You can safely hold on to <var>menu</var> (and any items created
     * from it), making modifications to it as desired, until the next
     * time onCreateOptionsMenu() is called.
     *
     * <p>When you add items to the menu, you can implement the Activity's
     * {@link #onOptionsItemSelected} method to handle them there.
     *
     * @param menu The options menu in which you place your items.
     * @return You must return true for the menu to be displayed;
     * if you return false it will not be shown.
     * @see #onPrepareOptionsMenu
     * @see #onOptionsItemSelected
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     * The default implementation simply returns false to have the normal
     * processing happen (calling the item's Runnable or sending a message to
     * its Handler as appropriate).  You can use this method for any items
     * for which you would like to do processing without those other
     * facilities.
     *
     * <p>Derived classes should call through to the base class for it to
     * perform the default menu handling.</p>
     *
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to
     * proceed, true to consume it here.
     * @see #onCreateOptionsMenu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reticle_enable:
                boolean newState = !graphicOverlay.getReticleEnabled();
                graphicOverlay.setReticleEnabled(newState);
                if (newState) {
                    clicker.show();
                } else {
                    clicker.hide();
                }
                return true;
            case R.id.tutorial_button:
                launchTutorial();
                return true;
            case R.id.clear_shared_preferences:
                getPreferences(MODE_PRIVATE).edit().clear().apply();
                return true;
            case R.id.clear_data_cache:
                BarcodeDataCache.getInstance().clear();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Called right before the activity if displayed and "live"
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (arSceneView == null) {
            return;
        }
        if (arSceneView.getSession() == null) {
            try {
                setupArSession(new Session(this));
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
        barcodeProcessor.start();
        configureDisplaySize(this.getResources().getConfiguration().orientation);
        if (!backgroundHandlerThread.isAlive()) backgroundHandlerThread.start();
        if (getSupportActionBar() != null) getSupportActionBar().show();
        mainHandler.postDelayed(() -> {
            // hide appbar after some time
            if (getSupportActionBar() != null) getSupportActionBar().hide();
        }, 3000);
    }

    /**
     * Called when the activity if paused
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (arSceneView != null) {
            arSceneView.pause();
        }
        barcodeProcessor.stop();
    }

    /**
     * Called when the activity is destroyed
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (arSceneView != null) {
            arSceneView.destroy();
        }
        backgroundHandlerThread.quitSafely();
    }

    /**
     * Save UI state for configuration changes
     *
     * @param outState Bundle for outstate
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("recyclerViewVisibility", recyclerView.getVisibility());
        outState.putStringArrayList("adapter contents", recyclerViewAdapter.getItemDataStrings());
    }

    /**
     * Restore activity data from previous instance data from savedInstanceState
     *
     * @param savedInstanceState savedInstanceState
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            int visibility = savedInstanceState.getInt("recyclerViewVisibility");
            if (visibility == RecyclerView.VISIBLE) {
                animateRecyclerViewVisible(recyclerView);
            } else { //Gone and invisible
                animateRecyclerViewGone(recyclerView);
            }
        }
    }

    /**
     * Get the ViewModel
     *
     * @return ViewModel
     */
    public DataViewModel getViewModel() {
        return viewModel;
    }

    /**
     * Get the root View
     *
     * @return view
     */
    public View getRootView() {
        return rootView;
    }

    /**
     * Launch the tutorial activity
     */
    private void launchTutorial() {
        Intent tutorialIntent = new Intent(this, TutorialActivity.class);
        this.startActivity(tutorialIntent);
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
        arSceneView.setupSession(arSession);
        Log.i(TAG, "The camera is current in focus mode " + arConfig.getFocusMode().name());
    }

    /**
     * resets the display parameters for BarcodeProcessor and graphicOverlay
     */
    private void configureDisplaySize(int orientation) {
        try {
            //noinspection ConstantConditions
            barcodeProcessor.setRotation(BarcodeProcessor.getRotationCompensation(
                    arSceneView.getSession().getCameraConfig().getCameraId(),
                    this,
                    this
            ));
            graphicOverlay.setCameraSize(arSceneView.getSession().getCameraConfig().getImageSize(),
                    orientation);
        } catch (CameraAccessException | NullPointerException e) {
            Utils.displayErrorSnackbar(rootView, "error on configuration change", e);
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

                        graphicOverlay.drawBoundingBox(box);
                        graphicOverlay.invalidate();

                        // get item if it is already in the cache
                        Item item = viewModel.getBarcodeItem(value);
                        if (item != null) {
                            // make sure both these conditions are checked
                            // that's why there is not an else if here
                            if (!item.isScanned()) {
                                Utils.vibrate(vibrator, 300);
                                updateRecyclerView(item);
                            }
                            if (!item.isPlaced()) {
                                // item was already fetched and cached in barcodeData
                                Point[] corners = (Point[]) data.getParcelableArray("cornerPoints");
                                if (corners != null) {
                                    Point topCenter = Utils.midPoint(corners[0], corners[1]);
                                    boolean success = arScene.tryPlaceARCard(topCenter.x, topCenter.y, item);
                                    if (success) {
                                        Utils.vibrate2(vibrator);
                                    } else {
                                        Utils.displayErrorSnackbar(rootView, "unable to attach Anchor", null);
                                    }
                                }
                            }
                        } else if (!viewModel.requestPending(value)) {
                            // first time requesting this item so network request is issued
                            viewModel.fetchBarcodeData(value);
                            progressBar.setVisibility(ProgressBar.VISIBLE);
                            Log.i(TAG, "network request issued for " + value);
                            Utils.vibrate(vibrator, 300);
                        }
                        break;
                    case Constants.BARCODE_READ_EMPTY:
                        graphicOverlay.clear();
                        graphicOverlay.invalidate();
                        break;
                    case Constants.BARCODE_READ_FAILURE:
                        String errMsg = msg.getData().getString("error reading barcodes");
                        Utils.displayErrorSnackbar(graphicOverlay.getRootView(), errMsg, null);
                        break;
                    case Constants.REQUEST_ISSUED:
                        progressBar.setVisibility(ProgressBar.VISIBLE);
                        break;
                    case Constants.REQUEST_PENDING:
                        progressBar.setVisibility(ProgressBar.VISIBLE);
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
        this.backgroundHandlerThread = backgroundHandlerThread;
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
                    Item item = viewModel.getBarcodeItem(value);
                    if (item != null && (!item.isScanned() || !item.isPlaced()) && item.getName() != null) {
                        // item was already fetched and cached in barcodeData
                        Point[] corners = (Point[]) data.getParcelableArray("cornerPoints");
//                        Log.i(TAG, "corners" + corners.toString());
                        if (corners != null) {
                            Point topCenter = Utils.midPoint(corners[0], corners[1]);
                            boolean success = arScene.tryPlaceARCard(topCenter.x, topCenter.y, item);
                            if (success) {
                                Utils.vibrate2(vibrator);
                            } else {
                                Utils.displayErrorSnackbar(rootView, "unable to attach Anchor", null);
                            }
                        }
                    } else if (item != null && item.getName() == null) {
                        // there is a current request for this item that is pending
                        Message msgOut = mainHandler.obtainMessage(Constants.REQUEST_PENDING);
                        msgOut.sendToTarget();

//                        Log.i(TAG, "network request pending" + value);
                    } else if (item == null) {
                        // first time requesting this item and a network request was issued
                        Message msgOut = mainHandler.obtainMessage(Constants.REQUEST_ISSUED);
                        msgOut.sendToTarget();

                        Log.i(TAG, "network request issued for " + value);
                        Utils.vibrate(vibrator, 300);
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
    private ItemRecyclerViewAdapter setRecyclerViewAdapter(DataViewModel dvm, Bundle savedInstanceState) {
        if (dvm.getCacheData().getValue() != null &&
                !dvm.getCacheData().getValue().isEmpty() &&
                savedInstanceState != null) {
            return new ItemRecyclerViewAdapter(this,
                    getItemListFromStringList(dvm.getCacheData().getValue(),
                            Objects.requireNonNull(
                                    savedInstanceState.getStringArrayList("adapter contents"))),
                    itemTouchHelper);
        } else {
            // initialize data list if no data
            return new ItemRecyclerViewAdapter(this, new ArrayList<>(), itemTouchHelper);
        }
    }

    /**
     * Retrieves a list of Item objects from barcodeData given a list of barcode Strings.
     * Helper method for setRecyclerViewAdapter()
     *
     * @param b_data   BarcodeDataCache object
     * @param barcodes List of barcodes to get from the data object
     * @return List of Items corresponding to the barcodes passed in
     */
    private ArrayList<Item> getItemListFromStringList(BarcodeDataCache b_data, ArrayList<String> barcodes) {
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
            Image frameImage = arSceneView.getArFrame().acquireCameraImage();
            barcodeProcessor.pushFrame(frameImage);

//            List<HitResult> hits = arSceneView.getArFrame().hitTest(center.x, center.y);
//            if (!hits.isEmpty()) {
//                if ((hits.get(0).getTrackable() instanceof Plane || hits.get(0).getTrackable() instanceof Point) &&
//                    graphicOverlay.isAnimating()) {
////                    Log.i(TAG, " " + hits.get(0).getTrackable().getClass());
//                    graphicOverlay.startClickAnimation();
//                }
//            } else {
//                graphicOverlay.stopClickAnimation();
//            }
        } catch (NullPointerException | DeadlineExceededException |
                ResourceExhaustedException | NotYetAvailableException e) {
            if (e instanceof NotYetAvailableException) {
                Utils.displayErrorSnackbar(rootView, "Waiting for Camera", null);
            } else {
                Utils.displayErrorSnackbar(rootView, "On retrieving frame", e);
            }
        }
    }

    /**
     * handles the displaying of barcode data upon observing a data update
     *
     * @param barcodeDataCache data object
     */
    private void dataObserver(BarcodeDataCache barcodeDataCache) {
        // check if item card already exists in view, only update for a new item.
        if (!barcodeDataCache.isEmpty()) {
            updateRecyclerView(barcodeDataCache.getLatest());
        }
//        progressBar.setVisibility(ProgressBar.GONE);
    }

    /**
     * updates the recycler view with an item and
     * checks if the item is already in the recyclerView before adding it
     *
     * @param item item to update the recyclerView with
     */
    private void updateRecyclerView(Item item) {
        // add latest item to the top of recyclerView
        if (!recyclerViewAdapter.getItemData().contains(item)) {
            Log.i(TAG, "inserted new item to recyclerView");
            recyclerViewAdapter.addItem(item);
            recyclerView.scrollToPosition(0);
            Toast.makeText(this, "scan again to anchor AR elements", Toast.LENGTH_SHORT).show();
        } else {
            // if item already exists remove it and update it
            recyclerViewAdapter.updateItem(item);
        }
    }

    /**
     * Observes an error that is passed through the LiveData dedicated to errors in
     * ViewModel
     */
    private void errorObserver(String error) {
        Utils.displayErrorSnackbar(rootView, error, null);
//        progressBar.setVisibility(ProgressBar.GONE);
    }

    /**
     * animate the recycler view when it appears
     *
     * @param view recyclerView
     */
    private void animateRecyclerViewVisible(RecyclerView view) {
        view.setVisibility(RecyclerView.VISIBLE);
        TranslateAnimation animator = new TranslateAnimation(view.getWidth(), 0, 0, 0);
        animator.setDuration(500);
        view.startAnimation(animator);
    }

    /**
     * animate the recycler view when it disappears
     *
     * @param view recyclerView
     */
    private void animateRecyclerViewGone(RecyclerView view) {
        TranslateAnimation animator = new TranslateAnimation(0, view.getWidth(), 0, 0);
        animator.setDuration(500);
        view.startAnimation(animator);
        view.setVisibility(RecyclerView.GONE);
    }

    /**
     * Handles gestures on the main display. In this case the main display is the arSceneView
     */
    private class MainGestureListener extends OnSwipeListener {
        /**
         * The Direction enum will tell you how the user swiped.
         *
         * @param direction direction that user swiped
         */
        @Override
        public boolean onSwipe(Direction direction) {
            switch (direction) {
                case right:
                    if (recyclerView.getVisibility() == RecyclerView.VISIBLE) {
                        animateRecyclerViewGone(recyclerView);
                    }
                    break;
                case left:
                    if (recyclerView.getVisibility() == RecyclerView.GONE) {
                        animateRecyclerViewVisible(recyclerView);
                    }
                    break;
                case up:
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().hide();
                    }
                    break;
                case down:
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().show();
                    }
                    break;
            }
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }
    }

    /**
     * Helper method for dispatching a simulated touch event at the center of the screen. Used for
     * the reticle tool.
     *
     * @param action action to be simulated (i.e. MotionEvent.DOWN)
     */
    private void dispatchTouchEventToArSceneView(int action) {
        long downtime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + 100;
        int metaState = 0;
        arSceneView.dispatchTouchEvent(
                MotionEvent.obtain(downtime,
                        eventTime,
                        action,
                        center.x, center.y,
                        metaState)
        );
    }
}
