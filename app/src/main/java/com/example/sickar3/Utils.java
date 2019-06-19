package com.example.sickar3;

import android.graphics.Point;

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
}
