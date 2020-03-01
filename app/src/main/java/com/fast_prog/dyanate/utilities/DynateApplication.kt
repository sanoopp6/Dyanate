package com.fast_prog.dyanate.utilities

import android.content.Context
import android.content.res.Configuration
import androidx.multidex.MultiDexApplication

class DynateApplication : MultiDexApplication() {

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()
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

}