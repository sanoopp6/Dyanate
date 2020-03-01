package com.fast_prog.dyanate.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fast_prog.dyanate.R
import com.fast_prog.dyanate.models.Order
import com.fast_prog.dyanate.utilities.ConnectionDetector
import com.fast_prog.dyanate.utilities.Constants
import com.fast_prog.dyanate.utilities.JsonParser
import com.fast_prog.dyanate.utilities.UtilityFunctions
import kotlinx.android.synthetic.main.activity_my_orders.*
import kotlinx.android.synthetic.main.content_my_orders.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class MyOrdersActivity : AppCompatActivity() {

    internal var ordersArrayList: MutableList<Order>? = null

    //internal lateinit var orderSelected: Order

    private lateinit var homeLayoutManager: LinearLayoutManager

    internal lateinit var mHomeAdapter: RecyclerView.Adapter<*>

    //internal var selectedId: String? = null

    internal var isLoaded = false

    private var backEnabled = true

    internal lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_orders)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        toolbar.setNavigationOnClickListener { backPressed() }

        customTitle(resources.getString(R.string.MyOrders))

        backEnabled = intent.getBooleanExtra("backEnabled", true)

        //show_button.setOnClickListener {
        //    val intent = Intent(this@MyOrdersActivity, OrderDetailActivity::class.java)
        //    intent.putExtra("order", orderSelected)
        //    startActivity(intent)
        //}
        //
        //if (sharedPreferences.getString(Constants.PREFS_LANG, "en")!!.equals("ar", true)) {
        //    time_value_text_view.textDirection = View.TEXT_DIRECTION_RTL
        //    subject_value_text_view.textDirection = View.TEXT_DIRECTION_RTL
        //    notes_value_text_view.textDirection = View.TEXT_DIRECTION_RTL
        //    from_addr_value_text_view.textDirection = View.TEXT_DIRECTION_RTL
        //    to_addr_value_text_view.textDirection = View.TEXT_DIRECTION_RTL
        //    veh_name_value_text_view.textDirection = View.TEXT_DIRECTION_RTL
        //} else {
        //    time_value_text_view.textDirection = View.TEXT_DIRECTION_LTR
        //    subject_value_text_view.textDirection = View.TEXT_DIRECTION_LTR
        //    notes_value_text_view.textDirection = View.TEXT_DIRECTION_LTR
        //    from_addr_value_text_view.textDirection = View.TEXT_DIRECTION_LTR
        //    to_addr_value_text_view.textDirection = View.TEXT_DIRECTION_LTR
        //    veh_name_value_text_view.textDirection = View.TEXT_DIRECTION_LTR
        //}

        recycler_my_orders.setHasFixedSize(true)
        homeLayoutManager = LinearLayoutManager(this@MyOrdersActivity)
        recycler_my_orders.layoutManager = homeLayoutManager
        mHomeAdapter = MyOrdersAdapter()
        recycler_my_orders.adapter = mHomeAdapter
    }

    override fun onBackPressed() {
        backPressed()
    }

    private fun backPressed() {
        if (backEnabled) {
            super.onBackPressed()
        } else {
            startActivity(Intent(this@MyOrdersActivity, SenderLocationActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()

        if (ConnectionDetector.isConnected(applicationContext)) {
            TripMasterListBackground().execute()
        } else {
            ConnectionDetector.errorSnackbar(coordinator_layout)
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class TripMasterListBackground : AsyncTask<Void, Void, JSONObject>() {

        override fun onPreExecute() {
            super.onPreExecute()
            if (!isLoaded) {
                UtilityFunctions.showProgressDialog(this@MyOrdersActivity)
            }
        }

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()

            params["user_id"] = sharedPreferences.getString(Constants.PREFS_USER_ID, "0")!!

            return jsonParser.makeHttpRequest(
                Constants.BASE_URL + "customer/my_orders",
                "POST",
                params
            )
        }

        override fun onPostExecute(response: JSONObject?) {
            if (!isLoaded) {
                isLoaded = true
                UtilityFunctions.dismissProgressDialog()
            }

            if (response != null) {
                try {
                    if (response.getBoolean("status")) {
                        val jsonArr = response.getJSONArray("data")
                        ordersArrayList = ArrayList()

                        if (jsonArr.length() > 0) {
                            for (i in 0 until jsonArr.length()) {
                                val order = Order()

                                order.tripId = jsonArr.getJSONObject(i).getString("id").trim()
                                order.tripNo = order.tripId + "/ 2020"
                                order.tripFromAddress =
                                    jsonArr.getJSONObject(i).getString("from_address").trim()
                                order.tripFromLat =
                                    jsonArr.getJSONObject(i).getString("from_lat").trim()
                                order.tripFromLng =
                                    jsonArr.getJSONObject(i).getString("from_long").trim()
                                try {
                                    order.tripFromSelf =
                                        jsonArr.getJSONObject(i).getString("from_is_self").trim()
                                            .toBoolean()
                                } catch (e: Exception) {
                                    order.tripFromSelf = false
                                }

                                order.tripFromName =
                                    jsonArr.getJSONObject(i).getString("from_name").trim()
                                order.tripFromMob =
                                    jsonArr.getJSONObject(i).getString("from_mobile").trim()
                                order.tripToAddress =
                                    jsonArr.getJSONObject(i).getString("to_address").trim()
                                order.tripToLat =
                                    jsonArr.getJSONObject(i).getString("to_lat").trim()
                                order.tripToLng =
                                    jsonArr.getJSONObject(i).getString("to_long").trim()
                                try {
                                    order.tripToSelf =
                                        jsonArr.getJSONObject(i).getString("to_is_self").trim()
                                            .toBoolean()
                                } catch (e: Exception) {
                                    order.tripToSelf = false
                                }

                                order.tripToName =
                                    jsonArr.getJSONObject(i).getString("to_name").trim()
                                order.tripToMob =
                                    jsonArr.getJSONObject(i).getString("to_mobile").trim()
//                                order.vehicleModel = jsonArr.getJSONObject(i).getString("VsName").trim()
                                order.scheduleDate =
                                    jsonArr.getJSONObject(i).getString("scheduled_date").trim()
                                order.scheduleTime =
                                    jsonArr.getJSONObject(i).getString("scheduled_time").trim()
//                                order.userName = jsonArr.getJSONObject(i).getString("UsrName").trim()
//                                order.userMobile = jsonArr.getJSONObject(i).getString("UsrMobNumber").trim()
//                                order.tripFilter = jsonArr.getJSONObject(i).getString("TripMFilterName").trim()
                                order.tripStatus =
                                    jsonArr.getJSONObject(i).getString("trip_status").trim()
                                order.tripSubject =
                                    jsonArr.getJSONObject(i).getString("subject").trim()
                                order.tripNotes = jsonArr.getJSONObject(i).getString("notes").trim()
                                order.tripDRate =
                                    jsonArr.getJSONObject(i).getString("trip_rate").trim()

                                ordersArrayList!!.add(order)

                                //if (selectedId != null && selectedId == order.tripId) {
                                //    orderSelected = order
                                //    show_button.isEnabled = true
                                //    order_no_value_text_view.text = orderSelected.tripNo
                                //    veh_name_value_text_view.text = orderSelected.vehicleModel
                                //    from_addr_value_text_view.text = orderSelected.tripFromAddress
                                //    to_addr_value_text_view.text = orderSelected.tripToAddress
                                //    date_value_text_view.text = orderSelected.scheduleDate
                                //    time_value_text_view.text = orderSelected.scheduleTime
                                //    subject_value_text_view.text = orderSelected.tripSubject
                                //    notes_value_text_view.text = orderSelected.tripNotes
                                //    status_value_text_view.text = orderSelected.tripFilter
                                //}
                            }
                            mHomeAdapter.notifyDataSetChanged()

                        } else {
                            UtilityFunctions.showAlertOnActivity(this@MyOrdersActivity,
                                response.getString("message"), resources.getString(R.string.Ok),
                                "", false, false, {}, {})
                        }
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }

    internal inner class MyOrdersAdapter : RecyclerView.Adapter<MyOrdersAdapter.ViewHolder>() {

        internal inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            var priceTextView: TextView = v.findViewById(R.id.priceTextView) as TextView
            var dateTextView: TextView = v.findViewById(R.id.dateTextView) as TextView
            var detailsButton: Button = v.findViewById(R.id.detailsButton) as Button
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): MyOrdersAdapter.ViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_my_orders, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.setIsRecyclable(false)

            holder.priceTextView.text = ordersArrayList!![position].tripDRate
            holder.dateTextView.text = ordersArrayList!![position].scheduleDate

            holder.detailsButton.setOnClickListener {
                //orderSelected = ordersArrayList!![position]
                //selectedId = orderSelected.tripId

                val intent = Intent(this@MyOrdersActivity, ShipmentDetailsActivity::class.java)
                intent.putExtra("order", ordersArrayList!![position])
                startActivity(intent)

                //show_button.isEnabled = true
                //order_no_value_text_view.text = orderSelected.tripNo
                //veh_name_value_text_view.text = orderSelected.vehicleModel
                //from_addr_value_text_view.text = orderSelected.tripFromAddress
                //to_addr_value_text_view.text = orderSelected.tripToAddress
                //date_value_text_view.text = orderSelected.scheduleDate
                //time_value_text_view.text = orderSelected.scheduleTime
                //subject_value_text_view.text = orderSelected.tripSubject
                //notes_value_text_view.text = orderSelected.tripNotes
                //status_value_text_view.text = orderSelected.tripFilter
            }
        }

        override fun getItemCount(): Int {
            return ordersArrayList?.size ?: 0
        }
    }
}
