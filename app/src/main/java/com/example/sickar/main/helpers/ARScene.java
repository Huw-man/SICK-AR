package com.example.sickar.main.helpers;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.sickar.R;
import com.example.sickar.Utils;
import com.example.sickar.image.ImageActivity;
import com.example.sickar.libs.SelfOrientatingNode;
import com.example.sickar.main.MainActivity;
import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.DpToMetersViewSizer;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ARScene {
    private static final String TAG = "app_" + ARScene.class.getSimpleName();

    private ArSceneView mArSceneView;
    private Context mContext;
    private DpToMetersViewSizer mViewSizer;
    private MainActivity mMainActivity;
    private ArFragment mArFragment;

    private ARScene(Context context, ArSceneView arSceneView) {
        mContext = context;
        mMainActivity = (MainActivity) context;
        mArSceneView = arSceneView;
        mViewSizer = new DpToMetersViewSizer(1000);
    }

    public ARScene(Context context, ArFragment arFragment) {
        this(context, arFragment.getArSceneView());
        mArFragment = arFragment;
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
                item.setAnchorAndAnchorNode(anchorNode, base.findByName("mainDisplayNode"));
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
        Node base = new TransformableNode(mArFragment.getTransformationSystem());
        Node mainDisplayNode = new SelfOrientatingNode();
        Node tamperNode = new Node();
        Node modelNode = new Node();

        mainDisplayNode.setParent(base);
        tamperNode.setParent(mainDisplayNode);
        modelNode.setParent(base);

        mainDisplayNode.setName("mainDisplayNode");

        CompletableFuture<ViewRenderable> mainDisplayStage =
                ViewRenderable.builder().setView(mContext, R.layout.ar_item).build();
        CompletableFuture<ViewRenderable> tamperDisplayStage =
                ViewRenderable.builder().setView(mContext, R.layout.ar_tamper).build();
        CompletableFuture<ModelRenderable> modelStage =
                ModelRenderable.builder().setSource(mContext, Uri.parse("1240 Neptune.sfb")).build();

        CompletableFuture.allOf(
                mainDisplayStage,
                tamperDisplayStage,
                modelStage)
                .handle((notUsed, throwable) -> {
//                    Log.i(TAG, "ARscene create "+Thread.currentThread().toString());
                    if (throwable != null) {
                        Utils.displayErrorSnackbar(mMainActivity.getRootView(), "Unable to load renderable", throwable);
                        return null;
                    }
                    View cardView;
                    View tamperView;
                    try {
                        ViewRenderable mainDisplayRenderable = mainDisplayStage.get();
                        setRenderableSettings(mainDisplayRenderable);
                        mainDisplayNode.setRenderable(mainDisplayRenderable);
                        mainDisplayNode.setLocalPosition(new Vector3(0.0f, 0.07f, 0.0f));
                        cardView = mainDisplayRenderable.getView();

                        ViewRenderable tamperDisplayRenderable = tamperDisplayStage.get();
                        setRenderableSettings(tamperDisplayRenderable);
                        tamperNode.setRenderable(tamperDisplayRenderable);
                        tamperNode.setLocalPosition(new Vector3(0.0f, 0.2f, 0.0f));
                        tamperView = tamperDisplayRenderable.getView();

                        ModelRenderable boxRenderable = modelStage.get();
                        modelNode.setRenderable(boxRenderable);
                        modelNode.setLocalScale(new Vector3(0.05f, 0.05f, 0.05f));

                        setMainDisplay(item, cardView, tamperNode);
                        // update tamper View once network request is finished
                        mMainActivity.getViewModel().getTamperInfo(item.getName())
                                .thenAccept(map -> setTamperDisplay(map, item, tamperView,
                                        tamperNode));
                    } catch (InterruptedException | ExecutionException ex) {
                        Utils.displayErrorSnackbar(mMainActivity.getRootView(), "Unable to load renderable", ex);
                        return null;
                    }
                    return null;
                });

        return base;
    }

    private void setMainDisplay(Item item, View cardView, Node tamperNode) {
        // set text
        TextView name = cardView.findViewById(R.id.item_name);
        name.setText(item.getName());
        TextView body = cardView.findViewById(R.id.item_body);
        body.setText(item.getPropsForARCard());

        // get button references
        ImageButton addButton = cardView.findViewById(R.id.ar_add);
        ImageButton minButton = cardView.findViewById(R.id.ar_min);
        ImageButton closeButton = cardView.findViewById(R.id.ar_close);
        ImageButton imageButton = cardView.findViewById(R.id.ar_image);

        // set button listeners
        addButton.setOnClickListener(v -> {
//            imageNode.setEnabled(!imageNode.isEnabled());
            tamperNode.setEnabled(!tamperNode.isEnabled());
        });
        minButton.setOnClickListener(v -> item.minimizeAR(false));
        closeButton.setOnClickListener(v -> item.detachFromAnchors());
        imageButton.setOnClickListener(v -> {
            Intent startImageActivityIntent = new Intent(mContext, ImageActivity.class);
            startImageActivityIntent.putExtra("value", item.getName());
            mMainActivity.startActivity(startImageActivityIntent);
        });
    }

    private void setImageDisplay(Item item, View pictureView) {
        LinearLayout layout = pictureView.findViewById(R.id.ar_picture_layout);
        boolean noImages = false;
        for (String system_id : item.getSystemList()) {
            item.setSystem(system_id);
            Map<String, String> rawImgMap = item.getPictureData();
            if (rawImgMap == null) {
                noImages = true;
                break;
            }
            for (String device_id : rawImgMap.keySet()) {
                // we only have picture from device 1 right now
                String imgData = rawImgMap.get(device_id);
                if (imgData != null) {
                    String pureBase64 = imgData.substring(imgData.indexOf(",") + 1);
                    byte[] decodedString = Base64.decode(pureBase64, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                    // create title for picture
                    StringBuilder pictureTitle = new StringBuilder()
                            .append(mContext.getResources().getString(R.string.system))
                            .append(": ")
                            .append(system_id)
                            .append(", ")
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

                    noImages = true;
                }
            }
        }
        if (!noImages) {
//            Log.i(TAG, "no images");
            TextView noPic = new TextView(layout.getContext());
            noPic.setText(mContext.getResources().getString(R.string.no_images));
            noPic.setTextSize(24);
            noPic.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            noPic.setTextColor(Color.WHITE);
            noPic.setBackgroundColor(mContext.getResources().getColor(R.color.ar_title_color, null));
            layout.addView(noPic);
        }
    }

    /**
     * Setup the tamper display
     *
     * @param tampers    the Map that is the JSON response from the backend
     * @param tamperView the root view of the tamper display
     * @param tamperNode the node the tamper display is attached to
     */
    private void setTamperDisplay(Map tampers, Item item, View tamperView, Node tamperNode) {
        LinearLayout layout = tamperView.findViewById(R.id.tamper_layout);
        TextView title = tamperView.findViewById(R.id.tamper_title);
        TextView body = tamperView.findViewById(R.id.tamper_info);

        try {
            if (tampers.containsKey("tamper") && (boolean) tampers.get("tamper")) {
                title.setText(mContext.getResources().getString(R.string.tamper_detected));

                StringBuilder bodyText = new StringBuilder();
                bodyText.append("From ")
                        .append(mContext.getResources().getString(R.string.system))
                        .append(" ")
                        .append(item.getSystemList().get(item.getSystemList().size() - 1))
                        .append("\n");
                Map tamperDetails = (Map) tampers.get("tamperDetails");
                for (Object Id :
                        (ArrayList) Objects.requireNonNull(tampers.get("tamperOrder"))) {
                    String systemId = (String) Id;

                    bodyText.append(mContext.getResources().getString(R.string.system))
                            .append(" ")
                            .append(systemId)
                            .append(" has changes in ")
                            .append(Objects.requireNonNull(tamperDetails).get(systemId))
                            .append("\n");

//                    for (String[] changed_prop :
//                            (String[][]) Objects.requireNonNull(tamperDetails.get(systemId))){
//                        bodyText.append(changed_prop[1])
//                                .append(" in ")
//                                .append(changed_prop[0])
//                                .append("\n");
//                    }
//                    bodyText.append(" from system ");

                }
                bodyText.deleteCharAt(bodyText.length() - 1);
                body.setText(bodyText.toString());
            } else {
                title.setText(mContext.getResources().getString(R.string.no_tamper_detected));
                title.setBackgroundColor(Color.GREEN);
                layout.removeView(body);
                tamperNode.setEnabled(false);
            }
        } catch (Resources.NotFoundException e) {
            Log.i(TAG, e.toString());
        }

    }

    private void setRenderableSettings(ViewRenderable renderable) {
        renderable.setShadowCaster(false);
        renderable.setShadowReceiver(false);
        renderable.setSizer(mViewSizer);
    }
}
