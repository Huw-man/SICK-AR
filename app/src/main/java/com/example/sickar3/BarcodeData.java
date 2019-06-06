package com.example.sickar3;

import org.json.JSONObject;

/**
 * Data class to hold Barcode data
 * safe to access data when isData is true otherwise data is null
 * Also pass error strings from network through here
 */
public class BarcodeData {
    private String barcode;
    private JSONObject json;
    private boolean containsData;
    private boolean containsError;

    public BarcodeData(String barcode, JSONObject json) {
        this.barcode = barcode;
        this.json = json;
        containsData = true;
        containsError = false;
    }

    public BarcodeData() {
        containsData = false;
        containsError = false;
    }

    public BarcodeData(String error) {
        this.barcode = error;
        containsData = false;
        containsError = true;
    }

    public boolean isData() {
        return containsData;
    }

    public boolean isError() {
        return containsError;
    }

    public boolean isNull() {
        return !containsData && !containsError;
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
