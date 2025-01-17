package com.example.sickar.main.helpers;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.sickar.Constants;
import com.example.sickar.main.DataViewModel;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Class to get and send network requests. Currently this is only configured to send requests to
 * the SICK AR backend service.
 */
public class NetworkRequest {
    private static final String TAG = "app_" + NetworkRequest.class.getSimpleName();

    /**
     * Time in milliseconds to wait before a request should timeout
     */
    private static final int INITIAL_TIMEOUT_MS = 10000;

    /**
     * Maximum number of times to retry a request
     */
    private static final int MAX_NUM_RETRIES = 2;

    private RequestQueue queue;
    private DataViewModel model;

    /**
     * Construct an instance with context and a ViewModel
     *
     * @param context Context
     * @param model   ViewModel
     */
    public NetworkRequest(Context context, DataViewModel model) {
        queue = Volley.newRequestQueue(context);
        this.model = model;
    }

    /**
     * Creates the request json for sendRequestDirect(String barcode)
     *
     * @deprecated
     * @param barcode barcode
     * @return JSONObject containing the formulated request json
     */
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
            String startDate = ZonedDateTime.now().minusDays(Constants.SEARCH_DAYS)
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
     *
     * @deprecated
     * @param barcode barcode
     */
    void sendRequestDirect(String barcode) {
        final String url = "http://10.102.11.96:8080/search/execute?offset=0&size=1&locale=en-US";
//        final String url = "http://10.102.11.208:8080/fa/api/v1/search/execute?offset=0&size=1&locale=en-US";

        JSONObject requestJSON = createJson(barcode);
//        Log.i(TAG, requestJSON.toString());

        // create json request
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url,
                requestJSON,
                response -> { // on response listener
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
            model.putError(barcode, errormsg);
        });
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(5000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(jsonObjectRequest);
    }

    /**
     * Sends a request to the SICK AR backend services for data
     *
     * @param barcode barcode string
     */
    public void sendRequest(String barcode) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Constants.API_ENDPOINT + "get/" + barcode, null,
                response -> {
                    Log.i(TAG, "successfully received " + response.toString());
                    //                    Log.i(TAG, "network "+Thread.currentThread().toString());

                    model.putBarcodeItem(barcode, response);

                }, error -> postError(barcode, error));
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(INITIAL_TIMEOUT_MS, MAX_NUM_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(jsonObjectRequest);

    }

    /**
     * Sends a request to the SICK AR backend service for image data
     *
     * @param barcode item to get images for
     */
    public CompletableFuture<JSONObject> sendPictureRequest(String barcode) {
        CompletableFuture<JSONObject> result = new CompletableFuture<>();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Constants.API_ENDPOINT + "get_pictures/" + barcode, null,
                response -> {
                    Log.i(TAG, "received pictures" + response.toString());
                    // add received picture data to item
//                    model.addPicturesToItem(barcode, response);
                    result.complete(response);
                }, error -> postError(barcode, error));
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(INITIAL_TIMEOUT_MS, MAX_NUM_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(jsonObjectRequest);
        return result;
    }

    /**
     * Sends a request to the SICK AR backend service for tamper detection
     *
     * @param barcode barcode
     * @return Future that can be blocked for the result with .get
     */
    public CompletableFuture<Map> sendTamperRequest(String barcode) {
        CompletableFuture<Map> result = new CompletableFuture<>();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Constants.API_ENDPOINT + "tamper/" + barcode, null,
                response -> {
                    Log.i(TAG, "received tamper data" + response.toString());
                    Map respMap = new Gson().fromJson(response.toString(), HashMap.class);
                    result.complete(respMap);
                }, error -> postError(barcode, error));
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(INITIAL_TIMEOUT_MS, MAX_NUM_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(jsonObjectRequest);
        return result;
    }

    public CompletableFuture<JSONObject> sendSystemConfigRequest() {
        CompletableFuture<JSONObject> result = new CompletableFuture<>();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Constants.API_ENDPOINT + "get_system_config", null,
                response -> {
                    Log.i(TAG, "received system config" + response.toString());
                    result.complete(response);
                }, error -> model.putError("on fetching system config: " + error.toString()));
        queue.add(jsonObjectRequest);
        return result;
    }

    /**
     * Post a network error to the ViewModel
     *
     * @param error error
     */
    private void postError(String barcode, VolleyError error) {
        StringBuilder errorMsg = new StringBuilder();
        errorMsg.append(error.toString());
        if (error.networkResponse != null) {
            errorMsg.append(" status: ").append(error.networkResponse.statusCode);
        }
        Log.i(TAG, errorMsg.toString());
        model.putError(barcode, errorMsg.toString());
    }
}
