package com.ozgurbaykal.hostmobile.control

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Button
import android.widget.ExpandableListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.ozgurbaykal.hostmobile.R
import com.ozgurbaykal.hostmobile.model.AppDatabase
import com.ozgurbaykal.hostmobile.model.CustomServerFolders
import com.ozgurbaykal.hostmobile.view.CustomServerFragment
import com.ozgurbaykal.hostmobile.view.ExpandableListAdapter
import com.ozgurbaykal.hostmobile.view.MainActivity
import com.ozgurbaykal.hostmobile.view.customdialog.CustomDialogManager
import com.ozgurbaykal.hostmobile.view.customdialog.CustomDialogTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream


class CopyFolderManagerCustomServer constructor(private val context: Context){
    private val TAG = "_CopyFolderManagerCustomServer"

    private var totalFiles = 0
    private var copiedFiles = 0

    fun copyFilesFromUriToAppFolder(context: Context, uri: Uri, destFolder: File, progressBar: ProgressBar) {
        val selectedFolder = DocumentFile.fromTreeUri(context, uri)
        val selectedFolderName = selectedFolder?.name ?: return
        val newDestFolder = File(destFolder, selectedFolderName)
        newDestFolder.mkdirs()

        val documentId = DocumentsContract.getTreeDocumentId(uri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, documentId)
        context.contentResolver.query(childrenUri, null, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            totalFiles = cursor.count
            copiedFiles = 0
            while (cursor.moveToNext()) {
                val childId = cursor.getString(idColumn)
                val childUri = DocumentsContract.buildDocumentUriUsingTree(uri, childId)
                copyFileFromUriToFolder(context, childUri, newDestFolder)  // Passing newDestFolder instead of destFolder
                copiedFiles++
                progressBar.progress = (copiedFiles * 100) / totalFiles
            }
        }





        //INSERT NEW FOLDER NAME TO ROOMDB
        GlobalScope.launch(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(context)
            val dao = database.folderDao()
            val totalData = dao.getAll().size

            val isSelected = totalData == 0  // if database is empty set isSelected true for selected folder

            val customServerFolders = CustomServerFolders(id = 0, folderName = selectedFolderName, isSelected = isSelected)

            try {
                dao.insert(customServerFolders)

                Log.i(TAG, " copyFilesFromUriToAppFolder() -> insert customServerFolders")
                MainActivity.getInstance()?.runOnUiThread {
                    /*val customDialogManager = CustomDialogManager(context, CustomDialogTypes.SIMPLE_DIALOG, "Success!","The folder named \"$selectedFolderName\" has been successfully copied to the custom folder of the Custom Server.", R.drawable.check)
                    customDialogManager.setSimpleDialogButtonText("Confirm")

                    customDialogManager.showCustomDialog()*/

                    val customDialogManager = CustomDialogManager(context, CustomDialogTypes.YES_OR_NO_DIALOG, "Success!","The folder named \"$selectedFolderName\" has been successfully copied to the custom folder of the Custom Server. \n\n Do you want to choose the starting HTML page for this folder?", R.drawable.check)
                    customDialogManager.setYesNoDialogYesButtonText("Select HTML")
                    customDialogManager.setYesNoDialogNoButtonText("Later")
                    customDialogManager.showCustomDialog(
                        onDialogCancel = {
                            Log.i(TAG, "customDialogManager() -> onDialogCancel")
                        },    onYesButtonClick = {
                            Log.i(TAG, "customDialogManager() -> onYesButtonClick")
                            customDialogManager.cancelCustomDialog()
                            openFolderAndFileListDialog()
                        },    onNoButtonClick = {
                            Log.i(TAG, "customDialogManager() -> onNoButtonClick")
                            customDialogManager.cancelCustomDialog()
                        })
                }



            } catch (e: SQLiteConstraintException) {
                // when folderName already selected
                MainActivity.getInstance()?.runOnUiThread {
                    val customDialogManager = CustomDialogManager(context, CustomDialogTypes.SIMPLE_DIALOG, context.getString(R.string.error),context.getString(R.string.already_copied), R.drawable.warning)
                    customDialogManager.setSimpleDialogButtonText(context.getString(R.string.confirm))

                    customDialogManager.showCustomDialog()

                }
            }
        }
    }

