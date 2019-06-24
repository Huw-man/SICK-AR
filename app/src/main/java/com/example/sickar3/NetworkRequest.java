package com.example.sickar3;

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

/**
 * class to get and send network requests
 */
class NetworkRequest {
    private static final String TAG = "app_" + NetworkRequest.class.getSimpleName();

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

    static void sendRequest(DataViewModel model, Context context, String barcode) {
        RequestQueue queue = Volley.newRequestQueue(context);
//        final String url = "http://10.102.11.208:8080/search/execute?offset=0&size=1&locale=en-US";
        final String url = "http://10.102.11.208:8080/fa/api/v1/search/execute?offset=0&size=1&locale=en-US";


        JSONObject requestJSON = createJson(barcode);
//        Log.i(TAG, requestJSON.toString());

        // create json request
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url,
                requestJSON, response -> { // on response listener
            Log.i(TAG, "successfully received " + response.toString());
            model.putBarcodeItem(barcode, response);
        }, error -> { // on error listener
            String errormsg = error.toString() + ", status code: " + error.networkResponse.statusCode;
            Log.i(TAG, errormsg);
            model.putError(errormsg);
        });
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(5000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(jsonObjectRequest);
    }
}
