package com.fast_prog.dyanate.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.view.Gravity
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
import com.fast_prog.dyanate.models.NotnModel
import com.fast_prog.dyanate.utilities.ConnectionDetector
import com.fast_prog.dyanate.utilities.Constants
import com.fast_prog.dyanate.utilities.JsonParser
import com.fast_prog.dyanate.utilities.UtilityFunctions
import kotlinx.android.synthetic.main.activity_notifications_list.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class NotificationsListActivity : AppCompatActivity() {

    internal lateinit var sharedPreferences: SharedPreferences

    internal lateinit var linearLayoutManagerNotifications: LinearLayoutManager

    internal lateinit var recyclerViewAdapterNotifications: RecyclerView.Adapter<*>

    internal var loaded = false

    companion object {
        internal var notnModels: MutableList<NotnModel>? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications_list)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        toolbar.setNavigationOnClickListener { finish() }

        customTitle(resources.getString(R.string.Notifications))

        loaded = intent.getBooleanExtra("loaded", false)

        recyclerView_notifications.setHasFixedSize(true)
        linearLayoutManagerNotifications = LinearLayoutManager(this@NotificationsListActivity)
        recyclerView_notifications.layoutManager = linearLayoutManagerNotifications
        recyclerViewAdapterNotifications = NotificationsAdapter()
        recyclerView_notifications.adapter = recyclerViewAdapterNotifications
    }

    override fun onResume() {
        super.onResume()

        if (ConnectionDetector.isConnected(applicationContext)) {
            if (loaded) {
                GetNotificationsListByCustIdBackground().execute()
            }
        } else {
            ConnectionDetector.errorSnackbar(coordinator_layout)
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class GetNotificationsListByCustIdBackground : AsyncTask<Void, Void, JSONObject>() {

        override fun onPreExecute() {
            super.onPreExecute()
            UtilityFunctions.showProgressDialog (this@NotificationsListActivity)
        }

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
            UtilityFunctions.dismissProgressDialog()

            if (response != null) {
                try {
                    if (response.getBoolean("status")) {
                        val jsonArray = response.getJSONArray("data")
                        notnModels = ArrayList()

                        for (i in 0 until jsonArray.length()) {
                            val notnModel = NotnModel()

                            notnModel.nfId = jsonArray.getJSONObject(i).getString("NfId").trim()
                            notnModel.nfUserId = jsonArray.getJSONObject(i).getString("NfUserId").trim()
                            notnModel.nfTripMId = jsonArray.getJSONObject(i).getString("NfTripMId").trim()
                            notnModel.nfTitle = jsonArray.getJSONObject(i).getString("NfTitle").trim()
                            notnModel.nfBody = jsonArray.getJSONObject(i).getString("NfBody").trim()
                            notnModel.nfcategory = jsonArray.getJSONObject(i).getString("Nfcategory").trim()
                            notnModel.nfReadStatus = jsonArray.getJSONObject(i).getString("NfReadStatus").trim()
                            notnModel.nfActive = jsonArray.getJSONObject(i).getString("NfActive").trim()
                            notnModel.nfCreateDtTime = jsonArray.getJSONObject(i).getString("NfCreateDtTime").trim()
                            notnModel.nfReadDtTime = jsonArray.getJSONObject(i).getString("NfReadDtTime").trim()

                            (notnModels as ArrayList<NotnModel>).add(notnModel)
                        }

                        recyclerViewAdapterNotifications.notifyDataSetChanged()

                    } else {
                        notnModels = ArrayList()
                        recyclerViewAdapterNotifications.notifyDataSetChanged()
                    }

                    if (notnModels?.isEmpty()!! || notnModels?.size?:0 < 1) {
                        textView_count.text = resources.getString(R.string.NoNotifications)
                        textView_count.gravity = Gravity.CENTER
                    } else {
                        textView_count.text = String.format(Locale.getDefault(), "%d %s", notnModels!!.size, resources.getString(R.string.Notifications))
                    }

                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }

    internal inner class NotificationsAdapter : RecyclerView.Adapter<NotificationsAdapter.ViewHolder>() {

        internal inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            var textViewTitle: TextView = v.findViewById<View>(R.id.textView_title) as TextView
            var textViewMessage: TextView = v.findViewById<View>(R.id.textView_message) as TextView
            var textViewTime: TextView = v.findViewById<View>(R.id.textView_time) as TextView
            var buttonShow: Button = v.findViewById<View>(R.id.button_show) as Button
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.layout_notifications_list, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.setIsRecyclable(false)

            val notnModel = notnModels!![position]

            holder.textViewTitle.text = notnModel.nfTitle
            holder.textViewMessage.text = notnModel.nfBody
            holder.textViewTime.text = notnModel.nfCreateDtTime

            if (sharedPreferences.getString(Constants.PREFS_LANG, "en").equals("ar", true)) {
                holder.textViewTitle.textDirection = View.TEXT_DIRECTION_RTL
                holder.textViewMessage.textDirection = View.TEXT_DIRECTION_RTL
                holder.textViewTime.textDirection = View.TEXT_DIRECTION_RTL
            } else {
                holder.textViewTitle.textDirection = View.TEXT_DIRECTION_LTR
                holder.textViewMessage.textDirection = View.TEXT_DIRECTION_LTR
                holder.textViewTime.textDirection = View.TEXT_DIRECTION_LTR
            }

            holder.buttonShow.setOnClickListener {
                //val selectedItemAt = position
                //val tripID = notnModel.nfId!!
            }
        }

        override fun getItemCount(): Int {
            loaded = true
            return notnModels?.size?:0
        }
    }
}
