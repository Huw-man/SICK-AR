package com.example.sickar3;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Set;

class Item {
    private static final String LOGTAG = "app_"+MainActivity.class.getSimpleName();
    private HashMap<String, String> properties;
    private String name;
    public Item(String name) {
        this.name = name;
        properties = new HashMap<>();
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

    public String getAllPropsAsString() {
        StringBuilder text =  new StringBuilder();
        if (properties.containsKey("noData")) {
            text.append(getProp("noData"));
        }

        text.append("systemLabel: ").append(getProp("systemLabel")).append("\n");

        // order in which to display the properties
        String[] propertiesOrder = {"beltSpeed", "length", "width", "height", "weight", "gap", "angle"};
        for (String prop: propertiesOrder) {
            text.append(prop).append(": ").append(properties.get(prop)).append("\n");
        }

        text.append("objectScanTime: ").append(getProp("objectScanTime")).append("\n");

        text.append("barcodes: ").append(getProp("barcodes"));
        return text.toString();
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
