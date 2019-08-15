package com.example.sickar.libs;

import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

/**
 * Custom Sceneform Node which will orient itself to the camera
 */
public class SelfOrientingNode extends Node {

    /**
     * Called on the update of a frame.
     * Here this node will update its orientation to face the user.
     *
     * @param frameTime FrameTime
     */
    @Override
    public void onUpdate(FrameTime frameTime) {
        if (getScene() == null || !this.isEnabled()) {
            return;
        }

        Vector3 cameraPosition = getScene().getCamera().getWorldPosition();
        Vector3 position = this.getWorldPosition();
        Vector3 direction = Vector3.subtract(position, cameraPosition);
        Quaternion lookRotation = Quaternion.lookRotation(direction, Vector3.up());
        this.setWorldRotation(lookRotation);
    }
}
