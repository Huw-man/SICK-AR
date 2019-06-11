package com.example.sickar3;

import android.content.Context;
import android.os.NetworkOnMainThreadException;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModel;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * class to get and send network requests
 */
class NetworkRequest {

    private static JSONObject createJson(String barcode) {
        // create json body to request with barcode
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("type", "byBarcode");
            JSONObject values = new JSONObject();
            values.put("systemName", JSONObject.NULL);
            values.put("systemGroupId", JSONObject.NULL);
            values.put("startDate", "2019-06-01T18:01:00.000Z");
            values.put("endDate", "2019-06-11T18:01:59.999Z");
            //TODO: replace with barcode when ready to test
            barcode = "42127679183";
            values.put("searchPattern", barcode);
            requestBody.put("values", values);
            requestBody.put("conditions", new JSONObject());
            return requestBody;
        } catch (JSONException e) {
            Log.i("app_Request", "error creating json "+ e.getMessage());
            return null;
        }
    }

    static void sendRequest(DataViewModel model, Context context, String barcode) {
        RequestQueue queue = Volley.newRequestQueue(context);
        final String url = "http://10.102.11.96:8080/search/execute?offset=0&size=5&locale=en-US";

        JSONObject requestJSON = createJson(barcode);
        Log.i("app_request_json", requestJSON.toString());

        // create json request
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url,
                requestJSON, response -> { // on response listener
                    model.putBarcodeItem(barcode, response);
                    Log.i("app_Request", response.toString());
                }, error -> { // on error listener
                    Log.i("app_Request", "error " + error.toString() +" "+error.networkResponse);
                    model.putNetworkError(error.getMessage());
                });
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(50000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(jsonObjectRequest);
    }
}
