package com.fast_prog.dyanate.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaPlayer
import android.os.AsyncTask
import android.os.Bundle
import android.os.CountDownTimer
import android.util.DisplayMetrics
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.fast_prog.dyanate.R
import com.fast_prog.dyanate.models.Drivers
import com.fast_prog.dyanate.models.Ride
import com.fast_prog.dyanate.utilities.ConnectionDetector
import com.fast_prog.dyanate.utilities.Constants
import com.fast_prog.dyanate.utilities.JsonParser
import com.fast_prog.dyanate.utilities.UtilityFunctions
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_wait_driver.*
import kotlinx.android.synthetic.main.content_wait_driver.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class WaitDriverActivity : AppCompatActivity(), OnMapReadyCallback {

    internal lateinit var sharedPreferences: SharedPreferences

    internal var tripID: String = ""

    private lateinit var googleMap: GoogleMap

    internal lateinit var userLocation: LatLng

//    internal lateinit var driver: Drivers

    internal var singleTimer: CountDownTimer? = null

    internal var SINGLE_TIME_OUT = 44000

//    internal var pendingTime = 150000

    internal var mp: MediaPlayer? = null

    internal var statusBool = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wait_driver)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        toolbar.setNavigationOnClickListener { backpressed() }

        customTitle(resources.getString(R.string.SearchingForDriver))

        pulsator.start()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

