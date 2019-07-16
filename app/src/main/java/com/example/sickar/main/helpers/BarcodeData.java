package com.example.sickar.main.helpers;

import android.util.Log;

import com.example.sickar.Constants;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Data class to hold barcode information retrieved from server.
 * Uses a list to keep track of the order of retrieved
 * barcodes from latest to oldest. (Newest items are place in front)
 * Holds the data associated with a barcode in a MAP
 */
public class BarcodeData {
    private static final String TAG = "app_"+BarcodeData.class.getSimpleName();

    // b_stack acts like indexable stack (newest items in the front at index 0)
    private List<String> b_stack;
    private Map<String, Item> data;

    public BarcodeData() {
        b_stack = new ArrayList<>();
        data = new ConcurrentHashMap<>();
    }

    public Boolean isEmpty() {
        return b_stack.isEmpty() || data.isEmpty();
    }

    public boolean put(String barcode, JSONObject response) {
        return put(barcode, jsonToItem(barcode, response));
    }

    public boolean put(String barcode, Item item) {
        // Right now only one item per barcode ever persists
        // for the application lifetime. Once an item is scanned no new network
        // fetch requests will be made. Might want to consider different
        // designs in the future.
        if (!containsBarcode(item.getName())) {
            Log.i(TAG, "inserted " + item.getName());
            b_stack.add(0, barcode);
            data.put(barcode, item);
            resize();
            return true;
        } else {
            Log.i(TAG, "repeat item request");
            return false;
        }
    }

    /**
     * Remove the specified item the cache
     *
     * @param barcode barcode
     */
    void remove(String barcode) {
        b_stack.remove(barcode);
        data.remove(barcode);
    }

    public Item get(String barcode) {
        return data.get(barcode);
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
    ArrayList<Item> getItemList() {
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

    public boolean addPictures(String barcode, JSONObject response) {
        try {
            if (containsBarcode(barcode) && response.getJSONObject("results") != null) {
                Objects.requireNonNull(data.get(barcode)).setPictureData(jsonToMap(response.getJSONObject("results")));
                return true;
            }
        } catch (JSONException | NullPointerException e) {
            Log.i(TAG, e.toString());
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
            JSONArray systems = json.getJSONArray("systems");
            JSONArray resultsArray = json.getJSONArray("results");
            if (resultsArray.length() > 0) {
                for (int x = 0; x < systems.length(); x++) {
//                Log.i(TAG, "array length: "+resultsArray.length());
                    itm.addSystem(systems.getString(x));
                    JSONObject itemData = resultsArray.getJSONObject(x);
                    itm.addProp(systems.getString(x),"systemLabel", itemData.getString("systemLabel"));

                    // read properties
                    String[] properties = {"beltSpeed", "length", "width", "height", "weight", "gap", "angle"};
                    for (String key : properties) {
                        JSONObject property = itemData.getJSONObject(key);
                        try {
                            double value = property.getDouble("value");
                            String unitLabel = property.getString("unitLabel");
                            itm.addProp(systems.getString(x), key, value + " " + unitLabel);
                        } catch (JSONException e) {
                            // only add property if the values exist for it
                            Log.i(TAG, key + " contains null data");
                        }
                    }

                    // boxFactor
                    itm.addProp(systems.getString(x),"boxFactor", String.valueOf(itemData.getDouble("boxFactor")));

                    // objectScanTime
                    itm.addProp(systems.getString(x),"objectScanTime", itemData.getString("objectScanTime"));

                    // id
                    itm.addProp(systems.getString(x),"id", itemData.getString("id"));

                    // parse barcodes
                    JSONArray barcodesArray = itemData.getJSONArray("barcodes");
                    if (barcodesArray.length() > 0) {
                        // only inset barcodes it there are multiple
                        StringBuilder barcodeStrings = new StringBuilder();
                        for (int i = 0; i < barcodesArray.length(); i++) {
                            barcodeStrings.append(barcodesArray.getJSONObject(i).getString("value"));
                            if (i != barcodesArray.length() - 1) {
                                barcodeStrings.append("\n");
                            }
                        }
                        itm.addProp(systems.getString(x), "barcodes", barcodeStrings.toString());
                    }
                }

            }
            return itm;
        } catch (JSONException e) {
            Log.i(TAG, "JsonException in parsing response " + e.getMessage());
        }
        return null;
    }

    private Map jsonToMap(JSONObject json) {
        //TODO: maybe convert base 64 string picture data to bitmap upon reception?
//        for (String system : data.keySet()) {
//            for (String device : data.get(system).keySet()) {
//                data.get()
//            }
//        }
        return new Gson().fromJson(json.toString(), HashMap.class);
    }


    /**
     * Resize this BarcodeData cache to be consistent with the maxSize
     */
    private void resize() {
        int size = b_stack.size();
        if (size > Constants.CACHE_SIZE) {
            for (int i = size - 1; i >= Constants.CACHE_SIZE; i--) {
                String key = b_stack.get(i);
                b_stack.remove(i);
                data.remove(key);
            }
        }
    }

    /**
     * Returns the latest barcode located at the top of the stack
     */
    private String peekLatest() {
        return b_stack.get(0);
    }
}
