package net.feheren_fekete.applist.launcherpage;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;

import net.feheren_fekete.applist.R;

public class MyAppWidgetHostView extends AppWidgetHostView {

    public static final int STATE_NORMAL = 0;
    public static final int STATE_SELECTED = 1;
    public static final int STATE_RESIZING = 2;

    private final int mWidgetBorderWidth;
    private Paint mPaint = new Paint();
    private int mState;
    private GestureDetectorCompat mGestureDetector;

    public MyAppWidgetHostView(Context context) {
        super(context);
        mWidgetBorderWidth = getContext().getResources().getDimensionPixelSize(R.dimen.widget_border_width_draw);
    }

    public MyAppWidgetHostView(Context context, int animationIn, int animationOut) {
        super(context, animationIn, animationOut);
        mWidgetBorderWidth = getContext().getResources().getDimensionPixelSize(R.dimen.widget_border_width_draw);
    }

    public void setGestureListener(GestureDetector.SimpleOnGestureListener gestureListener) {
        mGestureDetector = new GestureDetectorCompat(getContext(), gestureListener);
    }

    public void setState(int state) {
        mState = state;
        invalidate();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean handled = mGestureDetector.onTouchEvent(ev);
        handled |= super.dispatchTouchEvent(ev);
        return handled;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mState != STATE_NORMAL) {
            mPaint.setStyle(Paint.Style.STROKE);

            final int offset = mWidgetBorderWidth;

            mPaint.setColor(0x99000000);
            mPaint.setStrokeWidth(mWidgetBorderWidth * 2);
            canvas.drawRect(
                    0 + offset,
                    0 + offset,
                    canvas.getWidth() - offset,
                    canvas.getHeight() - offset,
                    mPaint);

            mPaint.setColor(mState == STATE_SELECTED ? Color.GRAY : Color.WHITE);
            mPaint.setStrokeWidth(mWidgetBorderWidth);
            canvas.drawRect(
                    0 + offset,
                    0 + offset,
                    canvas.getWidth() - offset,
                    canvas.getHeight() - offset,
                    mPaint);
        }
    }

}
