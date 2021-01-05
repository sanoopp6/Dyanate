package com.fast_prog.dyanate.views

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.fast_prog.dyanate.R
import com.fast_prog.dyanate.models.Order
import com.fast_prog.dyanate.models.Ride
import com.fast_prog.dyanate.utilities.*
import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_shipment_det.*
import kotlinx.android.synthetic.main.content_shipment_det.*
import net.alhazmy13.hijridatepicker.date.gregorian.GregorianDatePickerDialog
import net.alhazmy13.hijridatepicker.date.hijri.HijriDatePickerDialog
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class ShipmentDetActivity : AppCompatActivity(), GregorianDatePickerDialog.OnDateSetListener,
    HijriDatePickerDialog.OnDateSetListener {

    private var blinkText: Boolean = false
    internal lateinit var sharedPreferences: SharedPreferences

    private lateinit var orderList: List<Order>

    internal lateinit var vehicleSizeArray: JSONArray

    internal lateinit var vehicleSizeDataList: MutableList<String>
    internal lateinit var vehicleSizeIdList: MutableList<String>
    internal lateinit var vehicleImageList: MutableList<String>


    internal lateinit var vehicleSizeAdapter: ArrayAdapter<String>

    internal lateinit var shipmentTypeArray: JSONArray

    internal lateinit var shipmentTypeDataList: MutableList<String>
    internal lateinit var shipmentTypeIdList: MutableList<String>
    internal lateinit var workerCountList: MutableList<String>

    internal lateinit var shipmentTypeAdapter: ArrayAdapter<String>
    internal lateinit var workerCountAdapter: ArrayAdapter<String>


    internal lateinit var buildingLevelList: MutableList<String>
    internal lateinit var buildingLevelAdapter: ArrayAdapter<String>

    internal var runThread: Boolean? = null

    private lateinit var gpsTracker: GPSTracker

    private var clickedImg: String = ""

//    private var dateTimeUpdate: Thread? = null

    private var simpleDateFormat1 = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)
    private val simpleDateFormat2 = SimpleDateFormat("yyyy/MM/dd HH:mm aa", Locale.ENGLISH)
    private val simpleDateFormat3 = SimpleDateFormat("HH:mm aa", Locale.ENGLISH)

    private val PICK_CONTACT = 101

    private var progressDialog: Dialog? = null

    private val RESULT_LOAD_IMAGE = 103
    private val TAKE_PHOTO_CODE = 104

    private val MY_PERMISSIONS_REQUEST_CAMERA = 99

    private var mCurrentInvoicePhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shipment_det)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        customTitle(resources.getString(R.string.ShipmentDetails))

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener { finish() }

        orderList = ArrayList()
        workerCountList = mutableListOf("1", "2", "3", "4")

        try {
            Ride.instance
        } catch (e: Exception) {
            Ride.instance = Ride()
        }

        DisplayPrice().execute()
        dateTimeUpdateTextView()

        countryCodePicker_from.registerCarrierNumberEditText(edit_from_mobile)
        countryCodePicker_to.registerCarrierNumberEditText(edit_to_mobile)

        spnr_veh_size.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                val index = parent.selectedItemPosition

                Ride.instance.vehicleSizeId = vehicleSizeIdList[index]
                Ride.instance.vehicleSizeName = vehicleSizeDataList[index]

                //                Picasso.get().load(Constants.IMG_URL + "/vehicle_size/" + vehicleImageList[index])
                //                    .placeholder(R.drawable.progress_view).error(R.drawable.dynate_1)
                //                    .into(vehicle_size_image_view)

            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        workerCountAdapter = MySpinnerAdapter(
            this@ShipmentDetActivity,
            android.R.layout.select_dialog_item,
            workerCountList
        )
        loading_spnr_worker_count.adapter = workerCountAdapter

        loading_spnr_worker_count.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(p0: AdapterView<*>?) {

                }

                override fun onItemSelected(
                    p0: AdapterView<*>?,
                    p1: View?,
                    position: Int,
                    p3: Long
                ) {
                    Ride.instance.loadingCount = workerCountList[position]
                    DisplayPrice().execute()
                }
            }

        unloading_spnr_worker_count.adapter = workerCountAdapter

        unloading_spnr_worker_count.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(p0: AdapterView<*>?) {

                }

                override fun onItemSelected(
                    p0: AdapterView<*>?,
                    p1: View?,
                    position: Int,
                    p3: Long
                ) {
                    Ride.instance.unloadingCount = workerCountList[position]
                    DisplayPrice().execute()
                }
            }

        loading_check_box.setOnCheckedChangeListener { compoundButton, b ->
            if (b) {
                loading_spinner_container.visibility = View.VISIBLE
                loading_spnr_worker_count.setSelection(0)
            } else {
                loading_spinner_container.visibility = View.GONE
                Ride.instance.loadingCount = "0"
            }
            DisplayPrice().execute()
        }

        unloading_check_box.setOnCheckedChangeListener { compoundButton, b ->
            if (b) {
                unloading_spinner_container.visibility = View.VISIBLE
                unloading_spnr_worker_count.setSelection(0)
            } else {
                unloading_spinner_container.visibility = View.GONE
                Ride.instance.unloadingCount = "0"
            }
            DisplayPrice().execute()
        }


        buildingLevelList =
            mutableListOf(resources.getString(R.string.GroundFloor), "1", "2", "3", "4", "5")
        buildingLevelAdapter = MySpinnerAdapter(
            this@ShipmentDetActivity,
            android.R.layout.select_dialog_item,
            buildingLevelList
        )
        spnr_building_level.adapter = buildingLevelAdapter

        spnr_building_level.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(p0: AdapterView<*>?) {

                }

                override fun onItemSelected(
                    p0: AdapterView<*>?,
                    p1: View?,
                    position: Int,
                    p3: Long
                ) {
                    Ride.instance.buildingLevel = position.toString()
                    DisplayPrice().execute()
                }
            }

        spnr_shipment_type.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                val index = parent.selectedItemPosition

                if (position == 0) {
                    Ride.instance.shipmentTypeID = ""
                    Ride.instance.shipmentTypeName = ""
                } else {
                    Ride.instance.shipmentTypeID = shipmentTypeIdList[index]
                    Ride.instance.shipmentTypeName = shipmentTypeDataList[index]
                }
                edit_shipment.setText("")
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        if (ConnectionDetector.isConnected(applicationContext)) {
            ListVehicleSizeBackground().execute()
            GetShipmentTypeBackground().execute()
        } else {
            ConnectionDetector.errorSnackbar(coordinator_layout)
        }

        spnr_veh_size.setOnTouchListener { v, event ->
            hideKeyboard()
            false
        }

        installYesButton.setOnCheckedChangeListener { compoundButton, b ->
            if (b) {

                unpackAndInstallMsgTextView.visibility = View.VISIBLE
                Ride.instance.requiredUnpackAndInstall = "1"
            }
        }

        installNoButton.setOnCheckedChangeListener { compoundButton, b ->
            if (b) {
                unpackAndInstallMsgTextView.visibility = View.GONE
                Ride.instance.requiredUnpackAndInstall = "0"
            }
        }

        yesButton.setOnCheckedChangeListener { compoundButton, b ->
            if (b) {

                unpackAndInstallContainer.visibility = View.VISIBLE
                worker_layout.visibility = View.VISIBLE
                DisplayPrice().execute()
            }
        }

        noButton.setOnCheckedChangeListener { compoundButton, b ->
            if (b) {
                unpackAndInstallContainer.visibility = View.GONE
                worker_layout.visibility = View.GONE
                Ride.instance.requiredPersons = "0"
                Ride.instance.loadingCount = "0"
                Ride.instance.unloadingCount = "0"
                DisplayPrice().execute()
            }
        }

