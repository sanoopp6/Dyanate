package com.fast_prog.dyanate.views.edit

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fast_prog.dyanate.R
import com.fast_prog.dyanate.models.Dyana
import com.fast_prog.dyanate.models.Order
import com.fast_prog.dyanate.models.PlaceItem
import com.fast_prog.dyanate.models.Ride
import com.fast_prog.dyanate.utilities.*
import com.fast_prog.dyanate.views.customTitle
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.snappydb.DBFactory
import com.snappydb.SnappydbException
import kotlinx.android.synthetic.main.activity_edit_sender_location.*
import org.json.JSONArray
import org.json.JSONException
import java.util.*

class EditSenderLocationActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private val REQUEST_CODE_AUTOCOMPLETE = 1

    private var userLocation: Location? = null
    private var currentLocation: Location? = null
    private var locationLoaded = false

    private val MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 99

    private var mMap: GoogleMap? = null

    private lateinit var gpsTracker: GPSTracker

    private lateinit var latLng: LatLng

    private var mapViewSatellite: Boolean = false

    lateinit var sharedPreferences: SharedPreferences

    internal var placeItem: PlaceItem = PlaceItem()

    internal var placeItemSelectedKey: String? = null

    private var mGoogleApiClient: GoogleApiClient? = null

    private lateinit var permissionsList: MutableList<String>

    private val REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124

    internal var menuNotfn: MenuItem? = null

    //internal var notnModels: MutableList<NotnModel>? = null

    private var notfnCount = 0


    internal var dyanaList: ArrayList<Dyana>? = null

    internal var markerView: View? = null

    var selectedCountry = ""

    val AUTOCOMPLETE_REQUEST_CODE = 100

    lateinit var order: Order

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_sender_location)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        toolbar.setNavigationOnClickListener { finish() }

        order = intent.getSerializableExtra("order") as Order

        try {
            Ride.instance
        } catch (e: Exception) {
            Ride.instance = Ride()
        }
        Ride.instance.pickUpLatitude = order.tripFromLat
        Ride.instance.pickUpLongitude = order.tripFromLng
        Ride.instance.tripId = order.tripId!!

        Places.initialize(applicationContext, Constants.GOOGLE_API_KEY)


        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        customTitle(resources.getString(R.string.SelectPickUpLocation))


        val MyVersion = Build.VERSION.SDK_INT

        if (MyVersion > Build.VERSION_CODES.LOLLIPOP_MR1) {
            permissionsList = ArrayList()

            if (!checkIfAlreadyhavePermission()) {
                requestForSpecificPermission()
            }
        }

        mapViewSatellite = false

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        type_location_text_view.setOnClickListener {
//            val intent = Intent(this@SenderLocationActivity, PickLocationActivity::class.java)
//            startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE)
            val fields = listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.ADDRESS_COMPONENTS,
                Place.Field.LAT_LNG
            )

            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).setCountry("SA")
                .build(this)
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
        }

        location_select_gps_image_view.setOnClickListener {
//            val intent = Intent(this@SenderLocationActivity, PickLocationActivity::class.java)
//            startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE)
            val fields = listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.ADDRESS_COMPONENTS,
                Place.Field.LAT_LNG
            )

            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).setCountry("SA")
                .build(this)
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
        }

        search_location_image_view.setOnClickListener {
//            val intent = Intent(this@SenderLocationActivity, PickLocationActivity::class.java)
//            startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE)
            val fields = listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.ADDRESS_COMPONENTS,
                Place.Field.LAT_LNG
            )

            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).setCountry("SA")
                .build(this)
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
        }

        bookmark_location_image_view.setOnClickListener {
            if (placeItemSelectedKey.isNullOrEmpty()) {
                UtilityFunctions.showAlertOnActivity(this@EditSenderLocationActivity,
                    resources.getString(R.string.BookmarkLocation),
                    resources.getString(R.string.Yes),
                    resources.getString(R.string.No),
                    true,
                    false,
                    {
                        placeItem = PlaceItem()
                        placeItem.pLatitude = userLocation?.latitude.toString()
                        placeItem.pLongitude = userLocation?.longitude.toString()
                        placeItem.plName = type_location_text_view.text.toString()
                        placeItem.pVicinity = text_view_province.text.toString()

                        try {
                            val snappyDB =
                                DBFactory.open(this@EditSenderLocationActivity, Constants.DYNA_DB)
                            val keys = snappyDB.findKeys(Constants.DYNA_DB_KEY)
                            snappyDB.put(Constants.DYNA_DB_KEY + "_" + keys.size, placeItem)
                            snappyDB.close()

                            placeItemSelectedKey = Constants.DYNA_DB_KEY + "_" + keys.size
                            bookmark_location_image_view.setColorFilter(Color.parseColor(Constants.FILTER_COLOR))

                        } catch (e: SnappydbException) {
                            Log.e("", e.message)
                            e.printStackTrace()
                        }
                    },
                    {})

            } else {
                UtilityFunctions.showAlertOnActivity(this@EditSenderLocationActivity,
                    resources.getString(R.string.DeleteBookmarkedLocation),
                    resources.getString(R.string.Yes),
                    resources.getString(R.string.No),
                    true,
                    false,
                    {
                        try {
                            val snappyDB =
                                DBFactory.open(this@EditSenderLocationActivity, Constants.DYNA_DB)
                            snappyDB.del(placeItemSelectedKey)
                            snappyDB.close()

                            bookmark_location_image_view.setColorFilter(Color.TRANSPARENT)
                            placeItemSelectedKey = null

                        } catch (e: SnappydbException) {
                            e.printStackTrace()
                        }
                    },
                    {})
            }
        }

        image_view_map_change_icon.setOnClickListener {
            if (mMap != null) {
                if (!mapViewSatellite) {
                    mMap!!.mapType = GoogleMap.MAP_TYPE_SATELLITE
                    mapViewSatellite = true
                    image_view_map_change_icon.setColorFilter(Color.WHITE)
                    image_view_current_location_icon.setColorFilter(Color.WHITE)

                } else {
                    mMap!!.mapType = GoogleMap.MAP_TYPE_NORMAL
                    mapViewSatellite = false
                    image_view_map_change_icon.setColorFilter(Color.BLACK)
                    image_view_current_location_icon.setColorFilter(Color.BLACK)
                }

                mMap!!.uiSettings.isZoomControlsEnabled = false

                latLng = LatLng(userLocation!!.latitude, userLocation!!.longitude)

                val cameraPosition = CameraPosition.Builder().target(latLng).zoom(17f).build()

                if (ActivityCompat.checkSelfPermission(
                        this@EditSenderLocationActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this@EditSenderLocationActivity,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {

                    ActivityCompat.requestPermissions(
                        this@EditSenderLocationActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
                    )
                }

                mMap!!.isMyLocationEnabled = true
                mMap!!.uiSettings.isMyLocationButtonEnabled = false
                mMap!!.uiSettings.isCompassEnabled = false
                mMap!!.uiSettings.isRotateGesturesEnabled = false
                mMap!!.uiSettings.isMapToolbarEnabled = false
                mMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }
        }

        image_view_current_location_icon.setOnClickListener {
            if (mMap != null) {
                mMap!!.uiSettings.isZoomControlsEnabled = false

                gpsTracker.getLocation()

                if (gpsTracker.canGetLocation()) {
                    if (ActivityCompat.checkSelfPermission(
                            this@EditSenderLocationActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            this@EditSenderLocationActivity,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {

                        ActivityCompat.requestPermissions(
                            this@EditSenderLocationActivity,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
                        )
                    }

                    currentLocation =
                        LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)

                    if (currentLocation != null) {
                        locationLoaded = true
                        userLocation = currentLocation
                        changeMap(currentLocation)
                    }

                } else {
                    gpsTracker.showSettingsAlert()
                }
            }
        }

        btn_select_location.setOnClickListener {
            //            UtilityFunctions.showAlertOnActivity(this@SenderLocationActivity,
//                resources.getString(R.string.AreYouSure), resources.getString(R.string.Yes),
//                resources.getString(R.string.No), true, false,
//                {
            if (!type_location_text_view.text.toString().equals(
                    resources.getString(R.string.TypeYourLocation),
                    true
                )
            ) {

                Log.d("selected_country", selectedCountry)
                if (selectedCountry.toUpperCase() != "SA") {
                    UtilityFunctions.showAlertOnActivity(this@EditSenderLocationActivity,
                        getString(R.string.sorry_this_service_only_in_saudi),
                        getString(R.string.ok),
                        "",
                        showCancelButton = false,
                        setCancelable = true,
                        actionOk = {},
                        actionCancel = {})
                    return@setOnClickListener
                }

                if (sharedPreferences.getString(Constants.PREFS_LANG, "en")!!.equals(
                        "ar",
                        true
                    )
                ) {
                    Ride.instance.pickUpLocation =
                        text_view_province.text.toString() + " ،" + type_location_text_view.text.toString()
                } else {
                    Ride.instance.pickUpLocation =
                        type_location_text_view.text.toString() + ", " + text_view_province.text.toString()
                }
                Ride.instance.pickUpLatitude = userLocation!!.latitude.toString()
                Ride.instance.pickUpLongitude = userLocation!!.longitude.toString()

                startActivity(
                    Intent(
                        this@EditSenderLocationActivity,
                        EditReceiverLocationActivity::class.java
                    ).putExtra("order", order)
                )
            }
//                }, {})
        }

        gpsTracker = GPSTracker(this@EditSenderLocationActivity)

        btn_select_location.isEnabled = false
        btn_select_location.alpha = 0.5f
    }


    override fun onResume() {
        super.onResume()

        gpsTracker.getLocation()

        if (checkPlayServices()) {
            if (gpsTracker.canGetLocation()) {
                if (mGoogleApiClient == null || !mGoogleApiClient!!.isConnected) {
                    buildGoogleApiClient()
                }
            } else {
                gpsTracker.showSettingsAlert()
            }
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

    //override fun onCreateOptionsMenu(menu: Menu): Boolean {
    //    menuInflater.inflate(R.menu.home, menu)
    //    //val notfnOption = menu.findItem(R.id.notfn_option)
    //    //notfnOption.isVisible = (notfnCount > 0)
    //    //if (notfnCount > 0) { notfnOption.icon = UtilityFunctions.convertLayoutToImage(this@SenderLocationActivity, notfnCount, R.drawable.ic_notifications_white_24dp) }
    //    return true
    //}

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

        return result
    }

    private fun requestForSpecificPermission() {
        val stringArr = permissionsList.toTypedArray()
        ActivityCompat.requestPermissions(
            this@EditSenderLocationActivity,
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
            MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }
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

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (!Ride.instance.pickUpLatitude.isNullOrEmpty() && !Ride.instance.pickUpLongitude.isNullOrEmpty()) {
            val mLocation = Location("")
            mLocation.latitude = Ride.instance.pickUpLatitude!!.toDouble()
            mLocation.longitude = Ride.instance.pickUpLongitude!!.toDouble()
            userLocation = mLocation

            latLng = LatLng(userLocation!!.latitude, userLocation!!.longitude)
            val cameraPosition = CameraPosition.Builder().target(latLng).zoom(17f).build()
            mMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }

        mMap!!.setOnCameraMoveListener {
            latLng = mMap!!.cameraPosition.target
            try {
                val mLocation = Location("")
                mLocation.latitude = latLng.latitude
                mLocation.longitude = latLng.longitude
                userLocation = mLocation

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

//        mMap!!.setOnCameraChangeListener { cameraPosition ->
//            latLng = cameraPosition.target
//
//            try {
//                val mLocation = Location("")
//                mLocation.latitude = latLng.latitude
//                mLocation.longitude = latLng.longitude
//                userLocation = mLocation
//
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }

        mMap!!.setOnCameraIdleListener {
            if (ConnectionDetector.isConnected(this@EditSenderLocationActivity)) {

                if (userLocation != null) {
                    SetLocationNameBackground(
                        userLocation!!.latitude,
                        userLocation!!.longitude
                    ).execute()
                }
            } else {
                ConnectionDetector.errorSnackbar(coordinator_layout)
            }
        }
    }

    private fun showProgressBarMarker(show: Boolean) {
        if (show) {
            progress_bar_marker.visibility = View.VISIBLE
            you_are_here_text_view.visibility = View.GONE
            btn_select_location.isEnabled = false
            btn_select_location.alpha = 0.5f

        } else {
            progress_bar_marker.visibility = View.GONE
            you_are_here_text_view.visibility = View.VISIBLE
            btn_select_location.isEnabled = true
            btn_select_location.alpha = 1.0f
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mGoogleApiClient != null && mGoogleApiClient!!.isConnected) {
            mGoogleApiClient!!.disconnect()
        }
    }

    @Synchronized
    protected fun buildGoogleApiClient() {
        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API).build()

        try {
            mGoogleApiClient!!.connect()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkPlayServices(): Boolean {
        val gApi = GoogleApiAvailability.getInstance()
        val resultCode = gApi.isGooglePlayServicesAvailable(this@EditSenderLocationActivity)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (gApi.isUserResolvableError(resultCode)) {
                gApi.getErrorDialog(this@EditSenderLocationActivity, resultCode, 100).show()
            } else {
                Log.e("TAG", "This device is not supported.")
                //finish();
            }
            return false
        }
        return true
    }

    private fun changeMap(location: Location?) {
        if (mMap != null) {
            mMap!!.uiSettings.isZoomControlsEnabled = false

            if (location != null) {
                latLng = LatLng(location.latitude, location.longitude)
                userLocation = Location("")
                userLocation!!.latitude = latLng.latitude
                userLocation!!.longitude = latLng.longitude

                val cameraPosition = CameraPosition.Builder().target(latLng).zoom(17f).build()

                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {

                    ActivityCompat.requestPermissions(
                        this@EditSenderLocationActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
                    )
                }

                mMap!!.isMyLocationEnabled = true
                mMap!!.uiSettings.isMyLocationButtonEnabled = false
                mMap!!.uiSettings.isCompassEnabled = false
                mMap!!.uiSettings.isRotateGesturesEnabled = false
                mMap!!.uiSettings.isMapToolbarEnabled = false
                mMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }
        }
    }

    override fun onConnected(bundle: Bundle?) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this@EditSenderLocationActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }

        currentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)

        if (currentLocation != null) {
            locationLoaded = true
            if (Ride.instance.pickUpLatitude.isNullOrEmpty() || Ride.instance.pickUpLongitude.isNullOrEmpty()) {
                changeMap(currentLocation)
            }

        } else {
            try {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        try {
            val mLocationRequest = LocationRequest()
            mLocationRequest.interval = 10000
            mLocationRequest.fastestInterval = 5000
            mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient,
                mLocationRequest,
                this
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onConnectionSuspended(i: Int) {
        mGoogleApiClient!!.connect()
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.e(
            "TAG",
            "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.errorCode
        )
    }

    override fun onLocationChanged(location: Location?) {
        if (location != null) {
            currentLocation = location
            locationLoaded = true
            if (Ride.instance.pickUpLatitude.isNullOrEmpty() || Ride.instance.pickUpLongitude.isNullOrEmpty()) {
                changeMap(currentLocation)
            }
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this)
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class SetLocationNameBackground internal constructor(
        private val latitude: Double,
        private val longitude: Double
    ) : AsyncTask<Void, Void, JSONArray>() {

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressBarMarker(true)
        }

        override fun doInBackground(vararg voids: Void): JSONArray? {
            val locationNameParser = JsonParser()
            val params = HashMap<String, String>()

            params["latlng"] = latitude.toString() + "," + longitude
            params["sensor"] = "true"
            params["key"] = Constants.GOOGLE_API_KEY

            if (sharedPreferences.getString(Constants.PREFS_LANG, "en")!!.equals(
                    "ar",
                    ignoreCase = true
                )
            ) {
                params["language"] = "ar"
            }
            val locationNameObject = locationNameParser.makeHttpRequest(
                Constants.GOOGLE_LOCATION_NAME_URL,
                "GET",
                params
            )

            if (locationNameObject != null) {
                try {
                    val locationNameArray = locationNameObject.getJSONArray("results")
                    if (locationNameArray != null) {
                        return locationNameArray
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

            }
            return null
        }

        override fun onPostExecute(locationArray: JSONArray?) {
            super.onPostExecute(locationArray)
            showProgressBarMarker(false)
            if (locationArray != null) {
                try {
                    //String locationName = locationArray.getJSONObject(0).getString("formatted_address");
                    var provinceName = ""
                    var locationName = ""
                    val addressComponents =
                        locationArray.getJSONObject(0).getJSONArray("address_components")

                    for (i in 0 until addressComponents.length()) {
                        val types = addressComponents.getJSONObject(i).getJSONArray("types")

                        if (types.getString(0).equals(
                                "route",
                                ignoreCase = true
                            ) || types.getString(0).equals(
                                "locality",
                                ignoreCase = true
                            ) || types.length() > 1 && types.getString(1).equals(
                                "sublocality",
                                ignoreCase = true
                            ) || types.getString(0).equals("country")
                        ) {

                            when {
                                types.getString(0).equals("locality", true) -> provinceName =
                                    addressComponents.getJSONObject(i).getString("long_name")
                                locationName.trim().isNotEmpty() -> locationName =
                                    if (sharedPreferences.getString(
                                            Constants.PREFS_LANG,
                                            "en"
                                        )!!.equals("ar", true)
                                    ) {
                                        addressComponents.getJSONObject(i)
                                            .getString("long_name") + " ،" + locationName
                                    } else {
                                        locationName + ", " + addressComponents.getJSONObject(i)
                                            .getString(
                                                "long_name"
                                            )
                                    }
                                else -> locationName =
                                    addressComponents.getJSONObject(i).getString("long_name")
                            }

                            if (types.getString(0).equals("country", true)) {

                                selectedCountry =
                                    addressComponents.getJSONObject(i).getString("short_name")
                            }
                        }
                    }

                    text_view_province.text = provinceName
                    val regex = "\\d+"

                    if (!locationName.matches(regex.toRegex())) {
                        type_location_text_view.text = locationName

                    } else {
                        type_location_text_view.setText(R.string.YouAreHere)
                    }
                    type_location_text_view.isSelected = true

                    placeItemSelectedKey = null
                    bookmark_location_image_view.setColorFilter(Color.TRANSPARENT)

                    val locationOne = Location("")
                    val locationTwo = Location("")

                    locationOne.latitude = latitude
                    locationOne.longitude = longitude

                    try {
                        val snappyDB =
                            DBFactory.open(this@EditSenderLocationActivity, Constants.DYNA_DB)

                        val keys = snappyDB.findKeys(Constants.DYNA_DB_KEY)
                        var i = 0

                        while (i < keys.size) {
                            placeItem = snappyDB.get(keys[i], PlaceItem::class.java)

                            locationTwo.latitude = placeItem.pLatitude?.toDouble()!!
                            locationTwo.longitude = placeItem.pLongitude?.toDouble()!!

                            if (locationOne.distanceTo(locationTwo) <= 10) {
                                bookmark_location_image_view.setColorFilter(
                                    Color.parseColor(
                                        Constants.FILTER_COLOR
                                    )
                                )
                                placeItemSelectedKey = keys[i]
                                break
                            }

                            i++
                        }
                        snappyDB.close()

                    } catch (e: SnappydbException) {
                        e.printStackTrace()
                    }

                } catch (e: JSONException) {
                    e.printStackTrace()
                }

            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_AUTOCOMPLETE && resultCode == RESULT_OK) {
            val currentPlaceItem = data?.getSerializableExtra("PlaceItem") as PlaceItem

            try {
                val mLocation = Location("")
                mLocation.latitude = currentPlaceItem.pLatitude?.toDouble()!!
                mLocation.longitude = currentPlaceItem.pLongitude?.toDouble()!!
                userLocation = mLocation
            } catch (ignored: Exception) {
            }

            latLng = LatLng(userLocation!!.latitude, userLocation!!.longitude)
            val cameraPosition = CameraPosition.Builder().target(latLng).zoom(17f).build()
            mMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }


        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    data?.let {
                        val place = Autocomplete.getPlaceFromIntent(data)
                        Log.i("Place", "Place: ${place.name}, ${place.id}")


                        try {
                            val mLocation = Location("")
                            mLocation.latitude = place.latLng!!.latitude
                            mLocation.longitude = place.latLng!!.longitude
                            userLocation = mLocation
                        } catch (ignored: Exception) {
                        }

                        latLng = LatLng(userLocation!!.latitude, userLocation!!.longitude)
                        val cameraPosition =
                            CameraPosition.Builder().target(latLng).zoom(17f).build()
                        mMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                    }
                }
                AutocompleteActivity.RESULT_ERROR -> {
                    // TODO: Handle the error.
                    data?.let {
                        val status = Autocomplete.getStatusFromIntent(data)
                        Log.i("Place", status.statusMessage)
                    }
                }
                Activity.RESULT_CANCELED -> {
                    // The user canceled the operation.
                }
            }
            return
        }
    }
}
