package com.example.sickar3;

import android.content.Context;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.TranslateAnimation;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;


import javax.annotation.Nullable;



/**
 * Utilities for SickAR
 */
public class Utils {

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
     * @param view root view to display Snackbar
     * @param message error message
     * @param problem exception
     */
    public static void displayErrorSnackbar(View view, String message, @Nullable Throwable problem) {
        String displayMessage;
        if (problem == null) {
            displayMessage = message;
        } else {
            displayMessage = message + "; " + problem;
        }
//        Log.e("app_"+view.toString(), message);
        new Handler(Looper.getMainLooper()).post(() -> {
            Snackbar snkbr = Snackbar.make(view, displayMessage, Snackbar.LENGTH_SHORT);
            snkbr.show();
        });
    }
}
