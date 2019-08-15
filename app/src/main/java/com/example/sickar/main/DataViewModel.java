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
 * ViewModel for main activity. This ViewModel abstracts away the cache from mainActivity and
 * contains LiveData objects which are updated by network responses in the background. Once a
 * LiveData object is updated its corresponding observers will be notified in the MainActivity to
 * update the appropriate views.
 */
public class DataViewModel extends AndroidViewModel {
    /**
     * LiveData that holds the BarcodeDataCache to update observers about newly requested items
     */
    private MutableLiveData<BarcodeDataCache> cacheData;

    /**
     * LiveData that holds the errors associated with network requests to display them in the
     * activity
     */
    private MutableLiveData<String> errorData;

    /**
     * LiveData that keeps track of all the items with network requests currently running. We should
     * not send a new requests for an item if one is already in progress
     */
    private MutableLiveData<Set<String>> currentRequestsData;
    private NetworkRequest networkRequest;

    // kept nonlocal for better UML diagram generation
    @SuppressWarnings("FieldCanBeLocal")
    private BarcodeDataCache barcodeDataCache = BarcodeDataCache.getInstance();

    /**
     * Set containing the names of all Items with network requests currently running.
     */
    private Set<String> currentRequests;

    /**
     * Construct this ViewModel
     *
     * @param application application
     */
    public DataViewModel(@NonNull Application application) {
        super(application);
        cacheData = new MutableLiveData<>();
        cacheData.setValue(barcodeDataCache);
        errorData = new MutableLiveData<>();
        networkRequest = new NetworkRequest(this.getApplication(), this);
        currentRequests = ConcurrentHashMap.newKeySet();
        currentRequestsData = new MutableLiveData<>();
        currentRequestsData.postValue(currentRequests);
        fetchSystemConfig();
    }

    /**
     * @return the cache LiveData
     */
    public MutableLiveData<BarcodeDataCache> getCacheData() {
        return cacheData;
    }

    /**
     * @return the error LiveData
     */
    public MutableLiveData<String> getErrorLiveData() {
        return errorData;
    }

    /**
     * Get the current requests that are awaiting responses
     * @return LiveData containing a set of currently running requests.
     */
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
     * Post data with Item
     *
     * @deprecated
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
     * Add pictures to an item object
     *
     * @deprecated
     * @param barcode  barcode
     * @param response json response
     */
    public void addPicturesToItem(String barcode, JSONObject response) {
        BarcodeDataCache d = getBarcodeData();
        if (d != null) {
            if (!d.addImages(barcode, response)) {
                putError(barcode, "no item in cache or no pictures");
            }
        }
    }

    /**
     * Get the CompletableFuture for loading images
     *
     * @param barcode barcode string
     * @return images future
     */
    public CompletableFuture<JSONObject> getPicturesForItem(String barcode) {
        return networkRequest.sendPictureRequest(barcode);
    }

    /**
     * Get the CompletableFuture for loading tamper information
     *
     * @param barcode barcode string
     * @return tamper information future
     */
    public CompletableFuture<Map> getTamperInfo(String barcode) {
        return networkRequest.sendTamperRequest(barcode);
    }

    /**
     * Check if there is a request pending for a particular barcode
     *
     * @param barcode barcode string
     * @return true if there is a request pending, false if not
     */
    boolean requestPending(String barcode) {
        return currentRequests.contains(barcode);
    }

    /**
     * Issue network request to fetch data
     *
     * @param barcode barcode
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

    /**
     * Issue request for the system configuration details. Used to get the devices configured
     * for images.
     */
    private void fetchSystemConfig() {
        CompletableFuture<JSONObject> response =
                networkRequest.sendSystemConfigRequest();
        response.thenAccept(jsonObject -> getBarcodeData().setSystemConfig(jsonObject));
    }
}
