package net.feheren_fekete.applist.applistpage;

import android.app.Activity;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.utils.RunnableWithArg;
import net.feheren_fekete.applist.utils.RunnableWithRetArg;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

public class ApplistDialogs {
    public static void textInputDialog(Activity activity,
                                       int textId,
                                       String defaultInputText,
                                       final @Nullable RunnableWithRetArg<String, String> filter,
                                       final RunnableWithArg<String> onOk) {
        View dialogView = activity.getLayoutInflater().inflate(R.layout.create_item_dialog, null);

        TextView textView = (TextView) dialogView.findViewById(R.id.create_item_dialog_text);
        textView.setText(activity.getResources().getString(textId));

        final EditText editText = (EditText) dialogView.findViewById(R.id.create_item_dialog_edit_text);
        editText.setText(defaultInputText);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
        alertDialogBuilder
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    final String sectionName = editText.getText().toString().trim();
                    onOk.run(sectionName);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    // Nothing.
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
                .setPositiveButton(R.string.ok, (dialog, which) -> onOk.run())
                .setNegativeButton(R.string.cancel, (dialog, which) -> onCancel.run())
                .setOnCancelListener(dialog -> onCancel.run());
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public static void messageDialog(Activity activity,
                                     String title,
                                     String message,
                                     final Runnable onOk,
                                     final Runnable onCancel) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
        alertDialogBuilder
                .setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, (dialog, which) -> onOk.run())
                .setOnCancelListener(dialog -> onCancel.run());
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

}
