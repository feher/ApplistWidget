package net.feheren_fekete.applist.applistpage;

import android.graphics.PointF;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class DragGestureRecognizer {

    public interface Callback {
        boolean canDrag(DragGestureRecognizer gestureRecognizer);
        boolean canStartDragging(DragGestureRecognizer gestureRecognizer);
        void onStartDragging(DragGestureRecognizer gestureRecognizer);
        void onDragging(DragGestureRecognizer gestureRecognizer);
        void onDrop(DragGestureRecognizer gestureRecognizer);
        void onStopDragging(DragGestureRecognizer gestureRecognizer);
    }

    private Callback mCallback;
    private ViewGroup mTouchOverlay;
    private View mDelegateView;
    private boolean mIsFingerDown;
    private boolean mIsDraggingItem;
    private PointF mFingerDownPos = new PointF();
    private MotionEvent mMotionEvent;

    public DragGestureRecognizer(Callback callback, ViewGroup touchOverlay, View delegateView) {
        mCallback = callback;
        mTouchOverlay = touchOverlay;
        mDelegateView = delegateView;

        mTouchOverlay.setOnTouchListener(mTouchOverlayListener);
    }

    public MotionEvent getMotionEvent() {
        return mMotionEvent;
    }

    public PointF getFingerDownPos() {
        return mFingerDownPos;
    }

    private View.OnTouchListener mTouchOverlayListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mMotionEvent = event;
            int action = MotionEventCompat.getActionMasked(event);
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    mFingerDownPos.set(event.getRawX(), event.getRawY());
                    mIsFingerDown = true;
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (mIsFingerDown) {
                        if (mCallback.canDrag(DragGestureRecognizer.this) && !mIsDraggingItem) {
                            // Tell the parent ViewGroup to let us handle
                            // events from now on. Otherwise the parent would steal them
                            // for handling it's own mechanisms.
                            // For example, a ViewPager would steel side-swipe events.
                            ((ViewGroup)v).requestDisallowInterceptTouchEvent(true);

                            if (mCallback.canStartDragging(DragGestureRecognizer.this)) {
                                mIsDraggingItem = true;
                                mCallback.onStartDragging(DragGestureRecognizer.this);
                            }
                        }
                        if (mIsDraggingItem) {
                            mCallback.onDragging(DragGestureRecognizer.this);
                        }
                    }
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    if (mIsDraggingItem) {
                        mCallback.onDrop(DragGestureRecognizer.this);
                        mCallback.onStopDragging(DragGestureRecognizer.this);
                    }
                    mIsFingerDown = false;
                    mIsDraggingItem = false;
                    break;
                }
                case MotionEvent.ACTION_CANCEL: {
                    if (mIsDraggingItem) {
                        mCallback.onStopDragging(DragGestureRecognizer.this);
                    }
                    mIsFingerDown = false;
                    mIsDraggingItem = false;
                    break;
                }
            }
            if (!mIsDraggingItem) {
                mDelegateView.dispatchTouchEvent(event);
            }
            return true;
        }
    };

}
