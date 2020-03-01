package com.fast_prog.dyanate.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fast_prog.dyanate.R
import com.fast_prog.dyanate.utilities.ConnectionDetector
import com.fast_prog.dyanate.utilities.Constants
import com.fast_prog.dyanate.utilities.JsonParser
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_feedback_detail.*
import kotlinx.android.synthetic.main.content_feedback_detail.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class FeedbackDetailActivity : AppCompatActivity() {

    companion object {
        internal var jsonObject: JSONObject? = null
        internal var readStatus: Boolean = true
        internal var isComplaint = true
    }

    internal lateinit var sharedPreferences: SharedPreferences

    internal lateinit var linearLayoutManagerImagesList: LinearLayoutManager

    internal lateinit var recyclerViewAdapterImagesList: RecyclerView.Adapter<*>

    internal var stringImagesList: List<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback_detail)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener { finish() }

        customTitle(resources.getString(R.string.ShowFeedbacks))

        if (ConnectionDetector.isConnected(applicationContext)) {
            FeedbackUpdateIsReadBackground().execute()
        } else {
            ConnectionDetector.errorSnackbar(coordinator_layout)
        }

        recyclerView_feedbacks_images.setHasFixedSize(true)
        linearLayoutManagerImagesList = LinearLayoutManager(this@FeedbackDetailActivity, LinearLayoutManager.HORIZONTAL, false)
        recyclerView_feedbacks_images.layoutManager = linearLayoutManagerImagesList
        recyclerViewAdapterImagesList = ImagesAdapter()
        recyclerView_feedbacks_images.adapter = recyclerViewAdapterImagesList

        textView_username.text = String.format(Locale.getDefault(), "%s : %s", resources.getString(R.string.Name), jsonObject?.getString("FeedUserId")?.trim())
        textView_mobile.text = String.format(Locale.getDefault(), "%s : %s", resources.getString(R.string.Mobile), jsonObject?.getString("FeedRequest")?.trim())
        textView_date.text = String.format(Locale.getDefault(), "%s : %s", resources.getString(R.string.Date), jsonObject?.getString("FeedCrDtTm")?.trim())
        textView_message.text = jsonObject?.getString("FeedRequest")?.trim()
        textView_message.movementMethod = ScrollingMovementMethod()

//        if (jsonObject?.getString("adminReply")?.trim().isNullOrEmpty()) {
//            if (sharedPreferences.getString(Constants.BASE_URL_AR, "")!!.equals(Constants.USER_TYPE_CONST_ADMIN, true)) {
//                textView_admin_reply_title.text = resources.getString(R.string.TypeYourReply)
//                editText_admin_reply.visibility = View.VISIBLE
//                textView_admin_reply.visibility = View.GONE
//                button_submit.visibility = View.VISIBLE
//
//                button_submit.setOnClickListener {
//                    replyVal = editText_admin_reply.text.toString().trim()
//
//                    if (replyVal.isNullOrBlank()) {
//                        UtilityFunctions.showAlertOnActivity(this@FeedbackDetailActivity,
//                                resources.getString(R.string.ReplyIsEmpty).toString(), resources.getString(R.string.Ok).toString(),
//                                "", false, false, {}, {})
//
//                    } else {
//                        if (ConnectionDetector.isConnected(applicationContext)) {
//                            FeedbackUpdateAnswerBackground().execute()
//                        } else {
//                            ConnectionDetector.errorSnackbar(coordinator_layout)
//                        }
//                    }
//                }
//
//            } else {
        textView_admin_reply_title.visibility = View.GONE
        editText_admin_reply.visibility = View.GONE
        textView_admin_reply.visibility = View.GONE
        button_submit.visibility = View.GONE
        view_admin_reply.visibility = View.GONE
