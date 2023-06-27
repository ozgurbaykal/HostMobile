package com.ozgurbaykal.hostmobile.view

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.ozgurbaykal.hostmobile.R
import com.ozgurbaykal.hostmobile.control.CustomLocalAddress
import com.ozgurbaykal.hostmobile.control.CustomServerController
import com.ozgurbaykal.hostmobile.control.SharedPreferenceManager
import com.ozgurbaykal.hostmobile.databinding.FragmentCustomServerBinding
import com.ozgurbaykal.hostmobile.service.CustomHttpService
import com.ozgurbaykal.hostmobile.service.ServiceUtils

class CustomServerFragment : Fragment(R.layout.fragment_custom_server) {

    private val TAG = "_CustomServerFragment"

    private var _binding: FragmentCustomServerBinding? = null
    private val binding get() = _binding!!

    private lateinit var advancedSettingsButton : RelativeLayout
    private lateinit var dropDownLinear : LinearLayout

    private lateinit var localIpEditText : EditText
    private lateinit var customServerPortEditText : EditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val controller = CustomServerController

        _binding = FragmentCustomServerBinding.inflate(inflater, container, false)
        val view = binding.root

        advancedSettingsButton = binding.advancedSettingsRelativeLayoutButton
        dropDownLinear = binding.dropDownLinear

        localIpEditText = binding.customServerLocalIpEditText
        customServerPortEditText = binding.customServerPortEditText

        if(SharedPreferenceManager.readInteger("customServerPort", -1) != -1){

            customServerPortEditText.setText(SharedPreferenceManager.readInteger("customServerPort", -1).toString())
        }

        customServerPortEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val portNumber = customServerPortEditText.text.toString().toIntOrNull() ?: 0
                controller.customServerPort = portNumber
                Log.i(TAG, "After set customServerPort: " + controller.customServerPort)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

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
}