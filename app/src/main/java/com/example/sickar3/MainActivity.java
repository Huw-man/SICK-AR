package com.example.sickar3;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProviders;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.PixelCopy;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.DeadlineExceededException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.ResourceExhaustedException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {
    private ArFragment fragment;
    protected Session arSession;
    private TextView mBarcodeInfo;
    private ProgressBar progressBar;
    private DataViewModel mDataModel;
    private AtomicInteger live;
    // indicates whether or not a barcode scan is currently happening so the next frame to process is not issued until on barcode scan is done.
    // 0 no process running, 1 process running

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // start ar fragment
        fragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        try {
            live = new AtomicInteger(0);
            fragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdate);
        } catch (NullPointerException e) {
            Log.i("app_onUpdate", e.getLocalizedMessage());
        }
        fragment.getPlaneDiscoveryController().hide();
        fragment.getPlaneDiscoveryController().setInstructionView(null);


        // barcode info display
        mBarcodeInfo = findViewById(R.id.barcode_info);

        // progressBar
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(ProgressBar.GONE);

        // bind buttons
//        findViewById(R.id.photo_button).setOnClickListener(view -> takePhoto());
        findViewById(R.id.barcode_info_toggle_button).setOnClickListener(view -> toggleDisplay());

        // start data class
        mDataModel = ViewModelProviders.of(this).get(DataViewModel.class);
        mDataModel.getData().observe(this, barcodeData -> { //onChanged listener
            Log.i("app_onChanged", "data model changed");
            try {
                if (barcodeData.containsData()) {
                    mBarcodeInfo.setText(barcodeData.getJson().toString(2));
                } else {
                    // error data
                    mBarcodeInfo.setText(barcodeData.getError());
                }
            } catch (JSONException e) {
                Log.i("app_JSON exception", e.getMessage());
            }
            live.set(0);
            progressBar.setVisibility(ProgressBar.GONE);
        });

        // set barcodeInfo TextView to maintain information across configuration changes
        if (mDataModel.getData().getValue().containsData()) {
            mBarcodeInfo.setText(mDataModel.getData().getValue().getJson().toString());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        live.set(0);
        if (arSession == null) {
            try {
                arSession = new Session(this);
                setupAutoFocus(arSession);
            } catch (Exception e) {
                Log.e("Exception", "arSession failed to create " + e.getMessage());
            }
        }
    }

    @Override
    protected  void onPause() {
        super.onPause();
        // pause the background scanning and don't issue requests
        live.set(1);
    }

    /**
     * Toggle the display of barcode information
     */
    private void toggleDisplay() {
        if (mBarcodeInfo.getVisibility() == TextView.GONE) {
            mBarcodeInfo.setVisibility(TextView.VISIBLE);
        } else {
            mBarcodeInfo.setVisibility(TextView.GONE);
        }
    }

    /**
     * Sets the ArFragment to use autofocus with camera.
     *
     * @param arSession ArCore Session for this application
     */
    private void setupAutoFocus(Session arSession) {
        Config arConfig = new Config(arSession);

        if (arConfig.getFocusMode() == Config.FocusMode.FIXED) {
            arConfig.setFocusMode(Config.FocusMode.AUTO);
        }

        arConfig.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);

        arSession.configure(arConfig);

        fragment.getArSceneView().setupSession(arSession);

        Log.i("app_CAMERA", "The camera is current in focus mode " + arConfig.getFocusMode().name());
    }

    /**
     * run live barcode detection on frames
     * attached to Scene as OnUpdateListener
     */
    private void onUpdate(FrameTime frameTime) {
        if (live.get() == 0) { // no process running so we can issue another one
            live.set(1);
            ArSceneView view = fragment.getArSceneView();
            // check for valid view (I think problems happen during app startup and the view is not ready yet)
            if (view.getWidth() > 0 && view.getHeight() > 0) {
                //Create bitmap of the sceneview
                final Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                        Bitmap.Config.ARGB_8888);

                final HandlerThread handlerThread = new HandlerThread("PixelCopier");
                handlerThread.start();
                PixelCopy.request(view, bitmap, (copyResult -> {
                    if (copyResult == PixelCopy.SUCCESS) {
//                        Log.i("app_PICTURE", "picture taken");
                        runBarcodeScanner(bitmap);
                    } else {
                        Log.i("app_PICTURE", "picture failed");
                        live.set(0);
                    }
                    handlerThread.quitSafely();
                }), new Handler(handlerThread.getLooper()));
            } else {
                live.set(0);
            }
        }
    }

    /**
     * Takes a bitmap image and reads the barcode with FirebaseVisionBarcodeDetector
     * Todo: make this part of ViewModel?
     * @param bitmap bitmap image passed from takePhoto()
     */
    private void runBarcodeScanner(Bitmap bitmap) {
        // configure Barcode scanner
        FirebaseVisionBarcodeDetectorOptions options = new FirebaseVisionBarcodeDetectorOptions.Builder().setBarcodeFormats(
                // detect all barcode formats. improve performance by specifying which barcode formats to detect
                FirebaseVisionBarcode.FORMAT_ALL_FORMATS
//                ,FirebaseVisionBarcode.FORMAT_CODE_128
        ).build();

        // create FirebaseVisionImage
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);

        // get an instance of detector
        FirebaseVisionBarcodeDetector detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options);

        // detect barcodes
        Task<List<FirebaseVisionBarcode>> result = detector.detectInImage(image)
                .addOnSuccessListener(barcodes -> {
                    if (barcodes.isEmpty()) {// no barcodes read
                        progressBar.setVisibility(ProgressBar.GONE);
                        live.set(0);
                    } else {
                        progressBar.setVisibility(ProgressBar.VISIBLE);
                        // Task completed successfully
                        for (FirebaseVisionBarcode barcode : barcodes) {
                            Rect bounds = barcode.getBoundingBox();

                            String rawValue = barcode.getRawValue();

                            int valueType = barcode.getValueType();

                            Toast.makeText(getApplicationContext(), rawValue, Toast.LENGTH_SHORT).show();
                            Log.i("app_PICTURE_BARCODE", "detected: " + rawValue);
                            mDataModel.fetchBarcodeData(rawValue);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Task failed with an exception
                    progressBar.setVisibility(ProgressBar.GONE);
                    Toast.makeText(getApplicationContext(), "Sorry, something went wrong!", Toast.LENGTH_SHORT).show();
                    Log.i("app_PICTURE_NOREAD", "barcode not read with exception " + e.getMessage());
                });
    }

    private void drawBoundingBox(Rect boundingBox) {
        Canvas canvas = new Canvas();
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        canvas.drawRect(boundingBox, paint);
        fragment.getArSceneView().getRootView().onDrawForeground(canvas);
    }
}
