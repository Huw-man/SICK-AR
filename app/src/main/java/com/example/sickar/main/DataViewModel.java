package com.example.sickar.main;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.sickar.main.helpers.BarcodeDataCache;
import com.example.sickar.main.helpers.Item;
import com.example.sickar.main.helpers.NetworkRequest;

import org.json.JSONObject;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ViewModel for main activity
 */
public class DataViewModel extends AndroidViewModel {
    private MutableLiveData<BarcodeDataCache> cacheData;
    private MutableLiveData<String> errorData;
    private MutableLiveData<Set<String>> currentRequestsData;
    private NetworkRequest networkRequest;
    private Set<String> currentRequests;

    public DataViewModel(@NonNull Application application) {
        super(application);
        cacheData = new MutableLiveData<>();
        cacheData.setValue(BarcodeDataCache.getInstance());
        errorData = new MutableLiveData<>();
        networkRequest = new NetworkRequest(this.getApplication(), this);
        currentRequests = ConcurrentHashMap.newKeySet();
        currentRequestsData = new MutableLiveData<>();
        currentRequestsData.postValue(currentRequests);
        fetchSystemConfig();
    }

    public MutableLiveData<BarcodeDataCache> getCacheData() {
        return cacheData;
    }

    public MutableLiveData<String> getErrorLiveData() {
        return errorData;
    }

    public MutableLiveData<Set<String>> getCurrentRequestsData() {
        return currentRequestsData;
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
        BarcodeDataCache d = getBarcodeData();
        boolean enteredItem = false;
        if (d != null) {
            // check if response has data inside
            if (d.hasData(response)) {
                enteredItem = d.put(barcode, response);
                currentRequests.remove(barcode);
                currentRequestsData.postValue(currentRequests);
            } else {
                putError(barcode, "No data for this item: " + barcode);
            }
        }
        if (enteredItem) cacheData.postValue(d);
    }

    /**
     * Post data with Item (deprecated)
     */
    void putBarcodeItem(String barcode, Item item) {
        BarcodeDataCache d = getBarcodeData();
        if (d != null) {
            d.put(barcode, item);
        }
        cacheData.postValue(d);
    }

    /**
     * Post error message
     *
     * @param barcode barcode
     * @param error error String error message
     */
    public void putError(String barcode, String error) {
        // remove associated placeholder item if there is an error or no data
        currentRequests.remove(barcode);
        currentRequestsData.postValue(currentRequests);
        errorData.postValue(error);
    }

    /**
     * post a single error message
     *
     * @param message message
     */
    public void putError(String message) {
        errorData.postValue(message);
    }

    /**
     * add pictures to an item object
     *
     * @param barcode  barcode
     * @param response json response
     */
    public void addPicturesToItem(String barcode, JSONObject response) {
        BarcodeDataCache d = getBarcodeData();
        if (d != null) {
            if (!d.addPictures(barcode, response)) {
                putError(barcode, "no item in cache or no pictures");
            }
        }
    }

    public CompletableFuture<JSONObject> getPicturesForItem(String barcode) {
        return networkRequest.sendPictureRequest(barcode);
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
     * Retrieve the barcode data contained in cacheData
     *
     * @return BarcodeDataCache
     */
    private BarcodeDataCache getBarcodeData() {
        return cacheData.getValue();
    }

    private void fetchSystemConfig() {
        CompletableFuture<JSONObject> response =
                networkRequest.sendSystemConfigRequest();
        response.thenAccept(jsonObject -> getBarcodeData().setSystemConfig(jsonObject));
    }
}
