package com.fast_prog.dyanate.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Paint
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
import com.fast_prog.dyanate.utilities.ConnectionDetector
import com.fast_prog.dyanate.utilities.Constants
import com.fast_prog.dyanate.utilities.JsonParser
import com.fast_prog.dyanate.utilities.UtilityFunctions
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_show_feedback_list.*
import kotlinx.android.synthetic.main.content_show_feedback_list.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class ShowFeedbackListActivity : AppCompatActivity() {

    internal lateinit var sharedPreferences: SharedPreferences

    internal lateinit var linearLayoutManagerFeedbackList: LinearLayoutManager

    internal lateinit var recyclerViewAdapterFeedbackList: RecyclerView.Adapter<*>

    internal var jsonArrayFeedbackList: JSONArray? = null

    internal var isCommon = false

    internal var isComplaint = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_feedback_list)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        toolbar.setNavigationOnClickListener { finish() }

        isCommon = intent.getBooleanExtra("isCommon", false)

        customTitle(
            if (isCommon) resources.getString(R.string.CommonFeedbacks) else resources.getString(
                R.string.ShowFeedbacks
            )
        )

        recyclerView_feedbacks_list.setHasFixedSize(true)
        linearLayoutManagerFeedbackList = LinearLayoutManager(this@ShowFeedbackListActivity)
        recyclerView_feedbacks_list.layoutManager = linearLayoutManagerFeedbackList
        recyclerViewAdapterFeedbackList = FeedbackListAdapter()
        recyclerView_feedbacks_list.adapter = recyclerViewAdapterFeedbackList

        tabLayout_feedback.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (tab.position == 0) {
                    if (!isComplaint) {
                        isComplaint = true
                        reloadFeedback()
                    }
                } else {
                    if (isComplaint) {
                        isComplaint = false
                        reloadFeedback()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        floatingactionbutton_add.setOnClickListener {
            if (isComplaint) {
                FeedbackActivity.type = "complaints"
            } else {
                FeedbackActivity.type = "suggestions"
            }

            startActivity(Intent(this@ShowFeedbackListActivity, FeedbackActivity::class.java))
        }
    }

    private fun reloadFeedback() {
        textView_no_rows.visibility = View.GONE
        linearLayout_back.visibility = View.GONE

        jsonArrayFeedbackList = JSONArray()
        recyclerViewAdapterFeedbackList.notifyDataSetChanged()

        if (ConnectionDetector.isConnected(applicationContext)) {
            FeedbackListBackground().execute()
        } else {
            ConnectionDetector.errorSnackbar(coordinator_layout)
        }
    }

    override fun onResume() {
        super.onResume()

        if (ConnectionDetector.isConnected(applicationContext)) {
            FeedbackListBackground().execute()
        } else {
            ConnectionDetector.errorSnackbar(coordinator_layout)
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class FeedbackListBackground : AsyncTask<Void, Void, JSONObject>() {

        override fun onPreExecute() {
            super.onPreExecute()
            UtilityFunctions.showProgressDialog(this@ShowFeedbackListActivity)
        }

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()

            params["ArgFeedId"] = "0"
            params["ArgFeedUserId"] = sharedPreferences.getString(Constants.PREFS_USER_ID, "")!!
            params["ArgFeedIsComplaint"] = isComplaint.toString()
            params["ArgFeedIsComon"] = isCommon.toString()

            var BASE_URL =
                Constants.BASE_URL_EN + if (isCommon) "FeedbackIsComonList" else "FeedbackList"

            if (sharedPreferences.getString(Constants.PREFS_LANG, "en")!!.equals("ar", true)) {
                BASE_URL =
                    Constants.BASE_URL_AR + if (isCommon) "FeedbackIsComonList" else "FeedbackList"
            }

            return jsonParser.makeHttpRequest(BASE_URL, "POST", params)
        }

        override fun onPostExecute(jsonObject: JSONObject?) {
            UtilityFunctions.dismissProgressDialog()

            if (jsonObject != null) {
                try {
                    if (jsonObject.getBoolean("status")) {
                        jsonArrayFeedbackList = jsonObject.getJSONArray("data")
                        recyclerViewAdapterFeedbackList.notifyDataSetChanged()

                    } else {
                        jsonArrayFeedbackList = JSONArray()
                        recyclerViewAdapterFeedbackList.notifyDataSetChanged()
                    }

                    if (jsonArrayFeedbackList == null || jsonArrayFeedbackList?.length()!! <= 0) {
                        textView_no_rows.visibility = View.VISIBLE
                        linearLayout_back.visibility = View.GONE
                        if (!isComplaint) {
                            textView_no_rows.text = resources.getString(R.string.NoFeedbacks)
                        } else {
                            textView_no_rows.text = resources.getString(R.string.NoComplaints)
                        }
                    } else {
                        textView_no_rows.visibility = View.GONE
                        linearLayout_back.visibility = View.VISIBLE
                    }

                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                val snackbar = Snackbar.make(
                    coordinator_layout,
                    R.string.UnableToConnect,
                    Snackbar.LENGTH_LONG
                ).setAction(R.string.Ok) { }
                snackbar.show()
            }
        }
    }

    internal inner class FeedbackListAdapter :
        RecyclerView.Adapter<FeedbackListAdapter.ViewHolder>() {

        internal inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            var textView1Name: TextView = v.findViewById<View>(R.id.textView1_name) as TextView
            var textView1Mobile: TextView = v.findViewById<View>(R.id.textView1_mobile) as TextView
            var textView1Date: TextView = v.findViewById<View>(R.id.textView1_date) as TextView
            var button1View: Button = v.findViewById<View>(R.id.button1_view) as Button
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_feedbacks_list, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.setIsRecyclable(false)

            try {
                holder.textView1Name.text =
                    jsonArrayFeedbackList?.getJSONObject(position)?.getString("FeedUserId")?.trim()
                holder.textView1Mobile.text =
                    jsonArrayFeedbackList?.getJSONObject(position)?.getString("FeedUserId")?.trim()
                holder.textView1Date.text =
                    jsonArrayFeedbackList?.getJSONObject(position)?.getString("FeedCrDtTm")?.trim()
                holder.button1View.paintFlags =
                    holder.button1View.paintFlags or Paint.UNDERLINE_TEXT_FLAG

                //if (jsonArrayFeedbackList?.getJSONObject(position)?.getString("uploadedDocs")?.trim()?.equals("nil", true)!! ||
                //        jsonArrayFeedbackList?.getJSONObject(position)?.getString("uploadedDocs")?.trim().isNullOrEmpty()) {
                //    holder.imageViewAttachment.visibility = View.INVISIBLE
                //} else {
                //    holder.imageViewAttachment.visibility = View.VISIBLE
                //}

                holder.button1View.setOnClickListener {
                    val jsonObj = jsonArrayFeedbackList?.getJSONObject(position)

                    FeedbackDetailActivity.readStatus =
                        jsonObj?.getString("FeedIsRead")?.toBoolean() ?: false
                    FeedbackDetailActivity.isComplaint = isComplaint
                    FeedbackDetailActivity.jsonObject = jsonObj

                    val intent =
                        Intent(this@ShowFeedbackListActivity, FeedbackDetailActivity::class.java)
                    startActivity(intent)

                    //jsonArrayFeedbackList?.getJSONObject(position)?.put("FeedIsRead", "true")
                    //recyclerViewAdapterFeedbackList.notifyDataSetChanged()
                }

            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        override fun getItemCount(): Int {
            return jsonArrayFeedbackList?.length() ?: 0
        }
    }
}
