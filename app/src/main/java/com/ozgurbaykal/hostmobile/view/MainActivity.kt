package com.ozgurbaykal.hostmobile.view

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.ozgurbaykal.hostmobile.R
import com.ozgurbaykal.hostmobile.control.SharedPreferenceManager
import com.ozgurbaykal.hostmobile.databinding.ActivityMainBinding
import androidx.fragment.app.add
import androidx.fragment.app.commit
import com.ozgurbaykal.hostmobile.control.CustomLocalAddress
import com.ozgurbaykal.hostmobile.service.CustomHttpService
import com.ozgurbaykal.hostmobile.service.DefaultHttpService

class MainActivity : AppCompatActivity() {
    private val TAG = "_MainActivity"

    private lateinit var binding: ActivityMainBinding
    private lateinit var openServerButton: Button

    private lateinit var bottomNav : BottomNavigationView
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        openServerButton = binding.openServerButton

        openServerButton.setOnClickListener {
            Log.i(TAG, "openServerButton clicked")

            val intent = Intent(this, CustomHttpService::class.java)
            ContextCompat.startForegroundService(this@MainActivity, intent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel("CHANNEL_SERVICE", "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_LOW)
                val manager = getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(channel)
            }
        }

        SharedPreferenceManager.init(this@MainActivity)

        //IF APP OPEN FIRST TIME, SHOW TUTORIAL
        if(SharedPreferenceManager.readBoolean("isNewUser", true) == true){
            val intent = Intent (this@MainActivity, TutorialActivity::class.java)
            startActivity(intent)
        }

        bottomNav = binding.bottomNavigation

        bottomNav.setOnItemSelectedListener  {item ->
            when (item.itemId) {
                R.id.page_1_bottom_nav_button -> {
                    Log.i(TAG, "Bottom Nav ClickEvent -> PAGE 1")
                    changeFragment(CustomServerFragment(), R.id.main_fragment_view)
                    return@setOnItemSelectedListener true
                }
                R.id.page_2_bottom_nav_button -> {
                    Log.i(TAG, "Bottom Nav ClickEvent -> PAGE 2")
                    changeFragment(DefaultServerFragment(), R.id.main_fragment_view)
                    return@setOnItemSelectedListener true
                }
                R.id.page_3_bottom_nav_button -> {
                    Log.i(TAG, "Bottom Nav ClickEvent -> PAGE 3")
                    return@setOnItemSelectedListener true
                }
                R.id.page_4_bottom_nav_button -> {
                    Log.i(TAG, "Bottom Nav ClickEvent -> PAGE 4")
                    return@setOnItemSelectedListener true
                }
            }
            false
        }
            if (savedInstanceState == null) {
                supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    add<CustomServerFragment>(R.id.main_fragment_view)
                }
            }

            askNotificationPerm();

            Log.i(TAG, "IP ADRESS DEVICE -> " + CustomLocalAddress.getIpAddress(this@MainActivity))
        }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun askNotificationPerm(){

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Notification perm already been granted")
        } else {
            // Request notification permission
            val requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    Log.i(TAG, "Notification perm granted")
                } else {
                    Log.i(TAG, "Notification perm denied")
                }
            }
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

    }


    private fun changeFragment(fragment: Fragment, frameId: Int) {
        supportFragmentManager.beginTransaction().apply {
            replace(frameId, fragment)
            commit()
        }
    }

}