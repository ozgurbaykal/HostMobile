package com.ozgurbaykal.hostmobile.view

import android.graphics.Color
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.ozgurbaykal.hostmobile.R
import com.ozgurbaykal.hostmobile.databinding.FragmentLogBinding

class LogFragment : Fragment(R.layout.fragment_log) {
    private val TAG = "_LogFragment"

    private var _binding: FragmentLogBinding? = null
    private val binding get() = _binding!!

    private lateinit var scrollView : ScrollView
    private lateinit var logTextView : TextView
    private lateinit var clearLog : Button

    private val logItems: MutableList<LogItem> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentLogBinding.inflate(inflater, container, false)
        val view = binding.root

        scrollView = binding.logTextScroll
        logTextView = binding.logTextDialog
        clearLog = binding.clearLog

        clearLog.setOnClickListener {
            addLog("", Color.WHITE, true)
        }


        return view
    }

    private fun updateLogScreen(firstLog: Boolean) {
        val builder = SpannableStringBuilder()

        if (firstLog) {
            activity?.runOnUiThread {
                logItems.clear()
                logTextView?.text = ""
            }
        }

        for (item in logItems) {
            val color = item.color
            val start = builder.length
            builder.append(item.message)
            val end = builder.length

            builder.setSpan(ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.append("\n")
        }

        activity?.runOnUiThread {
            logTextView?.text = builder
            scrollView?.post { scrollView?.fullScroll(View.FOCUS_DOWN) }
        }
    }


    fun addLog(message: String, color: Int, firstLog: Boolean) {
            val item = LogItem(message, color)
            logItems.add(item)
            //val addNewLineText = LogItem("-----------------------------------", ContextCompat.getColor(requireContext(), R.color.custom_gray))
            //logItems.add(addNewLineText)
            updateLogScreen(firstLog)
    }

    data class LogItem(val message: String, val color: Int)
}