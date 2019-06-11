package com.example.sickar3;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Data class to hold barcode information retrieved from server.
 * Uses a stack to keep track of  the order of retrieved
 * barcodes from latest to oldest.
 * Holds the data associated with a barcode in a MAP
 */
public class BarcodeData {

    private Stack<String> b_stack;
    private HashMap<String, JSONObject> data;

    public BarcodeData() {
        b_stack = new Stack<>();
        data = new HashMap<>();
    }

    public Boolean isEmpty() {
        return b_stack.isEmpty() || data.isEmpty();
    }

    public void put(String barcode, JSONObject response) {
        b_stack.push(barcode);
        data.put(barcode, response);
    }

    public JSONObject get(String barcode) {
        return data.get(barcode);
    }

    public String peekLatest() {
        return b_stack.peek();
    }

    public Boolean containsBarcode(String barcode) {
        return data.containsKey(barcode);
    }

    /**
     * returns the latest barcode data added
     */
    public JSONObject getLatest() {
        return get(peekLatest());
    }

    /**
     * returns the 1 based position where a barcode is in the stack.
     * (topmost item is at distance 1)
     * @param barcode
     * @return
     */
    public int search(String barcode) {
        return b_stack.search(barcode);
    }
}
