package com.ozgurbaykal.hostmobile.view

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ExpandableListView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ozgurbaykal.hostmobile.R
import com.ozgurbaykal.hostmobile.control.CopyFolderManagerCustomServer
import com.ozgurbaykal.hostmobile.control.CustomLocalAddress
import com.ozgurbaykal.hostmobile.control.CustomServerController
import com.ozgurbaykal.hostmobile.control.SharedPreferenceManager
import com.ozgurbaykal.hostmobile.databinding.FragmentCustomServerBinding
import com.ozgurbaykal.hostmobile.model.AppDatabase
import com.ozgurbaykal.hostmobile.view.customdialog.CustomDialogManager
import com.ozgurbaykal.hostmobile.view.customdialog.CustomDialogTypes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class CustomServerFragment : Fragment(R.layout.fragment_custom_server) {

    private val TAG = "_CustomServerFragment"

    private var _binding: FragmentCustomServerBinding? = null
    private val binding get() = _binding!!

    private lateinit var advancedSettingsButton : RelativeLayout
    private lateinit var dropDownLinear : LinearLayout

    private lateinit var localIpEditText : EditText
    private lateinit var customServerPortEditText : EditText

    private lateinit var uploadFileLinear : LinearLayout

    private lateinit var progressBarToCopyFolder : ProgressBar
    private lateinit var copyProgressLinear : LinearLayout

    private lateinit var currentFolderName : TextView
    private lateinit var currentFileName : TextView

    private lateinit var folderViewModel: FolderViewModel

    private lateinit var folderListLinear : LinearLayout

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

        progressBarToCopyFolder = binding.copyFolderProgressBar
        copyProgressLinear = binding.copyProgressLinear

        currentFolderName = binding.currentFolderName
        currentFileName = binding.currentFileName

        folderListLinear = binding.folderListLinear

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

        folderListLinear.setOnClickListener {

            openFolderAndFileListDialog()
        }









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


        folderViewModel = ViewModelProvider(this).get(FolderViewModel::class.java)

        return view
    }

   /* fun getHtmlFiles(folderName: String?, context: Context): List<HtmlFile> {
        val directory = context.getExternalFilesDir(folderName)
        val files = directory?.listFiles { file ->
            file.extension == "html"
        }
        return files?.map { HtmlFile(it.name, it.path) } ?: emptyList()
    }*/

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        folderViewModel.selectedFolder.observe(viewLifecycleOwner) { folder ->
            folder?.let {
                currentFolderName.text = it.folderName
                if (it.selectedFile != null)
                    currentFileName.text = it.selectedFile
            }
        }

    }

    private fun openFolderAndFileListDialog(){
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(requireContext())
            val dao = database.folderDao()
            val folderNames = dao.getAll().map { it.folderName }

            if(folderNames.isEmpty()){
                MainActivity.getInstance()?.runOnUiThread {
                    val customDialogManager = CustomDialogManager(requireContext(), CustomDialogTypes.SIMPLE_DIALOG, "Warning!","No folders found. Please upload a folder and try again.", R.drawable.warning)
                    customDialogManager.setSimpleDialogButtonText("Confirm")

                    customDialogManager.showCustomDialog()
                }
            }else{

                val folderFilesMap = mutableMapOf<String?, List<Pair<String, String>>>() // Use Pair<String, String> to store file name and file path together

                for (folderName in folderNames) {
                    val folderDirectory = File(requireContext().getExternalFilesDir(null), folderName)
                    if (folderDirectory.exists() && folderDirectory.isDirectory) {
                        val files = folderDirectory.listFiles { file -> file.extension == "html" }
                        if (files != null && files.isNotEmpty()) {
                            val fileNamesWithPaths = files.map { it.name to it.path } // Pair file name with file path
                            folderFilesMap[folderName] = fileNamesWithPaths
                        }
                    }
                }

                MainActivity.getInstance()?.runOnUiThread {
                    val dialog = Dialog(requireContext())
                    dialog.setContentView(R.layout.custom_dialog_list)

                    val expandableListView = dialog.findViewById<ExpandableListView>(R.id.expandableListView)
                    val adapter = ExpandableListAdapter(requireContext(), folderNames, folderFilesMap)
                    expandableListView.setAdapter(adapter)


                    expandableListView.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
                        val folderName = folderNames[groupPosition]
                        val (fileName, filePath) = folderFilesMap[folderName]?.get(childPosition) ?: Pair("", "") // Retrieve both file name and file path using destructuring
                        // Handle the click event for the child item and pass both file name and file path
                        adapter.updateChildSelectedFile(groupPosition, childPosition, fileName, filePath)
                        adapter.notifyDataSetChanged()
                        true
                    }
                    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    dialog.show()
                }
            }

        }
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val treeUri: Uri? = result.data?.data
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            treeUri?.let { requireActivity().contentResolver.takePersistableUriPermission(it, takeFlags) }

            if (treeUri != null) {
                val appSpecificExternalDir = ContextCompat.getExternalFilesDirs(requireContext(), null)[0]
                val fileUtils = CopyFolderManagerCustomServer(requireContext())

                CoroutineScope(Dispatchers.IO).launch {
                    Log.i(TAG, "Start copy process call -> copyFilesFromUriToAppFolder() func")
                    MainActivity.getInstance()?.runOnUiThread{
                        copyProgressLinear.visibility = View.VISIBLE
                    }
                    fileUtils.copyFilesFromUriToAppFolder(requireContext(), treeUri, appSpecificExternalDir, progressBarToCopyFolder)
                    MainActivity.getInstance()?.runOnUiThread{
                        copyProgressLinear.visibility = View.GONE
                        progressBarToCopyFolder.progress = 0
                    }
                    Log.i(TAG, "Finish copy process")
                }
            }
        }
    }




}