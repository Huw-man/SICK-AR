package com.example.sickar.libs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

/**
 * Simple Viewpager that cannot be swiped.
 */
public class SwipeDisabledViewPager extends ViewPager {

    /**
     * Construct a SwipeDisabledViewPager
     *
     * @param context Context
     * @param attrs   AttributeSet
     */
    public SwipeDisabledViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Called on the dispatch of a touch event.
     *
     * @param ev MotionEvent
     * @return false because we do not consume the motion event
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

    /**
     * Called on the reception of a touch event.
     *
     * @param ev MotionEvent
     * @return false because we do not consume the motion event
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return false;
    }
}
