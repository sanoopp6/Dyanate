package com.fast_prog.dyanate.views

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import com.fast_prog.dyanate.R
import com.fast_prog.dyanate.utilities.ConnectionDetector
import com.fast_prog.dyanate.utilities.Constants
import com.fast_prog.dyanate.utilities.JsonParser
import com.fast_prog.dyanate.utilities.UtilityFunctions
import com.yariksoffice.lingver.Lingver
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.content_login.*
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.collections.HashMap

class LoginActivity : AppCompatActivity() {

    internal var countryCode: String? = null
    internal var mobileNumber: String? = null

    internal lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.hide()

//        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

//        toolbar.setNavigationOnClickListener { finish() }

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

        textView_terms_conditions.setOnClickListener {
            getTermsAndConditions()
        }

        button_lang.setOnClickListener {
            if (sharedPreferences.getString(Constants.PREFS_LANG, "").equals("ar", true)) {
                reloadActivity("en")
                Lingver.getInstance().setLocale(this, "en")
            } else {
                reloadActivity("ar")
                Lingver.getInstance().setLocale(this, "ar")
            }
        }

        if (sharedPreferences.getBoolean(Constants.PREFS_IS_LOGIN, false)) {
            startActivity(Intent(this@LoginActivity, SenderLocationActivity::class.java))
        }
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

