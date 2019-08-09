package com.example.sickar;

/**
 * Useful constants
 */
public class Constants {
    /**
     * number of itemd to hold in BarcodeDataCache cache and RecyclerView adapter
     */
    public static final int CACHE_SIZE = 10;

    /**
     * API endpoint that should direct to the SICK AR backend
     */
    public static final String API_ENDPOINT = "http://192.168.0.221:5000/";

    /**
     * How far to search in the past if using NetworkRequest.sendRequestDirect Now this parameter is
     * set in the backend
     *
     * @deprecated
     */
    public static final int SEARCH_DAYS = 14;

    /**
     * Labels for messages sent from the BarcodeProcessor to the Main Handler
     */
    public static final int BARCODE_READ_SUCCESS = 0;
    public static final int BARCODE_READ_FAILURE = 1;
    public static final int BARCODE_READ_EMPTY = 2;
    public static final int REQUEST_ISSUED = 4;
    public static final int REQUEST_PENDING = 5;
}
