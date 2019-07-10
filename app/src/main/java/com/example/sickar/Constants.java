package com.example.sickar;

public class Constants {
    // number of item to hold in BarcodeData cache and RecyclerView adapter
    public static final int CACHE_SIZE = 10;
    public static final String API_ENDPOINT = "http://192.168.0.221:5000/";
    public static final int SEARCH_DAYS = 14;

    // constants for message.what to label certain events
    static final int BARCODE_READ_SUCCESS = 0;
    static final int BARCODE_READ_FAILURE = 1;
    static final int BARCODE_READ_EMPTY = 2;
    static final int REQUEST_ISSUED = 4;
    static final int REQUEST_PENDING = 5;
}
