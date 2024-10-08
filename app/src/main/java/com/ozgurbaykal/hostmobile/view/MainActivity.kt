package com.ozgurbaykal.hostmobile.view

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.content.res.ColorStateList
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.ozgurbaykal.hostmobile.R
import com.ozgurbaykal.hostmobile.control.CustomLocalAddress
import com.ozgurbaykal.hostmobile.control.CustomServerController
import com.ozgurbaykal.hostmobile.control.DefaultServerSharedPreferenceManager
import com.ozgurbaykal.hostmobile.control.SharedPreferenceManager
import com.ozgurbaykal.hostmobile.databinding.ActivityMainBinding
import com.ozgurbaykal.hostmobile.model.AppDatabase
import com.ozgurbaykal.hostmobile.service.CustomHttpService
import com.ozgurbaykal.hostmobile.service.DefaultHttpService
import com.ozgurbaykal.hostmobile.service.NetworkUtils
import com.ozgurbaykal.hostmobile.service.ServiceUtils
import com.ozgurbaykal.hostmobile.view.customdialog.CustomDialogManager
import com.ozgurbaykal.hostmobile.view.customdialog.CustomDialogTypes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar


class MainActivity : AppCompatActivity() , CustomServerFragment.AuthCodeProcessStarter{
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
            DefaultServerSharedPreferenceManager.init(this@MainActivity)

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
            }

            openServerButton.setOnClickListener {

                CoroutineScope(Dispatchers.IO).launch {
                    val database = AppDatabase.getDatabase(this@MainActivity)
                    val dao = database.folderDao()
                    val folderNames = dao.getAll()
                    val selectedFolder = dao.getSelectedFolder()

                    withContext(Dispatchers.Main) {
                        if (folderNames.isEmpty()) {
                            val customDialogManager = CustomDialogManager(this@MainActivity, CustomDialogTypes.SIMPLE_DIALOG, getString(R.string.warning), getString(R.string.no_folders_found), R.drawable.warning)
                            customDialogManager.setSimpleDialogButtonText(getString(R.string.confirm))
                            customDialogManager.showCustomDialog()
                            return@withContext
                        }else{
                            if (selectedFolder != null) {
                                if(selectedFolder.selectedFile == null){
                                    val customDialogManager = CustomDialogManager(this@MainActivity, CustomDialogTypes.SIMPLE_DIALOG, getString(R.string.warning),getString(R.string.no_selected_file_found), R.drawable.warning)
                                    customDialogManager.setSimpleDialogButtonText(getString(R.string.confirm))

                                    customDialogManager.showCustomDialog()
                                }else{
                                    val defaultHttpIntent = Intent(this@MainActivity, DefaultHttpService::class.java)
                                    val customHttpIntent = Intent(this@MainActivity, CustomHttpService::class.java)

                                    if (openServerButton.tag.equals("CLOSE")) {
                                        Log.i(TAG, "openServerButton clicked customServerPort: " + CustomServerController.customServerPort)

                                        if (CustomServerController.customServerPort == 0) {
                                            val customDialogManager = CustomDialogManager(this@MainActivity, CustomDialogTypes.SIMPLE_DIALOG, getString(R.string.empty_fields), getString(R.string.port_field_empty), R.drawable.empty)
                                            customDialogManager.setSimpleDialogButtonText(getString(R.string.confirm))
                                            customDialogManager.showCustomDialog()
                                            return@withContext
                                        } else {
                                            if (!NetworkUtils.isPortAvailable(CustomServerController.customServerPort)) {
                                                Log.e(TAG, "CustomServerPort is already used!!")
                                                val customDialogManager = CustomDialogManager(this@MainActivity, CustomDialogTypes.SIMPLE_DIALOG, getString(R.string.port_conflict), getString(R.string.port_conflict_message, CustomServerController.customServerPort), R.drawable.conflict)
                                                customDialogManager.setSimpleDialogButtonText(getString(R.string.confirm))
                                                customDialogManager.showCustomDialog()
                                                return@withContext
                                            }

                                            // DEFAULT SUNUCU SERVİSİ BAŞLATMA
                                            ContextCompat.startForegroundService(this@MainActivity, defaultHttpIntent)

                                            // CUSTOM SUNUCU SERVİSİ BAŞLATMA
                                            ContextCompat.startForegroundService(this@MainActivity, customHttpIntent)

                                            openServerButton.text = "Server(s) Running  -  STOP"
                                            val colorStateList = ColorStateList.valueOf(ContextCompat.getColor(this@MainActivity, R.color.custom_blue))
                                            openServerButton.backgroundTintList = colorStateList
                                            openServerButton.tag = "OPEN"

                                            try {
                                                val findCustomFragment = supportFragmentManager.findFragmentByTag("CustomFragmentTag") as CustomServerFragment
                                                findCustomFragment.startAuthCodeProcess()
                                            }catch (e: Exception){
                                                e.printStackTrace()
                                            }

                                            try {
                                                val findDefaultDefaultFragment = supportFragmentManager.findFragmentByTag("DefaultFragmentTag") as DefaultServerFragment
                                                findDefaultDefaultFragment.startAuthCodeProcess()
                                            }catch (e: Exception){
                                                e.printStackTrace()
                                            }

                                            SharedPreferenceManager.writeInteger("customServerPort", CustomServerController.customServerPort)
                                        }
                                    } else {
                                        stopService(customHttpIntent)
                                        stopService(defaultHttpIntent)

                                        openServerButton.text = "OPEN SERVER(S) LETS GOOOOOOO!"
                                        val colorStateList = ColorStateList.valueOf(ContextCompat.getColor(this@MainActivity, R.color.custom_red))
                                        openServerButton.backgroundTintList = colorStateList
                                        openServerButton.tag = "CLOSE"

                                        try {
                                            val findCustomFragment = supportFragmentManager.findFragmentByTag("CustomFragmentTag") as CustomServerFragment
                                            findCustomFragment.stopAuthCodeProcess()

                                        }catch (e: Exception){
                                            e.printStackTrace()
                                        }

                                        try {
                                            val findDefaultDefaultFragment = supportFragmentManager.findFragmentByTag("DefaultFragmentTag") as DefaultServerFragment
                                            findDefaultDefaultFragment.stopAuthCodeProcess()
                                        }catch (e: Exception){
                                            e.printStackTrace()
                                        }

                                    }
                                }
                            }
                        }


                    }
                }
            }




            bottomNav = binding.bottomNavigation

        bottomNav.setOnItemSelectedListener  {item ->
            when (item.itemId) {
                R.id.page_1_bottom_nav_button -> {
                    Log.i(TAG, "Bottom Nav ClickEvent -> PAGE 1")
                    changeFragment(CustomServerFragment(), R.id.main_fragment_view, "CustomFragmentTag")
                    return@setOnItemSelectedListener true
                }
                R.id.page_2_bottom_nav_button -> {
                    Log.i(TAG, "Bottom Nav ClickEvent -> PAGE 2")
                    changeFragment(DefaultServerFragment(), R.id.main_fragment_view, "DefaultFragmentTag")
                    return@setOnItemSelectedListener true
                }
                R.id.page_3_bottom_nav_button -> {
                    Log.i(TAG, "Bottom Nav ClickEvent -> PAGE 3")
                    changeFragment(LogFragment(), R.id.main_fragment_view, "LogFragmentTag")

                    return@setOnItemSelectedListener true
                }
                R.id.page_4_bottom_nav_button -> {
                    Log.i(TAG, "Bottom Nav ClickEvent -> PAGE 4")
                    changeFragment(SettingsFragment(), R.id.main_fragment_view, "SettingsFragmentTag")

                    return@setOnItemSelectedListener true
                }
            }
            false
        }
            if (savedInstanceState == null) {
                supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    add<CustomServerFragment>(R.id.main_fragment_view, "CustomFragmentTag")
                }
            }

            askNotificationPerm();

            Log.i(TAG, "IP ADRESS DEVICE -> " + CustomLocalAddress.getIpAddress(this@MainActivity))

            //CREATE ROOM DATABASE
            GlobalScope.launch(Dispatchers.IO) {
                val database = AppDatabase.getDatabase(this@MainActivity)
            }


            instance = this


            //CODE THAT COMPARES THE VERSION OF THE APPLICATION WITH THE GOOGLE PLAY VERSION
            val appUpdateManager = AppUpdateManagerFactory.create(
                applicationContext
            )
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo: AppUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                ) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            this, // RUN ON THIS ACTIVITY
                            142536 //MY INTENT CODE
                        )
                    } catch (e: SendIntentException) {
                        e.printStackTrace()
                    }
                }
            }

        }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "-> onResume()")
    }

    companion object {
        private var instance: MainActivity? = null

        fun getInstance(): MainActivity? {
            return instance
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }


    fun getCurrentTime(): String {
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("HH:mm:ss")
        return format.format(calendar.time)
    }

    fun addLogFromInstance(message: String, color: Int, firstLog: Boolean){
        try {
            val findLogFragment = supportFragmentManager.findFragmentByTag("LogFragmentTag") as LogFragment
            findLogFragment.addLog(message, color, firstLog)
        }catch (e: Exception){
            Log.e(TAG, " LogFragment cant find addLog get error")
            e.message
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


    private fun changeFragment(fragment: Fragment, frameId: Int, tag: String) {
        supportFragmentManager.beginTransaction().apply {
            replace(frameId, fragment, tag)
            commit()
        }
    }

    override fun startProcess() {
        val fragment = supportFragmentManager.findFragmentByTag("YourFragmentTag") as CustomServerFragment
        fragment.startAuthCodeProcess()
    }

}