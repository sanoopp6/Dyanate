package com.fast_prog.dyanate.utilities

import android.content.Context
import android.net.ConnectivityManager
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.fast_prog.dyanate.R
import com.google.android.material.snackbar.Snackbar

object ConnectionDetector {

    fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo

        return activeNetwork != null && activeNetwork.isConnected
    }

    fun isConnectedOrConnecting(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting
    }

    fun errorSnackbar(coordinatorLayout: CoordinatorLayout) {
        val snackbar = Snackbar
            .make(coordinatorLayout, R.string.UnableToConnect, Snackbar.LENGTH_LONG)
            .setAction(R.string.Ok) { }
        snackbar.show()
    }
}
