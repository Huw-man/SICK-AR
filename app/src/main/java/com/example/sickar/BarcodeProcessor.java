package com.example.sickar;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import static android.content.Context.CAMERA_SERVICE;

/**
 * Runs the background barcode process.
 * There is only ever one instance of this class so all barcode processing threads
 * are spawned from the same thread pool. Access the instance of this class using
 * BarcodeProcessor.getInstance()
 */
class BarcodeProcessor {
    private static final String TAG = "app_" + BarcodeProcessor.class.getSimpleName();

    /**
     * Status indicators used by the messages sent to the Handler in this class
     */
    private static final int READ_FAILED = 0;
    private static final int READ_SUCCESS = 1;
    private static final int READ_EMPTY = 2;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static BarcodeProcessor sInstance;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
        sInstance = new BarcodeProcessor();
    }

    private final LinkedBlockingDeque<FirebaseVisionImage> mFrameStack;
    private FirebaseVisionBarcodeDetector mDetector;
    private BarcodeGraphicOverlay mOverlay;
    private boolean running;
    private Handler mHandler;
    private int mRotation;
    /**
     * Dedicated thread and associated runnable to run the barcode detector
     */
    private ExecutorService mThreadPool;

    /**
     * Initialize the FirebaseVisionBarcodeDetector, frame stack, and Handler
     */
    private BarcodeProcessor() {
        //                FirebaseVisionBarcode.FORMAT_ALL_FORMATS,
        FirebaseVisionBarcodeDetectorOptions mOptions = new FirebaseVisionBarcodeDetectorOptions.Builder().setBarcodeFormats(
//                FirebaseVisionBarcode.FORMAT_ALL_FORMATS,
                FirebaseVisionBarcode.FORMAT_CODE_128
        ).build();
        mDetector = FirebaseVision.getInstance().getVisionBarcodeDetector(mOptions);
        mFrameStack = new LinkedBlockingDeque<>(10);
        // requires a minimum of 2 threads because 1 thread will be held
        // be the detection process and another is used to execute the listeners
        mThreadPool = Executors.newFixedThreadPool(2);

        /*
          The handler must be run on the main thread to update views.
          This constructor will be invoked when this class is first referenced which
          should be on the main thread.
         */
        mHandler = new Handler(Looper.getMainLooper()) {
            /**
             * Handle messages sent from the BarcodeProcessRunnable upon completion
             * of barcode detection on a single frame.
             *
             * @param msg new message to process
             */
            @Override
            public void handleMessage(Message msg) {
                if (mOverlay != null) {
                    switch (msg.what) {
                        case READ_SUCCESS:
                            Rect box = msg.getData().getParcelable("boundingBox");
                            mOverlay.drawBoundingBox(box);
                            mOverlay.invalidate();
                            break;
                        case READ_EMPTY:
                            mOverlay.clear();
                            mOverlay.invalidate();
                            break;
                        case READ_FAILED:
                            String errMsg = msg.getData().getString("error");
                            Utils.displayErrorSnackbar(mOverlay.getRootView(), errMsg, null);
                            break;
                    }
                }
            }
        };
    }

    /**
     * Returns a reference to the instance of BarcodeProcessor
     *
     * @return BarcodeProcessor
     */
    static BarcodeProcessor getInstance() {
        return sInstance;
    }

    /**
     * Get the angle by which an image must be rotated given the device's current
     * orientation.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    static int getRotationCompensation(String cameraId, Activity activity, Context context)
            throws CameraAccessException {
        // Get the device's current mRotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's mRotation.
        int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int rotationCompensation = ORIENTATIONS.get(deviceRotation);

        // On most devices, the sensor orientation is 90 degrees, but for some
        // devices it is 270 degrees. For devices with a sensor orientation of
        // 270, rotate the image an additional 180 ((270 + 270) % 360) degrees.
        CameraManager cameraManager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
        @SuppressWarnings("ConstantConditions") int sensorOrientation = cameraManager
                .getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.SENSOR_ORIENTATION);
        rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360;

        // Return the corresponding FirebaseVisionImageMetadata mRotation value.
        int result;
        switch (rotationCompensation) {
            case 0:
                result = FirebaseVisionImageMetadata.ROTATION_0;
                break;
            case 90:
                result = FirebaseVisionImageMetadata.ROTATION_90;
                break;
            case 180:
                result = FirebaseVisionImageMetadata.ROTATION_180;
                break;
            case 270:
                result = FirebaseVisionImageMetadata.ROTATION_270;
                break;
            default:
                result = FirebaseVisionImageMetadata.ROTATION_0;
                Log.e(TAG, "Bad mRotation value: " + rotationCompensation);
        }
        return result;
    }

    /**
     * Start the barcode process by adding the BarcodeProcessRunnable to the thread pool
     */
    void start() {
        running = true;
        mThreadPool.execute(new BarcodeProcessRunnable());
    }

    /**
     * Stop the barcode runnable process and shutdown the thread pool
     */
    void stop() {
        running = false;
        mThreadPool.shutdownNow();
    }

    /**
     * Sets a reference to the BarcodeGraphicOverlay which will display the bounding boxes
     * of detected barcodes.
     *
     * @param overlay BarcodeGraphicOverlay
     */
    void setGraphicOverlay(BarcodeGraphicOverlay overlay) {
        mOverlay = overlay;
    }

    /**
     * Adds a new frame to the stack and makes sure there is room for this new frame by
     * removing old frames from the bottom of the stack
     *
     * @param frame new frame
     */
    void pushFrame(Image frame) {
//        while (!mFrameStack.offerFirst(frame)) {
//            mFrameStack.removeLast();
//        }
        FirebaseVisionImage fvImage = FirebaseVisionImage.fromMediaImage(frame, mRotation);
        frame.close();
        if (!mFrameStack.offerFirst(fvImage)) {
            mFrameStack.removeLast();
            mFrameStack.offerFirst(fvImage);
        }
    }

    void setRotation(int r) {
        mRotation = r;
    }

    /**
     * Runnable for processing the contents of a frame
     */
    private class BarcodeProcessRunnable implements Runnable {
        /**
         * Pops a frame off the top of frameStack and runs barcode detection on it.
         */
        @Override
        public void run() {
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            while (running) {
                try {
                    FirebaseVisionImage image = mFrameStack.takeFirst();
                    detect(image, mRotation);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Runs detectInImage on the detector and attaches listeners
         *
         * @param image    FirebaseVisionImage
         * @param rotation rotation compensation
         */
        private void detect(FirebaseVisionImage image, int rotation) {
            mDetector.detectInImage(image)
                    .addOnSuccessListener(mThreadPool, firebaseVisionBarcodes -> {
                        if (firebaseVisionBarcodes.isEmpty()) {
                            Message msg = mHandler.obtainMessage(READ_EMPTY);
                            msg.sendToTarget();
                        }
                        for (FirebaseVisionBarcode barcodes : firebaseVisionBarcodes) {
                            Bundle data = new Bundle();
                            data.putParcelable("boundingBox", barcodes.getBoundingBox());

                            Message msg = mHandler.obtainMessage(READ_SUCCESS);
                            msg.setData(data);
                            msg.sendToTarget();
                        }
                    }).addOnFailureListener(mThreadPool, e -> {
                Bundle data = new Bundle();
                data.putString("error", "error reading frame: " + e.toString());

                Message msg = mHandler.obtainMessage(READ_FAILED);
                msg.setData(data);
                msg.sendToTarget();
            });
        }
    }
}
