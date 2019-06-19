package com.example.sickar3;

import androidx.annotation.Nullable;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Set;

class Item {
    private static final String LOGTAG = "app_" + MainActivity.class.getSimpleName();
    private HashMap<String, String> properties;
    private String name;
    private boolean placedCard;
    private Anchor anchor;
    private AnchorNode anchorNode;

    public Item(String name) {
        this.name = name;
        properties = new HashMap<>();
        placedCard = false;
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
    public void setPlaced(boolean placedCard) {
        this.placedCard = placedCard;
    }

    public String getName() {
        return name;
    }

    public void addProp(String label, String value) {
        properties.put(label, value);
    }

    public String getProp(String label) {
        return properties.get(label);
    }

    /**
     * returns a String with all properties of this Item. Used for display on card
     * @return String of all properties
     */
    public String getAllPropsAsString() {
        StringBuilder text = new StringBuilder();
        if (properties.containsKey("noData")) {
            text.append(getProp("noData"));
        }

        text.append("systemLabel: ").append(getProp("systemLabel")).append("\n");

        // order in which to display the properties
        String[] propertiesOrder = {"beltSpeed", "length", "width", "height", "weight", "gap", "angle"};
        for (String prop : propertiesOrder) {
            text.append(prop).append(": ").append(properties.get(prop)).append("\n");
        }

        text.append("objectScanTime: ").append(getProp("objectScanTime")).append("\n");

        text.append("barcodes: ").append(getProp("barcodes"));
        return text.toString();
    }

    /**
     * returns a String of properties meant to be displayed on the AR Card
     * @return String of properties
     */
    public String getPropsForARCard() {
        StringBuilder text = new StringBuilder();
        if (properties.containsKey("noData")) {
            text.append(getProp("noData"));
        }

        // order in which to display the properties
        String[] propertiesOrder = {"length", "width", "height", "weight"};
        for (String prop : propertiesOrder) {
            text.append(prop).append(": ").append(properties.get(prop)).append("\n");
        }

        return text.toString();
    }

    /**
     * Places references to the anchor and AnchorNode this item's AR Card is attached to.
     * These are used to clear and detach the card when necessary.
     */
    public void setAnchorAndAnchorNode(Anchor anchor, AnchorNode anchorNode) {
        this.anchor = anchor;
        this.anchorNode = anchorNode;
    }

    public boolean detachFromAnchors() {
        anchor.detach();
        for (Node child: anchorNode.getChildren()) {
            anchorNode.removeChild(child);
        }
        anchor = null;
        anchorNode = null;
        return true;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof Item) {
            Item itm = (Item) obj;
            return this.properties.equals(itm.properties);
        }
        return false;
    }
}
