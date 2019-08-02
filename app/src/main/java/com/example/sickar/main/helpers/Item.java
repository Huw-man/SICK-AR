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

public class Item {
    private static final String TAG = "app_" + Item.class.getSimpleName();
    private Map<String, Map<String, String>> mData;
    private ArrayList<String> mSystems;
    private Map<String, Map<String, String>> mPictureData;
    private int mCurrentSysIdx;
    private String mName;
    private boolean mPlacedCard;
    private boolean mScanned;
    private boolean mHasImages;
    private Anchor mAnchor;
    private AnchorNode mAnchorNode;
    private Node mDisplayNode;
    private WeakReference<Switch> mVisibleToggle;

    Item(String name) {
        this.mName = name;
        mPlacedCard = false;
        mScanned = true;
        mHasImages = false;
        // if an Item is created it must have been mScanned

        mData = new HashMap<>();
        mSystems = new ArrayList<>();
        mCurrentSysIdx = 0;
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
            return this.mName.equals(itm.getName());
        }
        return false;
    }

    /**
     * @return true if Item has AR Card placed
     */
    public boolean isPlaced() {
        return mPlacedCard;
    }

    /**
     * Set if Item has ARCard placed.
     */
    void setPlaced(boolean placedCard) {
        this.mPlacedCard = placedCard;
    }

    /**
     * @return true if Item has been mScanned and presented in recyclerView
     */
    public boolean isScanned() {
        return mScanned;
    }

    /**
     * Set if Item has been mScanned
     */
    public void setScanned(boolean scanned) {
        this.mScanned = scanned;
    }

    public String getName() {
        return mName;
    }

    void addSystem(String systemId) {
        mSystems.add(systemId);
    }

    public void setSystem(String systemId) {
        mCurrentSysIdx = mSystems.indexOf(systemId);
    }

    /**
     * Get the systemIds associate with this item
     *
     * @return list of systemIds
     */
    public List<String> getSystemList() {
        return mSystems;
    }

    void addProp(String systemId, String label, String value) {
        try {
            if (mData.get(systemId) == null) {
                mData.put(systemId, new LinkedHashMap<>());
                Objects.requireNonNull(mData.get(systemId)).put(label, value);
            } else {
                Objects.requireNonNull(mData.get(systemId)).put(label, value);
            }
        } catch (NullPointerException e) {
            Log.i(TAG, "no such system: " + systemId);
        }
    }

    public String getProp(String systemId, String label) {
        return Objects.requireNonNull(mData.get(systemId)).get(label);
    }

    /**
     * returns a String with all properties of this Item. Used for display on card
     *
     * @return String of all properties
     */
    public String getAllPropsAsString() {
        StringBuilder text = new StringBuilder();
        Map<String, String> oneSystemData = mData.get(mSystems.get(mCurrentSysIdx));
        for (String label : Objects.requireNonNull(oneSystemData).keySet()) {
            text.append(Utils.unpackCamelCase(label)).append(": ").append(getProp(label)).append(
                    "\n");
        }
        text.deleteCharAt(text.length() - 1);

//        text.append("systemLabel: ").append(getProp("systemLabel")).append("\n");

        /*
        // order in which to display the properties
        String[] propertiesOrder = {"beltSpeed", "length", "width", "height", "weight", "gap", "angle"};
        for (String prop : propertiesOrder) {
            text.append(prop).append(": ").append(getProp(prop)).append("\n");
        }
        text.append("id: ").append(getProp("id")).append("\n");
        text.append("objectScanTime: ").append(getProp("objectScanTime")).append("\n");
        text.append("barcodes: ").append(getProp("barcodes"));
        */
        return text.toString();
    }

    /**
     * return the map containing properties and values
     *
     * @return data from a single system
     */
    public Map<String, String> getOneSystemData() {
        return mData.get(mSystems.get(mCurrentSysIdx));
    }

    /**
     * returns a String of properties meant to be displayed on the AR Card
     *
     * @return String of properties
     */
    String getPropsForARCard() {
        StringBuilder text = new StringBuilder();
        // display info from only the latest system
        int orig = mCurrentSysIdx;
        mCurrentSysIdx = 0;
        // order in which to display the properties
        String[] propertiesOrder = {"length", "width", "height", "volume", "weight"};
        for (String prop : propertiesOrder) {
            text.append(prop).append(" : ").append(getProp(prop)).append("\n");
        }
        text.deleteCharAt(text.length() - 1);
        mCurrentSysIdx = orig;
        return text.toString();
    }

    /**
     * Places references to the mAnchor and AnchorNode this item's AR Card is attached to.
     * These are used to clear and detach the card when necessary.
     */
    void setAnchorAndAnchorNode(AnchorNode anchorNode, Node displayNode) {
        mAnchor = anchorNode.getAnchor();
        mAnchorNode = anchorNode;
        mDisplayNode = displayNode;
        mPlacedCard = true;
        if (mVisibleToggle != null && mVisibleToggle.get() != null) {
            mVisibleToggle.get().setChecked(true);
        }
    }

    public boolean detachFromAnchors() {
        if (mPlacedCard) {
            for (Node child : mAnchorNode.getChildren()) {
                mAnchorNode.removeChild(child);
            }
            mAnchorNode.setParent(null);
            mAnchor.detach();
            mAnchor = null;
            mAnchorNode = null;
            mPlacedCard = false;
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
        if (mPlacedCard) {
            mDisplayNode.setEnabled(isDisplay);
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
            mVisibleToggle = new WeakReference<>(button);
        } else {
            mVisibleToggle.clear();
        }
    }

    boolean hasPictures() {
        return mHasImages;
    }

    Map<String, String> getPictureData() {
        if (mHasImages) {
            return mPictureData.get(mSystems.get(mCurrentSysIdx));
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    void setPictureData(Map pics) {
        mPictureData = pics;
        mHasImages = true;
    }

    private String getProp(String label) {
        try {
            if (mData.get(mSystems.get(mCurrentSysIdx)) != null) {
                return Objects.requireNonNull(mData.get(mSystems.get(mCurrentSysIdx))).get(label);
            }
        } catch (NullPointerException e) {
            Log.i(TAG, "no such system: " + mSystems.get(mCurrentSysIdx));
        }
        return null;
    }

    private void setVisibleToggle(boolean visible) {
        if (mVisibleToggle != null && mVisibleToggle.get() != null) {
            if (visible) {
                mVisibleToggle.get().setChecked(true);
            } else {
                mVisibleToggle.get().setChecked(false);
            }
        }
    }
}
