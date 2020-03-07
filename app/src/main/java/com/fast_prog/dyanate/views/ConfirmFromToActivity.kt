package com.fast_prog.dyanate.views

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.fast_prog.dyanate.R
import com.fast_prog.dyanate.models.Ride
import com.fast_prog.dyanate.utilities.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_confirm_from_to.*
import kotlinx.android.synthetic.main.content_confirm_from_to.*
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class ConfirmFromToActivity : AppCompatActivity(), OnMapReadyCallback {

    internal lateinit var sharedPreferences: SharedPreferences

    private var mMap: GoogleMap? = null

    private var mapViewSatellite: Boolean = false

    private lateinit var start: LatLng
    private lateinit var stop: LatLng

    private lateinit var startMarker: Marker
    private lateinit var stopMarker: Marker

    internal lateinit var builder: LatLngBounds.Builder
    private lateinit var bounds: LatLngBounds

    private var tripID: String? = null
    private var goingBack: Boolean = false

    internal var distanceStr = ""
    internal var durationStr = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_from_to)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        toolbar.setNavigationOnClickListener {
            if (tripID != null && Integer.parseInt(tripID!!) > 0) {
                if (ConnectionDetector.isConnected(this@ConfirmFromToActivity)) {
                    goingBack = true
                    TripMasterStatusUpdateBackground("4").execute()
                } else {
                    ConnectionDetector.errorSnackbar(coordinator_layout)
                }
            } else { finish() }
        }

        customTitle(resources.getString(R.string.ConfirmTrip))

        try {
            Ride.instance
        } catch (e: Exception) {
            Ride.instance = Ride()
        }

        builder = LatLngBounds.Builder()

        mapViewSatellite = false

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

