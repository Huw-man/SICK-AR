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
    private static final String TAG = "app_"+BarcodeData.class.getSimpleName();
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
        put(barcode, jsonToItem(barcode, response));
    }

    public void put(String barcode, Item item) {
        // TODO: put in request for fetching new item
        // Right now only one item per barcode ever persists
        // for the application lifetime. Once an item is scanned no new network
        // fetch requests will be made. Might want to consider different
        // designs in the future.
        if (!data.containsValue(item)) {
            b_stack.add(0, barcode);
            data.put(barcode, item);
        } else {
            Log.i(TAG, "repeat item request");
        }
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
     * check if there is data contained in network response
     * returns false if no data found in the JSON Response
     */
    public boolean hasData(JSONObject response) {
        try {
            JSONArray resultsArray = response.getJSONArray("results");
            if (resultsArray.length() > 0) {
                return true;
            }
        } catch (JSONException e) {
            Log.i(TAG, e.getMessage());
        }
        return false;
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
//                Log.i(TAG, "array length: "+resultsArray.length());
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

                // parse barcodes
                JSONArray barcodesArray = firstItem.getJSONArray("barcodes");
                StringBuilder barcodeStrings = new StringBuilder();
                for (int i = 0; i < barcodesArray.length(); i++) {
                    barcodeStrings.append(barcodesArray.getJSONObject(i).getString("value"))
                            .append("\n");
                }
                // remove newLine at the very end
                barcodeStrings.setLength(barcodeStrings.length() - 1);
                itm.addProp("barcodes", barcodeStrings.toString());

            } else {
                // no results for item
                itm.addProp("noData", "No data for this item\n" + json.toString());
            }
            return itm;
        } catch (JSONException e) {
            Log.i(TAG, "JsonException in parsing response " + e.getMessage());
        }
        return null;
    }
}
