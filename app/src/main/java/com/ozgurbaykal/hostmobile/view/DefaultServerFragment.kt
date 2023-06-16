package com.ozgurbaykal.hostmobile.view

import android.os.Bundle
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
import com.ozgurbaykal.hostmobile.databinding.FragmentDefaultServerBinding

class DefaultServerFragment : Fragment(R.layout.fragment_default_server) {

    private val TAG = "_DefaultServerFragment"

    private var _binding: FragmentDefaultServerBinding? = null
    private val binding get() = _binding!!

    private lateinit var advancedSettingsButton : RelativeLayout
    private lateinit var dropDownLinear : LinearLayout

    private lateinit var localIpEditText : EditText

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