//        edit_subject.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
//            if (!hasFocus) {
//                val subject = edit_subject.text.toString().trim()
//
//                if (subject.isEmpty()) {
//                    UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
//                        resources.getString(R.string.InvalidSubject),
//                        resources.getString(R.string.Ok),
//                        "",
//                        false,
//                        false,
//                        {},
//                        {})
//                } else {
//                    Ride.instance.subject = subject
//                }
//            }
//        }

        edit_shipment.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                val shipment = edit_shipment.text.toString().trim()

                if (shipment.isEmpty()) {
                    UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                        resources.getString(R.string.InvalidShipment),
                        resources.getString(R.string.Ok),
                        "",
                        false,
                        false,
                        {},
                        {})
                } else {
                    Ride.instance.shipment = shipment
                }
            }
        }

        edit_from_name.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                val name = edit_from_name.text.toString().trim()

                if (name.isEmpty()) {
                    UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                        resources.getString(R.string.InvalidSenderName),
                        resources.getString(R.string.Ok),
                        "",
                        false,
                        false,
                        {},
                        {})
                } else if (stringContainsNumber(name)) {

                    UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                        resources.getString(R.string.InvalidSenderName),
                        resources.getString(R.string.Ok),
                        "",
                        false,
                        false,
                        {},
                        {})

                } else {
                    Ride.instance.fromName = name
                }
            }
        }

        edit_from_name.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {

            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                if (sameSenderCheckBox.isChecked) {
                    edit_to_name.setText(edit_from_name.text.toString().trim())
                }
            }

        })

        edit_from_mobile.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {

            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                if (sameSenderCheckBox.isChecked) {
                    edit_to_mobile.setText(edit_from_mobile.text.toString().trim())
                }
            }

        })

        sameSenderCheckBox.setOnCheckedChangeListener { compoundButton, b ->
            if (b) {

                if (edit_from_name.text.toString().trim()
                        .isNotEmpty() && edit_from_mobile.text.toString().trim().isNotEmpty()
                ) {
                    edit_to_name.setText(edit_from_name.text.toString().trim())
                    edit_to_mobile.setText(edit_from_mobile.text.toString().trim())
                } else {
                    UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                        resources.getString(R.string.InvalidSenderDetail),
                        resources.getString(R.string.Ok),
                        "",
                        false,
                        false,
                        {},
                        {})
                    sameSenderCheckBox.isChecked = false
                }
            } else {
                edit_to_name.setText("")
                edit_to_mobile.setText("")
            }
        }

        edit_from_mobile.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                val mobileNo =
                    countryCodePicker_from.selectedCountryCodeWithPlus + edit_from_mobile.text.removePrefix(
                        "0"
                    )

                if (!countryCodePicker_from.isValidFullNumber) {
                    UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                        resources.getString(R.string.InvalidSenderMob),
                        resources.getString(R.string.Ok),
                        "",
                        false,
                        false,
                        {},
                        {})
                } else {
                    Ride.instance.fromMobile = mobileNo
                }
            }
        }

        img_from_mobile.setOnClickListener {
            clickedImg = "sender"

            val intent =
                Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            startActivityForResult(intent, PICK_CONTACT)
        }

        edit_to_name.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                val name = edit_to_name.text.toString().trim()

                if (name.isEmpty()) {
                    UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                        resources.getString(R.string.InvalidReceiverName),
                        resources.getString(R.string.Ok),
                        "",
                        false,
                        false,
                        {},
                        {})
                } else if (stringContainsNumber(name)) {
                    UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                        resources.getString(R.string.InvalidReceiverName),
                        resources.getString(R.string.Ok),
                        "",
                        false,
                        false,
                        {},
                        {})
                } else {
                    Ride.instance.toName = name
                }
            }
        }

        edit_to_mobile.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                val mobileNo =
                    countryCodePicker_to.selectedCountryCodeWithPlus + edit_to_mobile.text.removePrefix(
                        "0"
                    )

                if (!countryCodePicker_to.isValidFullNumber) {
                    UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                        resources.getString(R.string.InvalidReceiverMob),
                        resources.getString(R.string.Ok),
                        "",
                        false,
                        false,
                        {},
                        {})
                } else {
                    Ride.instance.toMobile = mobileNo
                }
            }
        }

        img_to_mobile.setOnClickListener {
            clickedImg = "receiver"

            val intent =
                Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            startActivityForResult(intent, PICK_CONTACT)
        }

        gpsTracker = GPSTracker(this@ShipmentDetActivity)

