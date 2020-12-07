package com.fast_prog.dyanate.views

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
import com.google.android.material.snackbar.Snackbar
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

        contactSupportLayout.setOnClickListener {
            val url = "https://wa.me/966500280135"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
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

            params["lang"] = sharedPreferences.getString(Constants.PREFS_LANG, "en")!!
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
//                                order.tripNo = order.tripId + "/ 2020"
                                order.tripNo = jsonArr.getJSONObject(i).getString("trip_no").trim()
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
                                order.estimatedPrice =
                                    jsonArr.getJSONObject(i).getString("estimated_price").trim()
                                order.driverID =
                                    jsonArr.getJSONObject(i).getString("driver_id").trim()
                                order.driverMobileNumber =
                                    jsonArr.getJSONObject(i).getString("driver_mobile").trim()
                                    order.vehicleSizeID =
                                    jsonArr.getJSONObject(i).getString("vehicle_size_id").trim()
                                order.isUnpackInstallRequired =
                                    jsonArr.getJSONObject(i).getString("unpack_install_required")
                                        .trim()
                                order.workersRequired =
                                    jsonArr.getJSONObject(i).getString("workers_required").trim()
                                order.currentStatusText =
                                    jsonArr.getJSONObject(i).getString("trip_status_text").trim()
                                order.chatEnabled =
                                    jsonArr.getJSONObject(i).getBoolean("chat_enabled")
                                order.shipmentId =
                                    jsonArr.getJSONObject(i).getString("shipment_id")
                                order.vehicleModel =
                                    jsonArr.getJSONObject(i).getString("vehicle_size")
                                order.loadingCount =
                                    jsonArr.getJSONObject(i).getString("loading_count")
                                order.unloadingCount =
                                    jsonArr.getJSONObject(i).getString("unloading_count")
                                order.is_loading_unloading_calculation =
                                    jsonArr.getJSONObject(i).getString("is_loading_unloading_calculation")
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
            var tripNoTextView: TextView = v.findViewById(R.id.tripNoTextView) as TextView
            var tripStatusTextView: TextView = v.findViewById(R.id.tripStatusTextView) as TextView
            var dateTextView: TextView = v.findViewById(R.id.dateTextView) as TextView
            var sarTextView: TextView = v.findViewById(R.id.sarTextView) as TextView
            var estimatedTextView: TextView = v.findViewById(R.id.estimatedTextView) as TextView
            var detailsButton: Button = v.findViewById(R.id.detailsButton) as Button
            var whatsAppButton: ImageView = v.findViewById(R.id.whatsAppButton) as ImageView
            var rate_driver_text_view: TextView =
                v.findViewById(R.id.rate_driver_text_view) as TextView
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

            holder.whatsAppButton.visibility = View.VISIBLE

//            if (ordersArrayList!![position].tripStatus == "3") {
//            }

            if (ordersArrayList!![position].tripStatus == "11") {
                holder.rate_driver_text_view.visibility = View.VISIBLE
                holder.whatsAppButton.visibility = View.GONE
            }

            holder.tripNoTextView.text = ordersArrayList!![position].tripNo

            if (ordersArrayList!![position].tripStatus == "2") {
                holder.tripStatusTextView.text = getString(R.string.new_trip)
            } else if (ordersArrayList!![position].tripStatus == "13") {
                holder.tripStatusTextView.text = getString(R.string.assigned_to_driver)
            } else if (ordersArrayList!![position].tripStatus == "7") {
                holder.tripStatusTextView.text = getString(R.string.on_trip)
            } else if (ordersArrayList!![position].tripStatus == "11") {
                holder.tripStatusTextView.text = getString(R.string.trip_finished)
            }

            holder.rate_driver_text_view.setOnClickListener {

                val builder = AlertDialog.Builder(this@MyOrdersActivity)
                val inflaterAlert =
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val viewDialog = inflaterAlert.inflate(R.layout.customer_review_layout, null)
                builder.setView(viewDialog)
                val dialog = builder.create()

                val buttonSubmit = viewDialog.findViewById<Button>(R.id.submitButton)
                val serviceRatingBar = viewDialog.findViewById<RatingBar>(R.id.serviceRatingBar)
                val notesEditText = viewDialog.findViewById<EditText>(R.id.commentEditText)


                buttonSubmit.setOnClickListener {
                    dialog.dismiss()

                    var rating = serviceRatingBar.rating
                    var notes = notesEditText.text.toString().trim()

                    if (rating <= 0f) {

                        UtilityFunctions.showAlertOnActivity(this@MyOrdersActivity,
                            getString(R.string.pls_add_your_rating)
                            ,
                            getString(R.string.ok),
                            "",
                            false,
                            true,
                            {},
                            {})
                        return@setOnClickListener
                    }

                    if (notes.isEmpty()) {
                        UtilityFunctions.showAlertOnActivity(this@MyOrdersActivity,
                            getString(R.string.pls_add_your_rating)
                            ,
                            getString(R.string.ok),
                            "",
                            false,
                            true,
                            {},
                            {})
                        return@setOnClickListener
                    }


                    SubmitReview(
                        notes,
                        rating,
                        ordersArrayList!![position].tripId!!,
                        ordersArrayList!![position].driverID!!
                    ).execute()
                }

                dialog.setCancelable(true)
                dialog.show()
            }

            holder.whatsAppButton.setOnClickListener {

                val url = "https://wa.me/=${ordersArrayList!![position].driverMobileNumber}"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }

            holder.priceTextView.text = ordersArrayList!![position].tripDRate
            holder.dateTextView.text =
                ordersArrayList!![position].scheduleDate + " " + ordersArrayList!![position].scheduleTime

            if (ordersArrayList!![position].tripDRate.isNullOrEmpty()) {
                //holder.sarTextView.text = ""
//                holder.estimatedTextView.visibility = View.VISIBLE
                holder.priceTextView.text = ordersArrayList!![position].estimatedPrice
            }

            holder.detailsButton.setOnClickListener {
                //orderSelected = ordersArrayList!![position]
                //selectedId = orderSelected.tripId

//                TripDetailsMasterListBackground(ordersArrayList!![position].tripId!!).execute()
                val intent =
                    Intent(this@MyOrdersActivity, ShipmentDetailsActivity::class.java)
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

    private inner class SubmitReview internal constructor(
        val notes: String,
        val rating: Float,
        val tripId: String,
        val driverId: String
    ) : AsyncTask<Void, Void, JSONObject>() {

        override fun onPreExecute() {
            super.onPreExecute()
            UtilityFunctions.showProgressDialog(this@MyOrdersActivity)
        }

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()

            params["driver_id"] = driverId
            params["user_id"] = sharedPreferences.getString(Constants.PREFS_USER_ID, "")!!
            params["rating"] = rating.toString()
            params["notes"] = notes
            params["trip_id"] = tripId

            return jsonParser.makeHttpRequest(
                Constants.BASE_URL + "customer/add_driver_rating",
                "POST",
                params
            )
        }

        override fun onPostExecute(response: JSONObject?) {
            UtilityFunctions.dismissProgressDialog()

            if (response != null) {
                try {

                    if (response.getBoolean("status")) {

                        UtilityFunctions.showAlertOnActivity(this@MyOrdersActivity,
                            resources.getString(R.string.YourReviewSaved),
                            resources.getString(R.string.Ok),
                            "",
                            false,
                            false,
                            {
                            },
                            {})
                    } else {

                        UtilityFunctions.showAlertOnActivity(this@MyOrdersActivity,
                            response.getString("message"), resources.getString(R.string.Ok),
                            "", false, false,
                            {
                            }, {})
                    }


                } catch (e: JSONException) {
                    e.printStackTrace()
                }

            } else {
                val snackbar = Snackbar.make(
                    coordinator_layout,
                    R.string.UnableToConnect,
                    Snackbar.LENGTH_LONG
                ).setAction(R.string.Ok) { finish() }
                snackbar.show()
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class TripDetailsMasterListBackground internal constructor(internal var tripID: String = "") :
        AsyncTask<Void, Void, JSONObject>() {

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()

            params["trip_id"] = tripID
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
                            order.driverID =
                                ordersJSONArray.getJSONObject(i).getString("driver_id").trim()

                            if (order.driverID != "0" && order.driverID.isNotEmpty()) {

                                order.driverName =
                                    ordersJSONArray.getJSONObject(i).getJSONObject("0")
                                        .getString("name").trim()
                                order.driverMobileNumber =
                                    "+${ordersJSONArray.getJSONObject(i).getJSONObject("0")
                                        .getString(
                                            "country_code"
                                        ).trim()}${ordersJSONArray.getJSONObject(i)
                                        .getJSONObject("0").getString(
                                            "mobile_number"
                                        ).trim()}"

                                Log.d("url_", order!!.driverName)
                            }

                            val intent =
                                Intent(this@MyOrdersActivity, ShipmentDetailsActivity::class.java)
                            intent.putExtra("order", order)
                            startActivity(intent)
                        }

                    } else {
//                        noRowMsg = response.getString("message").trim()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }
}
