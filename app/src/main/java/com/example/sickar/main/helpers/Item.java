package com.example.sickar.main.helpers;

import android.util.Log;
import android.widget.Switch;

import androidx.annotation.Nullable;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class Item {
    private static final String TAG = "app_" + Item.class.getSimpleName();
    private Map<String, Map<String, String>> data;
    private ArrayList<String> systems;
    private Map<String, Map<String, String>> pictureData;
    private int currentSysIdx;
    private String name;
    private boolean placedCard;
    private boolean scanned;
    private boolean hasPictures;
    private Anchor anchor;
    private AnchorNode anchorNode;
    private WeakReference<Switch> visible_toggle;

    Item(String name) {
        this.name = name;
        placedCard = false;
        scanned = true;
        hasPictures = false;
        // if an Item is created it must have been scanned

        data = new HashMap<>();
        systems = new ArrayList<>();
        currentSysIdx = 0;
    }

    /**
     * compares equality over barcode
     * //TODO: ensure equality over only item id:
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

    public String getName() {
        return name;
    }

    void addSystem(String systemId) {
        systems.add(systemId);
    }

    public void setSystem(String systemId) {
        currentSysIdx = systems.indexOf(systemId);
    }

    public ArrayList<String> getSystemList() {
        return systems;
    }

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

    /**
     * returns a String with all properties of this Item. Used for display on card
     *
     * @return String of all properties
     */
    public String getAllPropsAsString() {
        StringBuilder text = new StringBuilder();
        Map<String, String> oneSystemData = data.get(systems.get(currentSysIdx));
        for (String label : Objects.requireNonNull(oneSystemData).keySet()) {
            text.append(label).append(": ").append(getProp(label)).append("\n");
        }

//        text.append("systemLabel: ").append(getProp("systemLabel")).append("\n");
//
//        // order in which to display the properties
//        String[] propertiesOrder = {"beltSpeed", "length", "width", "height", "weight", "gap", "angle"};
//        for (String prop : propertiesOrder) {
//            text.append(prop).append(": ").append(getProp(prop)).append("\n");
//        }
////        text.append("id: ").append(getProp("id")).append("\n");
//        text.append("objectScanTime: ").append(getProp("objectScanTime")).append("\n");
//        text.append("barcodes: ").append(getProp("barcodes"));
        return text.toString();
    }

    /**
     * returns a String of properties meant to be displayed on the AR Card
     * @return String of properties
     */
    String getPropsForARCard() {
        StringBuilder text = new StringBuilder();
        // display info from only the latest system
        int orig = currentSysIdx;
        currentSysIdx = 0;
        // order in which to display the properties
        String[] propertiesOrder = {"length", "width", "height", "weight"};
        for (String prop : propertiesOrder) {
            text.append(prop).append(": ").append(getProp(prop)).append("\n");
        }
        currentSysIdx = orig;
        return text.toString();
    }

    /**
     * Places references to the anchor and AnchorNode this item's AR Card is attached to.
     * These are used to clear and detach the card when necessary.
     */
    void setAnchorAndAnchorNode(Anchor anchor, AnchorNode anchorNode) {
        this.anchor = anchor;
        this.anchorNode = anchorNode;
        placedCard = true;
        if (visible_toggle != null && visible_toggle.get() != null) {
            visible_toggle.get().setChecked(true);
        }
    }

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
     * @param isChecked true to display false to minimize
     */
    public void minimizeAR(boolean isChecked) {
        if (placedCard) {
            if (isChecked) {
                for (Node child : anchorNode.getChildren()) {
                    child.setEnabled(true);
                }
                setVisibleToggle(true);
            } else {
                for (Node child : anchorNode.getChildren()) {
                    child.setEnabled(false);
                }
                setVisibleToggle(false);
            }
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
            visible_toggle = new WeakReference<>(button);
        } else {
            visible_toggle.clear();
        }
    }

    boolean hasPictures() {
        return hasPictures;
    }

    Map<String, String> getPictureData() {
        if (hasPictures) {
            return pictureData.get(systems.get(currentSysIdx));
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    void setPictureData(Map pics) {
        pictureData = pics;
        hasPictures = true;
    }

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

    private void setVisibleToggle(boolean visible) {
        if (visible_toggle != null && visible_toggle.get() != null) {
            if (visible) {
                visible_toggle.get().setChecked(true);
            } else {
                visible_toggle.get().setChecked(false);
            }
        }
    }
}
