package com.ozgurbaykal.hostmobile.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.ozgurbaykal.hostmobile.R
import com.ozgurbaykal.hostmobile.control.CustomLocalAddress
import com.ozgurbaykal.hostmobile.databinding.FragmentDefaultServerBinding
import com.ozgurbaykal.hostmobile.service.CustomServerData
import com.ozgurbaykal.hostmobile.service.DefaultServerData
import java.util.Timer
import java.util.TimerTask
import kotlin.random.Random

class DefaultServerFragment : Fragment(R.layout.fragment_default_server) {

    private val TAG = "_DefaultServerFragment"

    private var _binding: FragmentDefaultServerBinding? = null
    private val binding get() = _binding!!

    private lateinit var advancedSettingsButton : RelativeLayout
    private lateinit var dropDownLinear : LinearLayout

    private lateinit var localIpEditText : EditText

    private lateinit var authCodeText : TextView
    private lateinit var authCodeProgress : ProgressBar
    private lateinit var authRandomCodeLinear : LinearLayout

    private var timer: Timer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentDefaultServerBinding.inflate(inflater, container, false)
        val view = binding.root

        advancedSettingsButton = binding.advancedSettingsRelativeLayoutButton
        dropDownLinear = binding.dropDownLinear

        localIpEditText = binding.defaultServerLocalIpEditText

        authCodeText = binding.authCodeText
        authCodeProgress = binding.authCodeProgress
        authRandomCodeLinear = binding.authRandomCodeLinear

        localIpEditText.setText(CustomLocalAddress.getIpAddress(requireContext()))

        advancedSettingsButton.setOnClickListener {
            Log.i(TAG, " Advanced Settings Clicked")

            if(dropDownLinear.isVisible)
                dropDownLinear.visibility = View.GONE
            else
                dropDownLinear.visibility = View.VISIBLE
        }

        return view
    }

    fun startAuthCodeProcess() {
        Log.i(TAG, "DefaultServerFragment -> startAuthCodeProcess()")
        stopAuthCodeProcess() // Eğer bir timer zaten çalışıyorsa durdur

        generateAndSetAuthCode()
        authRandomCodeLinear.visibility = View.VISIBLE

        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                MainActivity.getInstance()?.runOnUiThread {


                    val currentProgress = authCodeProgress.progress
                    if (currentProgress == 0) {
                        authCodeProgress.progress = 100
                        generateAndSetAuthCode()
                    } else {
                        authCodeProgress.progress = currentProgress - 10 // Her 1 saniyede %10 artır
                    }
                }
            }
        }, 0, 1000) // 1000 ms = 1 saniye
    }

    fun stopAuthCodeProcess() {
        timer?.cancel()
        timer = null
        authCodeProgress.progress = 0
        authCodeText.text = "----"
        authRandomCodeLinear.visibility = View.GONE
    }
    private fun generateAndSetAuthCode() {
        val randomCode = Random.nextInt(1000, 9999)
        DefaultServerData.defaultServerAuthPassword = randomCode
        authCodeText.text = randomCode.toString()
    }
}