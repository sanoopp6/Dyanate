package com.fast_prog.dyanate.utilities

import android.content.Context
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService

/**
 * Created by sarathk on 12/13/16.
 */

class DynateFbInstanceIdService : FirebaseInstanceIdService() {

    override fun onTokenRefresh() {
        val refreshedToken = FirebaseInstanceId.getInstance().token
        val preferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putString(Constants.PREFS_FCM_TOKEN, refreshedToken)
        editor.commit()
    }

}

