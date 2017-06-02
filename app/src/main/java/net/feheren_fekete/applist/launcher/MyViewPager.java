package net.feheren_fekete.applist.launcher;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class MyViewPager extends ViewPager {

    private boolean mIsInterceptingTouchEvents;
    private @Nullable GestureDetectorCompat mGestureDetector;

    public MyViewPager(Context context) {
        super(context);
    }

    public MyViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setInterceptingTouchEvents(boolean intercept, GestureDetector.SimpleOnGestureListener gestureListener) {
        mIsInterceptingTouchEvents = intercept;
        if (intercept) {
            mGestureDetector = new GestureDetectorCompat(getContext(), gestureListener);
        } else {
            mGestureDetector = null;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mIsInterceptingTouchEvents) {
            return true;
        } else {
            return super.onInterceptTouchEvent(ev);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mIsInterceptingTouchEvents) {
            if (mGestureDetector != null) {
                mGestureDetector.onTouchEvent(ev);
            }
            return true;
        } else {
            return super.onTouchEvent(ev);
        }
    }
}
