package com.example.sickar3;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONObject;

/**
 * ViewModel for main activity
 */
public class DataViewModel extends AndroidViewModel {
    private MutableLiveData<BarcodeData> liveData;
    private MutableLiveData<String> errorData;

    public DataViewModel(@NonNull Application application) {
        super(application);
        liveData = new MutableLiveData<>();
        liveData.setValue(new BarcodeData());

        errorData = new MutableLiveData<>();
    }

    public MutableLiveData<BarcodeData> getLiveData() {
        return liveData;
    }

    public MutableLiveData<String> getErrorLiveData() {
        return errorData;
    }

    /**
     * Gets barcode data and issues network fetch if no such entry
     *
     * @param barcode, barcode
     * @return JSONObject data, null if no such entry
     */
    public Item getBarcodeItem(String barcode) {
        if (liveData.getValue() != null &&
                liveData.getValue().containsBarcode(barcode)) {
            return liveData.getValue().get(barcode);
        } else {
            // issue network call to get item
            fetchBarcodeData(barcode);
            return null;
        }
    }

    /**
     * Post data with the JSON response
     *
     * @param barcode,  barcode
     * @param response, JSONObject
     */
    public void putBarcodeItem(String barcode, JSONObject response) {
        BarcodeData d = liveData.getValue();
        if (d != null) {
            d.put(barcode, response);
        }
        liveData.postValue(d);
    }

    /**
     * Post data with Item
     */
    public void putBarcodeItem(String barcode, Item item) {
        BarcodeData d = liveData.getValue();
        if (d != null) {
            d.put(barcode, item);
        }
        liveData.postValue(d);
    }


    /**
     * Issue network request to fetch data
     *
     * @param barcode, barcode
     */
    public void fetchBarcodeData(String barcode) {
        NetworkRequest.sendRequest(this, getApplication(), barcode);
    }

    /**
     * Post error message
     *
     * @param error, String error message
     */
    public void putNetworkError(String error) {
        errorData.postValue(error);
    }

}
