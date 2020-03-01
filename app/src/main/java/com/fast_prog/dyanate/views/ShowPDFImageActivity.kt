package com.fast_prog.dyanate.views

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.fast_prog.dyanate.R
import com.fast_prog.dyanate.utilities.Constants
import com.github.chrisbanes.photoview.OnPhotoTapListener
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_show_pdfimage.*

class ShowPDFImageActivity : AppCompatActivity(), OnPhotoTapListener,
    SeekBar.OnSeekBarChangeListener {

    companion object {
        internal var imgURL: String? = null
        internal var docType: String? = null
        //internal var isPdf: Boolean = false
    }

    internal lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_pdfimage)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener { finish() }

        customTitle(docType?.toUpperCase().toString())

        //if (isPdf) {
        //    pdf_view.visibility = View.VISIBLE
        //    photo_view.visibility = View.GONE
        //    seek_bar.visibility = View.GONE
        //} else {
        //    pdf_view.visibility = View.GONE

        photo_view.visibility = View.VISIBLE
        seek_bar.visibility = View.VISIBLE

        photo_view.minimumScale = 1.0f
        photo_view.maximumScale = 5.0f
        photo_view.setOnPhotoTapListener(this)
        seek_bar.setOnSeekBarChangeListener(this)

        Picasso.get().load(imgURL).placeholder(R.drawable.progress_view).error(R.drawable.dynate_1)
            .into(photo_view)

        //}
    }

    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
        if (p1.toFloat() / 20 > 1)
            photo_view.scale = p1.toFloat() / 20
        else {
            photo_view.scale = 1.0f
        }
    }

    override fun onStartTrackingTouch(p0: SeekBar?) {
    }

    override fun onStopTrackingTouch(p0: SeekBar?) {
    }

    override fun onPhotoTap(view: ImageView?, x: Float, y: Float) {
    }
}
