package com.fast_prog.dyanate.views

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.fast_prog.dyanate.R
import com.fast_prog.dyanate.models.PlaceItem
import com.fast_prog.dyanate.utilities.Constants
import com.snappydb.DBFactory
import com.snappydb.SnappydbException
import kotlinx.android.synthetic.main.content_pick_location.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.*

class PickLocationActivity : AppCompatActivity() {

    private var m_parts: ArrayList<PlaceItem>? = ArrayList()

    private var m_adapter: PlaceListCustomAdapter? = null

    private var cityLatLngArray: ArrayList<String>? = null

    private var sharedPreferences: SharedPreferences? = null

    private val PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place"

    private val PLACES_NEARBY = "https://maps.googleapis.com/maps/api/place/findplacefromtext"

    private val TYPE_NEARBY = "/nearbysearch"

    private val OUT_JSON = "/json"

    private var bookMarked: Boolean? = true

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pick_location)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        toolbar.setNavigationOnClickListener { finish() }

        customTitle(resources.getString(R.string.PickLocation))

        try {
            val snappyDB = DBFactory.open(this@PickLocationActivity, Constants.DYNA_DB)

            val keys = snappyDB.findKeys(Constants.DYNA_DB_KEY)
            var i = 0

            while (i < keys.size) {
                m_parts?.add(snappyDB.get(keys[i], PlaceItem::class.java))
                i++
            }
            snappyDB.close()

        } catch (e: SnappydbException) {
            e.printStackTrace()
        }

        val cityArray = ArrayList<String>()
        cityLatLngArray = ArrayList()
        for (i in 0 until resources.getStringArray(R.array.city_array).size) {
            cityArray.add(i, resources.getStringArray(R.array.city_array)[i])
        }

        for (i in 0 until resources.getStringArray(R.array.city_location_array).size) {
            cityLatLngArray!!.add(i, resources.getStringArray(R.array.city_location_array)[i])
        }

        val customSpinnerAdapter = CustomSpinnerAdapter(
            this@PickLocationActivity,
            R.layout.city_spinner_item,
            cityArray,
            resources
        )
        city_spinner.adapter = customSpinnerAdapter

        m_adapter = PlaceListCustomAdapter(this@PickLocationActivity, R.layout.place_item, m_parts)

        listView_results.adapter = m_adapter

        listView_results.isTextFilterEnabled = true

        edEnterLocation.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                bookMarked = false

                if (s != null) {
                    if (s.isNotEmpty()) {
                        GetPlaceNamesBackground(s.toString()).execute()
                    }
                }
            }
        })

        val Position = customSpinnerAdapter.getPosition(
            sharedPreferences!!.getString(
                Constants.PREFS_SEARCH_LOCATION_AREA,
                cityLatLngArray!![0]
            )
        )
        city_spinner.setSelection(Position)

        city_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
                val editor = sharedPreferences!!.edit()
                editor.putString(Constants.PREFS_SEARCH_LOCATION_AREA, cityLatLngArray!![i])
                editor.commit()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>) {}
        }

        listView_results.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                this@PickLocationActivity.supportFragmentManager.popBackStack()
                val selectedPlaceItem = m_parts!![i]

                val intent = Intent()
                intent.putExtra("PlaceItem", selectedPlaceItem)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class GetPlaceNamesBackground internal constructor(private val s: String) :
        AsyncTask<Void, Void, Void>() {

        override fun doInBackground(vararg voids: Void): Void? {
            m_parts = autocomplete(s)
            return null
        }

        override fun onPreExecute() {
            super.onPreExecute()
            if (m_parts != null) {
                m_parts!!.clear()
            }
        }

        override fun onPostExecute(jsonObject: Void?) {
            super.onPostExecute(jsonObject)
            if (m_parts != null) {
                m_adapter =
                    PlaceListCustomAdapter(this@PickLocationActivity, R.layout.place_item, m_parts)
                listView_results.adapter = m_adapter
            }
        }
    }

    fun autocomplete(input: String): ArrayList<PlaceItem>? {

        var resultList: ArrayList<PlaceItem>? = null
        var conn: HttpURLConnection? = null
        val jsonResults = StringBuilder()
        try {
//            val sb = PLACES_API_BASE + TYPE_NEARBY + OUT_JSON + "?key=" + Constants.GOOGLE_API_KEY +
//                    "&location=" + sharedPreferences!!.getString(
//                Constants.PREFS_SEARCH_LOCATION_AREA,
//                cityLatLngArray!![0]
//            ) +
//                    "&name=" + input.trim().replace(" ", "+") +
//                    "&language=" + sharedPreferences!!.getString(Constants.PREFS_LANG, "ar") +
//                    "&radius=" + 50000

            val sb = PLACES_NEARBY + OUT_JSON + "?key=" + Constants.GOOGLE_API_KEY +
                    "&input=" + input.trim().replace(" ", "+") +
                    "&language=" + sharedPreferences!!.getString(Constants.PREFS_LANG, "ar") + "&inputtype=textquery&fields=formatted_address,geometry,name"


            Log.d("place_", sb)


            val url = URL(sb)
            conn = url.openConnection() as HttpURLConnection
            val `in` = InputStreamReader(conn.inputStream)

            // Load the results into a StringBuilder
            var read: Int = -1
            val buff = CharArray(1024)
            while ({ read = `in`.read(buff); read }() != -1) {
                jsonResults.append(buff, 0, read)
            }
        } catch (e: MalformedURLException) {
            return null
        } catch (e: IOException) {
            return null
        } finally {
            if (conn != null) {
                conn.disconnect()
            }
        }

        try {

            Log.d("place_", jsonResults.toString())
            // Create a JSON object hierarchy from the results

            val jsonObj = JSONObject(jsonResults.toString())

            val resultsJsonArray = jsonObj.getJSONArray("candidates")

            if (resultsJsonArray != null) {
                resultList = ArrayList()

                for (i in 0 until resultsJsonArray.length()) {
                    val placeItem = PlaceItem()
                    placeItem.plName = resultsJsonArray.getJSONObject(i).getString("name")

                    if (resultsJsonArray.getJSONObject(i).getString("formatted_address") != null)
                        placeItem.pVicinity =
                            resultsJsonArray.getJSONObject(i).getString("formatted_address")

                    placeItem.pLatitude =
                        resultsJsonArray.getJSONObject(i).getJSONObject("geometry")
                            .getJSONObject("location").getString("lat")
                    placeItem.pLongitude =
                        resultsJsonArray.getJSONObject(i).getJSONObject("geometry")
                            .getJSONObject("location").getString("lng")
                    resultList.add(placeItem)
                }
            }


//            val jsonObj = JSONObject(jsonResults.toString())
//
//            val resultsJsonArray = jsonObj.getJSONArray("results")
//
//            if (resultsJsonArray != null) {
//                resultList = ArrayList()
//
//                for (i in 0 until resultsJsonArray.length()) {
//                    val placeItem = PlaceItem()
//                    placeItem.plName = resultsJsonArray.getJSONObject(i).getString("name")
//
//                    if (resultsJsonArray.getJSONObject(i).getString("vicinity") != null)
//                        placeItem.pVicinity =
//                            resultsJsonArray.getJSONObject(i).getString("vicinity")
//
//                    placeItem.pLatitude =
//                        resultsJsonArray.getJSONObject(i).getJSONObject("geometry")
//                            .getJSONObject("location").getString("lat")
//                    placeItem.pLongitude =
//                        resultsJsonArray.getJSONObject(i).getJSONObject("geometry")
//                            .getJSONObject("location").getString("lng")
//                    resultList.add(placeItem)
//                }
//            }
        } catch (ignored: JSONException) {
        }

        return resultList
    }

    private inner class PlaceListCustomAdapter internal constructor(
        context: Context,
        textViewResourceId: Int,
        private val objects: ArrayList<PlaceItem>?
    ) : ArrayAdapter<PlaceItem>(context, textViewResourceId, objects!!) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var v = convertView

            if (v == null) {
                val inflater =
                    context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                v = inflater.inflate(R.layout.place_item, null)
            }

            val placeItem = objects?.get(position)

            if (placeItem != null) {
                val placeNameTextView = v?.findViewById<View>(R.id.place_name_text_view) as TextView
                val placeVicinityTextView =
                    v.findViewById<View>(R.id.place_vicinity_text_view) as TextView
                val bookmarkLocationImageView =
                    v.findViewById<View>(R.id.bookmark_location_image_view) as ImageView

                placeNameTextView.text = placeItem.plName

                if (placeItem.pVicinity != null) {
                    placeVicinityTextView.text = placeItem.pVicinity
                }

                if (bookMarked!!) {
                    bookmarkLocationImageView.setColorFilter(Color.parseColor(Constants.FILTER_COLOR))
                    bookmarkLocationImageView.setOnClickListener {
                        m_parts?.removeAt(position)

                        try {
                            val snappyDB =
                                DBFactory.open(this@PickLocationActivity, Constants.DYNA_DB)
                            val keys = snappyDB.findKeys(Constants.DYNA_DB_KEY)
                            snappyDB.del(keys[position])
                            snappyDB.close()

                            m_adapter?.notifyDataSetChanged()

                        } catch (e: SnappydbException) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    bookmarkLocationImageView.visibility = View.GONE
                }
            }
            return v!!
        }
    }

    private inner class CustomSpinnerAdapter internal constructor(
        context: Context,
        textViewResourceId: Int,
        objects: ArrayList<String>,
        var res: Resources
    ) : ArrayAdapter<String>(context, textViewResourceId, objects) {

        private val data: ArrayList<String> = objects
        internal var inflater: LayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
            getCustomView(position, convertView, parent)

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
            getCustomView(position, convertView, parent)

        internal fun getCustomView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = inflater.inflate(R.layout.city_spinner_item, parent, false)
            val label = row.findViewById<View>(R.id.city_name_text_view) as TextView
            label.text = data[position]

            return row
        }
    }

}
