package com.example.sickar;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONObject;

import java.util.Map;

/**
 * ViewModel for main activity
 */
public class DataViewModel extends AndroidViewModel {
    private MutableLiveData<BarcodeData> liveData;
    private MutableLiveData<String> errorData;
    private NetworkRequest networkRequest;

    public DataViewModel(@NonNull Application application) {
        super(application);
        liveData = new MutableLiveData<>();
        liveData.setValue(new BarcodeData());
        errorData = new MutableLiveData<>();
        networkRequest = new NetworkRequest(this.getApplication(), this);
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
            // check if response has data inside
            if (d.hasData(response)) {
                d.put(barcode, response);
            } else {
                putError("No data for this item: " + barcode);
            }
        }
        liveData.postValue(d);
    }

    /**
     * Post data with Item (deprecated)
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
        networkRequest.sendRequest(barcode);
    }

    /**
     * Post error message
     *
     * @param error, String error message
     */
    public void putError(String error) {
        errorData.postValue(error);
    }

    public void addPicturesToItem(String barcode, JSONObject response) {
        BarcodeData d = liveData.getValue();
        if (d != null) {
            if (!d.addPictures(barcode, response)) {
                putError("no item in cache or no pictures");
            }
        }
    }

    /**
     * returns the picture data as a map from device id to picture
     *
     * @param barcode
     * @return
     */
    public Map<String, String> getPictures(String barcode) {
        BarcodeData d = liveData.getValue();
        if (d != null && d.containsBarcode(barcode) && d.get(barcode).hasPictures()) {
            Item itm = d.get(barcode);
            itm.setSystem("1");
            return itm.getPictureData();
        } else if (d != null && d.containsBarcode(barcode) && !d.get(barcode).hasPictures()) {
            return null;
        }
        return null;

    }

}
