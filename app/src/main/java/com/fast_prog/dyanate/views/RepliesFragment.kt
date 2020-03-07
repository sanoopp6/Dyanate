package com.fast_prog.dyanate.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fast_prog.dyanate.R
import com.fast_prog.dyanate.models.Order
import com.fast_prog.dyanate.utilities.ConnectionDetector
import com.fast_prog.dyanate.utilities.Constants
import com.fast_prog.dyanate.utilities.JsonParser
import com.fast_prog.dyanate.utilities.UtilityFunctions
import kotlinx.android.synthetic.main.fragment_replies.view.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class RepliesFragment : Fragment() {

    internal lateinit var sharedPreferences: SharedPreferences

    private var order: Order? = null

    internal var orderList: MutableList<Order>? = null

    internal lateinit var showDetailLayoutManager: LinearLayoutManager

    internal lateinit var mShowDetailAdapter: RecyclerView.Adapter<*>

    internal var recyclerDriverReplies: RecyclerView? = null

    internal var noRowTextView: TextView? = null

    internal var selectedIndex: Int? = null

    internal var rate: Double = 0.0

    internal var yesLabel = ""

    internal var noLabel = ""

    internal var areYouSureLabel = ""

    internal var yourReplyUpdatedLabel = ""

    internal var okLabel = ""

    internal var rejectionSuccessLabel = ""

    internal var noRowMsg = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        sharedPreferences =
            activity!!.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        val view = inflater.inflate(R.layout.fragment_replies, container, false)

        order = (activity as ShipmentDetailsActivity).order

        orderList = (activity as ShipmentDetailsActivity).orderList

        yesLabel = resources.getString(R.string.Yes)

        noLabel = resources.getString(R.string.No)

        areYouSureLabel = resources.getString(R.string.AreYouSure)

        yourReplyUpdatedLabel = resources.getString(R.string.YourReplyUpdated)

        okLabel = resources.getString(R.string.Ok)

        rejectionSuccessLabel = resources.getString(R.string.RejectionSuccess)

        noRowMsg = (activity as ShipmentDetailsActivity).noRowMsg

        recyclerDriverReplies = view.recycler_driver_replies
        noRowTextView = view.noRowTextView

        recyclerDriverReplies!!.setHasFixedSize(true)
        showDetailLayoutManager = LinearLayoutManager(activity!!)
        recyclerDriverReplies!!.layoutManager = showDetailLayoutManager
        mShowDetailAdapter = ShowDetailAdapter()
        recyclerDriverReplies!!.adapter = mShowDetailAdapter

        if (orderList?.size ?: 0 > 0) {
            mShowDetailAdapter.notifyDataSetChanged()

        } else {
            noRowTextView!!.visibility = View.VISIBLE
            noRowTextView!!.text = noRowMsg
            recyclerDriverReplies!!.visibility = View.GONE
        }

        return view
    }

    internal inner class ShowDetailAdapter : RecyclerView.Adapter<ShowDetailAdapter.ViewHolder>() {

        internal inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            var txtOrderNo: TextView = v.findViewById(R.id.txt_order_no) as TextView
            var txtDate: TextView = v.findViewById(R.id.txt_date) as TextView
            var textDistance: TextView = v.findViewById(R.id.text_distance) as TextView
            var edittextRate: EditText = v.findViewById(R.id.edittext_rate) as EditText
            var textNegotiable: TextView = v.findViewById(R.id.text_negotiable) as TextView
            //            var textStatus: TextView = v.findViewById(R.id.text_status) as TextView
            var textDriver: TextView = v.findViewById(R.id.text_driver) as TextView
            var textDriverNo: TextView = v.findViewById(R.id.text_driver_no) as TextView
            var btnApprove: Button = v.findViewById(R.id.btn_approve) as Button
            var btnCancel: Button = v.findViewById(R.id.btn_cancel) as Button

        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ShowDetailAdapter.ViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_driver_replies, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.setIsRecyclable(false)

            holder.txtOrderNo.text = orderList!![position].tripDNo
            holder.txtDate.text = orderList!![position].tripDDateTime
            holder.textDistance.text = orderList!![position].distanceKm
            holder.edittextRate.setText(orderList!![position].tripDRate)
            holder.textNegotiable.text = orderList!![position].tripDIsNegotiable
//            holder.textStatus.text = orderList!![position].tripDFilterName
            holder.textDriver.text = orderList!![position].dmName
            holder.textDriverNo.text = orderList!![position].dmMobNumber

            val tripDStatus = orderList!![position].tripDStatus

//            if (orderList!![position].tripDIsNegotiable!!.equals("true", ignoreCase = true)) {
//                holder.textNegotiable.text = yesLabel
//                if (tripDStatus!!.matches("1".toRegex())) {
//                    holder.edittextRate.isEnabled = true
//                    holder.edittextRate.textAlignment = View.TEXT_ALIGNMENT_CENTER
//                    holder.edittextRate.background = ContextCompat.getDrawable(
//                        activity!!,
//                        R.drawable.layout_rect_white_lightblue
//                    )
//                }
//            } else {
//                holder.textNegotiable.text = noLabel
//            }

//            if (!tripDStatus!!.equals("1", ignoreCase = true)) {
//                holder.btnApprove.visibility = View.GONE
//            }

//            if (tripDStatus.matches("3|4|5".toRegex())) {
//                holder.btnCancel.visibility = View.GONE
//            }

//            holder.btnApprove.setOnClickListener {
//                UtilityFunctions.showAlertOnActivity(activity!!,
//                    areYouSureLabel,
//                    yesLabel,
//                    noLabel,
//                    true,
//                    false,
//                    {
//                        if (ConnectionDetector.isConnected(activity!!)) {
//                            selectedIndex = position
//                            rate = holder.edittextRate.text.toString().trim().toDouble()
//                            TripDetailsBackground().execute()
//                        }
//                    },
//                    {})
//            }
//
//            holder.btnCancel.setOnClickListener {
//                UtilityFunctions.showAlertOnActivity(activity!!,
//                    areYouSureLabel,
//                    yesLabel,
//                    noLabel,
//                    true,
//                    false,
//                    {
//                        if (ConnectionDetector.isConnected(activity!!)) {
//                            selectedIndex = position
//                            TripDetailsStatusUpdateBackground().execute()
//                        }
//                    },
//                    {})
//            }
        }

        override fun getItemCount(): Int {
            return orderList?.size ?: 0
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class TripDetailsBackground : AsyncTask<Void, Void, JSONObject>() {
        internal var jsonStatus: JSONObject? = null
        internal var jsonDetails: JSONObject? = null

        override fun onPreExecute() {
            super.onPreExecute()
            UtilityFunctions.showProgressDialog(activity!!)
        }

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParserStatus = JsonParser()
            val jsonParserDetails = JsonParser()

            var BASE_URL = Constants.BASE_URL_EN

            if (sharedPreferences.getString(Constants.PREFS_LANG, "en")!!.equals(
                    "ar",
                    ignoreCase = true
                )
            ) {
                BASE_URL = Constants.BASE_URL_AR
            }

            var params = HashMap<String, String>()

            params["ArgTripDID"] = orderList!![selectedIndex!!].tripdId!!
            params["ArgTripDStatus"] = "2"

            jsonStatus = jsonParserStatus.makeHttpRequest(
                BASE_URL + "TripDetailsStatusUpdate",
                "POST",
                params
            )

            params = HashMap()

            params["ArgTripDID"] = orderList!![selectedIndex!!].tripdId!!
            params["ArgTripDRate"] = rate.toString()
            params["ArgTripDIsNegotiable"] = orderList!![selectedIndex!!].tripDIsNegotiable!!

            jsonDetails =
                jsonParserDetails.makeHttpRequest(BASE_URL + "TripDetailsUpdate", "POST", params)

            return jsonDetails
        }

        override fun onPostExecute(jsonObject: JSONObject) {
            UtilityFunctions.dismissProgressDialog()

            if (jsonDetails != null && jsonStatus != null) {
                try {
                    if (jsonDetails!!.getBoolean("status") && jsonStatus!!.getBoolean("status")) {
                        UtilityFunctions.showAlertOnActivity(activity!!,
                            yourReplyUpdatedLabel,
                            okLabel,
                            "",
                            false,
                            false,
                            {
                                //if (ConnectionDetector.isConnected(activity!!)) {
                                //    TripDetailsMasterListBackground(false).execute()
                                //}
                            },
                            {})

                    } else {
                        UtilityFunctions.showAlertOnActivity(
                            activity!!,
                            jsonObject.getString("message"),
                            okLabel,
                            "",
                            false,
                            false,
                            {},
                            {})
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class TripDetailsStatusUpdateBackground : AsyncTask<Void, Void, JSONObject>() {

        override fun onPreExecute() {
            super.onPreExecute()
            UtilityFunctions.showProgressDialog(activity!!)
        }

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()

            params["ArgTripDID"] = orderList!![selectedIndex!!].tripdId!!
            params["ArgTripDStatus"] = "3"

            var BASE_URL = Constants.BASE_URL_EN + "TripDetailsStatusUpdate"

            if (sharedPreferences.getString(Constants.PREFS_LANG, "en")!!.equals(
                    "ar",
                    ignoreCase = true
                )
            ) {
                BASE_URL = Constants.BASE_URL_AR + "TripDetailsStatusUpdate"
            }

            return jsonParser.makeHttpRequest(BASE_URL, "POST", params)
        }

        override fun onPostExecute(response: JSONObject?) {
            UtilityFunctions.dismissProgressDialog()

            if (response != null) {
                try {
                    if (response.getBoolean("status")) {
                        UtilityFunctions.showAlertOnActivity(activity!!,
                            rejectionSuccessLabel,
                            okLabel,
                            "",
                            false,
                            false,
                            {
                                //if (ConnectionDetector.isConnected(activity!!)) {
                                //    TripDetailsMasterListBackground(false).execute()
                                //}
                            },
                            {})

                    } else {
                        UtilityFunctions.showAlertOnActivity(
                            activity!!,
                            response.getString("message"),
                            okLabel,
                            "",
                            false,
                            false,
                            {},
                            {})
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }

}