package net.feheren_fekete.applist.applist;

import android.graphics.PointF;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class ApplistItemDragHandler {

    public interface Callback {
        boolean canDrag();
        boolean canStartDragging();
        void onStartDragging();
        void onDragging();
        void onDrop();
        void onStopDragging();
    }

    private Callback mCallback;
    private ViewGroup mTouchOverlay;
    private View mDelegateView;
    private boolean mIsFingerDown;
    private boolean mIsDraggingItem;
    private PointF mFingerDownPos = new PointF();
    private MotionEvent mMotionEvent;

    public ApplistItemDragHandler(Callback callback, ViewGroup touchOverlay, View delegateView) {
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
                        if (mCallback.canDrag() && !mIsDraggingItem) {
                            // Tell the parent ViewGroup to let us handle
                            // events from now on. Otherwise the parent would steal them
                            // for handling it's own mechanisms.
                            ((ViewGroup)v).requestDisallowInterceptTouchEvent(true);

                            if (mCallback.canStartDragging()) {
                                mIsDraggingItem = true;
                                mCallback.onStartDragging();
                            }
                        }
                        if (mIsDraggingItem) {
                            mCallback.onDragging();
                        }
                    }
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    if (mIsDraggingItem) {
                        mCallback.onDrop();
                        mCallback.onStopDragging();
                    }
                    mIsFingerDown = false;
                    mIsDraggingItem = false;
                    break;
                }
                case MotionEvent.ACTION_CANCEL: {
                    if (mIsDraggingItem) {
                        mCallback.onStopDragging();
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
