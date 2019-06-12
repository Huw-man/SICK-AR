package com.example.sickar3;

import java.util.HashMap;
import java.util.Hashtable;

class Item {
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
        // order in which to display the properties
        String[] propertiesOrder = {"beltSpeed", "length", "width", "height", "weight", "gap", "angle"};
        StringBuilder text =  new StringBuilder();
        for (String prop: propertiesOrder) {
            text.append(prop).append(": ").append(properties.get(prop)).append("\n");
        }
        return text.toString();
    }
}
