package com.example.sickar.main;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.sickar.main.helpers.BarcodeData;
import com.example.sickar.main.helpers.Item;
import com.example.sickar.main.helpers.NetworkRequest;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * ViewModel for main activity
 */
public class DataViewModel extends AndroidViewModel {
    private MutableLiveData<BarcodeData> liveData;
    private MutableLiveData<String> errorData;
    private NetworkRequest networkRequest;
    private Set<String> currentRequests;

    public DataViewModel(@NonNull Application application) {
        super(application);
        liveData = new MutableLiveData<>();
        liveData.setValue(new BarcodeData());
        errorData = new MutableLiveData<>();
        networkRequest = new NetworkRequest(this.getApplication(), this);
        currentRequests = new HashSet<>();
    }

    MutableLiveData<BarcodeData> getLiveData() {
        return liveData;
    }

    MutableLiveData<String> getErrorLiveData() {
        return errorData;
    }

    /**
     * Gets barcode data and issues network fetch if no such entry
     *
     * @param barcode barcode
     * @return JSONObject data or null if no such entry
     */
    Item getBarcodeItem(String barcode) {
        if (getBarcodeData() != null &&
                getBarcodeData().containsBarcode(barcode)) {
            return getBarcodeData().get(barcode);
        } else {
            return null;
        }
    }

    /**
     * Post data with the JSON response
     *
     * @param barcode  barcode
     * @param response JSONObject
     */
    public void putBarcodeItem(String barcode, JSONObject response) {
        BarcodeData d = getBarcodeData();
        boolean enteredItem = false;
        if (d != null) {
            // check if response has data inside
            if (d.hasData(response)) {
                enteredItem = d.put(barcode, response);
                currentRequests.remove(barcode);
            } else {
                putError(barcode, "No data for this item: " + barcode);
            }
        }
        if (enteredItem) liveData.postValue(d);
    }

    /**
     * Post data with Item (deprecated)
     */
    void putBarcodeItem(String barcode, Item item) {
        BarcodeData d = getBarcodeData();
        if (d != null) {
            d.put(barcode, item);
        }
        liveData.postValue(d);
    }

    /**
     * Post error message
     *
     * @param error, String error message
     */
    public void putError(String barcode, String error) {
        // remove associated placeholder item if there is an error or no data
        currentRequests.remove(barcode);
        errorData.postValue(error);
    }

    public void addPicturesToItem(String barcode, JSONObject response) {
        BarcodeData d = getBarcodeData();
        if (d != null) {
            if (!d.addPictures(barcode, response)) {
                putError(barcode, "no item in cache or no pictures");
            }
        }
    }

    public CompletableFuture<Map> getTamperInfo(String barcode) {
        return networkRequest.sendTamperRequest(barcode);
    }

    boolean requestPending(String barcode) {
        return currentRequests.contains(barcode);
    }

    /**
     * Issue network request to fetch data
     *
     * @param barcode, barcode
     */
    void fetchBarcodeData(String barcode) {
        if (!currentRequests.contains(barcode)) {
            networkRequest.sendRequest(barcode);
            currentRequests.add(barcode);
        }
    }

    /**
     * Retrieve the barcode data contained in liveData
     *
     * @return BarcodeData
     */
    private BarcodeData getBarcodeData() {
        return liveData.getValue();
    }
}
