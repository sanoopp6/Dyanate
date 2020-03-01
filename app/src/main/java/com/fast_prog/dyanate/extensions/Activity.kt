package com.fast_prog.dyanate.views

/**
 * Created by SANOOP on 04/09/17.
 */

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.fast_prog.dyanate.R

/**
 * Hides the soft keyboard
 */
fun Activity.hideKeyboard(): Boolean {
    val view = currentFocus
    view?.let {
        val inputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return inputMethodManager.hideSoftInputFromWindow(
            view.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }
    return false
}

fun AppCompatActivity.hideKeyboard(): Boolean {
    val view = currentFocus
    view?.let {
        val inputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return inputMethodManager.hideSoftInputFromWindow(
            view.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }
    return false
}

fun Activity.customTitle(title: String) {
    actionBar?.setDisplayShowTitleEnabled(false)
    actionBar?.setDisplayShowCustomEnabled(true)

    val titleTextView = TextView(this)
    val titleLayoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.MATCH_PARENT
    )
    titleTextView.layoutParams = titleLayoutParams
    titleTextView.text = title
    titleTextView.textSize = 16f
    titleTextView.isAllCaps = true
    titleTextView.typeface = ResourcesCompat.getFont(applicationContext, R.font.droid_arabic_kufi)
    titleTextView.setTextColor(Color.WHITE)
    titleTextView.gravity = Gravity.CENTER_VERTICAL
    actionBar?.customView = titleTextView
}

fun AppCompatActivity.customTitle(title: String) {
    supportActionBar?.setDisplayShowTitleEnabled(false)
    supportActionBar?.setDisplayShowCustomEnabled(true)

    val titleTextView = TextView(this)
    val titleLayoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.MATCH_PARENT
    )
    titleTextView.layoutParams = titleLayoutParams
    titleTextView.text = title
    titleTextView.textSize = 16f
    titleTextView.isAllCaps = true
    titleTextView.typeface = ResourcesCompat.getFont(applicationContext, R.font.droid_arabic_kufi)
    titleTextView.setTextColor(Color.WHITE)
    titleTextView.gravity = Gravity.CENTER_VERTICAL
    supportActionBar?.customView = titleTextView
}