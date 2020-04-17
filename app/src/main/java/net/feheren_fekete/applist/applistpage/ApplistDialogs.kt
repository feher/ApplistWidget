package net.feheren_fekete.applist.applistpage

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItems
import net.feheren_fekete.applist.R

object ApplistDialogs {

    fun textInputDialog(activity: Activity,
                        textId: Int,
                        defaultInputText: String,
                        filter: (String) -> String?,
                        onOk: (String) -> Unit) {
        MaterialDialog(activity).show {
            message(textId)
            input(prefill = defaultInputText, waitForPositiveButton = false, allowEmpty = true) { dialog, text ->
                dialog.getInputField().error = filter(text.toString())
            }
            positiveButton(R.string.ok) {
                onOk(it.getInputField().text.toString())
            }
            negativeButton(R.string.cancel)
        }
    }

    fun questionDialog(activity: Activity,
                       title: String,
                       message: String,
                       onOk: () -> Unit,
                       onCancel: () -> Unit) {
        val alertDialogBuilder = AlertDialog.Builder(activity)
        alertDialogBuilder
                .setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton(R.string.ok) { _, _ -> onOk() }
            .setNegativeButton(R.string.cancel) { _, _ -> onCancel() }
                .setOnCancelListener { onCancel() }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    fun messageDialog(activity: Activity,
                      title: String,
                      message: String,
                      onOk: () -> Unit,
                      onCancel: () -> Unit) {
        val alertDialogBuilder = AlertDialog.Builder(activity)
        alertDialogBuilder
                .setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton(R.string.ok) { _, _ -> onOk() }
                .setOnCancelListener { onCancel() }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    fun listDialog(activity: Activity,
                   title: String,
                   items: List<String>,
                   onSelected: (Int) -> Unit) {
        MaterialDialog(activity).show {
            title(text = title)
            listItems(items = items) { _, index, _ ->
                onSelected(index)
            }
        }
    }

}
