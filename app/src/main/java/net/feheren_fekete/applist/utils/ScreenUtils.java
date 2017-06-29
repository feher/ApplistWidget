package net.feheren_fekete.applist.utils;


import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;

import net.feheren_fekete.applist.launcher.ScreenshotUtils;

public class ScreenUtils {

    private static ScreenUtils sInstance;

    private DisplayMetrics mDisplayMetrics;
    private Point mScreenSize;
    private Point mScreenSizeDp;
    private int mStatusBarHeight = -1;
    private int mNavigationBarHeight = -1;
    private int mHasNavigationBar = -1;

    public static void initInstance() {
        if (sInstance == null) {
            sInstance = new ScreenUtils();
        }
    }

    public static ScreenUtils getInstance() {
        if (sInstance != null) {
            return sInstance;
        } else {
            throw new RuntimeException(ScreenshotUtils.class.getSimpleName() + " singleton is not initialized");
        }
    }

    public int getScreenWidth(Context context) {
        if (mScreenSize == null) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            mScreenSize = new Point();
            display.getSize(mScreenSize);
        }
        return mScreenSize.x;
    }

    public Point getScreenSize(Context context) {
        if (mScreenSize == null) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            mScreenSize = new Point();
            display.getSize(mScreenSize);
        }
        return mScreenSize;
    }

    public Point getScreenSizeDp(Context context) {
        if (mScreenSizeDp == null) {
            final Point screenSize = getScreenSize(context);
            mScreenSizeDp = new Point(
                    Math.round(pxToDp(context, screenSize.x)),
                    Math.round(pxToDp(context, screenSize.y)));
        }
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
                        ? Math.round(dpToPx(context, 24))
                        : Math.round(dpToPx(context, 25));
            }
        }
        return mStatusBarHeight;
    }

    public int getActionBarHeight(Context context) {
        // Calculate ActionBar height
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            if (mDisplayMetrics == null) {
                mDisplayMetrics = context.getResources().getDisplayMetrics();
            }
            return TypedValue.complexToDimensionPixelSize(
                    tv.data, mDisplayMetrics);
        } else {
            return Math.round(dpToPx(context, 32));
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
                mNavigationBarHeight = Math.round(dpToPx(context, 48));
            }
        }
        return mNavigationBarHeight;
    }

    public float dpToPx(Context context, float dp) {
        if (mDisplayMetrics == null) {
            mDisplayMetrics = context.getResources().getDisplayMetrics();
        }
        return dp * (mDisplayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public float pxToDp(Context context, float px) {
        if (mDisplayMetrics == null) {
            mDisplayMetrics = context.getResources().getDisplayMetrics();
        }
        return Math.round(px / (mDisplayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

}