//        txt_distance.startAnimation(UtilityFunctions.blinkAnimation)

        btn_confirm_route.setOnClickListener {
//            UtilityFunctions.showAlertOnActivity(this@ConfirmFromToActivity,
//                    resources.getString(R.string.AreYouSure), resources.getString(R.string.Yes),
//                    resources.getString(R.string.No), true, false,
//                    {
                        if (ConnectionDetector.isConnected(this@ConfirmFromToActivity)) {
                            AddTripMasterBackground().execute()
                        } else {
                            ConnectionDetector.errorSnackbar(coordinator_layout)
                        }
//                    }, {})
        }

        btn_show_details.setOnClickListener {
            startActivity(Intent(this@ConfirmFromToActivity, ConfirmDetailsActivity::class.java))
        }

        image_view_map_change_icon.setOnClickListener {
            if (mMap != null) {
                if (!mapViewSatellite) {
                    mMap!!.mapType = GoogleMap.MAP_TYPE_SATELLITE
                    mapViewSatellite = true
                    image_view_map_change_icon.setColorFilter(Color.WHITE)

                } else {
                    mMap!!.mapType = GoogleMap.MAP_TYPE_NORMAL
                    mapViewSatellite = false
                    image_view_map_change_icon.setColorFilter(Color.BLACK)
                }

                mMap!!.uiSettings.isZoomControlsEnabled = false
                mMap!!.uiSettings.isMyLocationButtonEnabled = false
                mMap!!.uiSettings.isCompassEnabled = false
                mMap!!.uiSettings.isRotateGesturesEnabled = false
                mMap!!.uiSettings.isMapToolbarEnabled = false
            }
        }

        goingBack = true
    }

    override fun onBackPressed() {
        super.onBackPressed()

        if (tripID != null && Integer.parseInt(tripID!!) > 0) {
            if (ConnectionDetector.isConnected(this@ConfirmFromToActivity)) {
                goingBack = true
                TripMasterStatusUpdateBackground("4").execute()
            } else {
                ConnectionDetector.errorSnackbar(coordinator_layout)
            }
        } else { finish() }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        try {
            start = LatLng(Ride.instance.pickUpLatitude!!.toDouble(), Ride.instance.pickUpLongitude!!.toDouble())
            stop = LatLng(Ride.instance.dropOffLatitude!!.toDouble(), Ride.instance.dropOffLongitude!!.toDouble())
        } catch (e: Exception) {
            start = LatLng(0.0, 0.0)
            stop = LatLng(0.0, 0.0)
        }

        builder.include(start)
        builder.include(stop)

        mMap!!.mapType = GoogleMap.MAP_TYPE_NORMAL
        mMap!!.uiSettings.isMyLocationButtonEnabled = false
        mMap!!.uiSettings.isCompassEnabled = false
        mMap!!.uiSettings.isRotateGesturesEnabled = false
        mMap!!.uiSettings.isMapToolbarEnabled = false

        mMap!!.setOnMapLoadedCallback {
            bounds = builder.build()

            val l1 = Location("l1")
            l1.latitude = start.latitude
            l1.longitude = start.longitude

            val l2 = Location("l2")
            l2.latitude = stop.latitude
            l2.longitude = stop.longitude

            val distanceTo = l1.distanceTo(l2)

            if (distanceTo > 100)
                mMap!!.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            else
                mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(start, 15.0f))
        }

        val marker1 = layoutInflater.inflate(R.layout.cab_marker_green, null)

        if (marker1 != null) {
            startMarker = googleMap.addMarker(MarkerOptions().position(start).icon(BitmapDescriptorFactory.fromBitmap(createDrawableFromView(this@ConfirmFromToActivity, marker1))))
        }

        val marker2 = layoutInflater.inflate(R.layout.cab_marker_red, null)

        if (marker2 != null) {
            stopMarker = googleMap.addMarker(MarkerOptions().position(stop).icon(BitmapDescriptorFactory.fromBitmap(createDrawableFromView(this@ConfirmFromToActivity, marker2))))
        }

        val url = getDirectionsUrl(start, stop)

        if (ConnectionDetector.isConnected(this@ConfirmFromToActivity)) {
            val downloadTask = DownloadTask()
            downloadTask.execute(url)
        } else {
            ConnectionDetector.errorSnackbar(coordinator_layout)
        }
    }

    private fun getDirectionsUrl(origin: LatLng, dest: LatLng): String {
        val str_origin = "origin=" + origin.latitude + "," + origin.longitude
        val str_dest = "destination=" + dest.latitude + "," + dest.longitude
        val sensor = "sensor=false"
        val key = "key=" + Constants.GOOGLE_API_KEY
        val parameters = "$str_origin&$str_dest&$sensor&$key"
        val output = "json"

        return "https://maps.googleapis.com/maps/api/directions/$output?$parameters"
    }

    @Throws(IOException::class)
    private fun downloadUrl(strUrl: String): String {
        var data = ""
        var iStream: InputStream? = null
        var urlConnection: HttpURLConnection? = null

        try {
            val url = URL(strUrl)
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.connect()
            iStream = urlConnection.inputStream
            val br = BufferedReader(InputStreamReader(iStream!!))
            val sb = StringBuilder()
            var line: String?
            do {
                line = br.readLine()
                sb.append(line)
            } while (line != null)
            data = sb.toString()
            br.close()

        } catch (e: Exception) {
            e.printStackTrace()

        } finally {
            iStream!!.close()
            urlConnection!!.disconnect()
        }

        return data
    }

    @SuppressLint("StaticFieldLeak")
    private inner class DownloadTask : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg url: String): String {
            var data = ""

            try {
                data = downloadUrl(url[0])

            } catch (e: Exception) {
                e.printStackTrace()
            }

            return data
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)

            val parserTask = ParserTask()
            parserTask.execute(result)
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class ParserTask : AsyncTask<String, Int, List<List<HashMap<String, String>>>>() {
        override fun doInBackground(vararg jsonData: String): List<List<HashMap<String, String>>>? {

            val jObject: JSONObject
            var routes: List<List<HashMap<String, String>>>? = null

            try {
                jObject = JSONObject(jsonData[0])
                val parser = DirectionsJSONParser()

                routes = parser.parse(jObject)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return routes
        }

        override fun onPostExecute(result: List<List<HashMap<String, String>>>) {
            var points: ArrayList<LatLng>? = null
            var lineOptions: PolylineOptions? = null

            for (i in result.indices) {
                points = ArrayList()
                lineOptions = PolylineOptions()

                val path = result[i]

                for (j in path.indices) {
                    val point = path[j]

                    if (j == 0) {    // Get distance from the list
                        distanceStr = point["distance"].toString()
                        continue
                    } else if (j == 1) { // Get duration from the list
                        durationStr = point["duration"].toString()
                        continue
                    }

                    try {
                        val lat = point["lat"]?.toDouble()
                        val lng = point["lng"]?.toDouble()
                        val position = LatLng(lat!!, lng!!)
                        builder.include(position)
                        points.add(position)

                    } catch (ignored: Exception) { }
                }

                lineOptions.addAll(points)
                lineOptions.width(5f)
                lineOptions.color(Color.RED)
            }

            if (lineOptions != null) {
                mMap!!.addPolyline(lineOptions)
                Ride.instance.distanceStr = resources.getString(R.string.Distance) + " : " + distanceStr + ", " + resources.getString(R.string.Duration) + " : " + durationStr
                txt_distance.text = Ride.instance.distanceStr
            }

            btn_confirm_route.visibility = View.VISIBLE
            btn_show_details.visibility = View.VISIBLE
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class AddTripMasterBackground : AsyncTask<Void, Void, JSONObject>() {

        override fun onPreExecute() {
            super.onPreExecute()
            UtilityFunctions.showProgressDialog (this@ConfirmFromToActivity)
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
            if (Ride.instance.distanceStr != null) {
                params["ArgTripMDistanceString"] = Ride.instance.distanceStr!!
            } else {
                params["ArgTripMDistanceString"] = "NA"
            }


            return jsonParser.makeHttpRequest(Constants.BASE_URL + "customer/add_trip", "POST", params)
        }

        override fun onPostExecute(response: JSONObject?) {
            if (response != null) {
                try {
                    if (response.getBoolean("status")) {
                        tripID = response.getString("data").toInt().toString()
//                        AutoAllocateNearestDriverByCustLatLngTripMIdBackground().execute()

                        val intent = Intent(this@ConfirmFromToActivity, WaitDriverActivity::class.java)
                        intent.putExtra("from_lat", Ride.instance.pickUpLatitude!!.toDouble())
                        intent.putExtra("from_long", Ride.instance.pickUpLongitude!!.toDouble())
                        intent.putExtra("trip_id", tripID)
//                        intent.putExtra("driver", driver)
                        startActivity(intent)

                    } else {
                        UtilityFunctions.dismissProgressDialog()
                        UtilityFunctions.showAlertOnActivity(this@ConfirmFromToActivity,
                                response.getString("message"), resources.getString(R.string.Ok),
                                "", false, false, {}, {})
                    }

                } catch (e: JSONException) {
                    UtilityFunctions.dismissProgressDialog()
                    e.printStackTrace()
                }
            } else {
                UtilityFunctions.dismissProgressDialog()
                val snackbar = Snackbar.make(coordinator_layout, R.string.UnableToConnect, Snackbar.LENGTH_LONG).setAction(R.string.Ok) { }
                snackbar.show()
            }
        }
    }

//    @SuppressLint("StaticFieldLeak")
//    private inner class AutoAllocateNearestDriverByCustLatLngTripMIdBackground : AsyncTask<Void, Void, JSONObject>() {
//
//        override fun doInBackground(vararg voids: Void): JSONObject? {
//            val jsonParser = JsonParser()
//            val params = HashMap<String, String>()
//
//            params["latitude"] = Ride.instance.pickUpLatitude!!
//            params["longitude"] = Ride.instance.pickUpLongitude!!
//            params["trip_id"] = tripID!!
//
//
//            return jsonParser.makeHttpRequest(Constants.BASE_URL + "customer/auto_allocate_nearest_driver", "POST", params)
//        }
//
//        override fun onPostExecute(response: JSONObject?) {
//            UtilityFunctions.dismissProgressDialog()
//
//            if (response != null) {
//                try {
//                    if (response.getBoolean("status")) {
//                        val driversJsonArray = response.getJSONArray("data")
//                        if (driversJsonArray.length() > 0) {
//                            val driver = Drivers()
//
//                            for (i in 0 until driversJsonArray.length()) {
//                                driver.dmId = driversJsonArray.getJSONObject(i).getString("DmId").trim()
//                                driver.dmName = driversJsonArray.getJSONObject(i).getString("DmName").trim()
//                                driver.dmAddress = driversJsonArray.getJSONObject(i).getString("DmAddress").trim()
//                                driver.dmMobNumber = driversJsonArray.getJSONObject(i).getString("DmMobNumber").trim()
//                                driver.dmLatitude = driversJsonArray.getJSONObject(i).getString("DmLatitude").trim()
//                                driver.dmLongitude = driversJsonArray.getJSONObject(i).getString("DmLongitude").trim()
//                                driver.distanceKm = driversJsonArray.getJSONObject(i).getString("DistanceKm").trim()
//                                driver.isAccepted = false
//                                driver.isRejected = false
//                            }
//
//                            val intent = Intent(this@ConfirmFromToActivity, WaitDriverActivity::class.java)
//                            intent.putExtra("from_lat", Ride.instance.pickUpLatitude!!.toDouble())
//                            intent.putExtra("from_long", Ride.instance.pickUpLongitude!!.toDouble())
//                            intent.putExtra("trip_id", tripID)
//                            intent.putExtra("driver", driver)
//                            startActivity(intent)
//
//                            Ride.instance = Ride()
//
//                        } else { cancelOrRetry() }
//                    } else { cancelOrRetry() }
//                } catch (e: JSONException) { e.printStackTrace() }
//            }
//        }
//    }

    private fun cancelOrRetry() {
        val builder = AlertDialog.Builder(this@ConfirmFromToActivity)
        val inflater = this@ConfirmFromToActivity.getLayoutInflater()
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
            alertDialog.dismiss()
            goingBack = false
            TripMasterStatusUpdateBackground("8").execute()
        }

        btnCancelTrip.setOnClickListener {
            alertDialog.dismiss()
            goingBack = false
            TripMasterStatusUpdateBackground("4").execute()
        }

        if (Ride.instance.vehicleSizeId!!.equals("0", ignoreCase = true)) {
            btnRetryWithAny.visibility = View.GONE

        } else {
            btnRetryWithAny.setOnClickListener {
                alertDialog.dismiss()
                Ride.instance.vehicleSizeId = "0"
              //  UpdateTripMasterBackground().execute()
            }
        }

        alertDialog.show()
    }



//    @SuppressLint("StaticFieldLeak")
//    private inner class UpdateTripMasterBackground : AsyncTask<Void, Void, JSONObject>() {
//
//        override fun onPreExecute() {
//            super.onPreExecute()
//            UtilityFunctions.showProgressDialog (this@ConfirmFromToActivity)
//        }
//
//        override fun doInBackground(vararg param: Void): JSONObject? {
//            val jsonParser = JsonParser()
//            val params = HashMap<String, String>()
//
//            params["ArgTripMId"] = tripID!!
//            params["ArgTripMVsId"] = "0"
//
//            var BASE_URL = Constants.BASE_URL_EN + "UpdateTripMaster"
//
//            if (sharedPreferences.getString(Constants.PREFS_LANG, "en")!!.equals("ar", ignoreCase = true)) {
//                BASE_URL = Constants.BASE_URL_AR + "UpdateTripMaster"
//            }
//
//            return jsonParser.makeHttpRequest(BASE_URL, "POST", params)
//        }
//
//        override fun onPostExecute(response: JSONObject?) {
//            if (response != null) {
//                try {
//                    if (response.getBoolean("status")) {
//                        AutoAllocateNearestDriverByCustLatLngTripMIdBackground().execute()
//                    } else {
//                        UtilityFunctions.dismissProgressDialog()
//                    }
//                } catch (e: JSONException) {
//                    UtilityFunctions.dismissProgressDialog()
//                    e.printStackTrace()
//                }
//            } else { UtilityFunctions.dismissProgressDialog() }
//        }
//    }

    @SuppressLint("StaticFieldLeak")
    private inner class TripMasterStatusUpdateBackground internal constructor(internal var status: String) : AsyncTask<Void, Void, JSONObject>() {

        override fun onPreExecute() {
            super.onPreExecute()
            UtilityFunctions.showProgressDialog (this@ConfirmFromToActivity)
        }

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()

            params["ArgTripMID"] = tripID!!
            params["ArgTripMStatus"] = status

            var BASE_URL = Constants.BASE_URL_EN + "TripMasterStatusUpdate"

            if (sharedPreferences.getString(Constants.PREFS_LANG, "en")!!.equals("ar", ignoreCase = true)) {
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
                            startActivity(Intent(this@ConfirmFromToActivity, ShipmentDetActivity::class.java))
                            finish()
                        }
                    }
                } catch (e: JSONException) { e.printStackTrace() }
            }
        }
    }

    private fun createDrawableFromView(context: Context, view: View): Bitmap {
        val displayMetrics = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        view.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        view.measure(displayMetrics.widthPixels, displayMetrics.heightPixels)
        view.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)
        view.buildDrawingCache()
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        view.draw(canvas)

        return bitmap
    }

}
