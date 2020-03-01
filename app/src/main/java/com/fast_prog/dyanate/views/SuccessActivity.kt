package com.fast_prog.dyanate.views

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import com.fast_prog.dyanate.R
import kotlinx.android.synthetic.main.activity_success.*

class SuccessActivity : AppCompatActivity() {

    internal var typeface: Typeface? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_success)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        typeface = ResourcesCompat.getFont(this, R.font.droid_arabic_kufi)

        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        customTitle(resources.getString(R.string.Completed))

        button_ok.setOnClickListener {
            startActivity(Intent(this@SuccessActivity, SenderLocationActivity::class.java))
            ActivityCompat.finishAffinity(this@SuccessActivity)
            finish()
        }
    }

    override fun onBackPressed() {}
}
