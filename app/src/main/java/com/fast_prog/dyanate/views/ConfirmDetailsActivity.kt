package com.fast_prog.dyanate.views

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.fast_prog.dyanate.R
import com.fast_prog.dyanate.models.Drivers
import com.fast_prog.dyanate.models.Ride
import com.fast_prog.dyanate.utilities.ConnectionDetector
import com.fast_prog.dyanate.utilities.Constants
import com.fast_prog.dyanate.utilities.JsonParser
import com.fast_prog.dyanate.utilities.UtilityFunctions
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_confirm_details.*
import kotlinx.android.synthetic.main.content_confirm_details.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class ConfirmDetailsActivity : AppCompatActivity() {

    internal lateinit var sharedPreferences: SharedPreferences

    private var goingBack: Boolean = false

    private var tripID: String? = null

    var estimated_trip_price = "0"
    var estimated_labour_price = "0"
    var estimated_price = "0"
    var estimated_distance = "0"
    var estimated_duration = "0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_details)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        estimated_price = intent.getStringExtra("estimated_price")
        estimated_distance = intent.getStringExtra("estimated_distance")
        estimated_duration = intent.getStringExtra("estimated_duration")
        estimated_trip_price = intent.getStringExtra("estimated_trip_price")
        estimated_labour_price = intent.getStringExtra("estimated_labour_price")

        toolbar.setNavigationOnClickListener {
            if (tripID != null && Integer.parseInt(tripID!!) > 0) {
                if (ConnectionDetector.isConnected(this@ConfirmDetailsActivity)) {
                    goingBack = true
                    TripMasterStatusUpdateBackground("4").execute()
                } else {
                    ConnectionDetector.errorSnackbar(coordinator_layout)
                }
            } else {
                finish()
            }
        }

        customTitle(resources.getString(R.string.ConfirmDetail))

        try {
            Ride.instance
        } catch (e: Exception) {
            Ride.instance = Ride()
        }

        shipmentTitleTextView.text =
            String.format(Locale.getDefault(), "%s :", resources.getString(R.string.Shipment))
        fromNameTitleTextView.text =
            String.format(Locale.getDefault(), "%s :", resources.getString(R.string.Name))
        fromMobTitleTextView.text =
            String.format(Locale.getDefault(), "%s :", resources.getString(R.string.Mobile))
//        engDateTitleTextView.text =
//            String.format(Locale.getDefault(), "%s :", resources.getString(R.string.Date))
//        arDateTitleTextView.text =
//            String.format(Locale.getDefault(), "%s :", resources.getString(R.string.Date))
        timeTitleTextView.text =
            String.format(Locale.getDefault(), "%s :", resources.getString(R.string.Time))
        toNameTitleTextView.text =
            String.format(Locale.getDefault(), "%s :", resources.getString(R.string.Name))
        toMobTitleTextView.text =
            String.format(Locale.getDefault(), "%s :", resources.getString(R.string.Mobile))

        val flag = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        val colorSpan = ForegroundColorSpan(
            ContextCompat.getColor(
                this@ConfirmDetailsActivity,
                R.color.lightBlueColor
            )
        )
        var builder = SpannableStringBuilder()

        var spannableString = SpannableString(resources.getString(R.string.Subject) + " : ")
        spannableString.setSpan(colorSpan, 0, spannableString.length, flag)
        builder.append(spannableString)
        builder.append(Ride.instance.shipmentTypeName)
        subjectTextView.text = builder

        shipmentTextView.text = Ride.instance.shipment

        builder = SpannableStringBuilder()
        spannableString = SpannableString(resources.getString(R.string.Size) + " : ")
        spannableString.setSpan(colorSpan, 0, spannableString.length, flag)
        builder.append(spannableString)
        builder.append(Ride.instance.vehicleSizeName)
        vehicleTextView.text = builder

        fromNameTextView.text = Ride.instance.fromName
        fromMobTextView.text = Ride.instance.fromMobile.trimStart { it <= '+' }
        engDateTextView.text = Ride.instance.date
        arDateTextView.text = Ride.instance.hijriDate
        timeTextView.text = Ride.instance.time
        toNameTextView.text = Ride.instance.toName
        toMobTextView.text = Ride.instance.toMobile.trimStart { it <= '+' }
        estimatedValueTextView.text = estimated_price + " " + getString(R.string.SAR)
//        workersRequiredValueTextView.text = Ride.instance.requiredPersons
        loading_labour_count.text = getString(R.string.loading) + ": " + Ride.instance.loadingCount
        unloading_labour_count.text =
            getString(R.string.unloading) + ": " + Ride.instance.unloadingCount
        if (Ride.instance.requiredUnpackAndInstall == "1") {
            installationRequiredValue.text = getString(R.string.Yes)
        } else {
            installationRequiredValue.text = getString(R.string.No)
        }

        confirmTripButton.setOnClickListener {
            UtilityFunctions.showAlertOnActivity(this@ConfirmDetailsActivity,
                resources.getString(R.string.AreYouSure), resources.getString(R.string.Yes),
                resources.getString(R.string.No), true, false,
                {
                    if (ConnectionDetector.isConnected(this@ConfirmDetailsActivity)) {
                        AddTripMasterBackground().execute()
                    } else {
                        ConnectionDetector.errorSnackbar(coordinator_layout)
                    }
                }, {})
        }

        goingBack = true
    }

    override fun onBackPressed() {
        super.onBackPressed()

        if (tripID != null && Integer.parseInt(tripID!!) > 0) {
            if (ConnectionDetector.isConnected(this@ConfirmDetailsActivity)) {
                goingBack = true
                TripMasterStatusUpdateBackground("4").execute()
            } else {
                ConnectionDetector.errorSnackbar(coordinator_layout)
            }

        } else {
            finish()
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class AddTripMasterBackground : AsyncTask<Void, Void, JSONObject>() {

        override fun onPreExecute() {
            super.onPreExecute()
            UtilityFunctions.showProgressDialog(this@ConfirmDetailsActivity)
        }

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()

            params["ArgTripMCustId"] = sharedPreferences.getString(Constants.PREFS_USER_ID, "")!!
            params["ArgTripMScheduleDate"] = Ride.instance.date
            params["ArgTripMScheduleTime"] = Ride.instance.time
            params["ArgTripMFromLat"] = Ride.instance.pickUpLatitude!!
            params["ArgTripMFromLng"] = Ride.instance.pickUpLongitude!!
            params["ArgTripMFromAddress"] = Ride.instance.pickUpLocation!!
            params["ArgTripMFromIsSelf"] = "false"
            params["ArgTripMFromName"] = Ride.instance.fromName
            params["ArgTripMFromMob"] = Ride.instance.fromMobile
            params["ArgTripMToLat"] = Ride.instance.dropOffLatitude!!
            params["ArgTripMToLng"] = Ride.instance.dropOffLongitude!!
            params["ArgTripMToAddress"] = Ride.instance.dropOffLocation!!
            params["ArgTripMToIsSelf"] = "false"
            params["ArgTripMToName"] = Ride.instance.toName
            params["ArgTripMToMob"] = Ride.instance.toMobile
            params["ArgTripMSubject"] = Ride.instance.shipmentTypeName
            params["ArgTripMNotes"] = Ride.instance.shipment
            params["ArgTripMVsId"] = Ride.instance.vehicleSizeId!!
            params["ArgTripMCustLat"] = "0"
            params["ArgTripMCustLng"] = "0"
            params["ArgTripMNoOfDrivers"] = "0"
            params["ArgTripMDistanceRadiusKm"] = "0"
            params["estimated_price"] = estimated_price
            params["estimated_trip_price"] = estimated_trip_price
            params["estimated_labour_price"] = estimated_labour_price
            params["estimated_distance"] = estimated_distance
            params["estimated_duration"] = estimated_duration
            if (Ride.instance.distanceStr != null) {
                params["ArgTripMDistanceString"] = Ride.instance.distanceStr!!
            } else {
                params["ArgTripMDistanceString"] = "NA"
            }

            params["required_persons"] = Ride.instance.requiredPersons
            params["unpack_and_install_requirement"] = Ride.instance.requiredUnpackAndInstall
            params["loading_count"] = Ride.instance.loadingCount
            params["unloading_count"] = Ride.instance.unloadingCount
            params["is_loading_unloading_calculation"] = "1"

            return jsonParser.makeHttpRequest(
                Constants.BASE_URL + "customer/add_trip",
                "POST",
                params
            )
        }

        override fun onPostExecute(response: JSONObject?) {
            if (response != null) {
                try {
                    if (response.getBoolean("status")) {
                        tripID = response.getString("data").toInt().toString()
//                        AutoAllocateNearestDriverByCustLatLngTripMIdBackground().execute()

//                        val intent = Intent(this@ConfirmDetailsActivity, WaitDriverActivity::class.java)
//                        intent.putExtra("from_lat", Ride.instance.pickUpLatitude!!.toDouble())
//                        intent.putExtra("from_long", Ride.instance.pickUpLongitude!!.toDouble())
//                        intent.putExtra("trip_id", tripID)
////                        intent.putExtra("driver", driver)
//                        startActivity(intent)

                        UtilityFunctions.showAlertOnActivity(this@ConfirmDetailsActivity,
                            getString(R.string.trip_added_successfully),
                            resources.getString(R.string.Ok),
                            "",
                            false,
                            false,
                            {
                                startActivity(
                                    Intent(
                                        this@ConfirmDetailsActivity,
                                        SenderLocationActivity::class.java
                                    )
                                )
                            },
                            {})

                    } else {
                        UtilityFunctions.dismissProgressDialog()
                        UtilityFunctions.showAlertOnActivity(this@ConfirmDetailsActivity,
                            response.getString("message"), resources.getString(R.string.Ok),
                            "", false, false, {}, {})
                    }

                } catch (e: JSONException) {
                    UtilityFunctions.dismissProgressDialog()
                    e.printStackTrace()
                }

            } else {
                UtilityFunctions.dismissProgressDialog()
                val snackbar = Snackbar.make(
                    coordinator_layout,
                    R.string.UnableToConnect,
                    Snackbar.LENGTH_LONG
                ).setAction(R.string.Ok) { }
                snackbar.show()
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class AutoAllocateNearestDriverByCustLatLngTripMIdBackground :
        AsyncTask<Void, Void, JSONObject>() {

        override fun doInBackground(vararg voids: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()

            params["ArgFromlat"] = Ride.instance.pickUpLatitude!!
            params["ArgFromlng"] = Ride.instance.pickUpLongitude!!
            params["ArgTripMID"] = tripID!!

            var BASE_URL = Constants.BASE_URL_EN + "AutoAllocateNearestDriverByCustLatLngTripMId"

            if (sharedPreferences.getString(Constants.PREFS_LANG, "en")!!.equals(
                    "ar",
                    ignoreCase = true
                )
            ) {
                BASE_URL = Constants.BASE_URL_AR + "AutoAllocateNearestDriverByCustLatLngTripMId"
            }

            return jsonParser.makeHttpRequest(BASE_URL, "POST", params)
        }

        override fun onPostExecute(response: JSONObject?) {
            UtilityFunctions.dismissProgressDialog()

            if (response != null) {
                try {
                    if (response.getBoolean("status")) {
                        val driversJsonArray = response.getJSONArray("data")
                        if (driversJsonArray.length() > 0) {
                            val driver = Drivers()

                            for (i in 0 until driversJsonArray.length()) {
//                                driver.dmId = driversJsonArray.getJSONObject(i).getString("DmId").trim()
//                                driver.dmName = driversJsonArray.getJSONObject(i).getString("DmName").trim()
//                                driver.dmAddress = driversJsonArray.getJSONObject(i).getString("DmAddress").trim()
                                driver.dmMobNumber =
                                    driversJsonArray.getJSONObject(i).getString("DmMobNumber")
                                        .trim()
                                driver.dmLatitude =
                                    driversJsonArray.getJSONObject(i).getString("DmLatitude").trim()
                                driver.dmLongitude =
                                    driversJsonArray.getJSONObject(i).getString("DmLongitude")
                                        .trim()
                                driver.distanceKm =
                                    driversJsonArray.getJSONObject(i).getString("DistanceKm").trim()
//                                driver.isAccepted = false
//                                driver.isRejected = false
                            }

                            val intent =
                                Intent(this@ConfirmDetailsActivity, WaitDriverActivity::class.java)
                            intent.putExtra("from_lat", Ride.instance.pickUpLatitude!!.toDouble())
                            intent.putExtra("from_long", Ride.instance.pickUpLongitude!!.toDouble())
                            intent.putExtra("trip_id", tripID)
                            intent.putExtra("driver", driver)
                            startActivity(intent)

                            Ride.instance = Ride()

                        } else {
                            cancelOrRetry()
                        }
                    } else {
                        cancelOrRetry()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun cancelOrRetry() {
        val builder = AlertDialog.Builder(this@ConfirmDetailsActivity)
        val inflater = this@ConfirmDetailsActivity.getLayoutInflater()
        val view = inflater.inflate(R.layout.alert_dialog, null)
        builder.setView(view)
        val alertDialog = builder.create()
        alertDialog.setCancelable(false)

        val txtAlert = view.findViewById(R.id.txt_alert) as TextView
        val btnPutInWait = view.findViewById(R.id.btn_wait) as Button
        val btnCancelTrip = view.findViewById(R.id.btn_cancel) as Button
        val btnRetryWithAny = view.findViewById(R.id.btn_ok) as Button

        txtAlert.setText(R.string.NoDriversFound)
        btnPutInWait.setText(R.string.PutInWait)
        btnPutInWait.visibility = View.VISIBLE
        btnCancelTrip.setText(R.string.CancelTrip)
        btnRetryWithAny.setText(R.string.RetryWithAny)

        btnPutInWait.setOnClickListener {
            alertDialog!!.dismiss()
            goingBack = false
            TripMasterStatusUpdateBackground("8").execute()
        }

        btnCancelTrip.setOnClickListener {
            alertDialog!!.dismiss()
            goingBack = false
            TripMasterStatusUpdateBackground("4").execute()
        }

        if (Ride.instance.vehicleSizeId!!.equals("0", ignoreCase = true)) {
            btnRetryWithAny.visibility = View.GONE

        } else {
            btnRetryWithAny.setOnClickListener {
                alertDialog!!.dismiss()
                Ride.instance.vehicleSizeId = "0"
                UpdateTripMasterBackground().execute()
            }
        }

        alertDialog.show()
    }

    @SuppressLint("StaticFieldLeak")
    private inner class UpdateTripMasterBackground : AsyncTask<Void, Void, JSONObject>() {

        override fun onPreExecute() {
            super.onPreExecute()
            UtilityFunctions.showProgressDialog(this@ConfirmDetailsActivity)
        }

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()

            params["ArgTripMId"] = tripID!!
            params["ArgTripMVsId"] = "0"

            var BASE_URL = Constants.BASE_URL_EN + "UpdateTripMaster"

            if (sharedPreferences.getString(Constants.PREFS_LANG, "en")!!.equals(
                    "ar",
                    ignoreCase = true
                )
            ) {
                BASE_URL = Constants.BASE_URL_AR + "UpdateTripMaster"
            }

            return jsonParser.makeHttpRequest(BASE_URL, "POST", params)
        }

        override fun onPostExecute(response: JSONObject?) {
            if (response != null) {
                try {
                    if (response.getBoolean("status")) {
                        AutoAllocateNearestDriverByCustLatLngTripMIdBackground().execute()
                    } else {
                        UtilityFunctions.dismissProgressDialog()
                    }
                } catch (e: JSONException) {
                    UtilityFunctions.dismissProgressDialog()
                    e.printStackTrace()
                }
            } else {
                UtilityFunctions.dismissProgressDialog()
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class TripMasterStatusUpdateBackground internal constructor(internal var status: String) :
        AsyncTask<Void, Void, JSONObject>() {

        override fun onPreExecute() {
            super.onPreExecute()
            UtilityFunctions.showProgressDialog(this@ConfirmDetailsActivity)
        }

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()

            params["ArgTripMID"] = tripID!!
            params["ArgTripMStatus"] = status

            var BASE_URL = Constants.BASE_URL_EN + "TripMasterStatusUpdate"

            if (sharedPreferences.getString(Constants.PREFS_LANG, "en")!!.equals(
                    "ar",
                    ignoreCase = true
                )
            ) {
                BASE_URL = Constants.BASE_URL_AR + "TripMasterStatusUpdate"
            }

            return jsonParser.makeHttpRequest(BASE_URL, "POST", params)
        }

        override fun onPostExecute(response: JSONObject?) {
            UtilityFunctions.dismissProgressDialog()

            if (response != null) {
                try {
                    if (response.getBoolean("status")) {
                        Ride.instance = Ride()

                        if (goingBack) {
                            finish()
                        } else {
                            val intent =
                                Intent(this@ConfirmDetailsActivity, ShipmentDetActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }

}
