package com.example.sickar.main.helpers;

import android.util.Log;
import android.widget.Switch;

import androidx.annotation.Nullable;

import com.example.sickar.Utils;
import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Item class represents the data associated with a particular barcode
 */
public class Item {
    private static final String TAG = "app_" + Item.class.getSimpleName();

    private Map<String, Map<String, String>> data;
    private ArrayList<String> systems;
    private Map<String, Map<String, String>> imageData;
    private int currentSysIdx;
    private String name;
    private boolean placedCard;
    private boolean scanned;
    private boolean hasImages;
    private Anchor anchor;
    private AnchorNode anchorNode;
    private Node displayNode;
    private WeakReference<Switch> visibleToggle;

    /**
     * Construct an item object from its barcode
     *
     * @param name barcode
     */
    Item(String name) {
        this.name = name;
        placedCard = false;
        scanned = true;
        hasImages = false;
        // if an Item is created it must have been scanned

        data = new HashMap<>();
        systems = new ArrayList<>();
        currentSysIdx = 0;
    }

    /**
     * compares equality over barcode
     *
     * @param obj other Item for comparison
     * @return true if equal false otherwise
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof Item) {
            Item itm = (Item) obj;
            return this.name.equals(itm.getName());
        }
        return false;
    }

    /**
     * @return true if Item has AR Card placed
     */
    public boolean isPlaced() {
        return placedCard;
    }

    /**
     * Set if Item has ARCard placed.
     */
    void setPlaced(boolean placedCard) {
        this.placedCard = placedCard;
    }

    /**
     * @return true if Item has been scanned and presented in recyclerView
     */
    public boolean isScanned() {
        return scanned;
    }

    /**
     * Set if Item has been scanned
     */
    public void setScanned(boolean scanned) {
        this.scanned = scanned;
    }

    /**
     * @return barcode of this item
     */
    public String getName() {
        return name;
    }

    /**
     * Add a new system to this item
     *
     * @param systemId string systemId
     */
    void addSystem(String systemId) {
        systems.add(systemId);
    }

    /**
     * Set the system that subsequent get methods will pull from
     *
     * @param systemId string systemId
     */
    public void setSystem(String systemId) {
        currentSysIdx = systems.indexOf(systemId);
    }

    /**
     * Get the systemIds associated with this item
     *
     * @return list of systemIds
     */
    public List<String> getSystemList() {
        return systems;
    }

    /**
     * Add a property of this item
     *
     * @param systemId system this property if from
     * @param label    name of this property
     * @param value    value of this property
     */
    void addProp(String systemId, String label, String value) {
        try {
            if (data.get(systemId) == null) {
                data.put(systemId, new LinkedHashMap<>());
                Objects.requireNonNull(data.get(systemId)).put(label, value);
            } else {
                Objects.requireNonNull(data.get(systemId)).put(label, value);
            }
        } catch (NullPointerException e) {
            Log.i(TAG, "no such system: " + systemId);
        }
    }

    public String getProp(String systemId, String label) {
        return Objects.requireNonNull(data.get(systemId)).get(label);
    }

    /**
     * returns a String with all properties of this Item. Used for display on card
     *
     * @deprecated
     * @return String of all properties
     */
    public String getAllPropsAsString() {
        StringBuilder text = new StringBuilder();
        Map<String, String> oneSystemData = data.get(systems.get(currentSysIdx));
        for (String label : Objects.requireNonNull(oneSystemData).keySet()) {
            text.append(Utils.unpackCamelCase(label)).append(": ").append(getProp(label)).append(
                    "\n");
        }
        text.deleteCharAt(text.length() - 1);
        return text.toString();
    }

    /**
     * return the map containing properties and values
     *
     * @return data from a single system
     */
    public Map<String, String> getOneSystemData() {
        return data.get(systems.get(currentSysIdx));
    }

    /**
     * returns a String of properties meant to be displayed on the AR Card
     *
     * @return String of properties
     */
    String getPropsForARCard() {
        StringBuilder text = new StringBuilder();
        // display info from only the latest system
        int orig = currentSysIdx;
        currentSysIdx = 0;
        // order in which to display the properties
        String[] propertiesOrder = {"length", "width", "height", "volume", "weight"};
        for (String prop : propertiesOrder) {
            text.append(prop).append(" : ").append(getProp(prop)).append("\n");
        }
        text.deleteCharAt(text.length() - 1);
        currentSysIdx = orig;
        return text.toString();
    }

    /**
     * Places references to the anchor and AnchorNode this item's AR Card is attached to.
     * These are used to clear and detach the card when necessary.
     *
     * @param anchorNode anchorNode
     * @param displayNode root node for minimizing
     */
    void setAnchorAndAnchorNode(AnchorNode anchorNode, Node displayNode) {
        anchor = anchorNode.getAnchor();
        this.anchorNode = anchorNode;
        this.displayNode = displayNode;
        placedCard = true;
        if (visibleToggle != null && visibleToggle.get() != null) {
            visibleToggle.get().setChecked(true);
        }
    }

    /**
     * Detach the AR elements from this item
     *
     * @return true if successful, false if not
     */
    public boolean detachFromAnchors() {
        if (placedCard) {
            for (Node child : anchorNode.getChildren()) {
                anchorNode.removeChild(child);
            }
            anchorNode.setParent(null);
            anchor.detach();
            anchor = null;
            anchorNode = null;
            placedCard = false;
            setVisibleToggle(false);
            return true;
        }
        return false;
    }

    /**
     * Minimize or display AR Card.
     *
     * @param isDisplay true to display false to minimize
     */
    public void minimizeAR(boolean isDisplay) {
        if (placedCard) {
            displayNode.setEnabled(isDisplay);
            setVisibleToggle(isDisplay);
        }
    }

    /**
     * sets the switch button that should be references by this Item.
     * If the parameter is null then it will clear the reference.
     *
     * @param button switch to be referenced
     */
    public void setVisibleToggleReference(Switch button) {
        if (button != null) {
            visibleToggle = new WeakReference<>(button);
        } else {
            visibleToggle.clear();
        }
    }

    /**
     * Check if this item has images
     */
    boolean hasImages() {
        return hasImages;
    }

    /**
     * Get the image data associated with this item
     *
     * @return Map containing the image data
     */
    Map<String, String> getImageData() {
        if (hasImages) {
            return imageData.get(systems.get(currentSysIdx));
        } else {
            return null;
        }
    }

    /**
     * Set the imageData of this item
     *
     * @param images image data
     */
    @SuppressWarnings("unchecked")
    void setImageData(Map images) {
        imageData = images;
        hasImages = true;
    }

    /**
     * Get the value of a particular property of this item
     *
     * @param label name of the property
     * @return value of the property
     */
    private String getProp(String label) {
        try {
            if (data.get(systems.get(currentSysIdx)) != null) {
                return Objects.requireNonNull(data.get(systems.get(currentSysIdx))).get(label);
            }
        } catch (NullPointerException e) {
            Log.i(TAG, "no such system: " + systems.get(currentSysIdx));
        }
        return null;
    }

    /**
     * Set the visibility of the item's AR card
     *
     * @param visible true for visible, false for invisible
     */
    private void setVisibleToggle(boolean visible) {
        if (visibleToggle != null && visibleToggle.get() != null) {
            if (visible) {
                visibleToggle.get().setChecked(true);
            } else {
                visibleToggle.get().setChecked(false);
            }
        }
    }
}
