package com.beinny.android.photorecord.ui.common

import android.content.Context
import android.content.SharedPreferences
import com.beinny.android.photorecord.R

class PreferenceUtil(context: Context) {
    private val pref: SharedPreferences = context.getSharedPreferences(context.getString(R.string.preferences_file_key),Context.MODE_PRIVATE)

    fun getInt(key: String, defValue: Int):Int {
        return pref.getInt(key, defValue)
    }

    fun setInt(key: String, int: Int) {
        pref.edit().putInt(key,int).apply()
    }
}