    private fun getTermsAndConditions() {
        val assetManager = assets
        var `in`: InputStream? = null
        var out: OutputStream? = null
        val file = File(filesDir, "terms_conditions.pdf")

        try {
            `in` = assetManager.open("terms_conditions.pdf")
            out = openFileOutput(file.name, Context.MODE_PRIVATE)
            copyFile(`in`, out)
            `in`!!.close()
            `in` = null
            out!!.flush()
            out.close()
            out = null
        } catch (e: Exception) {
            //Log.e("tag_", e.getMessage());
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val apkURI = FileProvider.getUriForFile(
                this@LoginActivity,
                applicationContext.packageName + ".provider",
                file
            )
            intent.setDataAndType(apkURI, "application/pdf")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)

        } catch (e: ActivityNotFoundException) {
            Toast.makeText(applicationContext, R.string.NoAppPDF, Toast.LENGTH_SHORT).show()
        }

    }

    @Throws(IOException::class)
    private fun copyFile(`in`: InputStream?, out: OutputStream?) {
        val buffer = ByteArray(1024)
        var read: Int = -1
        while ({ read = `in`!!.read(buffer); read }() != -1) {
            out!!.write(buffer, 0, read)
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

        startActivity(Intent(this@LoginActivity, LoginActivity::class.java))
        finish()
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

        if (!checkBox_i_agree.isChecked) {
            UtilityFunctions.showAlertOnActivity(this@LoginActivity,
                resources.getText(R.string.YouMustAgreeToTermsAndConditions).toString(),
                resources.getString(R.string.Ok).toString(),
                "",
                false,
                false,
                { checkBox_i_agree.requestFocus() },
                {})
            return false
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

                    if ((countryCode!! == sharedPreferences.getString(
                            Constants.PREFS_COUNTRY_CODE,
                            ""
                        )) && (mobileNumber!! == sharedPreferences.getString(
                            Constants.PREFS_USER_MOBILE_WITHOUT_COUNTRY,
                            ""
                        ))
                    ) {

                        GetUserDetailBackground().execute()
                    } else {

                        SendOTPBackground(
                            result.getJSONObject("data").getString("user_id")
                        ).execute()
                        VerifyOTPActivity.userId = result.getJSONObject("data").getString("user_id")
                        VerifyOTPActivity.otpAction = "login"
                        VerifyOTPActivity.mobileNumber = countryCode!! + mobileNumber!!
                        startActivity(Intent(this@LoginActivity, VerifyOTPActivity::class.java))
                    }


                } else {
                    UtilityFunctions.showAlertOnActivity(this@LoginActivity,
                        result.getString("message"), resources.getString(R.string.Ok).toString(),
                        "", false, true, {}, {})
                }

            }
        }
    }

    private inner class GetUserDetailBackground : AsyncTask<Void, Void, JSONObject?>() {

        override fun onPreExecute() {
            super.onPreExecute()
            UtilityFunctions.showProgressDialog(this@LoginActivity)
        }

        override fun doInBackground(vararg p0: Void?): JSONObject? {

            val jsonParser = JsonParser()
            val params = HashMap<String, String>()
            params["lang"] = sharedPreferences.getString(Constants.PREFS_LANG, "en")!!
            params["user_id"] = sharedPreferences.getString(Constants.PREFS_USER_ID, "")!!

            return jsonParser.makeHttpRequest(
                Constants.BASE_URL + "customer/get_user_detail",
                "POST",
                params
            )
        }

        override fun onPostExecute(result: JSONObject?) {
            UtilityFunctions.dismissProgressDialog()
            if (result != null) {
                if (result.getBoolean("status")) {

                    val editor = sharedPreferences!!.edit()
                    editor.putString(
                        Constants.PREFS_USER_MOBILE_WITHOUT_COUNTRY,
                        result.getJSONObject("data").getJSONObject("user_info")
                            .getString("mobile_number")
                    )
                    editor.putString(
                        Constants.PREFS_COUNTRY_CODE,
                        result.getJSONObject("data").getJSONObject("user_info")
                            .getString("country_code")
                    )
                    editor.putString(
                        Constants.PREFS_USER_ID,
                        result.getJSONObject("data").getJSONObject("user_info").getString("user_id")
                            .trim()
                    )
                    editor.putString(
                        Constants.PREFS_USER_TOKEN,
                        result.getJSONObject("data").getJSONObject("user_info").getString("token")
                            .trim()
                    )
                    editor.putString(
                        Constants.PREFS_USER_FULL_MOBILE,
                        result.getJSONObject("data").getJSONObject("user_info")
                            .getString("mobile_number").trim()
                    )

                    editor.putString(
                        Constants.PREFS_USER_FULL_NAME,
                        result.getJSONObject("data").getJSONObject("user_info")
                            .getString("full_name").trim()
                    )

                    editor.putString(
                        Constants.PREFS_USER_PIC,
                        result.getJSONObject("data").getJSONObject("user_info")
                            .getString("profile_pic").trim()
                    )

                    editor.putString(
                        Constants.PREFS_USER_PIC,
                        result.getJSONObject("data").getJSONObject("user_info")
                            .getString("profile_pic").trim()
                    )

                    editor.putBoolean(Constants.PREFS_IS_LOGIN, true)

                    editor.commit()
                    startActivity(
                        Intent(
                            this@LoginActivity,
                            SenderLocationActivity::class.java
                        )
                    )
                    finishAffinity()


                } else {
                    UtilityFunctions.showAlertOnActivity(this@LoginActivity,
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

    override fun onResume() {
        checkUpdate().execute()
        super.onResume()

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

    private inner class checkUpdate :
        AsyncTask<Void, Void, JSONObject?>() {

        override fun doInBackground(vararg p0: Void?): JSONObject? {

            val jsonParser = JsonParser()
            val params = HashMap<String, String>()
            params["lang"] = sharedPreferences.getString(Constants.PREFS_LANG, "en")!!
            params["app_version"] = Constants.APP_VERSION

            return jsonParser.makeHttpRequest(
                Constants.BASE_URL + "customer/check_app_version",
                "POST",
                params
            )
        }

        override fun onPostExecute(result: JSONObject?) {
            if (result != null) {
                if (result.getBoolean("status")) {

                    if (result.getJSONObject("data").getString("is_update_available") == "yes") {

                        val builder = AlertDialog.Builder(this@LoginActivity)
                        val inflaterAlert =
                            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                        val viewDialog = inflaterAlert.inflate(R.layout.update_dialog, null)
                        builder.setView(viewDialog)
                        val dialog = builder.create()

                        val titleTextView = viewDialog.findViewById<TextView>(R.id.titletextView)
                        val messageTextView = viewDialog.findViewById<TextView>(R.id.messageTextView)
                        val yesButton = viewDialog.findViewById<Button>(R.id.yesButton)
                        val noButton = viewDialog.findViewById<Button>(R.id.noButton)

                        messageTextView.text = result.getJSONObject("data").getString("message")
                        titleTextView.text = result.getJSONObject("data").getString("title")
                        yesButton.text = result.getJSONObject("data").getString("yes_button_title")
                        noButton.text = result.getJSONObject("data").getString("no_button_title")

                        if (result.getJSONObject("data").getBoolean("show_no_button")) {
                            noButton.visibility = View.VISIBLE
                        } else {
                            noButton.visibility = View.GONE
                        }

                        yesButton.setOnClickListener {
                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=com.fast_prog.dyanate")
                                )
                            )

                        }

                        dialog.setCancelable(false)
                        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        dialog.show()
                    }

                }
            }
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
