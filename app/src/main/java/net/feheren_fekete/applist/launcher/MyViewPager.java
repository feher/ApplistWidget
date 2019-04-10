package net.feheren_fekete.applist.launcher;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.MotionEventCompat;
import androidx.viewpager.widget.ViewPager;

import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class MyViewPager extends ViewPager {

    public interface OnUpListener {
        void onUp(MotionEvent event);
    }

    private boolean mIsInterceptingTouchEvents;
    private @Nullable GestureDetectorCompat mGestureDetector;
    private @Nullable OnUpListener mOnUpListener;

    public MyViewPager(Context context) {
        super(context);
    }

    public MyViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setInterceptingTouchEvents(boolean intercept,
                                           GestureDetector.SimpleOnGestureListener gestureListener,
                                           OnUpListener onUpListener) {
        mIsInterceptingTouchEvents = intercept;
        if (intercept) {
            mGestureDetector = new GestureDetectorCompat(getContext(), gestureListener);
            mOnUpListener = onUpListener;
        } else {
            mGestureDetector = null;
            mOnUpListener = null;
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
            if (mOnUpListener != null
                    && MotionEventCompat.getActionMasked(ev) == MotionEvent.ACTION_UP) {
                mOnUpListener.onUp(ev);
            }
            return true;
        } else {
            return super.onTouchEvent(ev);
        }
    }
}
