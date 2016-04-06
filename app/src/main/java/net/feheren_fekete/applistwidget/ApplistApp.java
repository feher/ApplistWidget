package net.feheren_fekete.applistwidget;

import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;
import android.util.TypedValue;

import net.feheren_fekete.applistwidget.model.DataModel;


public class ApplistApp extends MultiDexApplication {

    private static final String TAG = ApplistApp.class.getSimpleName();

    private static int[] mThemeColors;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        DataModel.initInstance(this, getPackageManager());

        mThemeColors = new int[8];
        mThemeColors[0] = getColorAttribute(R.attr.primaryColor);
        mThemeColors[1] = getColorAttribute(R.attr.primaryDarkColor);
        mThemeColors[2] = getColorAttribute(R.attr.primaryLightColor);
        mThemeColors[3] = getColorAttribute(R.attr.accentColor);
        mThemeColors[4] = getColorAttribute(R.attr.primaryTextColor);
        mThemeColors[5] = getColorAttribute(R.attr.secondaryTextColor);
        mThemeColors[6] = getColorAttribute(R.attr.iconsColor);
        mThemeColors[7] = getColorAttribute(R.attr.dividerColor);
    }

    private int getColorAttribute(int attrId) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attrId, typedValue, true);
        if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return typedValue.data;
        } else {
            return 0xff000000;
        }
    }

    public int[] getThemeColors() {
        return mThemeColors;
    }


}

