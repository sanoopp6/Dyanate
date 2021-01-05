package com.fast_prog.dyanate.utilities

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.multidex.MultiDexApplication
import com.yariksoffice.lingver.Lingver


class DynateApplication : MultiDexApplication() {

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()
        Lingver.init(
            this, applicationContext.getSharedPreferences(
                Constants.PREFS_NAME,
                Context.MODE_PRIVATE
            ).getString(Constants.PREFS_LANG, "en")!!
        )
//        val appSignatureHelper = AppSignatureHelper(this)
//        appSignatureHelper.appSignatures
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    companion object {
        var instance: DynateApplication? = null

        fun applicationContext(): Context {
            return instance!!.applicationContext
        }
    }

    private var mCurrentActivity: Activity? = null
    fun getCurrentActivity(): Activity? {
        return mCurrentActivity
    }

    fun setCurrentActivity(mCurrentActivity: Activity?) {
        this.mCurrentActivity = mCurrentActivity
    }

}