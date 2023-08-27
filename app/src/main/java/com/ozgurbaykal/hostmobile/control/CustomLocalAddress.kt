package com.ozgurbaykal.hostmobile.control

import android.content.Context
import android.net.ConnectivityManager

object CustomLocalAddress {

    private fun Context.getConnectivityManager() = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun getIpAddress(context: Context): String? {
        val linkProperties = context.getConnectivityManager().getLinkProperties(context.getConnectivityManager().activeNetwork)


        if (linkProperties == null || linkProperties.linkAddresses.size < 2) {
            return null
        }

        return linkProperties.linkAddresses[1].address.hostAddress
    }
}