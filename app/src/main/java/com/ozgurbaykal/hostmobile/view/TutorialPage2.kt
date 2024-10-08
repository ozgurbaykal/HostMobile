package com.ozgurbaykal.hostmobile.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.ozgurbaykal.hostmobile.R
import com.ozgurbaykal.hostmobile.databinding.FragmentTutorialPage2Binding

class TutorialPage2 : Fragment(R.layout.fragment_tutorial_page2) {

    private val TAG = "_TutorialPage2"

    private lateinit var listener: TutorialPageListener

    private var _binding: FragmentTutorialPage2Binding? = null
    private val binding get() = _binding!!

    private lateinit var nextPageButton : Button
    private lateinit var goToDocButton : Button

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is TutorialPageListener) {
            listener = context
        } else {
            throw ClassCastException("$context must implement TutorialPageListener")
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTutorialPage2Binding.inflate(inflater, container, false)
        val view = binding.root

        nextPageButton = binding.nextPageTutorial2
        goToDocButton = binding.goToDocumentation

        nextPageButton.setOnClickListener {
            Log.i(TAG, "nextPageButton CLICKED")
            activity?.finish()

        }

        goToDocButton.setOnClickListener {
            Log.i(TAG, "goToDocButton CLICKED")
            val openURL = Intent(Intent.ACTION_VIEW)
            openURL.data = Uri.parse("https://hostmobile.baykal.me")
            startActivity(openURL)
        }

        return view
    }

}

