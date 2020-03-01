package com.fast_prog.dyanate.views

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.fast_prog.dyanate.R
import com.fast_prog.dyanate.utilities.ConnectionDetector
import com.fast_prog.dyanate.utilities.Constants
import com.fast_prog.dyanate.utilities.JsonParser
import com.fast_prog.dyanate.utilities.UtilityFunctions
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.content_login.*
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    internal var countryCode: String? = null
    internal var mobileNumber: String? = null

    internal lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        toolbar.setNavigationOnClickListener { finish() }

        customTitle(resources.getString(R.string.Login))

        countryCodePicker.registerCarrierNumberEditText(edt_mobile_number)

//        btn_login.setOnClickListener {
//            if (validate()){
//                VerifyOTPActivity.username = username
//                VerifyOTPActivity.otp = "1234"
//                startActivity(Intent(this@LoginActivity, VerifyOTPActivity::class.java))
////                if (ConnectionDetector.isConnectedOrConnecting(applicationContext)) {
////                    CustLoginBackground().execute()
////                } else {
////                    ConnectionDetector.errorSnackbar(coordinator_layout)
////                }
//            }
//        }

        btn_login.setOnClickListener {
            if (validate()) {
                if (ConnectionDetector.isConnected(applicationContext)) {
                    LoginBackground().execute()
                } else {
                    ConnectionDetector.errorSnackbar(coordinator_layout)
                }
            }
        }
    }

    private fun validate(): Boolean {

        if (!countryCodePicker.isValidFullNumber) {
            UtilityFunctions.showAlertOnActivity(this@LoginActivity,
                resources.getString(R.string.InvalidMobileNumber), resources.getString(R.string.Ok),
                "", false, false, {}, {})
            edt_mobile_number.requestFocus()
            return false
        } else {
            edt_mobile_number.error = null
            countryCode = countryCodePicker.selectedCountryCode
            mobileNumber = edt_mobile_number.text.toString().trim().removePrefix("0")
        }

        return true
    }


    private inner class LoginBackground : AsyncTask<Void, Void, JSONObject?>() {

        override fun onPreExecute() {
            super.onPreExecute()
            UtilityFunctions.showProgressDialog(this@LoginActivity)
        }

        override fun doInBackground(vararg p0: Void?): JSONObject? {

            val jsonParser = JsonParser()
            val params = HashMap<String, String>()
            params["lang"] = sharedPreferences.getString(Constants.PREFS_LANG, "en")!!
            params["country_code"] = countryCode!!
            params["mobile_number"] = mobileNumber!!

            return jsonParser.makeHttpRequest(Constants.BASE_URL + "customer/login", "POST", params)
        }

        override fun onPostExecute(result: JSONObject?) {
            UtilityFunctions.dismissProgressDialog()
            if (result != null) {
                if (result.getBoolean("status")) {

                    SendOTPBackground(result.getJSONObject("data").getString("user_id")).execute()
                    VerifyOTPActivity.userId = result.getJSONObject("data").getString("user_id")
                    VerifyOTPActivity.otpAction = "login"
                    VerifyOTPActivity.mobileNumber = countryCode!! + mobileNumber!!
                    startActivity(Intent(this@LoginActivity, VerifyOTPActivity::class.java))


                } else {
                    UtilityFunctions.showAlertOnActivity(this@LoginActivity,
                        result.getString("message"), resources.getString(R.string.Ok).toString(),
                        "", false, true, {}, {})
                }

            }
        }
    }

    private inner class SendOTPBackground internal constructor(internal var userID: String) :
        AsyncTask<Void, Void, JSONObject?>() {

        override fun doInBackground(vararg p0: Void?): JSONObject? {

            val jsonParser = JsonParser()
            val params = HashMap<String, String>()
            params["lang"] = sharedPreferences.getString(Constants.PREFS_LANG, "en")!!
            params["user_id"] = userID
            params["country_code"] = countryCode!!
            params["mobile_number"] = mobileNumber!!
            params["action"] = "login"

            return jsonParser.makeHttpRequest(
                Constants.BASE_URL + "customer/generate_otp",
                "POST",
                params
            )
        }
    }


    //@SuppressLint("StaticFieldLeak")
    //private inner class CustLoginBackground : AsyncTask<Void, Void, JSONObject>() {
    //
    //    override fun onPreExecute() {
    //        super.onPreExecute()
    //        UtilityFunctions.showProgressDialog (this@LoginActivity)
    //    }
    //
    //    override fun doInBackground(vararg param: Void): JSONObject? {
    //        val jsonParser = JsonParser()
    //        val params = HashMap<String, String>()
    //
    //        params["ArgUserName"] = username
    //        params["ArgPassword"] = ""
    //        params["ArgUsrLoginType"] = Constants.LOG_CONST_NORMAL
    //
    //        var BASE_URL = Constants.BASE_URL_EN + "CustLogin"
    //
    //        if (sharedPreferences.getString(Constants.PREFS_LANG, "en")!!.equals("ar", ignoreCase = true)) {
    //            BASE_URL = Constants.BASE_URL_AR + "CustLogin"
    //        }
    //
    //        return jsonParser.makeHttpRequest(BASE_URL, "POST", params)
    //    }
    //
    //    override fun onPostExecute(response: JSONObject?) {
    //        UtilityFunctions.dismissProgressDialog()
    //
    //        if (response != null) {
    //            try {
    //                if (response.getBoolean("status")) {
    //                    val editor = sharedPreferences.edit()
    //
    //                    editor.putString(Constants.PREFS_USER_ID, response.getJSONArray("data").getJSONObject(0).getString("UsrID"))
    //                    editor.putString(Constants.PREFS_USER_MOBILE, response.getJSONArray("data").getJSONObject(0).getString("UsrMobNumber"))
    //                    editor.putBoolean(Constants.PREFS_IS_LOGIN, true)
    //                    editor.putBoolean(Constants.PREFS_IS_GUEST, false)
    //                    editor.putString(Constants.PREFS_SHARE_URL, "https://goo.gl/i7Qasx")
    //                    editor.putString(Constants.PREFS_LATITUDE, response.getJSONArray("data").getJSONObject(0).getString("UsrLatitude"))
    //                    editor.putString(Constants.PREFS_LONGITUDE, response.getJSONArray("data").getJSONObject(0).getString("UsrLongitude"))
    //                    editor.putString(Constants.PREFS_USER_NAME, response.getJSONArray("data").getJSONObject(0).getString("UsrName"))
    //                    editor.putString(Constants.PREFS_SAVED_USERNAME, response.getJSONArray("data").getJSONObject(0).getString("UsrName").trim())
    //
    //                    editor.commit()
    //
    //                    UpdateFCMToken(this@LoginActivity, true, sharedPreferences.getString(Constants.PREFS_USER_ID, "")!!).execute()
    //
    //                    startActivity(Intent(this@LoginActivity, ShipmentDetActivity::class.java))
    //                    ActivityCompat.finishAffinity(this@LoginActivity)
    //                    finish()
    //
    //                } else {
    //                    UtilityFunctions.showAlertOnActivity(this@LoginActivity,
    //                            response.getString("message"), resources.getString(R.string.Ok).toString(),
    //                            "", false, false, {}, {})
    //                }
    //
    //            } catch (e: JSONException) { e.printStackTrace() }
    //
    //        } else {
    //            val snackbar = Snackbar.make(coordinator_layout, R.string.UnableToConnect, Snackbar.LENGTH_LONG).setAction(R.string.Ok) { }
    //            snackbar.show()
    //        }
    //    }
    //}

}
