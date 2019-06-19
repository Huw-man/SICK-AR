package com.example.sickar3;

import android.graphics.Point;
import android.view.animation.TranslateAnimation;

import androidx.recyclerview.widget.RecyclerView;

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
        return new Point((p1.x + p2.x) / 2, (p1.y + p2.y) / 2 );
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
}
