package com.ozgurbaykal.hostmobile.view

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.ozgurbaykal.hostmobile.R
import com.ozgurbaykal.hostmobile.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private val TAG = "_SettingsFragment"

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var mailText : TextView
    private lateinit var goToDocsButton : Button
    private lateinit var rateAppButton : Button


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val view = binding.root

        mailText = binding.mailText
        goToDocsButton = binding.goToDocsButton
        rateAppButton = binding.rateAppButton


        mailText.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:ozgur_baykal@hotmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "About HostMobile App")
            }

            try {
                startActivity(emailIntent)
            } catch (ex: ActivityNotFoundException) {
                Toast.makeText(requireContext(), requireContext().getString(R.string.cant_find_mail_app), Toast.LENGTH_SHORT).show()
            }


        }

        goToDocsButton.setOnClickListener {
            val openURL = Intent(Intent.ACTION_VIEW)
            openURL.data = Uri.parse("https://hostmobile.baykal.me")
            startActivity(openURL)
        }

        rateAppButton.setOnClickListener {
            try {
                val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${requireContext().packageName}"))
                startActivity(playStoreIntent)
            } catch (e: Exception) {
                val playStoreWebIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${requireContext().packageName}"))
                startActivity(playStoreWebIntent)
            }
        }


        return view
    }


}