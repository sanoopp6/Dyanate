package com.fast_prog.dyanate.views

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.fast_prog.dyanate.R
import com.fast_prog.dyanate.models.Drivers
import com.fast_prog.dyanate.utilities.Constants
import com.fast_prog.dyanate.utilities.JsonParser
import com.fast_prog.dyanate.utilities.UtilityFunctions
import kotlinx.android.synthetic.main.activity_driver_reply.*
import org.json.JSONException
import org.json.JSONObject

class DriverReplyActivity : AppCompatActivity() {

    private var sharedPreferences: SharedPreferences? = null

    private var driver: Drivers? = null
    private var tripID: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_reply)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        toolbar.setNavigationOnClickListener { backPressed() }

        customTitle(resources.getString(R.string.DriverReplies))

        driver = intent.getSerializableExtra("driver_detail") as Drivers?
        tripID = intent.getStringExtra("trip_id")

        txt_order_no.text = "$tripID/ 2020"
        txt_date.text = driver!!.tripDate
        text_driver.text = driver!!.driverName
        text_driver_no.text = driver!!.driverMobile
        text_distance.text = driver!!.distanceKm
        edittext_rate.setText(driver!!.tripRate)
        text_negotiable.text = driver!!.tripIsNegotiable.toString()

        btn_approve.setOnClickListener {

            var tripRateCustomer = edittext_rate.text.toString()
            if (tripRateCustomer.isNotEmpty()) {
                ApproveTripBackground(tripRateCustomer).execute()
            } else {
                UtilityFunctions.showAlertOnActivity(this,
                        getString(R.string.pls_enter_trip_rate),
                        getString(R.string.ok),
                        "",
                        false,
                        setCancelable = true,
                        actionOk = {}, actionCancel = {})
            }
        }

        btn_cancel.setOnClickListener {

//            UtilityFunctions.showAlertOnActivity(this,
//                    getString(R.string.are_you_sure_cancel_trip),
//                    getString(R.string.Yes),
//                    getString(R.string.No),
//                    true,
//                    setCancelable = true,
//                    actionOk = {
//                        CancelTripBackground().execute()
//                    }, actionCancel = {})

            val builder = AlertDialog.Builder(this@DriverReplyActivity)
            val inflaterAlert = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val viewDialog = inflaterAlert.inflate(R.layout.cancel_reason_layout, null)
            builder.setView(viewDialog)
            val dialog = builder.create()

            val buttonSubmit = viewDialog.findViewById<Button>(R.id.submitButton)
            val reasonEditText = viewDialog.findViewById<EditText>(R.id.reasonET)


            buttonSubmit.setOnClickListener {
                dialog.dismiss()

                var reason = reasonEditText.text.toString().trim()
                if (reason.isEmpty()) {
                    UtilityFunctions.showAlertOnActivity(this@DriverReplyActivity,
                        getString(R.string.pls_add_your_rating),
                        getString(R.string.ok),
                        "",
                        false,
                        true,
                        {},
                        {})
                    return@setOnClickListener
                }


                CancelTripBackground(reason).execute()
            }

            dialog.setCancelable(true)
            dialog.show()
        }
    }

    private fun backPressed() {
        finish()
    }

    @SuppressLint("StaticFieldLeak")
    private inner class ApproveTripBackground internal constructor(internal var tripRateCustomer: String): AsyncTask<Void, Void, JSONObject>() {

        override fun onPreExecute() {
            super.onPreExecute()
            UtilityFunctions.showProgressDialog(this@DriverReplyActivity)
        }

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()

            params["trip_id"] = tripID!!
                params["customer_rate"] = tripRateCustomer

            return jsonParser.makeHttpRequest(Constants.BASE_URL + "customer/accept_trip", "POST", params)
        }

        override fun onPostExecute(response: JSONObject?) {
            UtilityFunctions.dismissProgressDialog()

            if (response != null) {
                try {
                    if (response.getBoolean("status")) {
                        UtilityFunctions.showAlertOnActivity(this@DriverReplyActivity, getString(R.string.trip_accepted_successfully), getString(R.string.ok)
                                , "", showCancelButton = false, setCancelable = true, actionOk = {
                            startActivity(Intent(this@DriverReplyActivity, SenderLocationActivity::class.java))
                            finishAffinity()
                        }, actionCancel = {})
                    } else {
                        UtilityFunctions.showAlertOnActivity(this@DriverReplyActivity, getString(R.string.error_occurred_pls_try_later), getString(R.string.ok)
                                , "", showCancelButton = false, setCancelable = true, actionOk = {}, actionCancel = {})
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                UtilityFunctions.showAlertOnActivity(this@DriverReplyActivity, getString(R.string.error_occurred_pls_try_later), getString(R.string.ok)
                , "", showCancelButton = false, setCancelable = true, actionOk = {}, actionCancel = {})
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class CancelTripBackground internal constructor(internal var reason: String): AsyncTask<Void, Void, JSONObject>() {

        override fun onPreExecute() {
            super.onPreExecute()
            UtilityFunctions.showProgressDialog(this@DriverReplyActivity)
        }

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()

            params["trip_id"] = tripID!!
            params["reason"] = reason
            params["lang"] = sharedPreferences!!.getString(Constants.PREFS_LANG, "ar")!!

            return jsonParser.makeHttpRequest(Constants.BASE_URL + "customer/cancel_trip", "POST", params)
        }

        override fun onPostExecute(response: JSONObject?) {
            UtilityFunctions.dismissProgressDialog()

            if (response != null) {
                try {
                    if (response.getBoolean("status")) {
                        UtilityFunctions.showAlertOnActivity(this@DriverReplyActivity, getString(R.string.trip_cancelled_successfully), getString(R.string.ok)
                                , "", showCancelButton = false, setCancelable = true, actionOk = {
                            startActivity(Intent(this@DriverReplyActivity, SenderLocationActivity::class.java))
                            finishAffinity()
                        }, actionCancel = {})
                    } else {
                        UtilityFunctions.showAlertOnActivity(this@DriverReplyActivity, getString(R.string.error_occurred_pls_try_later), getString(R.string.ok)
                                , "", showCancelButton = false, setCancelable = true, actionOk = {}, actionCancel = {})
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                UtilityFunctions.showAlertOnActivity(this@DriverReplyActivity, getString(R.string.error_occurred_pls_try_later), getString(R.string.ok)
                        , "", showCancelButton = false, setCancelable = true, actionOk = {}, actionCancel = {})
            }
        }
    }

}
