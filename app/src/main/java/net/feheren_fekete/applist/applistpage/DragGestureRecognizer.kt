package net.feheren_fekete.applist.applistpage

import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MotionEventCompat

class DragGestureRecognizer(private val callback: Callback,
                            touchOverlay: ViewGroup,
                            private val delegateView: View) {

    private var isDelegateEnabled = true
    private var isFingerDown: Boolean = false
    private var isDraggingItem: Boolean = false

    val fingerDownPos = PointF()
    var motionEvent: MotionEvent? = null
        private set

    private val touchOverlayListener = View.OnTouchListener { v, event ->
        motionEvent = event
        val action = MotionEventCompat.getActionMasked(event)
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                fingerDownPos.set(event.rawX, event.rawY)
                isFingerDown = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isFingerDown) {
                    if (callback.canDrag(this@DragGestureRecognizer) && !isDraggingItem) {
                        // Tell the parent ViewGroup to let us handle
                        // events from now on. Otherwise the parent would steal them
                        // for handling it's own mechanisms.
                        // For example, a ViewPager would steel side-swipe events.
                        (v as ViewGroup).requestDisallowInterceptTouchEvent(true)

                        if (callback.canStartDragging(this@DragGestureRecognizer)) {
                            isDraggingItem = true
                            callback.onStartDragging(this@DragGestureRecognizer)
                        }
                    }
                    if (isDraggingItem) {
                        callback.onDragging(this@DragGestureRecognizer)
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isDraggingItem) {
                    callback.onDrop(this@DragGestureRecognizer)
                    callback.onStopDragging(this@DragGestureRecognizer)
                }
                isFingerDown = false
                isDraggingItem = false
            }
            MotionEvent.ACTION_CANCEL -> {
                if (isDraggingItem) {
                    callback.onStopDragging(this@DragGestureRecognizer)
                }
                isFingerDown = false
                isDraggingItem = false
            }
        }
        if (!isDraggingItem) {
            if (isDelegateEnabled) {
                delegateView.dispatchTouchEvent(event)
            }
        }
        true
    }

    interface Callback {
        fun canDrag(gestureRecognizer: DragGestureRecognizer): Boolean
        fun canStartDragging(gestureRecognizer: DragGestureRecognizer): Boolean
        fun onStartDragging(gestureRecognizer: DragGestureRecognizer)
        fun onDragging(gestureRecognizer: DragGestureRecognizer)
        fun onDrop(gestureRecognizer: DragGestureRecognizer)
        fun onStopDragging(gestureRecognizer: DragGestureRecognizer)
    }

    init {
        touchOverlay.setOnTouchListener(touchOverlayListener)
    }

    fun setDelegateEnabled(enabled: Boolean) {
        isDelegateEnabled = enabled
    }

}
