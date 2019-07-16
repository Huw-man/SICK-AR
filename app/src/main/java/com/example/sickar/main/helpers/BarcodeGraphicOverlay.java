package com.example.sickar.main.helpers;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Size;
import android.view.View;

import java.util.LinkedList;

public class BarcodeGraphicOverlay extends View {
    private static final String TAG = "app_" + BarcodeGraphicOverlay.class.getSimpleName();

    private Paint paint;
    private LinkedList<RectF> drawCache;
    private Size mCameraConfigSize;

    /**
     * Simple constructor to use when creating a view from code.
     *
     * @param context The Context the view is running in, through which it can
     *                access the current theme, resources, etc.
     */
    public BarcodeGraphicOverlay(Context context) {
        super(context);
        drawCache = new LinkedList<>();
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(8);
        paint.setStyle(Paint.Style.STROKE);
    }

    /**
     * Implement this to do your drawing.
     *
     * @param canvas the canvas on which the background will be drawn
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!drawCache.isEmpty()) {
            float cornerRadius = 25;
            for (RectF box : drawCache) {
                canvas.drawRoundRect(box, cornerRadius, cornerRadius, paint);
            }
            drawCache.clear();
        } else {
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        }
    }

    /**
     * Adds a new rectangle to the drawCache to be drawn on the next update
     * @param rect rectangle to be drawn
     */
    public void drawBoundingBox(Rect rect) {
        RectF rectF = new RectF(rect);
        // resize the bounding bos to be same ratio as the root view
        float viewWidth = this.getRootView().getWidth();
        float viewHeight = this.getRootView().getHeight();

        // get the true coordinates of this view to draw the bounding box according to the screen
        // not the view
        int[] trueXY = new int[2];
        this.getLocationOnScreen(trueXY);
//        Log.i(TAG, "true " + Arrays.toString(trueXY));
        rectF.left = rectF.left * viewWidth / mCameraConfigSize.getWidth() + trueXY[0];
        rectF.right = rectF.right * viewWidth / mCameraConfigSize.getWidth() + trueXY[0];
        rectF.top = rectF.top * viewHeight / mCameraConfigSize.getHeight() - trueXY[1];
        rectF.bottom = rectF.bottom * viewHeight / mCameraConfigSize.getHeight() - trueXY[1];

        drawCache.push(rectF);
    }

    /**
     * Set the proper size for the camera frame. This is used for converting coordinates
     * referencing the frame resolution to reference the view resolution.
     *
     * @param size        camera image size
     * @param orientation orientation
     */
    public void setCameraSize(Size size, int orientation) {
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            mCameraConfigSize = new Size(size.getHeight(), size.getWidth());
        } else {
            mCameraConfigSize = size;
        }
    }

    /**
     * clear the drawCache
     */
    public void clear() {
        drawCache.clear();
    }

    /**
     * Converts 4 points to and array of 16 floats
     *
     * @param pts points
     * @return floats
     */
    private float[] pointsToFloats(Point[] pts) {
        float[] fts = new float[16];
        int j = 2;
        for (int i = 0; i < pts.length; i++) {
            fts[j % 16] = fts[(j + 2) % 16] = pts[(i + 1) % 4].x;
            fts[(j + 1) % 16] = fts[(j + 3) % 16] = pts[(i + 1) % 4].y;
            j += 4;
        }
        return fts;
    }

    /**
     * Debug method to print the float array
     */
    private String printFts(float[] f) {
        StringBuilder s = new StringBuilder();
        for (float fts : f) {
            s.append(fts).append(",");
        }
        return s.toString();
    }
}
