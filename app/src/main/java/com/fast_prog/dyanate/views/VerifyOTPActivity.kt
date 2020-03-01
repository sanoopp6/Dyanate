package com.fast_prog.dyanate.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.fast_prog.dyanate.R
import com.fast_prog.dyanate.utilities.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_verify_otp.*
import kotlinx.android.synthetic.main.content_verify_otp.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class VerifyOTPActivity : AppCompatActivity() {

    internal var otp: String = ""

    internal var otpExtra: String = ""

    internal var isRegistered: Boolean = false

    internal var receiver: SMSReceiver? = null

    internal lateinit var sharedPreferences: SharedPreferences

    internal lateinit var otpArray: MutableList<String>

    companion object {

        var userId = ""
        var otpAction = ""
        var mobileNumber = ""

        @SuppressLint("StaticFieldLeak")
        internal lateinit var editTextOTP1: EditText
        @SuppressLint("StaticFieldLeak")
        internal lateinit var editTextOTP2: EditText
        @SuppressLint("StaticFieldLeak")
        internal lateinit var editTextOTP3: EditText
        @SuppressLint("StaticFieldLeak")
        internal lateinit var editTextOTP4: EditText
        internal var username: String? = null
        internal var otp: String? = null

        fun updateData(otp: String) {
            editTextOTP1.setText(otp.split("")[1])
            editTextOTP2.setText(otp.split("")[2])
            editTextOTP3.setText(otp.split("")[3])
            editTextOTP4.setText(otp.split("")[4])
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_otp)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        toolbar.setNavigationOnClickListener { finish() }

        customTitle(resources.getString(R.string.VerifyOTP))

        editTextOTP1 = findViewById<View>(R.id.editText_otp1) as EditText
        editTextOTP2 = findViewById<View>(R.id.editText_otp2) as EditText
        editTextOTP3 = findViewById<View>(R.id.editText_otp3) as EditText
        editTextOTP4 = findViewById<View>(R.id.editText_otp4) as EditText

        editTextOTP1.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (count == 1) {
                    editTextOTP2.requestFocus()
                }
            }
        })

        editTextOTP2.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (count == 1) {
                    editTextOTP3.requestFocus()
                } else if (count == 0) {
                    editTextOTP1.requestFocus()
                }
            }
        })

        editTextOTP3.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (count == 1) {
                    editTextOTP4.requestFocus()
                } else if (count == 0) {
                    editTextOTP2.requestFocus()
                }
            }
        })

        editTextOTP4.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val otp =
                    editTextOTP1.text.toString().trim() + editTextOTP2.text.toString().trim() + editTextOTP3.text.toString().trim() + editTextOTP4.text.toString().trim()

                if (!otp.isEmpty()) {
                    if (otp.length == 4) {
                        button_submit.isEnabled = true
                    }
                }

                if (count == 0) {
                    editTextOTP3.requestFocus()
                }
            }
        })

        editTextOTP2.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                if (editTextOTP1.text.trim().isEmpty()) {
                    editTextOTP1.requestFocus()
                }
            }
        }

        editTextOTP3.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                if (editTextOTP1.text.trim().isEmpty()) {
                    editTextOTP1.requestFocus()
                } else if (editTextOTP2.text.trim().isEmpty()) {
                    editTextOTP2.requestFocus()
                }
            }
        }

        editTextOTP4.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                when {
                    editTextOTP1.text.trim().isEmpty() -> editTextOTP1.requestFocus()
                    editTextOTP2.text.trim().isEmpty() -> editTextOTP2.requestFocus()
                    editTextOTP3.text.trim().isEmpty() -> editTextOTP3.requestFocus()
                }
            }
        }

        editTextOTP2.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DEL) {
                editTextOTP1.requestFocus()
            }
            return@setOnKeyListener false
        }

        editTextOTP3.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DEL) {
                editTextOTP2.requestFocus()
            }
            return@setOnKeyListener false
        }

        editTextOTP4.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DEL) {
                editTextOTP3.requestFocus()
            }
            return@setOnKeyListener false
        }

        object : CountDownTimer(45000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                textView_timer.text =
                    String.format(Locale.getDefault(), "00: %d", millisUntilFinished / 1000)
            }

            override fun onFinish() {
                textView_timer.visibility = View.GONE
                textView_resend_otp.isEnabled = true
                textView_resend_otp.alpha = 1.0f
            }
        }.start()

        button_submit.setOnClickListener {
            if (validate()) {
                if (ConnectionDetector.isConnected(this@VerifyOTPActivity)) {
                    VerifyOTPBackground().execute()
                } else {
                    ConnectionDetector.errorSnackbar(coordinator_layout)
                }
            }
        }

        textView_otp2.text = String.format(
            "%s %s",
            this@VerifyOTPActivity.resources.getString(R.string.WeSendAMessageToNumber),
            username
        )

        textView_resend_otp.setOnClickListener {
            if (ConnectionDetector.isConnected(this@VerifyOTPActivity)) {
                SendOTPBackground().execute()
            } else {
                ConnectionDetector.errorSnackbar(coordinator_layout)
            }
        }

        if (!isRegistered) {
            receiver = SMSReceiver()
            val filter = IntentFilter()
            filter.addAction("android.provider.Telephony.SMS_RECEIVED")
            registerReceiver(receiver, filter)
            isRegistered = true
        }

        otpArray = ArrayList()
        otpArray.add(otpExtra)
    }

    override fun onResume() {
        super.onResume()

        if (!isRegistered) {
            receiver = SMSReceiver()
            val filter = IntentFilter()
            filter.addAction("android.provider.Telephony.SMS_RECEIVED")
            registerReceiver(receiver, filter)
            isRegistered = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isRegistered) {
            unregisterReceiver(receiver)
            isRegistered = false
        }
    }

    fun useLoop(targetValue: String): Boolean {
        for (s in otpArray) {
            if (s == targetValue)
                return true
        }
        return false
    }

    private fun validate(): Boolean {

        if (editTextOTP1.text.toString().trim().isEmpty() ||
            editTextOTP2.text.toString().trim().isEmpty() ||
            editTextOTP3.text.toString().trim().isEmpty() ||
            editTextOTP4.text.toString().trim().isEmpty()
        ) {
            UtilityFunctions.showAlertOnActivity(this@VerifyOTPActivity,
                resources.getString(R.string.InvalidOTPNumber), resources.getString(R.string.Ok),
                "", false, false, {}, {})
            editTextOTP4.requestFocus()
            return false
        }

        otp =
            editTextOTP1.text.toString().trim() + editTextOTP2.text.toString().trim() + editTextOTP3.text.toString().trim() + editTextOTP4.text.toString().trim()

//        if (!useLoop(otpExtra)) {
//            UtilityFunctions.showAlertOnActivity(this@VerifyOTPActivity,
//                    resources.getString(R.string.InvalidOTPNumber), resources.getString(R.string.Ok),
//                    "", false, false, {}, {})
//            return false
//        }

        return true
    }

