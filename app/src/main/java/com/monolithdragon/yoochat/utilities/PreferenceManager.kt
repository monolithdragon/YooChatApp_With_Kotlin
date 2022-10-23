package com.monolithdragon.yoochat.utilities

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private var sharedPreferences: SharedPreferences =
        context.getSharedPreferences(Constants.KEY_PREFERENCE_NAME, Context.MODE_PRIVATE)

    fun getString(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    fun putString(key: String, value: String) {
        with(sharedPreferences.edit()) {
            putString(key, value)
            apply()
        }
    }

}