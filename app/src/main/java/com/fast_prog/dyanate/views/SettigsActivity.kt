package com.fast_prog.dyanate.views

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.fast_prog.dyanate.R
import com.fast_prog.dyanate.utilities.Constants
import com.fast_prog.dyanate.utilities.UtilityFunctions
import kotlinx.android.synthetic.main.content_settings.*
import java.util.*

class SettigsActivity : AppCompatActivity() {

    internal lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        toolbar.setNavigationOnClickListener { finish() }

        customTitle(resources.getString(R.string.Settings))

        btn_change_lang.setOnClickListener {
            UtilityFunctions.showAlertOnActivity(this@SettigsActivity,
                resources.getText(R.string.ChangeLanguageRestart).toString(),
                resources.getString(R.string.Yes).toString(),
                resources.getString(R.string.No).toString(),
                true,
                false,
                {
                    UtilityFunctions.showProgressDialog(this@SettigsActivity)

                    Handler().postDelayed({
                        UtilityFunctions.dismissProgressDialog()

                        var lang = "ar"

                        if (sharedPreferences.getString(Constants.PREFS_LANG, "").equals(
                                "ar",
                                true
                            )
                        ) {
                            lang = "en"
                        }

                        val locale = Locale(lang)
                        Locale.setDefault(locale)
                        val confg = Configuration()
                        confg.locale = locale
                        baseContext.resources.updateConfiguration(
                            confg,
                            baseContext.resources.displayMetrics
                        )

                        val editor = sharedPreferences.edit()
                        editor.putString(Constants.PREFS_LANG, lang)
                        editor.commit()

                        startActivity(
                            Intent(
                                this@SettigsActivity,
                                SenderLocationActivity::class.java
                            )
                        )
                        ActivityCompat.finishAffinity(this@SettigsActivity)
                        finish()
                    }, 2000)
                },
                {})
        }
    }
}
