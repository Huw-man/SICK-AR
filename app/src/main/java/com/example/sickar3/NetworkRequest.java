package com.example.sickar3;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

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
public class NetworkRequest {
    private static JSONObject createJson(String barcode) {
        // create json body to request with barcode
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("type", "byBarcode");
            JSONObject values = new JSONObject();
            values.put("systemName", JSONObject.NULL);
            values.put("systemGroupId", JSONObject.NULL);
            values.put("startDate", "2019-05-21T18:01:00.000Z");
            values.put("endDate", "2019-05-29T18:01:59.999Z");
            //TODO: replace with barcode when ready to test
            values.put("searchPattern", "9611019741449370121357");
            requestBody.put("values", values);
            requestBody.put("conditions", new JSONObject());
            return requestBody;
        } catch (JSONException e) {
            Log.i("app_Request", "error creating json "+ e.getMessage());
            return null;
        }
    }

    public static void sendRequest(Context context, String barcode) {
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "http://10.102.11.96:8080/search/execute?offset=0&size=100&locale=en-US";

        JSONObject requestJSON = createJson(barcode);
        Log.i("app_Json", requestJSON.toString());

        // create json request
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url,
                requestJSON, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Toast.makeText(context, "success! "+ response.toString(), Toast.LENGTH_SHORT).show();
                Log.i("app_Request", response.toString());
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO: Handle error
                Log.i("app_Request", "error " + error.toString() +" "+error.networkResponse);
            }
        });
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(50000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(jsonObjectRequest);
    }
}
