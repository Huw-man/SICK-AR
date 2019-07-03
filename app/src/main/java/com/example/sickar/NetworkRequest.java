package com.example.sickar;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;

/**
 * class to get and send network requests
 */
class NetworkRequest {
    private static final String TAG = "app_" + NetworkRequest.class.getSimpleName();
    private RequestQueue queue;
    private DataViewModel model;

    public NetworkRequest(Context context, DataViewModel model) {
        queue = Volley.newRequestQueue(context);
        this.model = model;

    }

    private static JSONObject createJson(String barcode) {
        // create json body to request with barcode
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("type", "byBarcode");
            JSONObject values = new JSONObject();
            values.put("systemName", JSONObject.NULL);
            values.put("systemGroupId", JSONObject.NULL);

            // construct query dates
            String endDate = ZonedDateTime.now()
                    .format(DateTimeFormatter.ISO_INSTANT);
            String startDate = ZonedDateTime.now().minusDays(7)
                    .format(DateTimeFormatter.ISO_INSTANT);

            values.put("startDate", startDate);
            values.put("endDate", endDate);
//            barcode = "9612850147114161000158";
            values.put("searchPattern", barcode);
            requestBody.put("values", values);
            requestBody.put("conditions", new JSONObject());
            return requestBody;
        } catch (JSONException e) {
            Log.i(TAG, "error creating json " + e.getMessage());
            return null;
        }
    }

    /**
     * Sends a request directly to the Sick AN services for data
     */
    public void sendRequestDirect(String barcode) {
        final String url = "http://10.102.11.96:8080/search/execute?offset=0&size=1&locale=en-US";
//        final String url = "http://10.102.11.208:8080/fa/api/v1/search/execute?offset=0&size=1&locale=en-US";

        JSONObject requestJSON = createJson(barcode);
//        Log.i(TAG, requestJSON.toString());

        // create json request
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url,
                requestJSON, response -> { // on response listener
                Log.i(TAG, "successfully received " + response.toString());
                model.putBarcodeItem(barcode, response);
            }, error -> { // on error listener
                String errormsg;
                if (error.networkResponse != null) {
                    errormsg = error.toString() + ", status code: " + error.networkResponse.statusCode;
                } else {
                    errormsg = error.toString();
                }
                Log.i(TAG, errormsg);
                model.putError(errormsg);
            });
            jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(5000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(jsonObjectRequest);
    }

    /**
     * Sends a request to the SickAR backend services for data
     */
    public void sendRequest(String barcode) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
            Constants.API_ENDPOINT + "get/" + barcode, null, response -> {
                Log.i(TAG, "successfully received " + response.toString());
            Executors.newSingleThreadExecutor().submit(() -> {
//                    Log.i(TAG, "network "+Thread.currentThread().toString());
                model.putBarcodeItem(barcode, response);
                sendPictureRequest(barcode);
            });
            }, error -> {
                String errormsg;
                if (error.networkResponse != null) {
                    errormsg = error.toString() + ", status code: " + error.networkResponse.statusCode;
                } else {
                    errormsg = error.toString();
                }
                Log.i(TAG, errormsg);
                model.putError(errormsg);
            });
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(5000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(jsonObjectRequest);

    }

    /**
     * Sends a request to the SickAR backend service for picture data
     *
     * @param barcode item to get pictures for
     */
    public void sendPictureRequest(String barcode) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Constants.API_ENDPOINT + "get_pictures/" + barcode, null, response -> {
            Log.i(TAG, "received pictures" + response.toString());
            Executors.newSingleThreadExecutor().submit(() -> {
                model.addPicturesToItem(barcode, response);
            });
        }, error -> {
            String errormsg;
            if (error.networkResponse != null) {
                errormsg = error.toString() + "while fetching pictures, status code: " + error.networkResponse.statusCode;
            } else {
                errormsg = error.toString();
            }
            Log.i(TAG, errormsg);
            model.putError(errormsg);
        });
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(5000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(jsonObjectRequest);
    }
}