//        edit_subject.setText(Ride.instance.subject)
        edit_shipment.setText(Ride.instance.shipment)
        txt_datepicker1.text = Ride.instance.date
        txt_datepicker2.text = Ride.instance.hijriDate
        txt_timepicker.text = Ride.instance.time
        edit_from_name.setText(Ride.instance.fromName)
        if (Ride.instance.fromMobile.isNotEmpty()) countryCodePicker_from.fullNumber =
            Ride.instance.fromMobile
        edit_to_name.setText(Ride.instance.toName)
        if (Ride.instance.toMobile.isNotEmpty()) countryCodePicker_to.fullNumber =
            Ride.instance.toMobile

        val newCalendar = Calendar.getInstance()

        txt_datepicker1.setOnClickListener { v ->
            hideKeyboard()

            val now = Calendar.getInstance()
            val gregorianDatePickerDialog = GregorianDatePickerDialog.newInstance(
                this@ShipmentDetActivity,
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
            )
            gregorianDatePickerDialog.minDate = now
            gregorianDatePickerDialog.setVersion(GregorianDatePickerDialog.Version.VERSION_2)
            //now.add(Calendar.YEAR, 100);
            //gregorianDatePickerDialog.setMaxDate(now);
            gregorianDatePickerDialog.show(fragmentManager, "GregorianDatePickerDialog")
        }

        txt_datepicker2.setOnClickListener { v ->
            hideKeyboard()

            val now = UmmalquraCalendar()
            val hijriDatePickerDialog = HijriDatePickerDialog.newInstance(
                this@ShipmentDetActivity,
                now.get(UmmalquraCalendar.YEAR),
                now.get(UmmalquraCalendar.MONTH),
                now.get(UmmalquraCalendar.DAY_OF_MONTH)
            )
            hijriDatePickerDialog.minDate = now
            hijriDatePickerDialog.setVersion(HijriDatePickerDialog.Version.VERSION_2)
            hijriDatePickerDialog.show(fragmentManager, "HijriDatePickerDialog")
        }

        txt_timepicker.setOnClickListener { v ->
            hideKeyboard()

            val toDatePickerDialog = TimePickerDialog(
                this@ShipmentDetActivity,
                TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
                    val AM_PM: String
                    var time: String

                    if (hourOfDay < 12) {
                        AM_PM = "AM"
                    } else {
                        AM_PM = "PM"
                    }

                    if (hourOfDay < 10) {
                        time = "0$hourOfDay:"
                    } else {
                        time = hourOfDay.toString() + ":"
                    }

                    if (minute < 10) {
                        time += "0$minute $AM_PM"
                    } else {
                        time += minute.toString() + " " + AM_PM
                    }

                    txt_timepicker.text = time
                    Ride.instance.time = time
                },
                newCalendar.get(Calendar.HOUR_OF_DAY),
                newCalendar.get(Calendar.MINUTE),
                false
            )

            toDatePickerDialog.show()
        }

        btn_book_vehicle.setOnClickListener {
            hideKeyboard()

            if (validate()) {
                if (Ride.instance.dropOffLatitude == "0" || Ride.instance.dropOffLongitude == "0") {

                    Toast.makeText(
                        this@ShipmentDetActivity,
                        getString(R.string.no_drop_off_detail),
                        Toast.LENGTH_SHORT
                    ).show()
//                    startActivity(
//                        Intent(
//                            this@ShipmentDetActivity,
//                            ConfirmDetailsActivity::class.java
//                        )
//                    )
                } else {

                    CalcuateTripPrice().execute()
//                    startActivity(
//                        Intent(
//                            this@ShipmentDetActivity,
//                            ConfirmFromToActivity::class.java
//                        )
//                    )
                }

            }

            first_disable_view.setOnClickListener {

                UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                    getString(R.string.this_type_not_available_now),
                    resources.getString(R.string.Ok),
                    "",
                    false,
                    false,
                    {},
                    {})
            }

            second_disable_view.setOnClickListener {

                UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                    getString(R.string.this_type_not_available_now),
                    resources.getString(R.string.Ok),
                    "",
                    false,
                    false,
                    {},
                    {})
            }

            third_disable_view.setOnClickListener {

                UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                    getString(R.string.this_type_not_available_now),
                    resources.getString(R.string.Ok),
                    "",
                    false,
                    false,
                    {},
                    {})
            }
        }

        vehicle_one_image_view.setOnClickListener {
            vehicle_one_image_view.setBackgroundColor(Color.parseColor("#3589c4"))
            vehicle_two_image_view.setBackgroundColor(Color.parseColor("#FFFFFF"))
            vehicle_three_image_view.setBackgroundColor(Color.parseColor("#FFFFFF"))

            Ride.instance.vehicleSizeId = vehicleSizeIdList[0]
            Ride.instance.vehicleSizeName = vehicleSizeDataList[0]
        }

        vehicle_two_image_view.setOnClickListener {
            vehicle_one_image_view.setBackgroundColor(Color.parseColor("#FFFFFF"))
            vehicle_two_image_view.setBackgroundColor(Color.parseColor("#3589c4"))
            vehicle_three_image_view.setBackgroundColor(Color.parseColor("#FFFFFF"))

            Ride.instance.vehicleSizeId = vehicleSizeIdList[1]
            Ride.instance.vehicleSizeName = vehicleSizeDataList[1]
        }

        vehicle_three_image_view.setOnClickListener {
            vehicle_one_image_view.setBackgroundColor(Color.parseColor("#FFFFFF"))
            vehicle_two_image_view.setBackgroundColor(Color.parseColor("#FFFFFF"))
            vehicle_three_image_view.setBackgroundColor(Color.parseColor("#3589c4"))

            Ride.instance.vehicleSizeId = vehicleSizeIdList[2]
            Ride.instance.vehicleSizeName = vehicleSizeDataList[2]
        }


        invoice_image_button.setOnClickListener {

            val builder = AlertDialog.Builder(this@ShipmentDetActivity)
            val inflater = this@ShipmentDetActivity.getLayoutInflater()
            val view = inflater.inflate(R.layout.alert_dialog_add_image, null)
            builder.setView(view)
            val alertDialog = builder.create()
            alertDialog.setCancelable(false)
            val linearLayoutTakePhoto =
                view.findViewById<LinearLayout>(R.id.linearLayout_take_photo)
            val linearLayoutChooseFromGallery =
                view.findViewById<LinearLayout>(R.id.linearLayout_choose_from_gallery)
            val linearLayoutCancel = view.findViewById<LinearLayout>(R.id.linearLayout_cancel)

            linearLayoutTakePhoto.setOnClickListener {
                alertDialog.dismiss()

                if (ActivityCompat.checkSelfPermission(
                        this@ShipmentDetActivity,
                        Manifest.permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this@ShipmentDetActivity,
                        arrayOf(Manifest.permission.CAMERA),
                        MY_PERMISSIONS_REQUEST_CAMERA
                    )

                } else {
//                UtilityFunctions.showAlertOnActivity(this@UploadDocsActivity,
//                        resources.getString(R.string.ShootClearly), resources.getString(R.string.Ok),
//                        "", false, false,
//                        {
                    val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    try {
                        val photoFile = UtilityFunctions.createImageFile()
                        mCurrentInvoicePhotoPath = photoFile.absolutePath
                        val uri = FileProvider.getUriForFile(
                            this@ShipmentDetActivity,
                            applicationContext.packageName + ".provider",
                            photoFile
                        )
                        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                        startActivityForResult(takePhotoIntent, TAKE_PHOTO_CODE)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

//                        }, {})
                }
            }

            linearLayoutChooseFromGallery.setOnClickListener {
                alertDialog.dismiss()

                val i = Intent(
                    Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                )
                startActivityForResult(i, RESULT_LOAD_IMAGE)
            }

            linearLayoutCancel.setOnClickListener {
                alertDialog.dismiss()
            }

            alertDialog.show()
        }

        deleteButton.setOnClickListener {
            Ride.instance.storeInvoiceName = ""
            Ride.instance.invoiceImage = null
            invoice_image_button.text = resources.getString(R.string.AttachInvoice)
            invoice_image_container_view.visibility = View.GONE
            invoiceImageView.setImageBitmap(null)
        }
    }


    @SuppressLint("StaticFieldLeak")
    private inner class CalcuateTripPrice : AsyncTask<Void, Void, JSONObject>() {

        override fun onPreExecute() {
            super.onPreExecute()
            progressDialog = UtilityFunctions.showProgressDialog(this@ShipmentDetActivity)
        }

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()
            params["lang"] = sharedPreferences.getString(Constants.PREFS_LANG, "ar")!!
//            params["persons"] = Ride.instance.requiredPersons
            params["loading_count"] = Ride.instance.loadingCount
            params["unloading_count"] = Ride.instance.unloadingCount
            params["start_lat"] = Ride.instance.pickUpLatitude!!
            params["start_lon"] = Ride.instance.pickUpLongitude!!
            params["end_lat"] = Ride.instance.dropOffLatitude!!
            params["end_lon"] = Ride.instance.dropOffLongitude!!
            params["is_loading_unloading_calculation"] = "1"
            params["building_level"] = Ride.instance.buildingLevel

            return jsonParser.makeHttpRequest(
                Constants.BASE_URL + "customer/calculate_price",
                "POST",
                params
            )
        }

        override fun onPostExecute(response: JSONObject?) {
            if (progressDialog != null) {
                UtilityFunctions.dismissProgressDialog()
            }
            if (response != null) {
                try {
                    if (response.getBoolean("status")) {

                        startActivity(
                            Intent(
                                this@ShipmentDetActivity,
                                ConfirmFromToActivity::class.java
                            ).putExtra(
                                "distance",
                                response.getJSONObject("data").getString("distance")
                            )
                                .putExtra("time", response.getJSONObject("data").getString("time"))
                                .putExtra(
                                    "price",
                                    response.getJSONObject("data").getString("price")
                                )
                                .putExtra(
                                    "trip_price",
                                    response.getJSONObject("data").getString("trip_price")
                                )
                                .putExtra(
                                    "labour_price",
                                    response.getJSONObject("data").getString("labour_price")
                                )
                        )
                    } else {

                        UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                            response.getString("message"), resources.getString(R.string.Ok),
                            "", false, false, {}, {})
                    }

                } catch (e: JSONException) {
                    UtilityFunctions.dismissProgressDialog()
                    e.printStackTrace()
                }

            } else {
                UtilityFunctions.dismissProgressDialog()
                val snackbar = Snackbar.make(
                    coordinator_layout,
                    R.string.UnableToConnect,
                    Snackbar.LENGTH_LONG
                ).setAction(R.string.Ok) { }
                snackbar.show()
            }
        }
    }

    override fun onDateSet(
        view: HijriDatePickerDialog,
        year: Int,
        monthOfYear: Int,
        dayOfMonth: Int
    ) {
        var monthOfYear = monthOfYear
        monthOfYear += 1
        var dateString: String

        if (monthOfYear < 10)
            dateString = "$year/0$monthOfYear"
        else
            dateString = "$year/$monthOfYear"

        if (dayOfMonth < 10)
            dateString += "/0" + dayOfMonth
        else
            dateString += "/" + dayOfMonth

        if (ConnectionDetector.isConnected(this@ShipmentDetActivity)) {
            GetDateBackground(false, dateString).execute()
        } else {
            ConnectionDetector.errorSnackbar(coordinator_layout)
        }
    }

    override fun onDateSet(
        view: GregorianDatePickerDialog,
        year: Int,
        monthOfYear: Int,
        dayOfMonth: Int
    ) {
        var monthOfYear = monthOfYear
        monthOfYear += 1
        var dateString: String

        if (monthOfYear < 10)
            dateString = year.toString() + "/0" + monthOfYear
        else
            dateString = year.toString() + "/" + monthOfYear

        if (dayOfMonth < 10)
            dateString += "/0" + dayOfMonth
        else
            dateString += "/" + dayOfMonth

        if (ConnectionDetector.isConnected(this@ShipmentDetActivity)) {
            GetDateBackground(true, dateString).execute()
        } else {
            ConnectionDetector.errorSnackbar(coordinator_layout)
        }
    }

    override fun onPause() {
        super.onPause()

//        if (dateTimeUpdate != null) {
//            runThread = false
//            dateTimeUpdate!!.interrupt()
//        }
    }

    override fun onResume() {
        super.onResume()

//        runThread = true
//
//        dateTimeUpdate = object : Thread() {
//            override fun run() {
//                try {
//                    while (!isInterrupted && runThread!!) {
//                        runOnUiThread { dateTimeUpdateTextView() }
//                        Thread.sleep(10000)
//                    }
//                } catch (ignored: InterruptedException) {
//                }
//
//            }
//        }
//        dateTimeUpdate!!.start()
    }

    private fun dateTimeUpdateTextView() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, 5)
        val date = cal.time
        var temp: String
        val getMyDateTime = txt_datepicker1.text.toString().trim()

        if (getMyDateTime.isNotEmpty()) {
            var getMyDate: Date? = null

            try {
                getMyDate = simpleDateFormat1.parse(getMyDateTime)
            } catch (e: ParseException) {
                e.printStackTrace()
            }

            if (getMyDate!!.before(date) || getMyDate == date) {
                temp = simpleDateFormat3.format(date)
                Ride.instance.time = temp
                txt_timepicker.text = temp
                temp = simpleDateFormat1.format(date)

                if (ConnectionDetector.isConnected(this@ShipmentDetActivity)) {
                    GetDateBackground(true, temp).execute()
                }
            }
        } else {
            temp = simpleDateFormat3.format(date)
            Ride.instance.time = temp
            txt_timepicker.text = temp
            temp = simpleDateFormat1.format(date)

            if (ConnectionDetector.isConnected(this@ShipmentDetActivity)) {
                GetDateBackground(true, temp).execute()
            }
        }
    }

    private fun dateTimeAdd2HourUpdateTextView() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.HOUR, 2)
        cal.add(Calendar.MINUTE, 2)
        val date = cal.time
        var temp: String
        val getMyDateTime = txt_datepicker1.text.toString().trim()

        if (getMyDateTime.isNotEmpty()) {
            var getMyDate: Date? = null

            try {
                getMyDate = simpleDateFormat1.parse(getMyDateTime)
            } catch (e: ParseException) {
                e.printStackTrace()
            }

            if (getMyDate!!.before(date) || getMyDate == date) {
                temp = simpleDateFormat3.format(date)
                Ride.instance.time = temp
                txt_timepicker.text = temp


                Log.d("date_", temp)

                temp = simpleDateFormat1.format(date)

                if (ConnectionDetector.isConnected(this@ShipmentDetActivity)) {
                    GetDateBackground(true, temp).execute()
                }
            }
        } else {
            temp = simpleDateFormat3.format(date)
            Ride.instance.time = temp
            txt_timepicker.text = temp
            temp = simpleDateFormat1.format(date)

            if (ConnectionDetector.isConnected(this@ShipmentDetActivity)) {
                GetDateBackground(true, temp).execute()
            }
        }
    }

    private fun validate(): Boolean {
//        val subject = edit_subject.text.toString()
        val shipment = edit_shipment.text.toString()

        if (Ride.instance.shipmentTypeID.isNullOrEmpty()) {
            UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                resources.getString(R.string.Pls_select_shipment_type),
                resources.getString(R.string.Ok),
                "",
                false,
                false,
                {},
                {})
            return false
        }
        val fromName = edit_from_name.text.toString().trim()
        val fromNumber =
            countryCodePicker_from.selectedCountryCodeWithPlus + edit_from_mobile.text.toString()
                .trim().removePrefix(
                    "0"
                )
        val dateText1 = txt_datepicker1.text.toString().trim()
        val dateText2 = txt_datepicker2.text.toString().trim()
        val timeText = txt_timepicker.text.toString().trim()
        val toName = edit_to_name.text.toString().trim()
        val toNumber =
            countryCodePicker_to.selectedCountryCodeWithPlus + edit_to_mobile.text.toString().trim()
                .removePrefix(
                    "0"
                )

        if (Ride.instance.vehicleSizeId.isNullOrEmpty()) {
            UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                resources.getString(R.string.InvalidVehicleSize), resources.getString(R.string.Ok),
                "", false, false, {}, {})
            return false
        }

