package com.fast_prog.dyanate.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
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

//        if (ConnectionDetector.isConnected(this@ShipmentDetailsActivity)) {
//            //UpdateTripNotifiedCustStatusBackground(order?.tripId!!).execute()
//            TripDetailsMasterListBackground().execute()
//        } else {
//            ConnectionDetector.errorSnackbar(main_content)
//        }
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
                0 -> DetailsFragment()
                1 -> RouteFragment()
//                else -> RepliesFragment()
                else -> DetailsFragment()
            }
        }

        override fun getCount(): Int {
            return 3
        }
    }
}
