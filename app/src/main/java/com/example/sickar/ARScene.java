package com.example.sickar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import java.util.concurrent.CompletableFuture;

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

        CompletableFuture<ViewRenderable> mainDisplayStage =
                ViewRenderable.builder().setView(mContext, R.layout.ar_item).build();
        CompletableFuture<ViewRenderable> pictureDisplayStage =
                ViewRenderable.builder().setView(mContext, R.layout.ar_picture).build();

        CompletableFuture.allOf(
                mainDisplayStage,
                pictureDisplayStage)

                .handle((notUsed, throwable) -> {
                    if (throwable != null) {
                        Utils.displayErrorSnackbar(mainActivity.getRootView(), "Unable to load renderable", throwable);
                        return null;
                    }
                    try {
                        ViewRenderable mainDisplayRenderable = mainDisplayStage.get();

                        mainDisplayRenderable.setShadowCaster(false);
                        mainDisplayRenderable.setShadowReceiver(false);
                        mainDisplayRenderable.setSizer(mViewSizer);
                        mainDisplay.setRenderable(mainDisplayRenderable);
                        mainDisplay.setLocalPosition(new Vector3(0.0f, 0.0f, 0.0f));
                        View cardView = mainDisplayRenderable.getView();

                        // set text
                        TextView name = cardView.findViewById(R.id.item_name);
                        name.setText(item.getName());
                        TextView body = cardView.findViewById(R.id.item_body);
                        body.setText(item.getPropsForARCard());
                        ImageButton add = cardView.findViewById(R.id.add);
                        ImageButton min = cardView.findViewById(R.id.min);
                        ImageButton close = cardView.findViewById(R.id.close);
                        add.setOnClickListener(v -> {
                            images.setEnabled(!images.isEnabled());

                        });
                        min.setOnClickListener(v -> item.minimizeAR(false));
                        close.setOnClickListener(v -> item.detachFromAnchors());

                        ViewRenderable pictureDisplayRenderable = pictureDisplayStage.get();
                        pictureDisplayRenderable.setShadowCaster(false);
                        pictureDisplayRenderable.setShadowReceiver(false);
                        pictureDisplayRenderable.setSizer(mViewSizer);
                        images.setRenderable(pictureDisplayRenderable);
                        images.setLocalPosition(new Vector3(0.4f, 0.0f, 0.0f));
                        View pictureView = pictureDisplayRenderable.getView();
                        ImageView imageView = pictureView.findViewById(R.id.imageView);

                        // we only have picture from device 1 right now
                        String imgData = item.getPictureData().get("1");
                        if (imgData != null) {
                            String pureBase64 = imgData.substring(imgData.indexOf(",") + 1);
                            byte[] decodedString = Base64.decode(pureBase64, Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            imageView.setImageBitmap(decodedByte);
                        }


                    } catch (Exception ex) {
//                    InterruptedException | ExecutionException |
                        Utils.displayErrorSnackbar(mainActivity.getRootView(), "Unable to load renderable", ex);
                    }
                    return null;
                });

//        Node images = new Node();
//        images.setParent(base);
//        ViewRenderable.builder().setView(mContext, R.layout.ar_picture).build()
//                .thenAccept(viewRenderable -> {
//                    // load the pictures
//                    viewRenderable.setShadowCaster(false);
//                    viewRenderable.setShadowReceiver(false);
//                    viewRenderable.setSizer(mViewSizer);
//                    images.setRenderable(viewRenderable);
//                    images.setLocalPosition(new Vector3(0.0f, 0.9f, 0.0f));
//                    View cardView = viewRenderable.getView();
//                    ImageView imageView = cardView.findViewById(R.id.image);
//
//                    // Todo: populate all images and their device id's
//                    String imgData = mainActivity.getViewModel().getPictures(item.getName()).get("1");
//                    if (imgData != null) {
//                        byte[] decodedString = Base64.decode(imgData, Base64.DEFAULT);
//                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
//                        imageView.setImageBitmap(decodedByte);
//                    }
//                });


//        Node mainDisplay = new Node();
//        mainDisplay.setParent(base);
//        ViewRenderable.builder().setView(mContext, R.layout.ar_item).build()
//                .thenAccept(viewRenderable -> {
//                    // consumer
//                    viewRenderable.setShadowCaster(false);
//                    viewRenderable.setShadowReceiver(false);
//                    viewRenderable.setSizer(mViewSizer);
//                    mainDisplay.setRenderable(viewRenderable);
//                    mainDisplay.setLocalPosition(new Vector3(0.0f, 0.0f, 0.0f));
//                    View cardView = viewRenderable.getView();
//
//                    // set text
//                    TextView name = cardView.findViewById(R.id.item_name);
//                    name.setText(item.getName());
//                    TextView body = cardView.findViewById(R.id.item_body);
//                    body.setText(item.getPropsForARCard());
//                    ImageButton add = cardView.findViewById(R.id.add);
//                    ImageButton min = cardView.findViewById(R.id.min);
//                    ImageButton close = cardView.findViewById(R.id.close);
//                    min.setOnClickListener(v -> item.minimizeAR(false));
//                    close.setOnClickListener(v -> item.detachFromAnchors());
//                });

        return base;
    }
}
