package com.example.sickar.main.helpers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Size;
import android.view.View;

import java.util.LinkedList;

/**
 * Graphic Overlay draws the highlight box around detected barcodes and enables the reticle tool
 */
public class GraphicOverlay extends View {
    private static final String TAG = "app_" + GraphicOverlay.class.getSimpleName();

    private static final int ANIMATION_DURATION = 1000;
    private static final int START_ANGLE = 270;

    private LinkedList<RectF> drawCache;
    private Size cameraConfigSize;
    private int[] trueXY;
    private float angle;
    private RectF arcBox;
    private PointF center;
    private float arcRadius;
    private ValueAnimator animator;

    private Paint boxPaint;
    private Paint reticlePaint;
    private Paint arcPaint;
    private boolean reticleEnabled;

    /**
     * Simple constructor to use when creating a view from code.
     *
     * @param context The Context the view is running in, through which it can
     *                access the current theme, resources, etc.
     */
    public GraphicOverlay(Context context) {
        super(context);
        // get the true coordinates of this view to draw the bounding box according to the screen
        // not the view
        trueXY = new int[2];
        drawCache = new LinkedList<>();
        center = new PointF();

        // paints
        boxPaint = new Paint();
        boxPaint.setColor(Color.RED);
        boxPaint.setStrokeWidth(8);
        boxPaint.setStyle(Paint.Style.STROKE);

        reticlePaint = new Paint();
        reticlePaint.setColor(Color.WHITE);

        arcPaint = new Paint();
        arcPaint.setColor(Color.WHITE);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(5f);

        arcRadius = 40;
        arcBox = new RectF();

        reticleEnabled = false;

        // fill in values once the view is actually laid out
        this.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            trueXY[0] = left;
            trueXY[1] = top;
            center.x = (right - left) / 2f;
            center.y = (bottom - top) / 2f;
            arcBox.set(center.x - arcRadius,
                    center.y - arcRadius,
                    center.x + arcRadius,
                    center.y + arcRadius);
        });

        angle = 0;
        animator = ValueAnimator.ofFloat(0, 360);
        animator.addUpdateListener(animation -> angle = (float) animation.getAnimatedValue());
        animator.addListener(new AnimatorListenerAdapter() {
            /**
             * {@inheritDoc}
             *
             * @param animation
             */
            @Override
            public void onAnimationEnd(Animator animation) {
                angle = 0;
            }
        });
        animator.setDuration(ANIMATION_DURATION);
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
                canvas.drawRoundRect(box, cornerRadius, cornerRadius, boxPaint);
            }
            drawCache.clear();
        } else {
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        }

        if (reticleEnabled) {
            // draw reticle
            canvas.drawCircle(this.getWidth() / 2f, this.getHeight() / 2f, 5, reticlePaint);
            canvas.drawArc(arcBox, START_ANGLE, angle, false, arcPaint);
        }
    }

    /**
     * Set listener to handle touches.
     *
     * @param animatorListenerAdapter animatorListenerAdapter
     */
    public void setAnimatorListenerAdapter(AnimatorListenerAdapter animatorListenerAdapter) {
        animator.addListener(animatorListenerAdapter);
    }

    /**
     * Start the click animation for the reticle
     */
    public void startClickAnimation() {
        if (reticleEnabled) {
            animator.start();
        }
    }

    /**
     * Stop the click animation for the reticle
     */
    public void stopClickAnimation() {
        if (reticleEnabled) {
            animator.cancel();
        }
    }

    /**
     * Check if the reticle is animating
     *
     * @return true if the animation is running, false if not
     */
    public boolean isAnimating() {
        return animator.isRunning();
    }

    /**
     * Check if the reticle is enabled
     *
     * @return true is reticle is enabled, false if not
     */
    public boolean getReticleEnabled() {
        return reticleEnabled;
    }

    /**
     * Enable of disable the reticle
     *
     * @param reticleEnabled true to enable, false to disable
     */
    public void setReticleEnabled(boolean reticleEnabled) {
        this.reticleEnabled = reticleEnabled;
    }

    /**
     * Adds a new rectangle to the drawCache to be drawn on the next update
     *
     * @param rect rectangle to be drawn
     */
    public void drawBoundingBox(Rect rect) {
        RectF rectF = new RectF(rect);
        // resize the bounding bos to be same ratio as the root view
        float viewWidth = this.getRootView().getWidth();
        float viewHeight = this.getRootView().getHeight();

//        Log.i(TAG, "true " + Arrays.toString(trueXY));
        rectF.left = rectF.left * viewWidth / cameraConfigSize.getWidth() + trueXY[0];
        rectF.right = rectF.right * viewWidth / cameraConfigSize.getWidth() + trueXY[0];
        rectF.top = rectF.top * viewHeight / cameraConfigSize.getHeight() - trueXY[1];
        rectF.bottom = rectF.bottom * viewHeight / cameraConfigSize.getHeight() - trueXY[1];

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
            cameraConfigSize = new Size(size.getHeight(), size.getWidth());
        } else {
            cameraConfigSize = size;
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
     * @deprecated
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
