package com.example.sickar;

import android.content.Context;
import android.util.Log;
import android.view.View;
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

public class ARScene {
    private static final String TAG = "app_" + ARScene.class.getSimpleName();

    private ArSceneView mArSceneView;
    private Context context;
    private DpToMetersViewSizer viewSizer;

    public ARScene(Context context, ArSceneView arSceneView) {
        this.context = context;
        mArSceneView = arSceneView;
        viewSizer = new DpToMetersViewSizer(500);
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
     * @return base Node
     */
    private Node createNode(Item item) {
        Node base = new Node();
        ViewRenderable.builder().setView(context, R.layout.ar_item_card).build()
                .thenAccept(viewRenderable -> {
                    // consumer
                    viewRenderable.setShadowCaster(false);
                    viewRenderable.setShadowReceiver(false);
                    viewRenderable.setSizer(viewSizer);
                    base.setRenderable(viewRenderable);
                    base.setLocalPosition(new Vector3(0.0f, 0.0f, 0.0f));
                    View cardView = viewRenderable.getView();

                    // set text
                    TextView name = cardView.findViewById(R.id.item_name);
                    name.setText(item.getName());
                    TextView body = cardView.findViewById(R.id.item_body);
                    body.setText(item.getPropsForARCard());
                });

        return base;
    }
}
