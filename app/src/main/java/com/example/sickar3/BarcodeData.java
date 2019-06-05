package com.example.sickar3;

import org.json.JSONObject;

/**
 * Data class to hold Barcode data
 */
public class BarcodeData {
    private String barcode;
    private JSONObject json;
    private boolean containsData;


    public BarcodeData(String barcode, JSONObject json) {
        this.barcode = barcode;
        this.json = json;
        containsData = true;
    }

    public BarcodeData() {
        containsData = false;
    }

    public boolean containsData() {
        return containsData;
    }

    public String getBarcode() {
        return barcode;
    }

    public JSONObject getJson() {
        return json;
    }
}
