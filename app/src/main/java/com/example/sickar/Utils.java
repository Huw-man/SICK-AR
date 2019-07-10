package com.example.sickar;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;



/**
 * Utilities for SickAR
 */
public class Utils {
    private static final String TAG = "app_" + Utils.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     * <p>Finishes the activity if Sceneform can not run
     */
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }

    /**
     * calculates midpoint between two points.
     * Truncates ints if midpoint is not integral
     *
     * @param p1 Point 1
     * @param p2 Point 2
     * @return midpoint
     */
    public static Point midPoint(Point p1, Point p2) {
        return new Point((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
    }

    public static void animateRecyclerViewVisible(RecyclerView view) {
        view.setVisibility(RecyclerView.VISIBLE);
        TranslateAnimation animator = new TranslateAnimation(view.getWidth(), 0, 0, 0);
        animator.setDuration(500);
        view.startAnimation(animator);
    }

    public static void animateRecyclerViewGone(RecyclerView view) {
        TranslateAnimation animator = new TranslateAnimation(0, view.getWidth(), 0, 0);
        animator.setDuration(500);
        view.startAnimation(animator);
        view.setVisibility(RecyclerView.GONE);
    }

    /**
     * Displays an error in a Snackbar.
     * Appends the exception to the message if one it passed in.
     *
     * @param view    root view to display Snackbar
     * @param message error message
     * @param problem exception
     */
    public static void displayErrorSnackbar(View view, String message, @Nullable Throwable problem) {
        String displayMessage;
        if (problem == null) {
            displayMessage = message;
        } else {
            displayMessage = message + ": " + problem;
        }
//        Log.e("app_"+view.toString(), message);
        new Handler(Looper.getMainLooper()).post(() -> {
            Snackbar snkbr = Snackbar.make(view, displayMessage, Snackbar.LENGTH_SHORT);
            snkbr.show();
        });
        Log.i(TAG, displayMessage);
    }

    /**
     * Creates and shows a Toast containing an error message. If there was an exception passed in it
     * will be appended to the toast. The error will also be written to the Log
     */
    public static void displayErrorToast(
            final Context context, final String errorMsg, @Nullable final Throwable problem) {
        final String tag = context.getClass().getSimpleName();
        final String toastText;
        if (problem != null && problem.getMessage() != null) {
            Log.e(tag, errorMsg, problem);
            toastText = errorMsg + ": " + problem.getMessage();
        } else if (problem != null) {
            Log.e(tag, errorMsg, problem);
            toastText = errorMsg;
        } else {
            Log.e(tag, errorMsg);
            toastText = errorMsg;
        }

        new Handler(Looper.getMainLooper())
                .post(
                        () -> {
                            Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        });
    }

    /**
     * Default vibration, one pulse
     *
     * @param vibrator     vibrator
     * @param milliseconds time
     */
    public static void vibrate(Vibrator vibrator, long milliseconds) {
        vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    /**
     * Two pulse vibration for placing AR Card
     *
     * @param vibrator     vibrator
     */
    public static void vibrate2(Vibrator vibrator) {
        long[] pattern = {0, 100, 50, 100};
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
    }

    /**
     * Resize an Arraylist to a defined max size by
     * removing all elements at indices exceeding the maximum size
     *
     * @param list to be resized
     * @param maxSize size to be resized to
     */
    public static void resizeList(List list, int maxSize) {
        int size = list.size();
        if (size > maxSize) {
            for (int i = size - 1; i >= maxSize; i--) {
                list.remove(i);
            }
        }
    }
}
