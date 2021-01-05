package com.fast_prog.dyanate.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.fast_prog.dyanate.R
import com.fast_prog.dyanate.utilities.Constants
import com.fast_prog.dyanate.utilities.JsonParser
import com.fast_prog.dyanate.utilities.UtilityFunctions
import org.json.JSONObject

class PaymentActivity : AppCompatActivity() {

    private var webView: WebView? = null
    private var progressBar: ProgressBar? = null
    private var url = ""


    var sharedPreferences: SharedPreferences? = null

    var tripNo = ""
    var tripPrice = ""
    var labourPrice = ""
    var installationPrice = ""
    var totalPrice = ""
    var payment_type = ""
    var trip_id = ""

    internal var checkPayment: Thread? = null
    internal var checkPaymentStatus = true


    var lang = "en"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_payment)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        tripNo = intent.getStringExtra("trip_no")
        tripPrice = intent.getStringExtra("trip_price")
        labourPrice = intent.getStringExtra("labour_price")
        installationPrice = intent.getStringExtra("installation_price")
        totalPrice = intent.getStringExtra("total_price")
        payment_type = intent.getStringExtra("payment_type")
        trip_id = intent.getStringExtra("trip_id")

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)

        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)

        lang = sharedPreferences!!.getString(Constants.PREFS_LANG, "en")!!

        webView!!.setWebViewClient(MyWebViewClient())
        webView!!.getSettings().javaScriptEnabled = true
        webView!!.getSettings().allowFileAccess = true
        webView!!.getSettings().builtInZoomControls = true
        webView!!.getSettings().displayZoomControls = true
        webView!!.getSettings().domStorageEnabled = true
        webView!!.getSettings().setAppCacheEnabled(true)


        if (payment_type == "creditcard") {
            if (lang == "ar") {
                 url = "https://afz3.fastprog.com.sa/payment/card_payment_form_ar.php?title=$tripNo&trip_price_value=$tripPrice SAR&trip_price_title=${getString(R.string.TripPrice)}&labour_price_value=$labourPrice SAR&labour_price_title=${getString(R.string.LabourPrice)}&total_title=${getString(R.string.Total)}&total_value=$totalPrice SAR&installation_price_title=${getString(R.string.InstallationPrice)}&installation_price_value=$installationPrice&credit_card_details_title=Credit card details&card_holder_name_title=Credit card holder name&expiry_date_title=Expiration date&card_number_title=Card number&proceed_title=Proceed&source_type=$payment_type&description=${sharedPreferences!!.getString(Constants.PREFS_USER_ID, "")!!}&trip_id=$trip_id".replace(" ", "%20")
            } else {
                url = "https://afz3.fastprog.com.sa/payment/card_payment_form_en.php?title=$tripNo&trip_price_value=$tripPrice SAR&trip_price_title=${getString(R.string.TripPrice)}&labour_price_value=$labourPrice SAR&labour_price_title=${getString(R.string.LabourPrice)}&total_title=${getString(R.string.Total)}&total_value=$totalPrice SAR&installation_price_title=${getString(R.string.InstallationPrice)}&installation_price_value=$installationPrice&credit_card_details_title=Credit card details&card_holder_name_title=Credit card holder name&expiry_date_title=Expiration date&card_number_title=Card number&proceed_title=Proceed&source_type=$payment_type&description=${sharedPreferences!!.getString(Constants.PREFS_USER_ID, "")!!}&trip_id=$trip_id".replace(" ", "%20")
            }
        }
        webView!!.loadUrl(url)


        checkPayment = Thread(object : Runnable {
            var handler: Handler = @SuppressLint("HandlerLeakPayment")

            object : Handler() {
                override fun handleMessage(msg: Message) {
                    super.handleMessage(msg)
//                    GetOnlineDyanaLocBackground().execute()
                    GetCurrentPaymentStatus().execute()
                }

            }

            override fun run() {
                while (checkPaymentStatus) {
                    threadMsg("payment")

                    try {
                        Thread.sleep(4000)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }

            private fun threadMsg(msg: String) {
                if (msg != "") {
                    val msgObj = handler.obtainMessage()
                    val b = Bundle()
                    b.putString("message", msg)
                    msgObj.data = b
                    handler.sendMessage(msgObj)
                }
            }
        })

        checkPayment?.start()
    }

    override fun onResume() {
        super.onResume()
        checkPaymentStatus = true
    }

    override fun onPause() {
        checkPaymentStatus = false
        super.onPause()
    }

    private inner class GetCurrentPaymentStatus :
        AsyncTask<Void, Void, JSONObject?>() {

        override fun doInBackground(vararg p0: Void?): JSONObject? {

            val jsonParser = JsonParser()
            val params = HashMap<String, String>()
            params["lang"] = sharedPreferences!!.getString(Constants.PREFS_LANG, "en")!!
            params["trip_id"] = trip_id

            return jsonParser.makeHttpRequest(
                Constants.BASE_URL + "customer/check_payment_status",
                "POST",
                params
            )
        }

        override fun onPostExecute(result: JSONObject?) {
            if (result != null) {
                if (result.getBoolean("status")) {

                    if (result.getString("data") == "paid") {

                        checkPaymentStatus = false
                        UtilityFunctions.showAlertOnActivity(this@PaymentActivity,
                                                        getString(R.string.PaymentCompleted), resources.getString(R.string.Ok),
                                                        "", false, false, {
                                                                          this@PaymentActivity.finish()
                            }, {})
                    }

                }
            }
        }
    }

    inner class MyWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            progressBar!!.setVisibility(View.VISIBLE)
            view.loadUrl(url)
            return true
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            progressBar!!.setVisibility(View.GONE)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView!!.canGoBack()) {
            webView!!.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

