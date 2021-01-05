package com.fast_prog.dyanate.views

import android.annotation.SuppressLint
import android.app.AlertDialog
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
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.fast_prog.dyanate.R
import com.fast_prog.dyanate.models.Order
import com.fast_prog.dyanate.models.Ride
import com.fast_prog.dyanate.utilities.ConnectionDetector
import com.fast_prog.dyanate.utilities.Constants
import com.fast_prog.dyanate.utilities.JsonParser
import com.fast_prog.dyanate.utilities.UtilityFunctions
import com.fast_prog.dyanate.views.edit.EditSenderLocationActivity
import kotlinx.android.synthetic.main.content_confirm_details.*
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

    lateinit var editTripButton: Button
    lateinit var cancelTripButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        sharedPreferences =
            activity!!.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        val view = inflater.inflate(R.layout.fragment_details, container, false)

        order = (activity as ShipmentDetailsActivity).order

        cancelSuccessLabel = resources.getString(R.string.CancelSuccess)
        okLabel = resources.getString(R.string.Ok)
        cancelFailedLabel = resources.getString(R.string.CancelFailed)

        view.shipmentTitleTextView.text =
            String.format(Locale.getDefault(), "%s :", resources.getString(R.string.Shipment))
        view.fromNameTitleTextView.text =
            String.format(Locale.getDefault(), "%s :", resources.getString(R.string.Name))
        view.fromMobTitleTextView.text =
            String.format(Locale.getDefault(), "%s :", resources.getString(R.string.Mobile))
//        view.engDateTitleTextView.text =
//            String.format(Locale.getDefault(), "%s :", resources.getString(R.string.Date))
//        view.arDateTitleTextView.text =
//            String.format(Locale.getDefault(), "%s :", resources.getString(R.string.Date))
        view.timeTitleTextView.text =
            String.format(Locale.getDefault(), "%s :", resources.getString(R.string.Time))
        view.toNameTitleTextView.text =
            String.format(Locale.getDefault(), "%s :", resources.getString(R.string.Name))
        view.toMobTitleTextView.text =
            String.format(Locale.getDefault(), "%s :", resources.getString(R.string.Mobile))

        editTripButton = view.editTripButton
        cancelTripButton = view.cancelTripButton

        view.tripNoTextView.text = order?.tripNo

        val flag = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        val colorSpan =
            ForegroundColorSpan(ContextCompat.getColor(activity!!, R.color.lightBlueColor))
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
        builder.append(order?.vehicleModel)
        view.vehicleTextView.text = builder

        view.fromNameTextView.text = order?.tripFromName
        view.fromMobTextView.text = order?.tripFromMob?.trimStart { it <= '+' }
        view.engDateTextView.text = order?.scheduleDate
        view.arDateTextView.text = order?.scheduleDate
        view.timeTextView.text = order?.scheduleTime
        view.toNameTextView.text = order?.tripToName
        view.toMobTextView.text = order?.tripToMob?.trimStart { it <= '+' }
        view.estimatedValueTextView.text = order?.estimatedPrice + " " + getString(R.string.SAR)
//        view.workersRequiredValueTextView.text = order?.workersRequired
        view.loading_labour_count.text = getString(R.string.loading) + ": " + order?.loadingCount
        view.unloading_labour_count.text =
            getString(R.string.unloading) + ": " + order?.unloadingCount
        if (order?.isUnpackInstallRequired == "1") {
            view.installationRequiredValue.text = getString(R.string.Yes)
        } else {
            view.installationRequiredValue.text = getString(R.string.No)
        }

        if (order?.tripStatus!! == "2" || order?.tripStatus!! == "13" || order?.tripStatus!! == "9" || order?.tripStatus!! == "8" || order?.tripStatus!! == "15") {
            editTripButton.visibility = View.VISIBLE
        }

        if (order?.tripStatus!! == "2") {
            cancelTripButton.visibility = View.VISIBLE
        }

        if (order?.is_loading_unloading_calculation == "0") {
            editTripButton.visibility = View.GONE
        }
        view.cancelTripButton.setOnClickListener {
//            UtilityFunctions.showAlertOnActivity(activity!!,
//                resources.getString(R.string.AreYouSure), resources.getString(R.string.Yes),
//                resources.getString(R.string.No), true, false,
//                {
//                    if (ConnectionDetector.isConnected(activity!!)) {
//                        TripMasterStatusUpdateBackground(order?.tripId!!).execute()
//                    }
//                }, {})


            val builder = AlertDialog.Builder(activity!!)
            val inflaterAlert =
                activity!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val viewDialog = inflaterAlert.inflate(R.layout.cancel_reason_layout, null)
            builder.setView(viewDialog)
            val dialog = builder.create()

            val buttonSubmit = viewDialog.findViewById<Button>(R.id.submitButton)
            val reasonEditText = viewDialog.findViewById<EditText>(R.id.reasonET)


            buttonSubmit.setOnClickListener {
                dialog.dismiss()

                var reason = reasonEditText.text.toString().trim()
                if (reason.isEmpty()) {
                    UtilityFunctions.showAlertOnActivity(activity!!,
                        getString(R.string.pls_add_your_rating),
                        getString(R.string.ok),
                        "",
                        false,
                        true,
                        {},
                        {})
                    return@setOnClickListener
                }


                TripMasterStatusUpdateBackground(order?.tripId!!, reason).execute()
            }

            dialog.setCancelable(true)
            dialog.show()
        }

        view.editTripButton.setOnClickListener {
            startActivity(
                Intent(
                    activity!!,
                    EditSenderLocationActivity::class.java
                ).putExtra("order", order!!)
            )
        }

        return view
    }

    @SuppressLint("StaticFieldLeak")
    private inner class TripMasterStatusUpdateBackground internal constructor(internal var tripDMId: String, internal var reason: String) :
        AsyncTask<Void, Void, JSONObject>() {

        override fun onPreExecute() {
            super.onPreExecute()
            UtilityFunctions.showProgressDialog(activity!!)
        }

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()

            params["trip_id"] = tripDMId
            params["lang"] = sharedPreferences.getString(Constants.PREFS_LANG, "ar")!!
            params["reason"] = reason
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
                        UtilityFunctions.showAlertOnActivity(activity!!,
                            cancelSuccessLabel, okLabel, "", false, false,
                            {
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