//        if (subject.trim().isEmpty()) {
//            UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
//                resources.getString(R.string.InvalidSubject), resources.getString(R.string.Ok),
//                "", false, false, {}, {})
//            return false
//        }

        if (shipment.trim().isEmpty()) {
            UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                resources.getString(R.string.InvalidShipment), resources.getString(R.string.Ok),
                "", false, false, {}, {})
            return false
        }

        if (fromName.isEmpty()) {
            UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                resources.getString(R.string.InvalidSenderName), resources.getString(R.string.Ok),
                "", false, false, {}, {})
            return false
        }

        if (stringContainsNumber(fromName)) {
            UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                resources.getString(R.string.InvalidSenderName), resources.getString(R.string.Ok),
                "", false, false, {}, {})
            return false
        }

        if (!countryCodePicker_from.isValidFullNumber) {
            UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                resources.getString(R.string.InvalidSenderMob), resources.getString(R.string.Ok),
                "", false, false, {}, {})
            return false
        }

        if (dateText1.isEmpty() || dateText2.isEmpty() || timeText.isEmpty()) {
            UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                resources.getString(R.string.InvalidDate), resources.getString(R.string.Ok),
                "", false, false, {}, {})
            return false
        }

        val getMyDateTime = "$dateText1 $timeText"
        var getCurrentDate: Date? = null
        var getMyDate: Date? = null

        try {
            getCurrentDate = simpleDateFormat2.parse(simpleDateFormat2.format(Date()))
            getMyDate = simpleDateFormat2.parse(getMyDateTime)

        } catch (e: ParseException) {
            e.printStackTrace()
        }

        if (getMyDate!!.before(getCurrentDate) || getMyDate == getCurrentDate) {
            UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                resources.getString(R.string.PreviousDateOrTime), resources.getString(R.string.Ok),
                "", false, false, {
                }, {})
            return false
        }

        var calendar = Calendar.getInstance();

        calendar.time = getCurrentDate
        calendar.add(Calendar.HOUR, 2)

        // if using 12 hour system sdf.format(calendar.getTime())

        if (getMyDate!!.before(calendar.time)) {
            UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                resources.getString(R.string.minimum_2_hour_required),
                resources.getString(R.string.Ok),
                "",
                false,
                false,
                {
                    dateTimeAdd2HourUpdateTextView()

                },
                {})
            return false
        }

        var calendarDateAfterOneMonth = Calendar.getInstance();

        calendarDateAfterOneMonth.time = getCurrentDate
        calendarDateAfterOneMonth.add(Calendar.MONTH, 1)

        if (getMyDate!!.after(calendarDateAfterOneMonth.time)) {
            UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                getString(R.string.date_cannot_be_more_than_a_month),
                resources.getString(R.string.Ok),
                "",
                false,
                false,
                {},
                {})
            return false
        }

        var calendarCheckNight = Calendar.getInstance()
        calendarCheckNight.time = getMyDate

        if ((calendarCheckNight.get(Calendar.HOUR_OF_DAY) > 21) || (calendarCheckNight.get(Calendar.HOUR_OF_DAY) < 8 )) {

            var hour = calendarCheckNight.get(Calendar.HOUR_OF_DAY)
            var timeToAdd = 0
            if (hour == 22) {
                timeToAdd = 10
            } else if (hour == 23){
                timeToAdd = 9
            } else if (hour == 24) {
                timeToAdd = 8
            } else {
                timeToAdd = 8 - hour
            }

            calendarCheckNight.add(Calendar.HOUR, timeToAdd)

            UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                getString(R.string.TimeBetween8And10),
                resources.getString(R.string.Ok),
                "",
                false,
                false,
                {

                    var monthOfYear = calendarCheckNight.get(Calendar.MONTH)
                    monthOfYear += 1
                    var dateString: String

                    if (monthOfYear < 10)
                        dateString = calendarCheckNight.get(Calendar.YEAR).toString() + "/0" + monthOfYear
                    else
                        dateString = calendarCheckNight.get(Calendar.YEAR).toString() + "/" + monthOfYear

                    if (calendarCheckNight.get(Calendar.DAY_OF_MONTH) < 10) {
                        dateString += "/0" + calendarCheckNight.get(Calendar.DAY_OF_MONTH)
                    } else
                        dateString += "/" + calendarCheckNight.get(Calendar.DAY_OF_MONTH)

                    if (ConnectionDetector.isConnected(this@ShipmentDetActivity)) {
                        GetDateBackground(true, dateString).execute()
                    } else {
                        ConnectionDetector.errorSnackbar(coordinator_layout)
                    }

                    val AM_PM: String
                    var time: String

                    if (calendarCheckNight.get(Calendar.HOUR_OF_DAY) < 12) {
                        AM_PM = "AM"
                    } else {
                        AM_PM = "PM"
                    }

                    if (calendarCheckNight.get(Calendar.HOUR_OF_DAY) < 10) {
                        time = "0${calendarCheckNight.get(Calendar.HOUR_OF_DAY)}:"
                    } else {
                        time = calendarCheckNight.get(Calendar.HOUR_OF_DAY).toString() + ":"
                    }

                    if (calendarCheckNight.get(Calendar.MINUTE) < 10) {
                        time += "0${calendarCheckNight.get(Calendar.MINUTE)} $AM_PM"
                    } else {
                        time += calendarCheckNight.get(Calendar.MINUTE).toString() + " " + AM_PM
                    }

                    txt_timepicker.text = time
                    Ride.instance.time = time

                },
                {})
            return false
        }

        if (toName.isEmpty()) {
            UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                resources.getString(R.string.InvalidReceiverName), resources.getString(R.string.Ok),
                "", false, false, {}, {})
            return false
        }

        if (stringContainsNumber(toName)) {
            UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                resources.getString(R.string.InvalidReceiverName), resources.getString(R.string.Ok),
                "", false, false, {}, {})
            return false
        }

        if (!countryCodePicker_to.isValidFullNumber) {
            UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                resources.getString(R.string.InvalidReceiverMob), resources.getString(R.string.Ok),
                "", false, false, {}, {})
            return false
        }

        Ride.instance.storeName = store_name_et.text.toString().trim();

