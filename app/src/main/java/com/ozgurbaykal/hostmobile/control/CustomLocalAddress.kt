package com.ozgurbaykal.hostmobile.control

import android.content.Context
import android.net.ConnectivityManager

object CustomLocalAddress {

    private fun Context.getConnectivityManager() = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun getIpAddress(context: Context) = with(context.getConnectivityManager()) {
        getLinkProperties(activeNetwork)!!.linkAddresses[1].address.hostAddress!!
    }
}