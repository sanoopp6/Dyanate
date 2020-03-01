package com.fast_prog.dyanate.views

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.os.*
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.util.DisplayMetrics
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.fast_prog.dyanate.R
import com.fast_prog.dyanate.models.Dyana
import com.fast_prog.dyanate.models.PlaceItem
import com.fast_prog.dyanate.models.Ride
import com.fast_prog.dyanate.utilities.*
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
import com.google.android.gms.maps.model.*
import com.google.android.material.navigation.NavigationView
import com.snappydb.DBFactory
import com.snappydb.SnappydbException
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_sender_location.*
import kotlinx.android.synthetic.main.content_sender_location.*
import kotlinx.android.synthetic.main.nav_header_home.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class SenderLocationActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, LocationListener,
    NavigationView.OnNavigationItemSelectedListener {

    private val REQUEST_CODE_AUTOCOMPLETE = 1

    private var userLocation: Location? = null
    private var currentLocation: Location? = null
    private var locationLoaded = false

    private val MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 99

    private var mMap: GoogleMap? = null

    private lateinit var gpsTracker: GPSTracker

    private lateinit var latLng: LatLng

    private var mapViewSatellite: Boolean = false

    internal lateinit var sharedPreferences: SharedPreferences

    internal var placeItem: PlaceItem = PlaceItem()

    internal var placeItemSelectedKey: String? = null

    private var mGoogleApiClient: GoogleApiClient? = null

    private lateinit var permissionsList: MutableList<String>

    private val REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124

    internal var menuNotfn: MenuItem? = null

    //internal var notnModels: MutableList<NotnModel>? = null

    private var notfnCount = 0

    internal var checkDyanaThread: Thread? = null

    internal var runThread = false

    internal var dyanaList: ArrayList<Dyana>? = null

    internal var markerView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sender_location)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            this,
            drawer,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

            override fun onDrawerOpened(drawerView: View) {
                textView_nav_name.text = String.format(
                    "%s : %s",
                    resources.getString(R.string.Welcome),
                    sharedPreferences.getString(Constants.PREFS_USER_NAME, "")
                )
            }

            override fun onDrawerClosed(drawerView: View) {}

            override fun onDrawerStateChanged(newState: Int) {}
        })
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView =
            findViewById<com.google.android.material.navigation.NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        menuNotfn = navigationView.menu.findItem(R.id.nav_notfn)

        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        customTitle(resources.getString(R.string.SelectPickUpLocation))

        try {
            Ride.instance
        } catch (e: Exception) {
            Ride.instance = Ride()
        }

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
            val intent = Intent(this@SenderLocationActivity, PickLocationActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE)
        }

        location_select_gps_image_view.setOnClickListener {
            val intent = Intent(this@SenderLocationActivity, PickLocationActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE)
        }

        search_location_image_view.setOnClickListener {
            val intent = Intent(this@SenderLocationActivity, PickLocationActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE)
        }

        bookmark_location_image_view.setOnClickListener {
            if (placeItemSelectedKey.isNullOrEmpty()) {
                UtilityFunctions.showAlertOnActivity(this@SenderLocationActivity,
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
                                DBFactory.open(this@SenderLocationActivity, Constants.DYNA_DB)
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
                UtilityFunctions.showAlertOnActivity(this@SenderLocationActivity,
                    resources.getString(R.string.DeleteBookmarkedLocation),
                    resources.getString(R.string.Yes),
                    resources.getString(R.string.No),
                    true,
                    false,
                    {
                        try {
                            val snappyDB =
                                DBFactory.open(this@SenderLocationActivity, Constants.DYNA_DB)
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
                        this@SenderLocationActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this@SenderLocationActivity,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {

                    ActivityCompat.requestPermissions(
                        this@SenderLocationActivity,
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
                            this@SenderLocationActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            this@SenderLocationActivity,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {

                        ActivityCompat.requestPermissions(
                            this@SenderLocationActivity,
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
            UtilityFunctions.showAlertOnActivity(this@SenderLocationActivity,
                resources.getString(R.string.AreYouSure), resources.getString(R.string.Yes),
                resources.getString(R.string.No), true, false,
                {
                    if (!type_location_text_view.text.toString().equals(
                            resources.getString(R.string.TypeYourLocation),
                            true
                        )
                    ) {
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
                                this@SenderLocationActivity,
                                ReceiverLocationActivity::class.java
                            )
                        )
                    }
                }, {})
        }

        gpsTracker = GPSTracker(this@SenderLocationActivity)

        btn_select_location.isEnabled = false
        btn_select_location.alpha = 0.5f

        if (ConnectionDetector.isConnected(this@SenderLocationActivity)) {
            if (sharedPreferences.getString(Constants.PREFS_FCM_TOKEN, "")!!.isNotEmpty()) {
                Log.e("refreshedToken", sharedPreferences.getString(Constants.PREFS_FCM_TOKEN, ""))
                UpdateFCMToken(
                    this@SenderLocationActivity,
                    true,
                    sharedPreferences.getString(Constants.PREFS_USER_ID, "")!!
                ).execute()
            }
        }

    }



    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)

        } else {
            UtilityFunctions.showAlertOnActivity(this@SenderLocationActivity,
                resources.getString(R.string.DoYouWantToExit), resources.getString(R.string.Yes),
                resources.getString(R.string.No), true, false,
                {
                    ActivityCompat.finishAffinity(this@SenderLocationActivity)
                    finish()
                }, {})
        }
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

        runThread = true

        checkDyanaThread = Thread(object : Runnable {
            var handler: Handler = @SuppressLint("HandlerLeak")

            object : Handler() {
                override fun handleMessage(msg: Message) {
                    super.handleMessage(msg)
                    GetOnlineDyanaLocBackground().execute()
                }
            }

            override fun run() {
                while (runThread) {
                    threadMsg("track")

                    try {
                        Thread.sleep(5000)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }

            private fun threadMsg(msg: String) {
                if (msg != "") {
                    val msgObj = handler.obtainMessage()
                    val b = Bundle()
                    b.putString("message", msg)
                    msgObj.data = b
                    handler.sendMessage(msgObj)
                }
            }
        })

        checkDyanaThread?.start()

        if (ConnectionDetector.isConnected(this@SenderLocationActivity)) {
//            IsAppLiveBackground().execute()
//            GetNotificationsListByCustIdBackground().execute()
        }

//        val fusedLocationClient =
//            LocationServices.getFusedLocationProviderClient(this@SenderLocationActivity)
//
//        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
//            location?.let { location: Location ->
//                if (ActivityCompat.checkSelfPermission(
//                        this,
//                        Manifest.permission.ACCESS_FINE_LOCATION
//                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
//                        this,
//                        Manifest.permission.ACCESS_COARSE_LOCATION
//                    ) != PackageManager.PERMISSION_GRANTED
//                ) {
//
//                    ActivityCompat.requestPermissions(
//                        this@SenderLocationActivity,
//                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
//                    )
//                }
//
//                currentLocation = location
//
//                if (currentLocation != null) {
//                    locationLoaded = true
//                    if (Ride.instance.pickUpLatitude.isNullOrEmpty() || Ride.instance.pickUpLongitude.isNullOrEmpty()) {
//                        changeMap(currentLocation)
//                    }
//
//                }
//
//                try {
//                    val mLocationRequest = LocationRequest()
//                    mLocationRequest.interval = 10000
//                    mLocationRequest.fastestInterval = 5000
//                    mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
//                    LocationServices.FusedLocationApi.requestLocationUpdates(
//                        mGoogleApiClient,
//                        mLocationRequest,
//                        this
//                    )
//
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//
//            } ?: kotlin.run {
//                // Handle Null case or Request periodic location update https://developer.android.com/training/location/receive-location-updates
//            }
//        }


    }

    override fun onPause() {
        super.onPause()

        runThread = false
        if (checkDyanaThread != null) checkDyanaThread?.interrupt()
    }


    @SuppressLint("StaticFieldLeak")
    private inner class GetOnlineDyanaLocBackground : AsyncTask<Void, Void, JSONObject>() {

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressBarMarker(true)
        }

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()

            params["lang"] = sharedPreferences.getString(Constants.PREFS_LANG, "en")!!
            params["token"] = sharedPreferences.getString(Constants.PREFS_USER_TOKEN, "")!!
            params["user_id"] = sharedPreferences.getString(Constants.PREFS_USER_ID, "")!!
            params["latitude"] = userLocation?.latitude.toString()
            params["longitude"] = userLocation?.longitude.toString()

            return jsonParser.makeHttpRequest(
                Constants.BASE_URL + "customer/get_online_dyana_loc",
                "POST",
                params
            )
        }

        override fun onPostExecute(response: JSONObject?) {
            showProgressBarMarker(false)

            if (response != null) {
                try {
                    dyanaList = ArrayList()
                    mMap!!.clear()

                    if (response.getBoolean("status")) {
                        val dyanaArray = response.getJSONArray("data")

                        var marker: Marker
                        var latLng: LatLng
                        markerView = layoutInflater.inflate(R.layout.cab_marker, null)
                        val circleImageView =
                            markerView!!.findViewById<View>(R.id.circleImageView) as CircleImageView

                        for (i in 0 until dyanaArray.length()) {
                            val dyana = Dyana()

                            dyana.latitude =
                                dyanaArray.getJSONObject(i).getString("latitude").trim()
                            dyana.longitude =
                                dyanaArray.getJSONObject(i).getString("longitude").trim()
                            dyana.user_id = dyanaArray.getJSONObject(i).getString("user_id").trim()

                            dyanaList!!.add(dyana)

                            if (dyanaList!![i].latitude.isNotEmpty() && dyanaList!![i].longitude.isNotEmpty()) {
                                latLng = LatLng(
                                    dyanaList!![i].latitude.toDouble(),
                                    dyanaList!![i].longitude.toDouble()
                                )

                                marker = mMap!!.addMarker(
                                    MarkerOptions().position(latLng).icon(
                                        BitmapDescriptorFactory.fromBitmap(
                                            createDrawableFromView(markerView!!)
                                        )
                                    )
                                )
//                                    marker.title = dyanaList!![i].wakeelName

//                                    if (markerView != null) {
//                                        Picasso.get().load(Constants.IMG_URL +  "profile_pic/" + wakeel.profPic)
//                                                .into(circleImageView, object : Callback {
//                                                    override fun onError(e: java.lang.Exception?) {
//                                                        marker = mMap!!.addMarker(MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromBitmap(createDrawableFromView(markerView!!))))
//                                                        marker.title = wakeelList!![i].wakeelName
//                                                    }
//
//                                                    override fun onSuccess() {
//                                                        marker = mMap!!.addMarker(MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromBitmap(createDrawableFromView(markerView!!))))
//                                                        marker.title = wakeelList!![i].wakeelName
//                                                    }
//                                                })
//                                    }
                            }
                        }
                    }

                } catch (e: JSONException) {
                    e.printStackTrace()
                }

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.exit_option) {
            UtilityFunctions.showAlertOnActivity(this@SenderLocationActivity,
                resources.getString(R.string.AreYouSure), resources.getString(R.string.Yes),
                resources.getString(R.string.No), true, false,
                {
                    if (ConnectionDetector.isConnected(this@SenderLocationActivity)) {
                        UpdateFCMToken(
                            this@SenderLocationActivity,
                            false,
                            sharedPreferences.getString(Constants.PREFS_USER_ID, "")!!
                        ).execute()
                    }

                    val editor = sharedPreferences.edit()
                    editor.putBoolean(Constants.PREFS_IS_LOGIN, false)
                    editor.putString(Constants.PREFS_USER_ID, "0")
                    editor.putString(Constants.PREFS_USER_NAME, "0")
                    editor.putString(Constants.PREFS_SHARE_URL, "")
                    editor.commit()

                    val intent = Intent(this@SenderLocationActivity, NoLoginActivity::class.java)
                    ActivityCompat.finishAffinity(this@SenderLocationActivity)
                    startActivity(intent)
                    finish()

                }, {})

        } else if (id == R.id.notfn_option) {
            val intent = Intent(this@SenderLocationActivity, NotificationsListActivity::class.java)
            intent.putExtra("loaded", true)
            startActivity(intent)
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.nav_add_trip) {
            startActivity(Intent(this@SenderLocationActivity, SenderLocationActivity::class.java))

        } else if (id == R.id.nav_my_trip) {
            startActivity(Intent(this@SenderLocationActivity, MyOrdersActivity::class.java))

        } else if (id == R.id.nav_feedback) {
            startActivity(Intent(this@SenderLocationActivity, ShowFeedbackListActivity::class.java))

        } else if (id == R.id.nav_settings) {
            startActivity(Intent(this@SenderLocationActivity, SettigsActivity::class.java))

        } else if (id == R.id.nav_faq) {
            startActivity(Intent(this@SenderLocationActivity, FaqListActivity::class.java))

        } else if (id == R.id.nav_notfn) {
            val intent = Intent(this@SenderLocationActivity, NotificationsListActivity::class.java)
            intent.putExtra("loaded", true)
            startActivity(intent)

        } else if (id == R.id.nav_share) {
            val sendIntent = Intent()
            sendIntent.action = Intent.ACTION_SEND
            sendIntent.putExtra(
                Intent.EXTRA_TEXT,
                resources.getString(R.string.ShareMessage) + " " + sharedPreferences.getString(
                    Constants.PREFS_SHARE_URL,
                    ""
                )
            )
            sendIntent.type = "text/plain"
            startActivity(sendIntent)

        } else if (id == R.id.nav_logout) {
            UtilityFunctions.showAlertOnActivity(this@SenderLocationActivity,
                resources.getString(R.string.AreYouSure), resources.getString(R.string.Yes),
                resources.getString(R.string.No), true, false,
                {
                    if (ConnectionDetector.isConnected(this@SenderLocationActivity)) {
                        UpdateFCMToken(
                            this@SenderLocationActivity,
                            false,
                            sharedPreferences.getString(Constants.PREFS_USER_ID, "")!!
                        ).execute()
                    }

                    val editor = sharedPreferences.edit()
                    editor.putBoolean(Constants.PREFS_IS_LOGIN, false)
                    editor.putString(Constants.PREFS_USER_ID, "0")
                    editor.putString(Constants.PREFS_USER_NAME, "0")
                    editor.putString(Constants.PREFS_SHARE_URL, "")
                    editor.commit()

                    val intent = Intent(this@SenderLocationActivity, NoLoginActivity::class.java)
                    ActivityCompat.finishAffinity(this@SenderLocationActivity)
                    startActivity(intent)
                    finish()

                }, {})
        }

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer.closeDrawer(GravityCompat.START)
        return true
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
            this@SenderLocationActivity,
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
                        ActivityCompat.finishAffinity(this@SenderLocationActivity)
                        val intent = Intent(this@SenderLocationActivity, UpdateActivity::class.java)
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
    private inner class GetNotificationsListByCustIdBackground :
        AsyncTask<Void, Void, JSONObject>() {

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()

            params["ArgNfUserId"] = "39" //sharedPreferences.getString(Constants.PREFS_USER_ID, "")

            var BASE_URL = Constants.BASE_URL_EN + "GetNotificationsListByCustId"

            if (sharedPreferences.getString(Constants.PREFS_LANG, "en")!!.equals("ar", true)) {
                BASE_URL = Constants.BASE_URL_AR + "GetNotificationsListByCustId"
            }

            return jsonParser.makeHttpRequest(BASE_URL, "POST", params)
        }

        override fun onPostExecute(response: JSONObject?) {
            if (response != null) {
                try {
                    if (response.getBoolean("status")) {
                        val jsonArray = response.getJSONArray("data")

                        notfnCount = jsonArray.length()
                        //invalidateOptionsMenu()

                        if (notfnCount > 0) {
                            val requestUnreadCntStr = notfnCount.toString()
                            val title = String.format(
                                Locale.getDefault(),
                                "%s    %s ",
                                resources.getString(R.string.Notifications),
                                requestUnreadCntStr
                            )
                            val sColored = SpannableString(title)

                            sColored.setSpan(
                                BackgroundColorSpan(Color.RED),
                                title.length - (requestUnreadCntStr.length + 2),
                                title.length,
                                0
                            )
                            sColored.setSpan(
                                ForegroundColorSpan(Color.WHITE),
                                title.length - (requestUnreadCntStr.length + 2),
                                title.length,
                                0
                            );

                            menuNotfn?.title = sColored
                        }

                        //notnModels = ArrayList()
                        //var gotoNoti = false
                        //
                        //for (i in 0 until jsonArray.length()) {
                        //    val notnModel = NotnModel()
                        //
                        //    if (sharedPreferences.getInt(Constants.PREFS_NOTI_ID,0) < jsonArray.getJSONObject(i).getString("NfId").trim().toInt()) {
                        //        gotoNoti = true
                        //        val editor = sharedPreferences.edit()
                        //        editor.putInt(Constants.PREFS_NOTI_ID, jsonArray.getJSONObject(i).getString("NfId").trim().toInt())
                        //        editor.commit()
                        //    }
                        //
                        //    notnModel.nfId = jsonArray.getJSONObject(i).getString("NfId").trim()
                        //    notnModel.nfUserId = jsonArray.getJSONObject(i).getString("NfUserId").trim()
                        //    notnModel.nfTripMId = jsonArray.getJSONObject(i).getString("NfTripMId").trim()
                        //    notnModel.nfTitle = jsonArray.getJSONObject(i).getString("NfTitle").trim()
                        //    notnModel.nfBody = jsonArray.getJSONObject(i).getString("NfBody").trim()
                        //    notnModel.nfcategory = jsonArray.getJSONObject(i).getString("Nfcategory").trim()
                        //    notnModel.nfReadStatus = jsonArray.getJSONObject(i).getString("NfReadStatus").trim()
                        //    notnModel.nfActive = jsonArray.getJSONObject(i).getString("NfActive").trim()
                        //    notnModel.nfCreateDtTime = jsonArray.getJSONObject(i).getString("NfCreateDtTime").trim()
                        //    notnModel.nfReadDtTime = jsonArray.getJSONObject(i).getString("NfReadDtTime").trim()
                        //
                        //    (notnModels as ArrayList<NotnModel>).add(notnModel)
                        //}
                        //
                        //if (gotoNoti) {
                        //    NotificationsListActivity.notnModels = notnModels
                        //    val intent = Intent(this@SenderLocationActivity, NotificationsListActivity::class.java)
                        //    startActivity(intent)
                        //}
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

            }
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
            if (ConnectionDetector.isConnected(this@SenderLocationActivity)) {

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
        val resultCode = gApi.isGooglePlayServicesAvailable(this@SenderLocationActivity)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (gApi.isUserResolvableError(resultCode)) {
                gApi.getErrorDialog(this@SenderLocationActivity, resultCode, 100).show()
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
                        this@SenderLocationActivity,
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
                this@SenderLocationActivity,
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
                            )
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
                                        addressComponents.getJSONObject(i).getString("long_name") + " ،" + locationName
                                    } else {
                                        locationName + ", " + addressComponents.getJSONObject(i).getString(
                                            "long_name"
                                        )
                                    }
                                else -> locationName =
                                    addressComponents.getJSONObject(i).getString("long_name")
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
                            DBFactory.open(this@SenderLocationActivity, Constants.DYNA_DB)

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
    }
}
