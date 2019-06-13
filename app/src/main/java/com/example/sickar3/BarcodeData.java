package com.example.sickar3;

import android.util.Log;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Data class to hold barcode information retrieved from server.
 * Uses a stack to keep track of  the order of retrieved
 * barcodes from latest to oldest.
 * Holds the data associated with a barcode in a MAP
 */
public class BarcodeData {
    // b_stack acts like indexable stack (newest items in the front at index 0)
    private ArrayList<String> b_stack;
    private HashMap<String, Item> data;

    public BarcodeData() {
        b_stack = new ArrayList<>();
        data = new HashMap<>();
    }

    public Boolean isEmpty() {
        return b_stack.isEmpty() || data.isEmpty();
    }

    public void put(String barcode, JSONObject response) {
        b_stack.add(0, barcode);
        data.put(barcode, jsonToItem(barcode, response));
    }

    public Item get(String barcode) {
        return data.get(barcode);
    }

    public String peekLatest() {
        return b_stack.get(0);
    }

    public Boolean containsBarcode(String barcode) {
        return data.containsKey(barcode);
    }

    /**
     * returns the latest barcode data added
     */
    public Item getLatest() {
        return get(peekLatest());
    }

    /**
     * Returns the information about each item as a list of Item objects
     * The original map stores the information about the entire response JSON Object
     * We only take the first entry as the item and the rest are ignored.
     * (might have to change this later)
     *
     * @return list of Items, null if no data
     */
    public ArrayList<Item> getItemList() {
        if (!isEmpty()) {
            ArrayList<Item> list = new ArrayList<>();
            for (String bcode : b_stack) {
                list.add(get(bcode));
            }
            return list;
        }
        return null;
    }

    /**
     * Converts a JSONObject response for a barcode to an Item object.
     * Mainly used for displaying the information about
     * each item in the recyclerView.
     * Change this method to parse more information from Json responses
     *
     * @param json, origin JSON response object
     * @return item, Item
     */
    private Item jsonToItem(String barcode, JSONObject json) {
        try {
            Item itm = new Item(barcode);
            JSONArray resultsArray = json.getJSONArray("results");
            if (resultsArray.length() > 0) {
                // no response
                Log.i("app_", "array length: "+resultsArray.length());
                JSONObject firstItem = resultsArray.getJSONObject(0);

                itm.addProp("systemLabel", firstItem.getString("systemLabel"));

                // read properties
                String[] properties = {"beltSpeed", "length", "width", "height", "weight", "gap", "angle"};
                for (String key : properties) {
                    JSONObject property = firstItem.getJSONObject(key);
                    double value = property.getDouble("value");
                    String unitLabel = property.getString("unitLabel");
                    itm.addProp(key, value + " " + unitLabel);
                }

                // boxFactor
                itm.addProp("boxFactor", String.valueOf(firstItem.getDouble("boxFactor")));

                // objectScanTime
                itm.addProp("objectScanTime", firstItem.getString("objectScanTime"));

                // barcodes
                JSONObject barcodes = firstItem.getJSONArray("barcodes").getJSONObject(0);
                itm.addProp("barcode", barcodes.getString("value"));
            } else {
                // no results for item
                itm.addProp("noData", "No data for this item\n" + json.toString());
            }
            return itm;
        } catch (JSONException e) {
            Log.i("app_", "JsonException in parsing response " + e.getMessage());
        }
        return null;
    }
}
