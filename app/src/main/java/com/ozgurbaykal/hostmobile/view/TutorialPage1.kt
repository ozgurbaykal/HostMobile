package com.ozgurbaykal.hostmobile.view

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.ozgurbaykal.hostmobile.R
import com.ozgurbaykal.hostmobile.databinding.FragmentTutorialPage1Binding

class TutorialPage1 : Fragment(R.layout.fragment_tutorial_page1) {

    private val TAG = "_TutorialPage1"

    private lateinit var listener: TutorialPageListener

    private var _binding: FragmentTutorialPage1Binding? = null
    private val binding get() = _binding!!

    private lateinit var nextPageButton : Button

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
        _binding = FragmentTutorialPage1Binding.inflate(inflater, container, false)
        val view = binding.root

        nextPageButton = binding.nextPageTutorial1

        nextPageButton.setOnClickListener {
            Log.i(TAG, "nextPageButton CLICKED")
            listener.onNextPage()

        }

        return view
    }

}

