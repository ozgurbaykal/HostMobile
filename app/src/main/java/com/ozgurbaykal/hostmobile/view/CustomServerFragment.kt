package com.ozgurbaykal.hostmobile.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.FileUtils
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import com.ozgurbaykal.hostmobile.R
import com.ozgurbaykal.hostmobile.control.CopyFolderManagerCustomServer
import com.ozgurbaykal.hostmobile.control.CustomLocalAddress
import com.ozgurbaykal.hostmobile.control.CustomServerController
import com.ozgurbaykal.hostmobile.control.SharedPreferenceManager
import com.ozgurbaykal.hostmobile.databinding.FragmentCustomServerBinding
import com.ozgurbaykal.hostmobile.view.customdialog.CustomDialogManager
import com.ozgurbaykal.hostmobile.view.customdialog.CustomDialogTypes
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException


class CustomServerFragment : Fragment(R.layout.fragment_custom_server) {

    private val TAG = "_CustomServerFragment"

    private val requestCodeForOpenDocumentTree = 10

    private var _binding: FragmentCustomServerBinding? = null
    private val binding get() = _binding!!

    private lateinit var advancedSettingsButton : RelativeLayout
    private lateinit var dropDownLinear : LinearLayout

    private lateinit var localIpEditText : EditText
    private lateinit var customServerPortEditText : EditText

    private lateinit var uploadFileLinear : LinearLayout

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
        uploadFileLinear = binding.uploadFileLinear

        localIpEditText = binding.customServerLocalIpEditText
        customServerPortEditText = binding.customServerPortEditText

        if(SharedPreferenceManager.readInteger("customServerPort", -1) != -1){

            customServerPortEditText.setText(SharedPreferenceManager.readInteger("customServerPort", -1).toString())
            Log.i(TAG, " customServerPort from preference: " + SharedPreferenceManager.readInteger("customServerPort", -1).toString())
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



        uploadFileLinear.setOnClickListener {
            Log.i(TAG, "uploadFileLinear clicked")

            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            resultLauncher.launch(intent)
        }

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

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val treeUri: Uri? = result.data?.data
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
// Check for the freshest data.
            treeUri?.let { requireActivity().contentResolver.takePersistableUriPermission(it, takeFlags) }

            if (treeUri != null) {
                val appSpecificExternalDir = ContextCompat.getExternalFilesDirs(requireContext(), null)[0]
                Log.i(TAG, "appSpecificExternalDir: " + appSpecificExternalDir)
                val fileUtils = CopyFolderManagerCustomServer(requireContext())

                fileUtils.copyFilesFromUriToAppFolder(requireContext(), treeUri, appSpecificExternalDir)

            }
        }
    }



}