//    @SuppressLint("StaticFieldLeak")
//    private inner class SignUpUserBackground : AsyncTask<Void, Void, JSONObject>() {
//
//        override fun onPreExecute() {
//            super.onPreExecute()
//            UtilityFunctions.showProgressDialog (this@VerifyOTPActivity)
//        }
//
//        override fun doInBackground(vararg param: Void): JSONObject? {
//            val jsonParser = JsonParser()
//            val params = HashMap<String, String>()
//
//            params["ArgUsrName"] = registerUserExtra.name!!
//            params["ArgUsrMobNumber"] = registerUserExtra.mobile!!
//            params["ArgUsrEmailId"] = registerUserExtra.mail!!
//            params["ArgUsrAddress"] = registerUserExtra.address!!
//            params["ArgUsrLatitude"] = registerUserExtra.latitude!!
//            params["ArgUsrLongitude"] = registerUserExtra.longitude!!
//            params["ArgUsrUserId"] = registerUserExtra.username!!
//            params["ArgUsrPassWord"] = registerUserExtra.password!!
//            params["ArgUsrLoginType"] = registerUserExtra.loginMethod!!
//
//            var BASE_URL = Constants.BASE_URL_EN + "SignUpUser"
//
//            if (sharedPreferences.getString(Constants.PREFS_LANG, "en")!!.equals("ar", ignoreCase = true)) {
//                BASE_URL = Constants.BASE_URL_AR + "SignUpUser"
//            }
//
//            return jsonParser.makeHttpRequest(BASE_URL, "POST", params)
//        }
//
//        override fun onPostExecute(response: JSONObject?) {
//            UtilityFunctions.dismissProgressDialog()
//
//            if (response != null) {
//                try {
//                    if (response.getBoolean("status")) {
//                        UtilityFunctions.showAlertOnActivity(this@VerifyOTPActivity,
//                                resources.getString(R.string.RegistrationSuccessful), resources.getString(R.string.Ok).toString(),
//                                "", false, false,
//                                { CustLoginBackground().execute() }, {})
//
//                    } else {
//                        UtilityFunctions.showAlertOnActivity(this@VerifyOTPActivity,
//                                response.getString("message"), resources.getString(R.string.Ok).toString(),
//                                "", false, false, {}, {})
//                    }
//
//                } catch (e: JSONException) { e.printStackTrace() }
//            } else {
//                val snackbar = Snackbar.make(coordinator_layout, R.string.UnableToConnect, Snackbar.LENGTH_LONG).setAction(R.string.Ok) { }
//                snackbar.show()
//            }
//        }
//    }

    private inner class VerifyOTPBackground : AsyncTask<Void, Void, JSONObject?>() {

        override fun onPreExecute() {
            super.onPreExecute()
            UtilityFunctions.showProgressDialog(this@VerifyOTPActivity)
        }

        override fun doInBackground(vararg p0: Void?): JSONObject? {

            val jsonParser = JsonParser()
            val params = HashMap<String, String>()
            params["lang"] = sharedPreferences.getString(Constants.PREFS_LANG, "en")!!
            params["user_id"] = userId!!
            params["otp"] = otp

            return jsonParser.makeHttpRequest(
                Constants.BASE_URL + "customer/verify_otp",
                "POST",
                params
            )
        }

        override fun onPostExecute(result: JSONObject?) {
            UtilityFunctions.dismissProgressDialog()
            if (result != null) {
                if (otpAction == "login" || otpAction == "login_resend") {
                    if (result.getBoolean("status")) {

                        GetUserDetailBackground().execute()

                    } else {
                        UtilityFunctions.showAlertOnActivity(this@VerifyOTPActivity,
                            result.getString("message"),
                            resources.getString(R.string.Ok).toString(),
                            "",
                            false,
                            true,
                            {},
                            {})
                    }
                }
            }
        }
    }
