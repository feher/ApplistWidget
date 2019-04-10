package net.feheren_fekete.applist.launcher;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.utils.RunnableWithArg;

import java.util.List;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

public class LauncherUtils {

    private static LauncherUtils sInstance;

    public static void initInstance() {
        if (sInstance == null) {
            sInstance = new LauncherUtils();
        }
    }

    public static LauncherUtils getInstance() {
        if (sInstance != null) {
            return sInstance;
        } else {
            throw new RuntimeException(LauncherUtils.class.getSimpleName() + " singleton is not initialized");
        }
    }

    private LauncherUtils() {
    }

    public void changeWallpaper(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
        chooseAndLaunchApp(activity, intent);
    }

    public void changeLiveWallpaper(Activity activity) {
        Intent intent = new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
        chooseAndLaunchApp(activity, intent);
    }

    public void chooseAppDialog(final Activity activity,
                                       String title,
                                       final List<ResolveInfo> apps,
                                       final RunnableWithArg<ResolveInfo> onAppSelected) {
        AppChooserAdapter adapter = new AppChooserAdapter(
                activity, R.layout.app_selector_item, R.id.app_selector_item_app_name, apps, onAppSelected);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
        alertDialogBuilder
                .setTitle(title)
                .setCancelable(true)
                .setAdapter(adapter, null);
        AlertDialog alertDialog = alertDialogBuilder.create();
        adapter.setDialog(alertDialog);
        alertDialog.show();
    }

    private void chooseAndLaunchApp(final Activity activity, final Intent implicitAppIntent) {
        List<ResolveInfo> availableResolveInfos = activity.getPackageManager().queryIntentActivities(
                implicitAppIntent, PackageManager.MATCH_DEFAULT_ONLY);

        if (availableResolveInfos.size() > 1) {
            chooseAppDialog(
                    activity,
                    activity.getResources().getString(R.string.change_wallpaper_dialog_title),
                    availableResolveInfos,
                    new RunnableWithArg<ResolveInfo>() {
                        @Override
                        public void run(ResolveInfo arg) {
                            startApp(activity, implicitAppIntent.getAction(), arg);
                        }
                    });
        } else if (availableResolveInfos.size() == 1) {
            startApp(activity, implicitAppIntent.getAction(), availableResolveInfos.get(0));
        } else {

        }
    }

    private void startApp(Context context, String intentAction, ResolveInfo resolveInfo) {
        Intent appIntent = new Intent();
        appIntent.setAction(intentAction);
        appIntent.setClassName(
                resolveInfo.activityInfo.packageName,
                resolveInfo.activityInfo.name);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        appIntent.putExtras(new Bundle());
        context.startActivity(appIntent);
    }

    private static class AppChooserAdapter extends ArrayAdapter<ResolveInfo> {
        private AlertDialog dialog;
        private RunnableWithArg<ResolveInfo> onAppSelected;
        public void setDialog(AlertDialog dialog) {
            this.dialog = dialog;
        }
        private class ViewHolder {
            ViewGroup layout;
            ImageView icon;
            TextView text;
        }
        public AppChooserAdapter(@NonNull Context context,
                                 @LayoutRes int resource,
                                 @IdRes int textViewResourceId,
                                 @NonNull List<ResolveInfo> objects,
                                 final RunnableWithArg<ResolveInfo> onAppSelected) {
            super(context, resource, textViewResourceId, objects);
            this.onAppSelected = onAppSelected;
        }
        @NonNull
        @Override
        public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.app_selector_item, parent, false);
                holder = new ViewHolder();
                holder.layout = (ViewGroup) convertView.findViewById(R.id.app_selector_item_layout);
                holder.icon = (ImageView) convertView.findViewById(R.id.app_selector_item_icon);
                holder.text = (TextView) convertView.findViewById(R.id.app_selector_item_app_name);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            PackageManager packageManager = getContext().getPackageManager();
            ResolveInfo resolveInfo = getItem(position);
            holder.icon.setImageDrawable(resolveInfo.loadIcon(packageManager));
            holder.text.setText(resolveInfo.loadLabel(packageManager));
            holder.layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onAppSelected.run(getItem(position));
                    dialog.dismiss();
                }
            });
            return convertView;
        }
    }

}
