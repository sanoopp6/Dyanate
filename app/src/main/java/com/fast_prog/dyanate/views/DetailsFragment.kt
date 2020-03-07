package com.fast_prog.dyanate.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.fast_prog.dyanate.R
import com.fast_prog.dyanate.models.Order
import com.fast_prog.dyanate.utilities.ConnectionDetector
import com.fast_prog.dyanate.utilities.Constants
import com.fast_prog.dyanate.utilities.JsonParser
import com.fast_prog.dyanate.utilities.UtilityFunctions
import kotlinx.android.synthetic.main.fragment_details.view.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class DetailsFragment : Fragment() {

    internal lateinit var sharedPreferences: SharedPreferences

    private var order: Order? = null

    internal var cancelSuccessLabel = ""

    internal var okLabel = ""

    internal var cancelFailedLabel = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        sharedPreferences = activity!!.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        val view = inflater.inflate(R.layout.fragment_details, container, false)

        order = (activity as ShipmentDetailsActivity).order

        cancelSuccessLabel = resources.getString(R.string.CancelSuccess)
        okLabel = resources.getString(R.string.Ok)
        cancelFailedLabel = resources.getString(R.string.CancelFailed)

        view.shipmentTitleTextView.text = String.format(Locale.getDefault(), "%s :", resources.getString(R.string.Shipment))
        view.fromNameTitleTextView.text = String.format(Locale.getDefault(), "%s :", resources.getString(R.string.Name))
        view.fromMobTitleTextView.text = String.format(Locale.getDefault(), "%s :", resources.getString(R.string.Mobile))
        view.engDateTitleTextView.text = String.format(Locale.getDefault(), "%s :", resources.getString(R.string.Date))
        view.arDateTitleTextView.text = String.format(Locale.getDefault(), "%s :", resources.getString(R.string.Date))
        view.timeTitleTextView.text = String.format(Locale.getDefault(), "%s :", resources.getString(R.string.Time))
        view.toNameTitleTextView.text = String.format(Locale.getDefault(), "%s :", resources.getString(R.string.Name))
        view.toMobTitleTextView.text = String.format(Locale.getDefault(), "%s :", resources.getString(R.string.Mobile))

        view.tripNoTextView.text = order?.tripNo

        val flag = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        val colorSpan = ForegroundColorSpan(ContextCompat.getColor(activity!!, R.color.lightBlueColor))
        var builder = SpannableStringBuilder()

        var spannableString = SpannableString(resources.getString(R.string.Subject) + " : ")
        spannableString.setSpan(colorSpan, 0, spannableString.length, flag)
        builder.append(spannableString)
        builder.append(order?.tripSubject)
        view.subjectTextView.text = builder

        view.shipmentTextView.text = order?.tripNotes

        builder = SpannableStringBuilder()
        spannableString = SpannableString(resources.getString(R.string.Size) + " : ")
        spannableString.setSpan(colorSpan, 0, spannableString.length, flag)
        builder.append(spannableString)
//        builder.append(order?.vehicleModel)
        view.vehicleTextView.text = builder

        view.fromNameTextView.text = order?.tripFromName
        view.fromMobTextView.text = order?.tripFromMob?.trimStart{ it <= '+'}
        view.engDateTextView.text = order?.scheduleDate
        view.arDateTextView.text = order?.scheduleDate
        view.timeTextView.text = order?.scheduleTime
        view.toNameTextView.text = order?.tripToName
        view.toMobTextView.text = order?.tripToMob?.trimStart{ it <= '+'}

//        view.cancelTripButton.setOnClickListener {
//            UtilityFunctions.showAlertOnActivity(activity!!,
//                    resources.getString(R.string.AreYouSure), resources.getString(R.string.Yes),
//                    resources.getString(R.string.No), true, false,
//                    {
//                        if (ConnectionDetector.isConnected(activity!!)) {
//                            TripMasterStatusUpdateBackground(order?.tripId!!).execute()
//                        }
//                    }, {})
//        }

        return view
    }

    @SuppressLint("StaticFieldLeak")
    private inner class TripMasterStatusUpdateBackground internal constructor(internal var tripDMId: String) : AsyncTask<Void, Void, JSONObject>() {

        override fun onPreExecute() {
            super.onPreExecute()
            UtilityFunctions.showProgressDialog (activity!!)
        }

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()

            params["ArgTripMID"] = tripDMId
            params["ArgTripMStatus"] = "4"

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
                        UtilityFunctions.showAlertOnActivity(activity!!,
                                cancelSuccessLabel, okLabel, "", false, false,
                                {
                                    startActivity(Intent(activity!!, MyOrdersActivity::class.java))
                                    activity!!.finish()
                                }, {})

                    } else {
                        UtilityFunctions.showAlertOnActivity(activity!!,
                                cancelFailedLabel, okLabel, "", false, false, {}, {})
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }
}