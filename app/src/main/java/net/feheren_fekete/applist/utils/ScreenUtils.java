package net.feheren_fekete.applist.utils;


import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;

public class ScreenUtils {

    private Point mScreenSize = new Point();
    private Point mScreenSizeDp = new Point();
    private int mStatusBarHeight = -1;
    private int mNavigationBarHeight = -1;
    private int mHasNavigationBar = -1;
    private TypedValue mTypedValue;

    public int getScreenWidth(Context context) {
        getDisplay(context).getSize(mScreenSize);
        return mScreenSize.x;
    }

    public Point getScreenSize(Context context) {
        getDisplay(context).getSize(mScreenSize);
        return mScreenSize;
    }

    public Point getScreenSizeDp(Context context) {
        final Point screenSize = getScreenSize(context);
        mScreenSizeDp.set(
                Math.round(pxToDp(screenSize.x)),
                Math.round(pxToDp(screenSize.y)));
        return mScreenSizeDp;
    }

    public void setStatusBarTranslucent(Activity activity, boolean makeTranslucent) {
        if (makeTranslucent) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        } else {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    public int getStatusBarHeight(Context context) {
        if (mStatusBarHeight == -1) {
            int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                mStatusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
            } else {
                mStatusBarHeight = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        ? Math.round(dpToPx(24))
                        : Math.round(dpToPx(25));
            }
        }
        return mStatusBarHeight;
    }

    public int getActionBarHeight(Context context) {
        // Calculate ActionBar height
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(
                    tv.data, getDisplayMetrics());
        } else {
            return Math.round(dpToPx(32));
        }
    }

    public boolean hasNavigationBar(Context context) {
        if (mHasNavigationBar == -1) {
            int id = context.getResources().getIdentifier("config_showNavigationBar", "bool", "android");
            if (id > 0) {
                mHasNavigationBar = context.getResources().getBoolean(id) ? 1 : 0;
            } else {
                // Assume we have navigation bar by default.
                mHasNavigationBar = 1;
            }
        }
        return mHasNavigationBar != 0;
    }

    public int getNavigationBarHeight(Context context) {
        if (mNavigationBarHeight == -1) {
            int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                mNavigationBarHeight = context.getResources().getDimensionPixelSize(resourceId);
            } else {
                mNavigationBarHeight = Math.round(dpToPx(48));
            }
        }
        return mNavigationBarHeight;
    }

    public int getDp(Context context, int resourceId) {
        return Math.round(
                context.getResources().getDimension(resourceId) / getDisplayMetrics().density
        );
    }

    public float dpToPx(float dp) {
        return dp * (getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public float pxToDp(float px) {
        return Math.round(px / (getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public int getColorAttribute(final Context context, int attributeId) {
        if (mTypedValue == null) {
            mTypedValue = new TypedValue();
        }
        context.getTheme().resolveAttribute(attributeId, mTypedValue, true);
        return mTypedValue.data;
    }

    public int calculateColumnCount(Context context, int columnWidthDp, int minColumnCount) {
        int columnSize = Math.round(dpToPx(columnWidthDp));
        int screenWidth = getScreenWidth(context);
        int columnCount = screenWidth / columnSize;
        if (columnCount <= minColumnCount) {
            columnCount = minColumnCount;
        }
        return columnCount;
    }

    private DisplayMetrics getDisplayMetrics() {
        return Resources.getSystem().getDisplayMetrics();
    }

    private Display getDisplay(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        return wm.getDefaultDisplay();
    }

}
