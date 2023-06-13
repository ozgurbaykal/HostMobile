package com.ozgurbaykal.hostmobile.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ozgurbaykal.hostmobile.R
import com.ozgurbaykal.hostmobile.control.SharedPreferenceManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        SharedPreferenceManager.init(this@MainActivity)

        //IF APP OPEN FIRST TIME, SHOW TUTORIAL
        if(SharedPreferenceManager.readBoolean("isNewUser", true) == true){
            val intent = Intent (this@MainActivity, TutorialActivity::class.java)
            startActivity(intent)
        }

    }
}