package com.example.sickar3;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * ViewModel for main activity
 */
public class DataViewModel extends AndroidViewModel {
    private MutableLiveData<BarcodeData> data;

    public DataViewModel(@NonNull Application application) {
        super(application);
    }

    public MutableLiveData<BarcodeData> getData() {
        if (data == null) {
            data = new MutableLiveData<>();
            data.setValue(new BarcodeData());
        }
        return data;
    }

    /**
     * Issue network fetch if no such entry
     * @param key, barcode
     * @return JSONObject data, null if no such entry
     */
    public JSONObject getBarcodeItem(String key) {
        if (getData().getValue().containsData()) {
            return getData().getValue().getJson();
        } else {
            fetchBarcodeData(key);
            return null;
        }
    }

    /**
     *
     * @param key, barcode
     * @param val, JSONObject
     */
    public void putBarcodeItem(String key, JSONObject val) {
        getData().postValue(new BarcodeData(key, val));
    }


    /**
     * Issue network request to fetch data
     * @param barcode, barcode
     */
    public void fetchBarcodeData(String barcode) {
        NetworkRequest.sendRequest(this, getApplication(), barcode);
    }

    /**
     * Post error message
     * @param error, String error message
     */
    public void putNetworkError(String error) {
        getData().postValue(new BarcodeData(error));
    }

}
