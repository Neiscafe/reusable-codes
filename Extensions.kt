

import android.content.DialogInterface
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

fun Fragment.toast(message: String): Toast =
    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).also { it.show() }

fun Fragment.snackbar(message: String): Snackbar? {
    return view?.let {
        Snackbar.make(it, message, Snackbar.LENGTH_SHORT)
    }.also { it?.show() }
}

fun Fragment.alert(message: String, onClick: (which: Int) -> Unit = {}): AlertDialog {
    return AlertDialog.Builder(requireContext()).setMessage(message).setTitle("Alert")
        .setNeutralButton("Ok") { _, which -> onClick(which) }.create().also { it.show() }
}
