package com.fast_prog.dyanate.views

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fast_prog.dyanate.R
import com.fast_prog.dyanate.utilities.ConnectionDetector
import com.fast_prog.dyanate.utilities.Constants
import com.fast_prog.dyanate.utilities.JsonParser
import kotlinx.android.synthetic.main.activity_no_login.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class NoLoginActivity : AppCompatActivity() {

    internal lateinit var sharedPreferences: SharedPreferences

    internal lateinit var permissionsList: MutableList<String>

    private val REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_no_login)
        supportActionBar?.hide()

        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        val MyVersion = Build.VERSION.SDK_INT

        if (MyVersion > Build.VERSION_CODES.LOLLIPOP_MR1) {
            permissionsList = ArrayList()

            if (!checkIfAlreadyhavePermission()) {
                requestForSpecificPermission()
            }
        }

        loginButton.setOnClickListener {
            startActivity(Intent(this@NoLoginActivity, LoginActivity::class.java))
        }

        button_lang.setOnClickListener {
            if (sharedPreferences.getString(Constants.PREFS_LANG, "").equals("ar", true)) {
                reloadActivity("en")
            } else {
                reloadActivity("ar")
            }
        }

        button_instructions.setOnClickListener {
            startActivity(Intent(this@NoLoginActivity, InstructionsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()

        if (ConnectionDetector.isConnectedOrConnecting(applicationContext)) {
//            AppInstructionsCountBackground().execute()
//            IsAppLiveBackground().execute()
        } else {
            ConnectionDetector.errorSnackbar(coordinator_layout)
        }
    }

    private fun reloadActivity(lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val confg = Configuration()
        confg.locale = locale
        baseContext.resources.updateConfiguration(confg, baseContext.resources.displayMetrics)

        val editor = sharedPreferences.edit()
        editor.putString(Constants.PREFS_LANG, lang)
        editor.commit()

        startActivity(Intent(this@NoLoginActivity, NoLoginActivity::class.java))
        finish()
    }

    private fun checkIfAlreadyhavePermission(): Boolean {
        var result = true

        val permission1 =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val permission2 =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        val permission3 =
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val permission4 =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
//        val permission5 = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
//        val permission6 = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
        val permission7 = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)

        if (!(permission1 == PackageManager.PERMISSION_GRANTED && permission2 == PackageManager.PERMISSION_GRANTED)) {
            permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION)
            result = false
        }

        if (!(permission3 == PackageManager.PERMISSION_GRANTED && permission4 == PackageManager.PERMISSION_GRANTED)) {
            permissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            result = false
        }

//        if (!(permission5 == PackageManager.PERMISSION_GRANTED && permission6 == PackageManager.PERMISSION_GRANTED)) {
//            permissionsList.add(Manifest.permission.READ_SMS)
//            result = false
//        }

        if (permission7 != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(Manifest.permission.CAMERA)
            result = false
        }

        return result
    }

    private fun requestForSpecificPermission() {
        val stringArr = permissionsList.toTypedArray<String>()
        ActivityCompat.requestPermissions(
            this@NoLoginActivity,
            stringArr,
            REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS -> when (grantResults[0]) {
                PackageManager.PERMISSION_GRANTED -> {
                }
                else -> finish()
            }//granted
            //System.exit(0);
            //not granted
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    @SuppressLint("StaticFieldLeak")
    inner class IsAppLiveBackground : AsyncTask<Void, Void, JSONObject>() {

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()

            params["ArgAppPackageName"] = Constants.APP_NAME
            params["ArgAppVersionNo"] = Constants.APP_VERSION

            return jsonParser.makeHttpRequest(Constants.BASE_URL_EN + "IsAppLive", "POST", params)
        }

        override fun onPostExecute(response: JSONObject?) {
            if (response != null) {
                try {
                    if (!response.getBoolean("status")) {
                        ActivityCompat.finishAffinity(this@NoLoginActivity)
                        val intent = Intent(this@NoLoginActivity, UpdateActivity::class.java)
                        intent.putExtra("message", response.getString("data"))
                        startActivity(intent)
                        finish()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class AppInstructionsCountBackground : AsyncTask<Void, Void, JSONObject>() {

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()

            params["ArgStartDBId"] =
                sharedPreferences.getString(Constants.LAST_INSTRUCTION_ID, "0")!!

            var BASE_URL = Constants.BASE_URL_EN + "AppInstructionsCount"

            if (sharedPreferences.getString(Constants.PREFS_LANG, "en")!!.equals("ar", true)) {
                BASE_URL = Constants.BASE_URL_AR + "AppInstructionsCount"
            }

            return jsonParser.makeHttpRequest(BASE_URL, "POST", params)
        }

        override fun onPostExecute(response: JSONObject?) {

            if (response != null) {
                try {
                    if (response.getBoolean("status")) {
                        val count = response.getString("data").trim().toIntOrNull()
                        if (count!! > 0) {
                            textView_instructions.visibility = View.VISIBLE
                            textView_instructions.text = count.toString()
                        } else {
                            textView_instructions.visibility = View.GONE
                        }
                    } else {
                        textView_instructions.visibility = View.GONE
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }
}
