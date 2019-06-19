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
import android.widget.TextView;
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
import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {
    private static final String LOGTAG = "app_" + MainActivity.class.getSimpleName();
    private ArFragment arFragment;
    protected Session arSession;
    private ProgressBar progressBar;
    private DataViewModel mDataModel;
    // indicates whether or not a barcode scan is currently happening so the next frame to process is not issued until on barcode scan is done.
    // 0 no process running, 1 process running
    private AtomicInteger live;
    private RecyclerView mBarcodeInfo;
    private InfoListAdapter mAdapter;
    private GestureDetectorCompat mDetector;
    private BarcodeGraphicOverlay mOverlay;

    // renderable for AR card
    private ViewRenderable itemInfoRenderable;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // start gesture listener
        mDetector = new GestureDetectorCompat(this, new MainGestureListener());

        // start ar arFragment
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        live = new AtomicInteger(0);
        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdate);
        arFragment.getArSceneView().setOnTouchListener((v, event) -> {
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

        // load ViewRenderable for AR Core
        ViewRenderable.builder().setView(this, R.layout.ar_item_card).build()
        .thenAccept(viewRenderable -> itemInfoRenderable = viewRenderable);
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
        if (arSession == null) {
            try {
                arSession = new Session(this);
                setupAutoFocus(arSession);
            } catch (Exception e) {
                Log.e(LOGTAG, "arSession failed to create " + e.getMessage());
            }
        }
    }

    // TODO: make progressBar continue on configuration change
    @Override
    protected void onPause() {
        super.onPause();
        // pause the background scanning and don't issue requests
        live.set(1);
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

        arFragment.getArSceneView().setupSession(arSession);

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
    private void onUpdate(FrameTime frameTime) {
        if (live.get() == 0) { // no process running so we can issue another one
            live.set(1);
            ArSceneView view = arFragment.getArSceneView();
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
        Snackbar.make(findViewById(R.id.main_constraint_layout), error, Snackbar.LENGTH_LONG).show();
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
                // detach from AR anchors
                Item item = mAdapter.getItemData().get(viewHolder.getAdapterPosition());
                item.detachFromAnchors();
                item.setPlaced(false);

                // Remove the item from the dataset.
                mAdapter.getItemData().remove(viewHolder.getAdapterPosition());
                // Notify the adapter.
                mAdapter.notifyItemRemoved(viewHolder.getAdapterPosition());


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
//                                mDataModel.putBarcodeItem(value, item);
                                Log.i(LOGTAG, "item already entered");
                                updateRecyclerView(item);

                                Point[] corners = barcode.getCornerPoints();
                                Point topCenter = Utils.midPoint(corners[0], corners[1]);
                                boolean success = tryPlaceArCard(topCenter.x, topCenter.y, arFragment.getArSceneView().getArFrame(), item);
                                if (!success) {
                                    errorObserver("unable to attach Anchor or anchor already attached");
                                }
                                progressBar.setVisibility(ProgressBar.GONE);
//                                live.set(0);
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
                        animateRecyclerViewGone(mBarcodeInfo);
                    }
                    break;
                case left:
                    if (mBarcodeInfo.getVisibility() == RecyclerView.GONE) {
                        animateRecyclerViewVisible(mBarcodeInfo);
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

    private void animateRecyclerViewVisible(RecyclerView view) {
        view.setVisibility(RecyclerView.VISIBLE);
        TranslateAnimation animator = new TranslateAnimation(view.getWidth(), 0, 0, 0);
        animator.setDuration(500);
        view.startAnimation(animator);
    }

    private void animateRecyclerViewGone(RecyclerView view) {
        TranslateAnimation animator = new TranslateAnimation(0, view.getWidth(), 0, 0);
        animator.setDuration(500);
        view.startAnimation(animator);
        view.setVisibility(RecyclerView.GONE);
    }

    /**
     * Try and place the AR card for an item at a point on screen.
     * Only places one card per item.
     *
     * @param xPx x pixel coordinate
     * @param yPx y pixel coordinate
     * @param frame ArFrame
     * @return true if successful false otherwise
     */
    private boolean tryPlaceArCard(float xPx, float yPx, Frame frame, Item item) {
        if (!item.isPlaced() && frame.getCamera().getTrackingState() == TrackingState.TRACKING) {
            List<HitResult> hitList = frame.hitTest(xPx, yPx);
            if (!hitList.isEmpty()) {
                HitResult firstHit = hitList.get(0);
                Log.i(LOGTAG, "placing anchor");
                // create Anchor
                Anchor anchor = firstHit.createAnchor();
                AnchorNode anchorNode = new AnchorNode(anchor);
                anchorNode.setParent(arFragment.getArSceneView().getScene());
                Node base = createNode(item);
                anchorNode.addChild(base);

                // notify that item has been placed
                item.setPlaced(true);
                item.setAnchorAndAnchorNode(anchor, anchorNode);
                return true;
            }
        }
        return false;
    }

    /**
     * Create the AR scene to be placed
     * @return base Node
     */
    private Node createNode(Item item) {
        Node base = new Node();

        Node card = new Node();
        card.setParent(base);
        card.setRenderable(itemInfoRenderable);
        card.setLocalPosition(new Vector3(0.0f, 0.1f, 0.0f));
        View cardView = itemInfoRenderable.getView();

        // set text
        TextView name = cardView.findViewById(R.id.item_name);
        name.setText(item.getName());
        TextView body = cardView.findViewById(R.id.item_body);
        body.setText(item.getPropsForARCard());

        return base;
    }
}