//        driver = intent.getSerializableExtra("driver") as Drivers

        tripID = intent.getStringExtra("trip_id")
        userLocation =
            LatLng(intent.getDoubleExtra("from_lat", 0.0), intent.getDoubleExtra("from_long", 0.0))

        searching_for_driver_text_view.startAnimation(UtilityFunctions.blinkAnimation)

        statusBool = false

        singleTimer = object : CountDownTimer(SINGLE_TIME_OUT.toLong(), 3000) {
            override fun onTick(millisUntilFinished: Long) {
//                pendingTime -= 3000
                if (ConnectionDetector.isConnected(this@WaitDriverActivity)) {
                    CheckDriverBackground().execute()
                }
            }

            override fun onFinish() {
//                pendingTime -= 3000

                if (ConnectionDetector.isConnected(this@WaitDriverActivity)) {
                    AutoAllocateNearestDriverBackground().execute()
                }
            }
        }.start()
        AutoAllocateNearestDriverBackground().execute()
    }


    override fun onBackPressed() {
        backpressed()
    }

    private fun backpressed() {
        UtilityFunctions.showAlertOnActivity(this@WaitDriverActivity,
            resources.getString(R.string.DoYouWantToWaitMore), resources.getString(R.string.Yes),
            resources.getString(R.string.No), true, false,
            {


            },
            {

                if (ConnectionDetector.isConnected(this@WaitDriverActivity)) {
                    CancelTripBackground().execute()
                } else {
                    ConnectionDetector.errorSnackbar(coordinator_layout)
                }

            })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        addDriverMarkers()
    }

    private fun addDriverMarkers() {
        this.googleMap.clear()

        this.googleMap.uiSettings.isCompassEnabled = false
        this.googleMap.uiSettings.isMyLocationButtonEnabled = false
        this.googleMap.uiSettings.isMapToolbarEnabled = false
        this.googleMap.uiSettings.isRotateGesturesEnabled = false

        val marker = layoutInflater.inflate(R.layout.cab_marker_green, null)

        if (marker != null) {
            val passengerMarker = googleMap.addMarker(
                MarkerOptions().position(userLocation).icon(
                    BitmapDescriptorFactory.fromBitmap(
                        createDrawableFromView(marker)
                    )
                )
            )
            passengerMarker.tag = -1
        }

        googleMap.setOnMapLoadedCallback {
            googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    userLocation,
                    15.0f
                )
            )
        }
    }

    private fun createDrawableFromView(view: View): Bitmap {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        view.layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        view.measure(displayMetrics.widthPixels, displayMetrics.heightPixels)
        view.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)
        view.buildDrawingCache()
        val bitmap =
            Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        view.draw(canvas)

        return bitmap
    }

    public override fun onDestroy() {
        super.onDestroy()

        if (singleTimer != null) {
            singleTimer!!.cancel()
        }
    }


    @SuppressLint("StaticFieldLeak")
    private inner class AutoAllocateNearestDriverBackground : AsyncTask<Void, Void, JSONObject>() {

        override fun onPreExecute() {
            super.onPreExecute()

        }

        override fun doInBackground(vararg voids: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()

            params["latitude"] = Ride.instance.pickUpLatitude!!
            params["longitude"] = Ride.instance.pickUpLongitude!!
            params["trip_id"] = tripID!!


            return jsonParser.makeHttpRequest(
                Constants.BASE_URL + "customer/auto_allocate_nearest_driver",
                "POST",
                params
            )
        }

        override fun onPostExecute(response: JSONObject?) {
            if (response != null) {
                try {
                    if (response.getBoolean("status")) {
//                        val driversJsonArray = response.getJSONArray("data")
//                        if (driversJsonArray.length() > 0) {


//                        }
                        singleTimer!!.start()
                    } else {
                        singleTimer!!.cancel()
                        UtilityFunctions.showAlertOnActivity(this@WaitDriverActivity,
                            getString(R.string.driver_not_found),
                            getString(R.string.Ok),
                            "", false, false, {
                                startActivity(
                                    Intent(
                                        this@WaitDriverActivity,
                                        SenderLocationActivity::class.java
                                    )
                                )
                                finishAffinity()
                            }, {}
                        )
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }


//    @SuppressLint("StaticFieldLeak")
//    private inner class TripDetailsMasterListBackground : AsyncTask<Void, Void, JSONObject>() {
//
//        override fun doInBackground(vararg voids: Void): JSONObject? {
//            val jsonParser = JsonParser()
//            val params = HashMap<String, String>()
//
//            params["ArgTripMCustId"] = "0"
//            params["ArgTripDDmId"] = "0"
//            params["ArgTripMID"] = tripID
//            params["ArgTripDID"] = "0"
//            params["ArgTripMStatus"] = "0"
//            params["ArgTripDStatus"] = "0"
//            params["ArgExcludeCustId"] = "0"
//
//            var BASE_URL = Constants.BASE_URL_EN + "TripDetailsMasterList"
//
//            if (sharedPreferences.getString(Constants.PREFS_LANG, "en")!!.equals("ar", ignoreCase = true)) {
//                BASE_URL = Constants.BASE_URL_AR + "TripDetailsMasterList"
//            }
//
//            return jsonParser.makeHttpRequest(BASE_URL, "POST", params)
//        }
//
//        override fun onPostExecute(result: JSONObject?) {
//            super.onPostExecute(result)
//
//            if (result != null) {
//                try {
//                    if (result.getBoolean("status")) {
//                        val ordersJSONArray = result.getJSONArray("data")
//
//                        if (ordersJSONArray.length() > 0) {
//                            driver.dmLatitude = ordersJSONArray.getJSONObject(0).getString("DmLatitude").trim()
//                            driver.dmLongitude = ordersJSONArray.getJSONObject(0).getString("DmLongitude").trim()
//
//                            if (ordersJSONArray.getJSONObject(0).getString("TripDStatus").trim().equals("1", true)) {
//                                if (!statusBool) {
//                                    statusBool = true
//
//                                    pulsator.stop()
//
//                                    if (singleTimer != null) {
//                                        singleTimer!!.cancel()
//                                    }
//
//                                    if (mp != null) {
//                                        mp = MediaPlayer.create(this@WaitDriverActivity, R.raw.demonstrative)
//                                        mp!!.start()
//                                    }
//
//                                    val intent = Intent(this@WaitDriverActivity, DriverRepliesActivity::class.java)
//                                    intent.putExtra("backEnabled", false)
//                                    intent.putExtra("tripID", tripID.toInt())
//
//                                    ActivityCompat.finishAffinity(this@WaitDriverActivity)
//                                    startActivity(intent)
//                                    finish()
//                                }
//
//                            } else if (ordersJSONArray.getJSONObject(0).getString("TripDStatus").trim().equals("5", true)) {
//                                if (!statusBool) {
//                                    statusBool = true
//
//                                    if (singleTimer != null) {
//                                        singleTimer!!.cancel()
//                                    }
//
//                                    AutoAllocateNearestDriverByCustLatLngTripMIdBackground().execute()
//                                }
//                            }
//                        }
//                    }
//                } catch (e: JSONException) {
//                    e.printStackTrace()
//                }
//
//            }
//        }
//    }

    @SuppressLint("StaticFieldLeak")
    private inner class CheckDriverBackground : AsyncTask<Void, Void, JSONObject>() {

        override fun onPreExecute() {
            super.onPreExecute()
        }

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()

            params["lang"] = sharedPreferences.getString(Constants.PREFS_LANG, "ar")!!
            params["trip_id"] = tripID
            params["user_id"] = sharedPreferences.getString(Constants.PREFS_USER_ID, "")!!

            return jsonParser.makeHttpRequest(
                Constants.BASE_URL + "customer/check_driver_assigned_to_trip",
                "POST",
                params
            )
        }

        override fun onPostExecute(response: JSONObject?) {

            if (response != null) {
                try {
                    if (response.getBoolean("status")) {

                        val driver = Drivers()

                        driver.driverID =
                            response.getJSONObject("data").getString("driver_id").trim()
                        driver.driverMobile =
                            response.getJSONObject("data").getString("driver_mobile").trim()
                        driver.driverName =
                            response.getJSONObject("data").getString("driver_name").trim()
                        driver.distanceKm =
                            response.getJSONObject("data").getString("distance").trim()
                        driver.tripRate =
                            response.getJSONObject("data").getString("trip_rate").trim()
                        driver.tripIsNegotiable =
                            response.getJSONObject("data").getBoolean("trip_negotiable")
                        driver.tripDate = response.getJSONObject("data").getString("scheduled_date")

                        if (!statusBool) {
                            statusBool = true

                            pulsator.stop()

                            if (mp != null) {
                                mp =
                                    MediaPlayer.create(this@WaitDriverActivity, R.raw.demonstrative)
                                mp!!.start()
                            }

                            val intent =
                                Intent(this@WaitDriverActivity, DriverReplyActivity::class.java)
                            intent.putExtra("driver_detail", driver)
                            intent.putExtra("trip_id", tripID)

                            ActivityCompat.finishAffinity(this@WaitDriverActivity)
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

    @SuppressLint("StaticFieldLeak")
    private inner class CancelTripBackground : AsyncTask<Void, Void, JSONObject>() {

        override fun onPreExecute() {
            super.onPreExecute()
            UtilityFunctions.showProgressDialog(this@WaitDriverActivity)
        }

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()

            params["trip_id"] = tripID
            params["user_id"] = sharedPreferences.getString(Constants.PREFS_USER_ID, "")!!

            return jsonParser.makeHttpRequest(
                Constants.BASE_URL + "customer/cancel_trip",
                "POST",
                params
            )
        }

        override fun onPostExecute(response: JSONObject?) {
            UtilityFunctions.dismissProgressDialog()

            if (response != null) {
                try {
                    if (response.getBoolean("status")) {

                        val intent =
                            Intent(this@WaitDriverActivity, ShipmentDetActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }

//    @SuppressLint("StaticFieldLeak")
//    private inner class TripMasterStatusUpdateBackground internal constructor(internal var status: String) : AsyncTask<Void, Void, JSONObject>() {
//
//        override fun onPreExecute() {
//            super.onPreExecute()
//            UtilityFunctions.showProgressDialog(this@WaitDriverActivity)
//        }
//
//        override fun doInBackground(vararg param: Void): JSONObject? {
//            val jsonParser = JsonParser()
//            val params = HashMap<String, String>()
//
//            params["ArgTripMID"] = tripID
//            params["ArgTripMStatus"] = status
//
//            var BASE_URL = Constants.BASE_URL_EN + "TripMasterStatusUpdate"
//
//            if (sharedPreferences.getString(Constants.PREFS_LANG, "en")!!.equals("ar", ignoreCase = true)) {
//                BASE_URL = Constants.BASE_URL_AR + "TripMasterStatusUpdate"
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
//                        val intent = Intent(this@WaitDriverActivity, ShipmentDetActivity::class.java)
//                        startActivity(intent)
//                        finish()
//                    }
//                } catch (e: JSONException) {
//                    e.printStackTrace()
//                }
//            }
//        }
//    }

//    @SuppressLint("StaticFieldLeak")
//    private inner class AutoAllocateNearestDriverByCustLatLngTripMIdBackground : AsyncTask<Void, Void, JSONObject>() {
//
//        override fun doInBackground(vararg voids: Void): JSONObject? {
//            val jsonParser = JsonParser()
//            val params = HashMap<String, String>()
//
//            params["latitude"] = userLocation.latitude.toString() + ""
//            params["longitude"] = userLocation.longitude.toString() + ""
//            params["trip_id"] = tripID
//
//            return jsonParser.makeHttpRequest(Constants.BASE_URL + "customer/auto_allocate_nearest_driver", "POST", params)
//        }
//
//        override fun onPostExecute(response: JSONObject?) {
//            if (response != null) {
//                try {
//                    if (response.getBoolean("status")) {
//                        val driversJsonArray = response.getJSONArray("data")
//
//                        if (pendingTime >= 0) {
//                            if (driversJsonArray.length() > 0) {
//                                val driver = Drivers()
//
//                                for (i in 0 until driversJsonArray.length()) {
////                                    driver.dmId = driversJsonArray.getJSONObject(i).getString("DmId").trim()
////                                    driver.dmName = driversJsonArray.getJSONObject(i).getString("DmName").trim()
////                                    driver.dmAddress = driversJsonArray.getJSONObject(i).getString("DmAddress").trim()
//                                    driver.dmMobNumber = driversJsonArray.getJSONObject(i).getString("DmMobNumber").trim()
//                                    driver.dmLatitude = driversJsonArray.getJSONObject(i).getString("DmLatitude").trim()
//                                    driver.dmLongitude = driversJsonArray.getJSONObject(i).getString("DmLongitude").trim()
//                                    driver.distanceKm = driversJsonArray.getJSONObject(i).getString("DistanceKm").trim()
//                                    driver.isAccepted = false
//                                    driver.isRejected = false
//                                }
//
//                                statusBool = false
//
//                                singleTimer = object : CountDownTimer(SINGLE_TIME_OUT.toLong(), 3000) {
//                                    override fun onTick(millisUntilFinished: Long) {
//                                        pendingTime -= 3000
//                                        TripDetailsMasterListBackground().execute()
//                                    }
//
//                                    override fun onFinish() {
//                                        pendingTime -= 3000
//                                        AutoAllocateNearestDriverByCustLatLngTripMIdBackground().execute()
//                                    }
//                                }.start()
//                            }
//
//                        } else {
//                            if (singleTimer != null) {
//                                singleTimer!!.cancel()
//                            }
//
//                            UtilityFunctions.showAlertOnActivity(this@WaitDriverActivity,
//                                    resources.getString(R.string.DoYouWantToWaitMore), resources.getString(R.string.Yes),
//                                    resources.getString(R.string.No), true, false,
//                                    { TripMasterStatusUpdateBackground("8").execute() },
//                                    { TripMasterStatusUpdateBackground("4").execute() })
//                        }
//                    } else {
//                        if (singleTimer != null) {
//                            singleTimer!!.cancel()
//                        }
//
//                        UtilityFunctions.showAlertOnActivity(this@WaitDriverActivity,
//                                resources.getString(R.string.NoDriverDoYouWantToWaitMore), resources.getString(R.string.Yes),
//                                resources.getString(R.string.No), true, false,
//                                { TripMasterStatusUpdateBackground("8").execute() },
//                                { TripMasterStatusUpdateBackground("4").execute() })
//                    }
//                } catch (e: JSONException) {
//                    e.printStackTrace()
//                }
//            }
//        }
//    }

}
