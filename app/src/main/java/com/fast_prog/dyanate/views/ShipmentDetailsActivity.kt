package com.fast_prog.dyanate.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.fast_prog.dyanate.R
import com.fast_prog.dyanate.models.Order
import com.fast_prog.dyanate.utilities.ConnectionDetector
import com.fast_prog.dyanate.utilities.Constants
import com.fast_prog.dyanate.utilities.JsonParser
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_shipment_details.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class ShipmentDetailsActivity : AppCompatActivity() {

    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null

    var order: Order? = null

    var orderList: MutableList<Order>? = null

    var noRowMsg = ""

    internal lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shipment_details)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        customTitle(resources.getString(R.string.ShipmentDetails))

        toolbar.setNavigationOnClickListener { finish() }

        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        order = intent.getSerializableExtra("order") as Order?
        if (order == null) finish()

        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        container.adapter = mSectionsPagerAdapter

        container.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabs))
        tabs.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(container))

        if (ConnectionDetector.isConnected(this@ShipmentDetailsActivity)) {
            //UpdateTripNotifiedCustStatusBackground(order?.tripId!!).execute()
            TripDetailsMasterListBackground().execute()
        } else {
            ConnectionDetector.errorSnackbar(main_content)
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class TripDetailsMasterListBackground : AsyncTask<Void, Void, JSONObject>() {

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()

            params["trip_id"] = order!!.tripId ?: ""
            return jsonParser.makeHttpRequest(
                Constants.BASE_URL + "driver/get_trip_detail",
                "POST",
                params
            )
        }

        override fun onPostExecute(response: JSONObject?) {

            if (response != null) {
                try {
                    if (response.getBoolean("status")) {
                        val ordersJSONArray = response.getJSONArray("data")
                        orderList = ArrayList()

                        for (i in 0 until ordersJSONArray.length()) {
                            val order = Order()

                            order.tripId = ordersJSONArray.getJSONObject(i).getString("id").trim()
                            order.tripNo = order.tripId + "/ 2020"
                            order.tripFromAddress =
                                ordersJSONArray.getJSONObject(i).getString("from_address").trim()
                            order.tripFromLat =
                                ordersJSONArray.getJSONObject(i).getString("from_lat").trim()
                            order.tripFromLng =
                                ordersJSONArray.getJSONObject(i).getString("from_long").trim()
                            try {
                                order.tripFromSelf =
                                    ordersJSONArray.getJSONObject(i).getString("from_is_self")
                                        .trim().toBoolean()
                            } catch (e: Exception) {
                                order.tripFromSelf = false
                            }

                            order.tripFromName =
                                ordersJSONArray.getJSONObject(i).getString("from_name").trim()
                            order.tripFromMob =
                                ordersJSONArray.getJSONObject(i).getString("from_mobile").trim()
                            order.tripToAddress =
                                ordersJSONArray.getJSONObject(i).getString("to_address").trim()
                            order.tripToLat =
                                ordersJSONArray.getJSONObject(i).getString("to_lat").trim()
                            order.tripToLng =
                                ordersJSONArray.getJSONObject(i).getString("to_long").trim()
                            try {
                                order.tripToSelf =
                                    ordersJSONArray.getJSONObject(i).getString("to_is_self").trim()
                                        .toBoolean()
                            } catch (e: Exception) {
                                order.tripToSelf = false
                            }

                            order.tripToName =
                                ordersJSONArray.getJSONObject(i).getString("to_name").trim()
                            order.tripToMob =
                                ordersJSONArray.getJSONObject(i).getString("to_mobile").trim()
//                                order.vehicleModel = jsonArr.getJSONObject(i).getString("VsName").trim()
                            order.scheduleDate =
                                ordersJSONArray.getJSONObject(i).getString("scheduled_date").trim()
                            order.scheduleTime =
                                ordersJSONArray.getJSONObject(i).getString("scheduled_time").trim()
//                                order.userName = jsonArr.getJSONObject(i).getString("UsrName").trim()
//                                order.userMobile = jsonArr.getJSONObject(i).getString("UsrMobNumber").trim()
//                                order.tripFilter = jsonArr.getJSONObject(i).getString("TripMFilterName").trim()
                            order.tripStatus =
                                ordersJSONArray.getJSONObject(i).getString("trip_status").trim()
                            order.tripSubject =
                                ordersJSONArray.getJSONObject(i).getString("subject").trim()
                            order.tripNotes =
                                ordersJSONArray.getJSONObject(i).getString("notes").trim()
                            order.tripDRate =
                                ordersJSONArray.getJSONObject(i).getString("trip_rate").trim()

                            (orderList as ArrayList<Order>).add(order)
                        }

                    } else {
                        noRowMsg = response.getString("message").trim()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class UpdateTripNotifiedCustStatusBackground internal constructor(internal var tripId: String) :
        AsyncTask<Void, Void, JSONObject>() {

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()

            params["ArgTripDMId"] = tripId
            params["ArgTripDIsNotifiedCust"] = "true"

            var BASE_URL = Constants.BASE_URL_EN + "UpdateTripNotifiedCustStatus"

            if (sharedPreferences.getString(Constants.PREFS_LANG, "en")!!.equals(
                    "ar",
                    ignoreCase = true
                )
            ) {
                BASE_URL = Constants.BASE_URL_AR + "UpdateTripNotifiedCustStatus"
            }

            return jsonParser.makeHttpRequest(BASE_URL, "POST", params)
        }

        override fun onPostExecute(response: JSONObject?) {
            if (response != null) {
                try {
                    if (response.getBoolean("status")) {
                        //Log.e("success");
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }

    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> RouteFragment()
                1 -> DetailsFragment()
//                else -> RepliesFragment()
                else -> RouteFragment()
            }
        }

        override fun getCount(): Int {
            return 3
        }
    }
}