    fun openFolderAndFileListDialog(){
        GlobalScope.launch(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(context)
            val dao = database.folderDao()
            val folderNames = dao.getAll().map { it.folderName }

            if(folderNames.isEmpty()){
                MainActivity.getInstance()?.runOnUiThread {
                    val customDialogManager = CustomDialogManager(context, CustomDialogTypes.SIMPLE_DIALOG, context.getString(R.string.warning),context.getString(R.string.no_folders_found), R.drawable.warning)
                    customDialogManager.setSimpleDialogButtonText(context.getString(R.string.confirm))

                    customDialogManager.showCustomDialog()
                }
            }else{

                val folderFilesMap = mutableMapOf<String?, List<Pair<String, String>>>() // Use Pair<String, String> to store file name and file path together

                for (folderName in folderNames) {
                    val folderDirectory = File(context.getExternalFilesDir(null), folderName)
                    if (folderDirectory.exists() && folderDirectory.isDirectory) {
                        val files = folderDirectory.listFiles { file -> file.extension == "html" }
                        if (files != null && files.isNotEmpty()) {
                            val fileNamesWithPaths = files.map { it.name to it.path } // Pair file name with file path
                            folderFilesMap[folderName] = fileNamesWithPaths
                        }
                    }
                }

                MainActivity.getInstance()?.runOnUiThread {
                    val dialog = Dialog(context)
                    dialog.setContentView(R.layout.custom_dialog_list)

                    val expandableListView = dialog.findViewById<ExpandableListView>(R.id.expandableListView)
                    val confirmButton = dialog.findViewById<Button>(R.id.listDialogClickButton)
                    val adapter = ExpandableListAdapter(context, folderNames, folderFilesMap)
                    expandableListView.setAdapter(adapter)

                    confirmButton.setOnClickListener {
                        dialog.cancel()
                    }

                    expandableListView.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
                        val folderName = folderNames[groupPosition]
                        val (fileName, filePath) = folderFilesMap[folderName]?.get(childPosition) ?: Pair("", "") // Retrieve both file name and file path using destructuring
                        // Handle the click event for the child item and pass both file name and file path
                        adapter.updateChildSelectedFile(groupPosition, childPosition, fileName, filePath)

                        true
                    }
                    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    dialog.show()
                }
            }

        }
    }
    fun copyFileFromUriToFolder(context: Context, uri: Uri, destFolder: File) {

        val documentFile = DocumentFile.fromSingleUri(context, uri)
        if (documentFile?.isDirectory == true) {
            // This is a directory, not a file. We should recurse.
            val newDestFolder = File(destFolder, documentFile.name ?: return)
            newDestFolder.mkdirs()

            // Get the children of this directory and recurse.
            val documentId = DocumentsContract.getDocumentId(uri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, documentId)
            context.contentResolver.query(childrenUri, null, null, null, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                while (cursor.moveToNext()) {
                    val childId = cursor.getString(idColumn)
                    val childUri = DocumentsContract.buildDocumentUriUsingTree(uri, childId)
                    copyFileFromUriToFolder(context, childUri, newDestFolder)
                }
            }
        }
        else {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val destFile = File(destFolder, documentFile?.name ?: return)
                if (!destFile.parentFile.exists()) {
                    destFile.parentFile.mkdirs()
                }
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }

                if (destFile.exists()) {
                    Log.d(TAG, "Folder successfully copy ${destFile.absolutePath}")

                    val selectedFolder = DocumentFile.fromTreeUri(context, uri)
                    val selectedFolderName = selectedFolder?.name

                    Log.i(TAG, "Selected Folder Name: $selectedFolderName")

                } else {
                    Log.e(TAG, "Folder cant copy: ${destFile.absolutePath}")

                    val customDialogManager = CustomDialogManager(context, CustomDialogTypes.SIMPLE_DIALOG, context.getString(R.string.error),context.getString(R.string.copy_folder_problem), R.drawable.warning)
                    customDialogManager.setSimpleDialogButtonText(context.getString(R.string.confirm))

                    customDialogManager.showCustomDialog()

                }
            }
        }

    }
}