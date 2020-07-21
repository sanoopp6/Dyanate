package com.fast_prog.dyanate.views

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.UserStateDetails
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferService
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.fast_prog.dyanate.R
import com.fast_prog.dyanate.utilities.ConnectionDetector
import com.fast_prog.dyanate.utilities.Constants
import com.fast_prog.dyanate.utilities.JsonParser
import com.fast_prog.dyanate.utilities.UtilityFunctions
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_change_nm_ph.*
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ChangeNmPhActivity : AppCompatActivity() {

    internal lateinit var sharedPreferences: SharedPreferences

    internal lateinit var editTextName: String
    internal var profBase64: String = ""
    internal var profBm: Bitmap? = null
    internal var curName: String = ""

    private val RESULT_LOAD_IMAGE = 101
    private val TAKE_PHOTO_CODE = 102

    private var mCurrentPhotoPath: String? = null

    private val MY_PERMISSIONS_REQUEST_CAMERA = 99

    var photoFile: File? = null
    var fileName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_nm_ph)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        //start aws transfer service
        applicationContext.startService(Intent(applicationContext, TransferService::class.java))


        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //supportActionBar?.setHomeAsUpIndicator(ContextCompat.getDrawable(applicationContext, R.drawable.home_up_icon))

        toolbar.setNavigationOnClickListener { finish() }

        customTitle(getString(R.string.change_name_and_photo))

        button_update.setOnClickListener {
            editTextName = editText_name.text.toString().trim()

            if (editTextName.isEmpty() && (profBase64.isEmpty() || profBm == null)) {
                UtilityFunctions.showAlertOnActivity(this@ChangeNmPhActivity,
                    getString(R.string.invalid_photo_or_name),
                    resources.getString(R.string.Ok).toString(),
                    "",
                    false,
                    false,
                    {},
                    {})

            } else {
                if (ConnectionDetector.isConnected(applicationContext)) {

//                    SetProfPicAndProfNameBackground().execute()
//

                    if (profBase64.isEmpty() || profBm == null) {
                        SetProfPicAndProfNameBackground().execute()
                    } else {
                        AWSMobileClient.getInstance().initialize(
                            applicationContext,
                            object : com.amazonaws.mobile.client.Callback<UserStateDetails> {
                                override fun onResult(result: UserStateDetails?) {

                                    runOnUiThread {
                                        UtilityFunctions.showProgressDialog(this@ChangeNmPhActivity)

                                    }
                                    uploadPhoto()
                                }

                                override fun onError(e: java.lang.Exception?) {
                                    e?.printStackTrace()
                                    ConnectionDetector.errorSnackbar(coordinator_layout)
                                }

                            })
                    }


                } else {
                    ConnectionDetector.errorSnackbar(coordinator_layout)
                }
            }
        }

        circleImageView.setOnClickListener {
            val builder = AlertDialog.Builder(this@ChangeNmPhActivity)
            val inflater = this@ChangeNmPhActivity.getLayoutInflater()
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
                        this@ChangeNmPhActivity,
                        Manifest.permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this@ChangeNmPhActivity,
                        arrayOf(Manifest.permission.CAMERA),
                        MY_PERMISSIONS_REQUEST_CAMERA
                    )

                } else {
//                    UtilityFunctions.showAlertOnActivity(this@ChangeNmPhActivity,
//                        resources.getString(R.string.ShootClearly).toString(),
//                        resources.getString(R.string.Ok).toString(),
//                        "",
//                        false,
//                        false,
//                        {
                    val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    try {
                        val photoFile = UtilityFunctions.createImageFile()
                        mCurrentPhotoPath = photoFile.absolutePath
                        val uri = FileProvider.getUriForFile(
                            this@ChangeNmPhActivity,
                            applicationContext.packageName + ".provider",
                            photoFile
                        )
                        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                        startActivityForResult(takePhotoIntent, TAKE_PHOTO_CODE)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
//                        },
//                        {})
                }
            }

            linearLayoutChooseFromGallery.setOnClickListener {
                alertDialog.dismiss()

                val i = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(i, RESULT_LOAD_IMAGE)
            }

            linearLayoutCancel.setOnClickListener {
                alertDialog.dismiss()
            }

            alertDialog.show()
        }

        if (ConnectionDetector.isConnected(applicationContext)) {
            GetUserDetailBackground().execute()
        } else {
            ConnectionDetector.errorSnackbar(coordinator_layout)
        }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == Activity.RESULT_OK && null != data) {
            val selectedImage = data.data
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)

            val cursor = contentResolver.query(selectedImage!!, filePathColumn, null, null, null)
            cursor!!.moveToFirst()

            val columnIndex = cursor.getColumnIndex(filePathColumn[0])
            val picturePath = cursor.getString(columnIndex)
            cursor.close()

            val baos = ByteArrayOutputStream()

            val f = File(picturePath)

            photoFile = f

            var bm = UtilityFunctions.decodeFile(f)
            bm = UtilityFunctions.scaleDownBitmap(bm, 100, this@ChangeNmPhActivity)
            bm!!.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val byteImage_photo = baos.toByteArray()

            circleImageView.setImageBitmap(bm)
            profBm = bm
            profBase64 = Base64.encodeToString(byteImage_photo, Base64.DEFAULT)

        } else if (requestCode == TAKE_PHOTO_CODE && resultCode == Activity.RESULT_OK) {
            val f = File(mCurrentPhotoPath)

            photoFile = f

            var bm1 = UtilityFunctions.decodeFile(f)
//            f.delete()

            val baos = ByteArrayOutputStream()

            bm1 = UtilityFunctions.scaleDownBitmap(bm1, 100, this@ChangeNmPhActivity)
            bm1!!.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val byteImage_photo = baos.toByteArray()

            circleImageView.setImageBitmap(bm1)
            profBm = bm1
            profBase64 = Base64.encodeToString(byteImage_photo, Base64.DEFAULT)
        }
    }

    private fun uploadPhoto() {
        if (photoFile == null) {
            return
        }

        val dateFormatter = SimpleDateFormat("dd_MM_yyyy_HH_mm_ss", Locale.US)
        val now = Date()
        fileName =
            sharedPreferences.getString(Constants.PREFS_USER_ID, "") + "_" + dateFormatter.format(
                now
            ) + photoFile!!.absolutePath.substring(photoFile!!.absolutePath.lastIndexOf("."))

//        var credentialsProvider = CognitoCachingCredentialsProvider(this@ChangeNmPhActivity,
//            "eu-central-1:e91ba098-84fc-4fe6-b763-dc8a2eaf0773", Regions.EU_CENTRAL_1)
//        var s3Client = AmazonS3Client(
//            AWSMobileClient.getInstance(),
//            Region.getRegion(Regions.EU_CENTRAL_1)
//        )

        val transferUtility = TransferUtility.builder()
            .context(applicationContext)
            .awsConfiguration(AWSMobileClient.getInstance().configuration)
            .s3Client(
                AmazonS3Client(
                    AWSMobileClient.getInstance(),
                    Region.getRegion(Regions.EU_CENTRAL_1)
                )
            ).defaultBucket("arn:aws:s3:::dyanate")
            .build()
        val uploadObserver = transferUtility.upload("profile_pic/customer/$fileName", photoFile)
        uploadObserver.setTransferListener(object : TransferListener {
            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {

            }

            override fun onStateChanged(id: Int, state: TransferState?) {

                if (TransferState.COMPLETED == state) {

                    runOnUiThread {
                        UtilityFunctions.dismissProgressDialog()
                        SetProfPicAndProfNameBackground().execute()
                    }
                }
            }

            override fun onError(id: Int, ex: java.lang.Exception?) {

                ex?.printStackTrace()
                UtilityFunctions.showAlertOnActivity(this@ChangeNmPhActivity,
                    getString(R.string.image_upload_failed),
                    resources.getString(R.string.Ok).toString(),
                    "",
                    false,
                    false,
                    {},
                    {})
            }

        })
    }

    @SuppressLint("StaticFieldLeak")
    private inner class SetProfPicAndProfNameBackground : AsyncTask<Void, Void, JSONObject>() {

        override fun onPreExecute() {
            super.onPreExecute()
            UtilityFunctions.showProgressDialog(this@ChangeNmPhActivity)
        }

        override fun doInBackground(vararg param: Void): JSONObject? {
            val jsonParser = JsonParser()
            val params = HashMap<String, String>()

            params["lang"] = sharedPreferences.getString(Constants.PREFS_LANG, "en")!!
            params["user_id"] = sharedPreferences.getString(Constants.PREFS_USER_ID, "")!!
            params["token"] = sharedPreferences.getString(Constants.PREFS_USER_TOKEN, "")!!

            if (editTextName.isEmpty()) {
                params["ProfName"] = curName
            } else {
                params["ProfName"] = editTextName
            }

            params["fileName"] = fileName
            return jsonParser.makeHttpRequest(
                Constants.BASE_URL + "customer/set_profile_pic_and_name",
                "POST",
                params
            )
        }

        override fun onPostExecute(response: JSONObject?) {
            UtilityFunctions.dismissProgressDialog()

            if (response != null) {
                try {

                    if (response.getBoolean("status")) {
                        UtilityFunctions.showAlertOnActivity(this@ChangeNmPhActivity,
                            getString(R.string.photo_and_name_updated_successfully),
                            resources.getString(R.string.Ok).toString(),
                            "",
                            false,
                            false,
                            { finish() },
                            {})

                    } else {
                        UtilityFunctions.showAlertOnActivity(this@ChangeNmPhActivity,
                            response.getString("message"),
                            resources.getString(R.string.Ok).toString(),
                            "",
                            false,
                            false,
                            {},
                            {})
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

    private inner class GetUserDetailBackground : AsyncTask<Void, Void, JSONObject?>() {

        override fun onPreExecute() {
            super.onPreExecute()
            UtilityFunctions.showProgressDialog(this@ChangeNmPhActivity)
        }

        override fun doInBackground(vararg p0: Void?): JSONObject? {

            val jsonParser = JsonParser()
            val params = HashMap<String, String>()
            params["lang"] = sharedPreferences.getString(Constants.PREFS_LANG, "en")!!
            params["user_id"] = sharedPreferences.getString(Constants.PREFS_USER_ID, "")!!

            return jsonParser.makeHttpRequest(
                Constants.BASE_URL + "customer/get_user_detail",
                "POST",
                params
            )
        }

        override fun onPostExecute(result: JSONObject?) {
            UtilityFunctions.dismissProgressDialog()
            if (result != null) {
                if (result.getBoolean("status")) {

                    val editor = sharedPreferences!!.edit()
                    editor.putString(
                        Constants.PREFS_USER_MOBILE_WITHOUT_COUNTRY,
                        result.getJSONObject("data").getJSONObject("user_info").getString("mobile_number")
                    )
                    editor.putString(
                        Constants.PREFS_COUNTRY_CODE,
                        result.getJSONObject("data").getJSONObject("user_info").getString("country_code")
                    )
                    editor.putString(
                        Constants.PREFS_USER_ID,
                        result.getJSONObject("data").getJSONObject("user_info").getString("user_id").trim()
                    )
                    editor.putString(
                        Constants.PREFS_USER_TOKEN,
                        result.getJSONObject("data").getJSONObject("user_info").getString("token").trim()
                    )
                    editor.putString(
                        Constants.PREFS_USER_FULL_MOBILE,
                        result.getJSONObject("data").getJSONObject("user_info").getString("country_code") + result.getJSONObject(
                            "data"
                        ).getJSONObject("user_info").getString("mobile_number").trim()
                    )

                    editor.putString(
                        Constants.PREFS_USER_FULL_NAME,
                        result.getJSONObject("data").getJSONObject("user_info").getString("full_name").trim()
                    )

                    editor.putString(
                        Constants.PREFS_USER_PIC,
                        result.getJSONObject("data").getJSONObject("user_info").getString("profile_pic").trim()
                    )

                    editor.putBoolean(Constants.PREFS_IS_LOGIN, true)

                    curName = result.getJSONObject("data").getJSONObject("user_info")
                        .getString("full_name")
                    val profPic =
                        Constants.IMG_URL + "profile_pic/" + result.getJSONObject("data").getJSONObject(
                            "user_info"
                        ).getString("profile_pic")

                    editText_name.setText(curName)
                    Picasso.get()
                        .load(
                            Constants.IMG_URL + "/profile_pic/customer/" + sharedPreferences.getString(
                                Constants.PREFS_USER_PIC,
                                ""
                            )
                        )
                        .into(circleImageView)


                } else {
                    UtilityFunctions.showAlertOnActivity(this@ChangeNmPhActivity,
                        result.getString("message"),
                        resources.getString(R.string.Ok).toString(),
                        "",
                        false,
                        true,
                        {},
                        {})
                }
            }
        }
    }


}