//
//    @SuppressLint("StaticFieldLeak")
//    private inner class CustLoginBackground : AsyncTask<Void, Void, JSONObject>() {
//
//        override fun onPreExecute() {
//            super.onPreExecute()
//            UtilityFunctions.showProgressDialog (this@VerifyOTPActivity)
//        }
//
//        override fun doInBackground(vararg param: Void): JSONObject? {
//            val jsonParser = JsonParser()
//            val params = HashMap<String, String>()
//
//            params["ArgUserName"] = "testcust"//registerUserExtra.username!!
//            params["ArgPassword"] = "123456"//registerUserExtra.password!!
//            params["ArgUsrLoginType"] = Constants.LOG_CONST_NORMAL//registerUserExtra.loginMethod!!
//
//            var BASE_URL = Constants.BASE_URL_EN + "CustLogin"
//
//            if (sharedPreferences.getString(Constants.PREFS_LANG, "en")!!.equals("ar", ignoreCase = true)) {
//                BASE_URL = Constants.BASE_URL_AR + "CustLogin"
//            }
//
//            return jsonParser.makeHttpRequest(BASE_URL, "POST", params)
//        }
//
//        override fun onPostExecute(response: JSONObject?) {
//            UtilityFunctions.dismissProgressDialog()
//
//            if (response != null) {
//                try {
//                    if (response.getBoolean("status")) {
//                        val editor = sharedPreferences.edit()
//
//                        editor.putString(Constants.PREFS_USER_ID, response.getJSONArray("data").getJSONObject(0).getString("UsrID"))
//                        editor.putString(Constants.PREFS_USER_MOBILE, response.getJSONArray("data").getJSONObject(0).getString("UsrMobNumber"))
//                        editor.putBoolean(Constants.PREFS_IS_LOGIN, true)
//                        editor.putBoolean(Constants.PREFS_IS_GUEST, false)
//                        editor.putString(Constants.PREFS_SHARE_URL, "https://goo.gl/i7Qasx")
//                        editor.putString(Constants.PREFS_LATITUDE, response.getJSONArray("data").getJSONObject(0).getString("UsrLatitude"))
//                        editor.putString(Constants.PREFS_LONGITUDE, response.getJSONArray("data").getJSONObject(0).getString("UsrLongitude"))
//                        editor.putString(Constants.PREFS_USER_NAME, response.getJSONArray("data").getJSONObject(0).getString("UsrName"))
//                        editor.putString(Constants.PREFS_SAVED_USERNAME, response.getJSONArray("data").getJSONObject(0).getString("UsrName").trim())
//                        editor.commit()
//
//                        UpdateFCMToken(this@VerifyOTPActivity, true, sharedPreferences.getString(Constants.PREFS_USER_ID, "")!!).execute()
//
//                        val intent = Intent(this@VerifyOTPActivity, SenderLocationActivity::class.java)
//                        ActivityCompat.finishAffinity(this@VerifyOTPActivity)
//                        startActivity(intent)
//                        finish()
//
//                    } else {
//                        UtilityFunctions.showAlertOnActivity(this@VerifyOTPActivity,
//                                resources.getString(R.string.InvalidLogin), resources.getString(R.string.Ok),
//                                "", false, false,
//                                {
//                                    val intent = Intent(this@VerifyOTPActivity, NoLoginActivity::class.java)
//                                    ActivityCompat.finishAffinity(this@VerifyOTPActivity)
//                                    startActivity(intent)
//                                    finish()
//                                }, {})
//                    }
//
//                } catch (e: JSONException) { e.printStackTrace() }
//
//            } else {
//                val snackbar = Snackbar.make(coordinator_layout, R.string.UnableToConnect, Snackbar.LENGTH_LONG).setAction(R.string.Ok) { }
//                snackbar.show()
//            }
//        }
//    }

    private inner class GetUserDetailBackground : AsyncTask<Void, Void, JSONObject?>() {

        override fun onPreExecute() {
            super.onPreExecute()
            UtilityFunctions.showProgressDialog(this@VerifyOTPActivity)
        }

        override fun doInBackground(vararg p0: Void?): JSONObject? {

            val jsonParser = JsonParser()
            val params = HashMap<String, String>()
            params["lang"] = sharedPreferences.getString(Constants.PREFS_LANG, "en")!!
            params["user_id"] = userId!!

            return jsonParser.makeHttpRequest(
                Constants.BASE_URL + "customer/get_user_detail",
                "POST",
                params
            )
        }

        override fun onPostExecute(result: JSONObject?) {
            UtilityFunctions.dismissProgressDialog()
            if (result != null) {
                if (otpAction == "login" || otpAction == "login_resend") {
                    if (result.getBoolean("status")) {

                        val editor = sharedPreferences!!.edit()
                        editor.putString(
                            Constants.PREFS_USER_MOBILE_WITHOUT_COUNTRY,
                            result.getJSONObject("data").getJSONObject("user_info").getString("mobile_number")
                        )
                        editor.putString(
                            Constants.PREFS_COUNTRY_CODE,
                            result.getJSONObject("data").getJSONObject("user_info").getString("country_code")
                        )
                        editor.putString(
                            Constants.PREFS_USER_ID,
                            result.getJSONObject("data").getJSONObject("user_info").getString("user_id").trim()
                        )
                        editor.putString(
                            Constants.PREFS_USER_TOKEN,
                            result.getJSONObject("data").getJSONObject("user_info").getString("token").trim()
                        )
                        editor.putString(
                            Constants.PREFS_USER_FULL_MOBILE,
                            result.getJSONObject("data").getJSONObject("user_info").getString("mobile_number").trim()
                        )
                        editor.putBoolean(Constants.PREFS_IS_LOGIN, true)

                        editor.commit()
                        startActivity(
                            Intent(
                                this@VerifyOTPActivity,
                                SenderLocationActivity::class.java
                            )
                        )
                        finishAffinity()


                    } else {
                        UtilityFunctions.showAlertOnActivity(this@VerifyOTPActivity,
                            result.getString("message"),
                            resources.getString(R.string.Ok).toString(),
                            "",
                            false,
                            true,
                            {},
                            {})
                    }
                }
            }
        }
    }

    private inner class SendOTPBackground : AsyncTask<Void, Void, JSONObject?>() {

        override fun onPreExecute() {
            super.onPreExecute()
            UtilityFunctions.showProgressDialog(this@VerifyOTPActivity)
        }

        override fun doInBackground(vararg p0: Void?): JSONObject? {

            val jsonParser = JsonParser()
            val params = HashMap<String, String>()
            params["lang"] = sharedPreferences.getString(Constants.PREFS_LANG, "en")!!
            params["user_id"] = userId
            params["action"] = "login"

            return jsonParser.makeHttpRequest(
                Constants.BASE_URL + "driver/generate_otp",
                "POST",
                params
            )
        }


        override fun onPostExecute(response: JSONObject?) {
            UtilityFunctions.dismissProgressDialog()

            if (response != null) {
                try {
                    if (response.getBoolean("status")) {
                        otpExtra = response.getJSONArray("data").getJSONObject(0).getString("OTP")
                        otpArray.add(otpExtra)
                        textView_timer.visibility = View.VISIBLE
                        textView_resend_otp.isEnabled = false
                        textView_resend_otp.alpha = 0.4f

                        object : CountDownTimer(45000, 1000) {
                            override fun onTick(millisUntilFinished: Long) {
                                textView_timer.text = String.format(
                                    Locale.getDefault(),
                                    "00: %d",
                                    millisUntilFinished / 1000
                                )
                            }

                            override fun onFinish() {
                                textView_timer.visibility = View.GONE
                                textView_resend_otp.isEnabled = true
                                textView_resend_otp.alpha = 1.0f
                            }
                        }.start()

                    } else {
                        UtilityFunctions.showAlertOnActivity(this@VerifyOTPActivity,
                            response.getString("message"),
                            resources.getString(R.string.Ok).toString(),
                            "",
                            false,
                            false,
                            {},
                            {})
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

            } else {
                val snackbar = Snackbar.make(
                    coordinator_layout,
                    R.string.UnableToConnect,
                    Snackbar.LENGTH_LONG
                ).setAction(R.string.Ok) { }
                snackbar.show()
            }
        }
    }

    //@SuppressLint("StaticFieldLeak")
    //private inner class SendOTPBackground : AsyncTask<Void, Void, JSONObject>() {
    //
    //    override fun onPreExecute() {
    //        super.onPreExecute()
    //        UtilityFunctions.showProgressDialog (this@VerifyOTPActivity)
    //    }
    //
    //    override fun doInBackground(vararg param: Void): JSONObject? {
    //        val jsonParser = JsonParser()
    //        val params = HashMap<String, String>()
    //
    //        params["ArgMobNo"] = username!!
    //        params["ArgIsDB"] = "false"
    //
    //        var BASE_URL = Constants.BASE_URL_EN + "SendOTP"
    //
    //        if (sharedPreferences.getString(Constants.PREFS_LANG, "en")!!.equals("ar", ignoreCase = true)) {
    //            BASE_URL = Constants.BASE_URL_AR + "SendOTP"
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
    //                    otpExtra = response.getJSONArray("data").getJSONObject(0).getString("OTP")
    //                    otpArray.add(otpExtra)
    //                    textView_timer.visibility = View.VISIBLE
    //                    textView_resend_otp.isEnabled = false
    //                    textView_resend_otp.alpha = 0.4f
    //
    //                    object : CountDownTimer(45000, 1000) {
    //                        override fun onTick(millisUntilFinished: Long) {
    //                            textView_timer.text = String.format(Locale.getDefault(), "00: %d", millisUntilFinished / 1000)
    //                        }
    //
    //                        override fun onFinish() {
    //                            textView_timer.visibility = View.GONE
    //                            textView_resend_otp.isEnabled = true
    //                            textView_resend_otp.alpha = 1.0f
    //                        }
    //                    }.start()
    //
    //                } else {
    //                    UtilityFunctions.showAlertOnActivity(this@VerifyOTPActivity,
    //                            response.getString("message"), resources.getString(R.string.Ok).toString(),
    //                            "", false, false, {}, {})
    //                }
    //            } catch (e: JSONException) { e.printStackTrace() }
    //
    //        } else {
    //            val snackbar = Snackbar.make(coordinator_layout, R.string.UnableToConnect, Snackbar.LENGTH_LONG).setAction(R.string.Ok) { }
    //            snackbar.show()
    //        }
    //    }
    //}

}
