package com.example.sickar3;

import org.json.JSONObject;

/**
 * Data class to hold Barcode data
 * safe to access data when containsData is true otherwise data is null
 * Also pass error strings from network through here
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

    public BarcodeData(String error) {
        this.barcode = error;
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

    public String getError() {
        return barcode;
    }
}
