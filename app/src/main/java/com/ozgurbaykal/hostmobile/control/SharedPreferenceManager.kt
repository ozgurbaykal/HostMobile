package com.ozgurbaykal.hostmobile.control

import android.content.Context
import android.content.SharedPreferences

object SharedPreferenceManager {
    private const val PREF_NAME = "host_mobile_app_prefs"
    private var sharedPreferences: SharedPreferences? = null

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun writeString(key: String, value: String) {
        val editor = sharedPreferences?.edit()
        editor?.putString(key, value)
        editor?.apply()
    }

    fun readString(key: String, default: String): String? {
        return sharedPreferences?.getString(key, default)
    }

    fun writeBoolean(key: String, value: Boolean) {
        val editor = sharedPreferences?.edit()
        editor?.putBoolean(key, value)
        editor?.apply()
    }

    fun readBoolean(key: String, default: Boolean): Boolean? {
        return sharedPreferences?.getBoolean(key, default)
    }

    fun remove(key: String) {
        val editor = sharedPreferences?.edit()
        editor?.remove(key)
        editor?.apply()
    }
}