//            }
//        } else {
//            textView_admin_reply_title.text = resources.getString(R.string.Reply)
//            textView_admin_reply.text = jsonObject?.getString("adminReply")?.trim()
//            textView_admin_reply.movementMethod = ScrollingMovementMethod()
//        }

        if (!jsonObject?.getString("FeedDocs")?.trim()?.equals("nil", true)!! && !jsonObject?.getString("FeedDocs")?.trim().isNullOrEmpty()) {
            stringImagesList = jsonObject?.getString("FeedDocs")?.trim()?.split(",")?.map { it.trim() }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class FeedbackUpdateIsReadBackground : AsyncTask<Void, Void, JSONObject>() {

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()

            params["ArgFeedId"] = jsonObject?.getString("FeedId")?.trim()!!
            params["ArgFeedIsRead"] = "true"

            var BASE_URL = Constants.BASE_URL_EN + "FeedbackUpdateIsRead"

            if (sharedPreferences.getString(Constants.PREFS_LANG, "en")!!.equals("ar", true)) {
                BASE_URL = Constants.BASE_URL_AR + "FeedbackUpdateIsRead"
            }

            return jsonParser.makeHttpRequest(BASE_URL, "POST", params)
        }

        override fun onPostExecute(jsonObject: JSONObject?) {
            if (jsonObject != null) {
                try {
                    if (jsonObject.getBoolean("status")) {
                        print("success")
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }

    internal inner class ImagesAdapter : RecyclerView.Adapter<ImagesAdapter.ViewHolder>() {

        internal inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            var textViewSlNo: TextView = v.findViewById<View>(R.id.textView_slNo) as TextView
            var imageViewFile: ImageView = v.findViewById<View>(R.id.imageView_file) as ImageView
            var imageViewIdProofDelete: ImageView = v.findViewById<View>(R.id.imageView_idProof_delete) as ImageView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.layout_image_list, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.setIsRecyclable(false)

            try {
                holder.textViewSlNo.text = String.format("%s.",(position + 1).toString())
                Picasso.get().load(Constants.IMG_URL + "/" + stringImagesList?.get(position)?.trim()).placeholder(R.drawable.progress_view).error(R.drawable.dynate_1).into(holder.imageViewFile)
                holder.imageViewIdProofDelete.visibility = View.GONE

                holder.imageViewFile.setOnClickListener {
                    ShowPDFImageActivity.imgURL = Constants.IMG_URL + "/" + stringImagesList?.get(position)?.trim()
                    ShowPDFImageActivity.docType = resources.getString(R.string.Attachment)
                    startActivity(Intent(this@FeedbackDetailActivity, ShowPDFImageActivity::class.java))
                }

            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        override fun getItemCount(): Int {
            return stringImagesList?.size?:0
        }
    }

//    @SuppressLint("StaticFieldLeak")
//    private inner class FeedbackUpdateAnswerBackground : AsyncTask<Void, Void, JSONObject>() {
//
//        override fun onPreExecute() {
//            super.onPreExecute()
//            UtilityFunctions.showProgressDialog(this@FeedbackDetailActivity)
//        }
//
//        override fun doInBackground(vararg param: Void): JSONObject? {
//            val jsonParser = JsonParser()
//            val params = HashMap<String, String>()
//
//            params["ArgFeedId"] = jsonObject?.getString("FeedId")?.trim()!!
//            params["ArgFeedAnswer"] = replyVal!!
//
//            var BASE_URL = Constants.BASE_URL_EN + "FeedbackUpdateAnswer"
//
//            if (sharedPreferences.getString(Constants.PREFS_LANG, "en")!!.equals("ar", true)) {
//                BASE_URL = Constants.BASE_URL_AR + "FeedbackUpdateAnswer"
//            }
//
//            return jsonParser.makeHttpRequest(BASE_URL, "POST", params)
//        }
//
//        override fun onPostExecute(jsonObject: JSONObject?) {
//            UtilityFunctions.dismissProgressDialog()
//
//            if (jsonObject != null) {
//                try {
//                    if (jsonObject.getBoolean("status")) {
//                        UtilityFunctions.showAlertOnActivity(this@FeedbackDetailActivity,
//                                resources.getString(R.string.ReplyUpdated), resources.getString(R.string.Ok).toString(),
//                                "", false, false, { finish() }, {})
//
//                    } else {
//                        UtilityFunctions.showAlertOnActivity(this@FeedbackDetailActivity,
//                                jsonObject.getString("message"), resources.getString(R.string.Ok).toString(),
//                                "", false, false, {}, {})
//                    }
//                } catch (e: JSONException) {
//                    e.printStackTrace()
//                }
//            }
//        }
//    }
}