//        Ride.instance.subject = subject
        Ride.instance.shipment = shipment
        Ride.instance.fromName = fromName
        Ride.instance.fromMobile = fromNumber
        Ride.instance.date = dateText1
        Ride.instance.hijriDate = dateText2
        Ride.instance.time = timeText
        Ride.instance.toName = toName
        Ride.instance.toMobile = toNumber

        return true
    }

    fun stringContainsNumber(s: String?): Boolean {
        return Pattern.compile("[0-9]").matcher(s).find()
    }

    @SuppressLint("StaticFieldLeak")
    private inner class ListVehicleSizeBackground : AsyncTask<Void, Void, JSONObject>() {

        override fun onPreExecute() {
            super.onPreExecute()
            progressDialog = UtilityFunctions.showProgressDialog(this@ShipmentDetActivity)
        }

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()
            params["lang"] = sharedPreferences.getString(Constants.PREFS_LANG, "en")!!
            return jsonParser.makeHttpRequest(
                Constants.BASE_URL + "customer/get_vehicle_size",
                "POST",
                params
            )
        }

        override fun onPostExecute(response: JSONObject?) {

            if (progressDialog != null) {
                UtilityFunctions.dismissProgressDialog()
            }


            if (response != null) {
                try {
                    if (response.getBoolean("status")) {
                        vehicleSizeArray = response.getJSONArray("data")

                        if (vehicleSizeArray.length() > 0) {
                            vehicleSizeIdList = ArrayList()
                            vehicleSizeDataList = ArrayList()
                            vehicleImageList = ArrayList()
                            var position = 0

                            for (i in 0 until vehicleSizeArray.length()) {
                                vehicleSizeIdList.add(
                                    vehicleSizeArray.getJSONObject(i).getString("id").trim()
                                )
                                vehicleSizeDataList.add(
                                    vehicleSizeArray.getJSONObject(i).getString(
                                        "size"
                                    ).trim()
                                )

                                vehicleImageList.add(
                                    vehicleSizeArray.getJSONObject(i).getString(
                                        "image_name"
                                    ).trim()
                                )

                                if (Ride.instance.vehicleSizeId == vehicleSizeArray.getJSONObject(i)
                                        .getString(
                                            "id"
                                        ).trim()
                                ) {
                                    position = i
                                }
                            }

                            Picasso.get()
                                .load(Constants.IMG_URL + "/vehicle_size/" + vehicleImageList[0])
                                .placeholder(R.drawable.progress_view).error(R.drawable.dynate_1)
                                .into(vehicle_one_image_view)
                            vehicle_one_text_view.text =
                                vehicleSizeArray.getJSONObject(0).getString("size")

                            if (vehicleSizeArray.getJSONObject(0).getString("status") == "0") {
                                first_disable_view.visibility = View.VISIBLE
                            }

                            if (vehicleSizeArray.getJSONObject(1).getString("status") == "0") {
                                second_disable_view.visibility = View.VISIBLE
                                vehicle2ComingSoonTV.visibility = View.VISIBLE
                            }

                            if (vehicleSizeArray.getJSONObject(2).getString("status") == "0") {
                                third_disable_view.visibility = View.VISIBLE
                                vehicle3ComingSoonTV.visibility = View.VISIBLE
                            }

                            Picasso.get()
                                .load(Constants.IMG_URL + "/vehicle_size/" + vehicleImageList[1])
                                .placeholder(R.drawable.progress_view).error(R.drawable.dynate_1)
                                .into(vehicle_two_image_view)
                            vehicle_two_text_view.text =
                                vehicleSizeArray.getJSONObject(1).getString("size")

                            Picasso.get()
                                .load(Constants.IMG_URL + "/vehicle_size/" + vehicleImageList[2])
                                .placeholder(R.drawable.progress_view).error(R.drawable.dynate_1)
                                .into(vehicle_three_image_view)
                            vehicle_three_text_view.text =
                                vehicleSizeArray.getJSONObject(2).getString("size")


                            Ride.instance.vehicleSizeId = vehicleSizeIdList[0]
                            Ride.instance.vehicleSizeName = vehicleSizeDataList[0]
//                            vehicleSizeAdapter = MySpinnerAdapter(
//                                this@ShipmentDetActivity,
//                                android.R.layout.select_dialog_item,
//                                vehicleSizeDataList
//                            )
//                            spnr_veh_size.adapter = vehicleSizeAdapter
//
//                            if (!Ride.instance.vehicleSizeId.isNullOrEmpty()) {
//                                Ride.instance.vehicleSizeId =
//                                    vehicleSizeArray.getJSONObject(0).getString("id").trim()
//                                Ride.instance.vehicleSizeName =
//                                    vehicleSizeArray.getJSONObject(0).getString("size").trim()
//                            } else {
//                                spnr_veh_size.setSelection(position)
//                            }
                        }
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class GetShipmentTypeBackground : AsyncTask<Void, Void, JSONObject>() {

        override fun onPreExecute() {
            super.onPreExecute()
//            UtilityFunctions.showProgressDialog(this@ShipmentDetActivity)
        }

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()
            params["lang"] = sharedPreferences.getString(Constants.PREFS_LANG, "en")!!
            return jsonParser.makeHttpRequest(
                Constants.BASE_URL + "customer/get_shipment_type",
                "POST",
                params
            )
        }

        override fun onPostExecute(response: JSONObject?) {
//            UtilityFunctions.dismissProgressDialog()

            if (response != null) {
                try {
                    if (response.getBoolean("status")) {
                        shipmentTypeArray = response.getJSONArray("data")

                        if (shipmentTypeArray.length() > 0) {
                            shipmentTypeIdList = ArrayList()
                            shipmentTypeDataList = ArrayList()
                            var position = 0

                            shipmentTypeIdList.add("-1")
                            shipmentTypeDataList.add(getString(R.string.choose))

                            for (i in 0 until shipmentTypeArray.length()) {
                                shipmentTypeIdList.add(
                                    shipmentTypeArray.getJSONObject(i).getString(
                                        "id"
                                    ).trim()
                                )
                                shipmentTypeDataList.add(
                                    shipmentTypeArray.getJSONObject(i).getString(
                                        "type"
                                    ).trim()
                                )

                                if (Ride.instance.shipmentTypeID == shipmentTypeArray.getJSONObject(
                                        i
                                    ).getString(
                                        "id"
                                    ).trim()
                                ) {
                                    position = i
                                }
                            }
                            shipmentTypeAdapter = MySpinnerAdapter(
                                this@ShipmentDetActivity,
                                android.R.layout.select_dialog_item,
                                shipmentTypeDataList
                            )
                            spnr_shipment_type.adapter = shipmentTypeAdapter

                            if (Ride.instance.shipmentTypeID.isNullOrEmpty()) {
                                Ride.instance.shipmentTypeID =
                                    shipmentTypeArray.getJSONObject(0).getString("id").trim()
                                Ride.instance.shipmentTypeName =
                                    shipmentTypeArray.getJSONObject(0).getString("type").trim()
                            } else {
                                spnr_shipment_type.setSelection(position)
                            }
                        }
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class DisplayPrice : AsyncTask<Void, Void, JSONObject>() {

        override fun onPreExecute() {
            super.onPreExecute()
//            UtilityFunctions.showProgressDialog(this@ShipmentDetActivity)
        }

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()
            params["lang"] = sharedPreferences.getString(Constants.PREFS_LANG, "en")!!
//            params["persons"] = Ride.instance.requiredPersons
            params["loading_count"] = Ride.instance.loadingCount
            params["unloading_count"] = Ride.instance.unloadingCount
            params["start_lat"] = Ride.instance.pickUpLatitude!!
            params["start_lon"] = Ride.instance.pickUpLongitude!!
            params["end_lat"] = Ride.instance.dropOffLatitude!!
            params["end_lon"] = Ride.instance.dropOffLongitude!!
            params["is_loading_unloading_calculation"] = "1"
            params["building_level"] = Ride.instance.buildingLevel
            return jsonParser.makeHttpRequest(
                Constants.BASE_URL + "customer/calculate_price",
                "POST",
                params
            )
        }

        override fun onPostExecute(response: JSONObject?) {
//            UtilityFunctions.dismissProgressDialog()

            if (response != null) {
                try {
                    if (response.getBoolean("status")) {

                        priceTextView.visibility = View.VISIBLE
                        priceTextView.text =
                            getString(R.string.TripPrice) + ": " + response.getJSONObject("data")
                                .getString("trip_price") + " SAR" + getString(R.string.LabourPrice) + ": " + response.getJSONObject(
                                "data"
                            ).getString("labour_price") + " SAR"
                    } else {
                        UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                            response.getString("message"), resources.getString(R.string.Ok),
                            "", false, false, {}, {})
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private inner class MySpinnerAdapter internal constructor(
        context: Context,
        resource: Int,
        items: List<String>
    ) : ArrayAdapter<String>(context, resource, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent) as TextView
            if (Build.VERSION.SDK_INT < 23) {
                view.setTextAppearance(this@ShipmentDetActivity, R.style.FontSizeFourteen)
            } else {
                view.setTextAppearance(R.style.FontSizeFourteen)
            }
            view.gravity = Gravity.CENTER
            return view
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getDropDownView(position, convertView, parent) as TextView
            if (Build.VERSION.SDK_INT < 23) {
                view.setTextAppearance(this@ShipmentDetActivity, R.style.FontSizeFourteen)
            } else {
                view.setTextAppearance(R.style.FontSizeFourteen)
            }
            return view
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_CONTACT && resultCode == RESULT_OK && data != null) {
            contactPicked(data!!)
        }

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImage = data.data
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)

            val cursor = contentResolver.query(selectedImage!!, filePathColumn, null, null, null)
            cursor!!.moveToFirst()

            val columnIndex = cursor.getColumnIndex(filePathColumn[0])
            val picturePath = cursor.getString(columnIndex)
            cursor.close()

            val f = File(picturePath)
            var bm = UtilityFunctions.decodeFile(f)
            bm = UtilityFunctions.scaleDownBitmap(bm, 400, this@ShipmentDetActivity)
            val baos = ByteArrayOutputStream()

            bm!!.compress(Bitmap.CompressFormat.JPEG, 100, baos)
//            val byteImage_photo = baos.toByteArray()

            if (bm != null) {
                Ride.instance.storeInvoiceName = f.name
                Ride.instance.invoiceImage =
                    Util.getCompressed(this@ShipmentDetActivity, f.absolutePath)
                invoice_image_button.text = Ride.instance.storeInvoiceName
                invoice_image_container_view.visibility = View.VISIBLE
                invoiceImageView.setImageBitmap(bm!!)
            }


//            addToClass(bm, byteImage_photo, f)

        } else if (requestCode == TAKE_PHOTO_CODE && resultCode == Activity.RESULT_OK) {

            val baos = ByteArrayOutputStream()
            val f = File(mCurrentInvoicePhotoPath)
            var bm1 = UtilityFunctions.decodeFile(f)
            // f.delete()
            bm1 = UtilityFunctions.scaleDownBitmap(bm1, 800, this@ShipmentDetActivity)
            bm1!!.compress(Bitmap.CompressFormat.JPEG, 100, baos)
//                val byteImage_photo = baos.toByteArray()

            if (bm1 != null) {
                Ride.instance.storeInvoiceName = f.name
                Ride.instance.invoiceImage =
                    Util.getCompressed(this@ShipmentDetActivity, f.absolutePath)
                invoice_image_button.text = Ride.instance.storeInvoiceName
                invoice_image_container_view.visibility = View.VISIBLE
                invoiceImageView.setImageBitmap(bm1!!)
            }

        }
    }

    private fun contactPicked(data: Intent) {
        var cursor: Cursor? = null
        try {
            var phoneNo: String? = null
            //val name: String? = null
            val uri = data.data

            cursor = contentResolver.query(uri!!, null, null, null, null)
            cursor!!.moveToFirst()

            val phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
//            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
//            phoneNo = cursor.getString(phoneIndex).trimStart { it <= '0' }
            phoneNo = cursor.getString(phoneIndex).removePrefix("0")
            phoneNo = phoneNo.removePrefix("966")
            phoneNo = phoneNo.removePrefix("+966")

            if (clickedImg.equals("sender", ignoreCase = true)) {
//                edit_from_name.setText(nameIndex.toString())
//                countryCodePicker_from.fullNumber = phoneNo.replace("[^\\d]".toRegex(), "")
//                countryCodePicker_from.fullNumber = phoneNo
                edit_from_mobile.setText(phoneNo)
            } else {
//                edit_to_name.setText(nameIndex.toString())
//                countryCodePicker_to.fullNumber = phoneNo.replace("[^\\d]".toRegex(), "")
//                countryCodePicker_to.fullNumber = phoneNo
                edit_to_mobile.setText(phoneNo)
            }

            cursor.close()

        } catch (e: Exception) {
            e.printStackTrace()
            UtilityFunctions.showAlertOnActivity(this@ShipmentDetActivity,
                resources.getString(R.string.NoMobileNumber), resources.getString(R.string.Ok),
                "", false, false, {}, {})
        }

    }

    @SuppressLint("StaticFieldLeak")
    private inner class GetDateBackground internal constructor(
        internal var isHijri: Boolean?,
        internal var dateString: String
    ) : AsyncTask<Void, Void, JSONObject>() {

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()

            params["data"] = dateString

            var BASE_URL = Constants.DATE_URL + "GetGregorianJson"

            if (isHijri!!) {
                BASE_URL = Constants.DATE_URL + "GethijiriJson"
            }

            return jsonParser.makeHttpRequest(BASE_URL, "POST", params)
        }

        override fun onPostExecute(response: JSONObject?) {
            if (response != null) {
                try {
                    if (response.getBoolean("status")) {
                        val date = response.getString("data")
                        val curDate = simpleDateFormat1.parse(simpleDateFormat1.format(Date()))
                        var getMyDate: Date? = null

                        if (isHijri!!) {
                            try {
                                getMyDate = simpleDateFormat1.parse(dateString)
                            } catch (e: ParseException) {
                                e.printStackTrace()
                            }

                            if (getMyDate!!.after(curDate) || getMyDate == curDate) {
                                txt_datepicker1.text = dateString
                                txt_datepicker2.text = date
                                Ride.instance.date = dateString
                                Ride.instance.hijriDate = date
                            }

                        } else {
                            try {
                                getMyDate = simpleDateFormat1.parse(date)
                            } catch (e: ParseException) {
                                e.printStackTrace()
                            }

                            if (getMyDate!!.after(curDate) || getMyDate == curDate) {
                                txt_datepicker1.text = date
                                txt_datepicker2.text = dateString
                                Ride.instance.date = date
                                Ride.instance.hijriDate = dateString
                            }
                        }
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                } catch (e: ParseException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun blink() {
        val handler = Handler()
        Thread(Runnable {
            val timeToBlink = 1000 //in milissegunds
            try {
                Thread.sleep(timeToBlink.toLong())
            } catch (e: java.lang.Exception) {
            }
            handler.post(Runnable {
                if (blinkText) {
                    val txt = unpackAndInstallMsgTextView
                    if (txt.visibility == View.VISIBLE) {
                        txt.visibility = View.INVISIBLE
                    } else {
                        txt.visibility = View.VISIBLE
                    }
                    blink()
                }

            })
        }).start()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_CAMERA -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }
        }// other 'case' lines to check for other
        // permissions this app might request
    }

}
