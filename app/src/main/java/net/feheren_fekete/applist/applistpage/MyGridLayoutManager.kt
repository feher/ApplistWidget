package net.feheren_fekete.applist.applistpage

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.util.DisplayMetrics

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

class MyGridLayoutManager : GridLayoutManager {
    private var context: Context? = null

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        this.context = context
    }

    constructor(context: Context, spanCount: Int) : super(context, spanCount) {
        this.context = context
    }

    constructor(context: Context, spanCount: Int, orientation: Int, reverseLayout: Boolean) : super(context, spanCount, orientation, reverseLayout) {
        this.context = context
    }

    override fun smoothScrollToPosition(recyclerView: RecyclerView,
                                        state: RecyclerView.State?, position: Int) {

        val smoothScroller = object : LinearSmoothScroller(context!!) {
            //This controls the direction in which smoothScroll looks
            //for your view
            override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
                return this@MyGridLayoutManager
                        .computeScrollVectorForPosition(targetPosition)
            }

            //This returns the milliseconds it takes to
            //scroll one pixel.
            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return MILLISECONDS_PER_INCH / displayMetrics.densityDpi
            }
        }

        smoothScroller.targetPosition = position
        startSmoothScroll(smoothScroller)
    }

    companion object {
        private const val MILLISECONDS_PER_INCH = 150f
    }
}
