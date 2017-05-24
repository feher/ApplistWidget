package net.feheren_fekete.applist;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import net.feheren_fekete.applist.utils.RunnableWithArg;
import net.feheren_fekete.applist.utils.RunnableWithRetArg;

import java.util.List;

public class ApplistDialogs {
    public static void textInputDialog(Activity activity,
                                       int textId,
                                       String defaultInputText,
                                       final @Nullable RunnableWithRetArg<String, String> filter,
                                       final RunnableWithArg<String> onOk) {
        View dialogView = activity.getLayoutInflater().inflate(R.layout.create_item_dialog, null);

        TextView textView = (TextView) dialogView.findViewById(R.id.text);
        textView.setText(activity.getResources().getString(textId));

        final EditText editText = (EditText) dialogView.findViewById(R.id.edit_text);
        editText.setText(defaultInputText);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
        alertDialogBuilder
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String sectionName = editText.getText().toString().trim();
                        onOk.run(sectionName);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Nothing.
                    }
                });

        final AlertDialog alertDialog = alertDialogBuilder.create();

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (filter != null) {
                    String errorText = filter.run(String.valueOf(s.toString().trim()));
                    if (errorText != null) {
                        editText.setError(errorText);
                        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                    } else {
                        editText.setError(null);
                        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        editText.requestFocus();
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        alertDialog.show();
    }

    public static void questionDialog(Activity activity,
                                      String title,
                                      String message,
                                      final Runnable onOk,
                                      final Runnable onCancel) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
        alertDialogBuilder
                .setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onOk.run();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onCancel.run();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
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
                holder.layout = (ViewGroup) convertView.findViewById(R.id.layout);
                holder.icon = (ImageView) convertView.findViewById(R.id.icon);
                holder.text = (TextView) convertView.findViewById(R.id.app_name);
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

    public static void chooseAppDialog(final Activity activity,
                                       String title,
                                       final List<ResolveInfo> apps,
                                       final RunnableWithArg<ResolveInfo> onAppSelected) {
        AppChooserAdapter adapter = new AppChooserAdapter(
                activity, R.layout.app_selector_item, R.id.app_name, apps, onAppSelected);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
        alertDialogBuilder
                .setTitle(title)
                .setCancelable(true)
                .setAdapter(adapter, null);
        AlertDialog alertDialog = alertDialogBuilder.create();
        adapter.setDialog(alertDialog);
        alertDialog.show();
    }
}
