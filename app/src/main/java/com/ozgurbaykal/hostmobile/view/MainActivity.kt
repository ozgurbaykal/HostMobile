package com.ozgurbaykal.hostmobile.view

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.room.Room
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.ozgurbaykal.hostmobile.R
import com.ozgurbaykal.hostmobile.control.CustomLocalAddress
import com.ozgurbaykal.hostmobile.control.CustomServerController
import com.ozgurbaykal.hostmobile.control.SharedPreferenceManager
import com.ozgurbaykal.hostmobile.databinding.ActivityMainBinding
import com.ozgurbaykal.hostmobile.model.AppDatabase
import com.ozgurbaykal.hostmobile.model.CustomServerFolders
import com.ozgurbaykal.hostmobile.service.CustomHttpService
import com.ozgurbaykal.hostmobile.service.DefaultHttpService
import com.ozgurbaykal.hostmobile.service.NetworkUtils
import com.ozgurbaykal.hostmobile.service.ServiceUtils
import com.ozgurbaykal.hostmobile.view.customdialog.CustomDialogManager
import com.ozgurbaykal.hostmobile.view.customdialog.CustomDialogTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException


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

            SharedPreferenceManager.init(this@MainActivity)

            //IF APP OPEN FIRST TIME, SHOW TUTORIAL
            if(SharedPreferenceManager.readBoolean("isNewUser", true) == true){
                val intent = Intent (this@MainActivity, TutorialActivity::class.java)
                startActivity(intent)
            }

            //THIS METOD REMOVED FOR NOW
            //createCustomServerFilesDirectory("${Environment.getExternalStorageDirectory()}/CustomServerFilesHostMobile")

        openServerButton = binding.openServerButton

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel("CHANNEL_SERVICE", "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_LOW)
                val manager = getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(channel)
            }

            val isCustomServerRunning = ServiceUtils.isServiceRunning(this, CustomHttpService::class.java)

            Log.i(TAG, " isCustomServerActive: $isCustomServerRunning")

            if(isCustomServerRunning){
                openServerButton.tag = "OPEN"
                openServerButton.text = "Server(s) Running  -  STOP"
                val colorStateList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.custom_blue))
                openServerButton.backgroundTintList = colorStateList


            }else{
                SharedPreferenceManager.writeInteger("customServerPort", -1)
            }

        openServerButton.setOnClickListener {

            var defaultHttpIntent = Intent(this, DefaultHttpService::class.java)
            val customHttpIntent = Intent(this, CustomHttpService::class.java)

            if(openServerButton.tag.equals("CLOSE")){

                Log.i(TAG, "openServerButton clicked customServerPort: " + CustomServerController.customServerPort)

                if(CustomServerController.customServerPort == 0){
                    val customDialogManager = CustomDialogManager(this@MainActivity, CustomDialogTypes.SIMPLE_DIALOG, "Empty Fields","Custom Server Port field is empty. Please enter the port value and try again.", R.drawable.empty)
                    customDialogManager.setSimpleDialogButtonText("Confirm")

                    customDialogManager.showCustomDialog()

                    return@setOnClickListener
                }else{

                    if (!NetworkUtils.isPortAvailable(CustomServerController.customServerPort)) {
                        Log.e(TAG, "CustomServerPort is already used")

                        val customDialogManager = CustomDialogManager(this@MainActivity, CustomDialogTypes.SIMPLE_DIALOG, "Port Conflict","The selected custom server ${CustomServerController.customServerPort} port is currently in use by another application. Please select a different port and try again.", R.drawable.conflict)
                        customDialogManager.setSimpleDialogButtonText("Confirm")

                        customDialogManager.showCustomDialog()

                        return@setOnClickListener
                    }

                    //DEFAULT SUNUCU SERVİSİ BAŞLATMA
                    ContextCompat.startForegroundService(this@MainActivity, defaultHttpIntent)

                    //CUSTOM SUNUCU SERVİSİ BAŞLATMA
                    ContextCompat.startForegroundService(this@MainActivity, customHttpIntent)

                    openServerButton.text = "Server(s) Running  -  STOP"
                    val colorStateList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.custom_blue))
                    openServerButton.backgroundTintList = colorStateList
                    openServerButton.tag = "OPEN"

                    SharedPreferenceManager.writeInteger("customServerPort", CustomServerController.customServerPort)

                }

            }else{
                stopService(customHttpIntent)
                stopService(defaultHttpIntent)

                openServerButton.text = "OPEN SERVER(S) LETS GOOOOOOO!"
                val colorStateList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.custom_red))
                openServerButton.backgroundTintList = colorStateList
                openServerButton.tag = "CLOSE"
            }

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

            //CREATE ROOM DATABASE
            GlobalScope.launch(Dispatchers.IO) {
                val database = AppDatabase.getDatabase(this@MainActivity)
            }


        }

    private fun createCustomServerFilesDirectory(path: String) {

        val folder = File(Environment.getExternalStorageDirectory().toString() + "/CustomServerFilesHostMobile")

        if (!folder.exists()) {
            folder.mkdirs()
        }

        val file = File(folder, "customServerConfig.txt")

        if (!file.exists()) {
            try {
                file.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

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