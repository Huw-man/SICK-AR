package com.example.sickar3;

import org.json.JSONObject;

import java.util.HashMap;

/**
 * Holds data from barcode queries. HashMap of barcode to JSON Objects
 */
class BarcodeData {
    public static HashMap<String, JSONObject> data;

    /**
     * call in onCreate method of MainActivity to initialize
     */
    public static void init() {
        data = new HashMap<>();
    }
}
