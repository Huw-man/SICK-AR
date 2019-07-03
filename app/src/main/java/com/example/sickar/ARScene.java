package com.example.sickar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.DpToMetersViewSizer;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class ARScene {
    private static final String TAG = "app_" + ARScene.class.getSimpleName();

    private ArSceneView mArSceneView;
    private Context mContext;
    private DpToMetersViewSizer mViewSizer;
    private MainActivity mainActivity;

    public ARScene(Context context, ArSceneView arSceneView) {
        this.mContext = context;
        mainActivity = (MainActivity) context;
        mArSceneView = arSceneView;
        mViewSizer = new DpToMetersViewSizer(1000);
    }

    /**
     * Try and place the AR card for an item at a point on screen.
     * Only places one card per item.
     *
     * @param xPx x pixel coordinate
     * @param yPx y pixel coordinate
     * @return true if successful false otherwise
     */
    public boolean tryPlaceARCard(float xPx, float yPx, Item item) {
        if (!item.isPlaced() && mArSceneView.getArFrame() != null &&
                mArSceneView.getArFrame().getCamera().getTrackingState() == TrackingState.TRACKING) {
            List<HitResult> hitList = mArSceneView.getArFrame().hitTest(xPx, yPx);
            if (!hitList.isEmpty()) {
                HitResult firstHit = hitList.get(0);
                Log.i(TAG, "placing anchor for " + item.getName());
                // create Anchor
                Anchor anchor = firstHit.createAnchor();
                AnchorNode anchorNode = new AnchorNode(anchor);
                anchorNode.setParent(mArSceneView.getScene());
                Node base = createNode(item);
                anchorNode.addChild(base);

                // notify that item has been placed
                item.setAnchorAndAnchorNode(anchor, anchorNode);
                return true;
            }
        }
        return false;
    }

    /**
     * Create the AR scene to be placed
     *
     * @return base Node
     */
    private Node createNode(Item item) {
        Node base = new Node();
        Node mainDisplay = new Node();
        Node images = new Node();

        mainDisplay.setParent(base);
        images.setParent(base);
        images.setEnabled(false);

        CompletableFuture<ViewRenderable> mainDisplayStage =
                ViewRenderable.builder().setView(mContext, R.layout.ar_item).build();
        CompletableFuture<ViewRenderable> pictureDisplayStage =
                ViewRenderable.builder().setView(mContext, R.layout.ar_picture).build();

        CompletableFuture.allOf(
                mainDisplayStage,
                pictureDisplayStage)

                .handle((notUsed, throwable) -> {
//                    Log.i(TAG, "ARscene create "+Thread.currentThread().toString());
                    if (throwable != null) {
                        Utils.displayErrorSnackbar(mainActivity.getRootView(), "Unable to load renderable", throwable);
                        return null;
                    }
                    View cardView;
                    View pictureView;
                    try {
                        ViewRenderable mainDisplayRenderable = mainDisplayStage.get();

                        mainDisplayRenderable.setShadowCaster(false);
                        mainDisplayRenderable.setShadowReceiver(false);
                        mainDisplayRenderable.setSizer(mViewSizer);
                        mainDisplay.setRenderable(mainDisplayRenderable);
                        mainDisplay.setLocalPosition(new Vector3(0.0f, 0.0f, 0.0f));
                        cardView = mainDisplayRenderable.getView();

                        ViewRenderable pictureDisplayRenderable = pictureDisplayStage.get();
                        pictureDisplayRenderable.setShadowCaster(false);
                        pictureDisplayRenderable.setShadowReceiver(false);
                        pictureDisplayRenderable.setSizer(mViewSizer);
                        images.setRenderable(pictureDisplayRenderable);
                        images.setLocalPosition(new Vector3(0.4f, 0.0f, 0.0f));
                        pictureView = pictureDisplayRenderable.getView();

                        Executors.newSingleThreadExecutor().submit(() -> {
                            setMainCard(item, cardView, images);
                            populateImages(item, pictureView);
                            mArSceneView.postInvalidate();
                        });
                    } catch (InterruptedException | ExecutionException ex) {
                        Utils.displayErrorSnackbar(mainActivity.getRootView(), "Unable to load renderable", ex);
                        return null;
                    }
                    return null;
                });
        return base;
    }

    private void setMainCard(Item item, View cardView, Node images) {
        // set text
        TextView name = cardView.findViewById(R.id.item_name);
        name.setText(item.getName());
        TextView body = cardView.findViewById(R.id.item_body);
        body.setText(item.getPropsForARCard());
        ImageButton add = cardView.findViewById(R.id.add);
        ImageButton min = cardView.findViewById(R.id.min);
        ImageButton close = cardView.findViewById(R.id.close);
        add.setOnClickListener(v -> images.setEnabled(!images.isEnabled()));
        min.setOnClickListener(v -> item.minimizeAR(false));
        close.setOnClickListener(v -> item.detachFromAnchors());
    }

    private void populateImages(Item item, View pictureView) {
        LinearLayout layout = pictureView.findViewById(R.id.ar_picture_layout);

        for (String system_id : item.getSystemList()) {
            item.setSystem(system_id);
            Map<String, String> rawImgMap = item.getPictureData();
            for (String device_id : rawImgMap.keySet()) {
                // we only have picture from device 1 right now
                String imgData = rawImgMap.get(device_id);
                if (imgData != null) {
                    String pureBase64 = imgData.substring(imgData.indexOf(",") + 1);
                    byte[] decodedString = Base64.decode(pureBase64, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                    // create title for picture
                    StringBuilder pictureTitle = new StringBuilder()
                            .append(mContext.getResources().getString(R.string.picture_title_system))
                            .append(": ")
                            .append(system_id)
                            .append(" ")
                            .append(mContext.getResources().getString(R.string.picture_title_device))
                            .append(": ")
                            .append(device_id);

                    TextView pic = new TextView(layout.getContext());
                    pic.setText(pictureTitle);
                    pic.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    pic.setTextColor(Color.WHITE);
                    pic.setCompoundDrawablesWithIntrinsicBounds(null, null, null,
                            new BitmapDrawable(mContext.getResources(), decodedByte));
                    pic.setBackgroundColor(mContext.getResources().getColor(R.color.ar_title_color, null));
                    layout.addView(pic);
                }
            }
        }
    }
}
