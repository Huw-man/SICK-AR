package com.example.sickar3;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds data from barcodes
 */
public class DataViewModel extends AndroidViewModel {
    private MutableLiveData<Map<String, JSONObject>> data;
    private String lastBarcode = "";

    public DataViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Map<String, JSONObject>> getData() {
        if (data == null) {
            data = new MutableLiveData<>();
            data.setValue(new HashMap<>());
        }
        return data;
    }

    /**
     *
     * @param key barcode
     * @return JSONObject data, null if no such entry
     */
    public JSONObject getBarcodeItem(String key) {
        if (!getData().getValue().containsKey(key)) {
            fetchBarcodeData(key);
        }
        return getData().getValue().get(key);
    }

    /**
     * Gets the last barcode updated into the MAP
     * @return JSONObject, the last item updated or null if no such item exists.
     */
    public JSONObject getLastItem() {
        if (!lastBarcode.isEmpty()) {
            return getBarcodeItem(lastBarcode);
        } else {
            return null;
        }
    }

    /**
     *
     * @param key, barcode
     * @param val, JSONObject
     * @return previous value associated with key, or null if there was no mapping
     */
    public JSONObject putBarcodeItem(String key, JSONObject val) {
        lastBarcode = key;
        return getData().getValue().put(key, val);
    }

    /**
     * Issue network request to fetch data
     * @param barcode, barcode
     */
    private void fetchBarcodeData(String barcode) {
        NetworkRequest.sendRequest(this, getApplication(), barcode);
    }



}
