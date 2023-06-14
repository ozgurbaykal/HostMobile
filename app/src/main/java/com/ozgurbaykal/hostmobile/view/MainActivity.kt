package com.ozgurbaykal.hostmobile.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.ozgurbaykal.hostmobile.R
import com.ozgurbaykal.hostmobile.control.SharedPreferenceManager
import com.ozgurbaykal.hostmobile.databinding.ActivityMainBinding
import androidx.fragment.app.add
import androidx.fragment.app.commit
class MainActivity : AppCompatActivity() {
    private val TAG = "_MainActivity"

    private lateinit var binding: ActivityMainBinding

    private lateinit var bottomNav : BottomNavigationView
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

        }

    private fun changeFragment(fragment: Fragment, frameId: Int) {
        supportFragmentManager.beginTransaction().apply {
            replace(frameId, fragment)
            commit()
        }
    }

}