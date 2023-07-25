package com.ozgurbaykal.hostmobile.control

import android.app.Activity
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import com.ozgurbaykal.hostmobile.R
import com.ozgurbaykal.hostmobile.model.AppDatabase
import com.ozgurbaykal.hostmobile.model.CustomServerFolders
import com.ozgurbaykal.hostmobile.view.CustomServerFragment
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
                        },    onNoButtonClick = {
                            Log.i(TAG, "customDialogManager() -> onNoButtonClick")
                            customDialogManager.cancelCustomDialog()
                        })
                }



            } catch (e: SQLiteConstraintException) {
                // when folderName already selected
                MainActivity.getInstance()?.runOnUiThread {
                    val customDialogManager = CustomDialogManager(context, CustomDialogTypes.SIMPLE_DIALOG, "Error!","This folder name already copied. Please try different folder or change folder name.", R.drawable.warning)
                    customDialogManager.setSimpleDialogButtonText("Confirm")

                    customDialogManager.showCustomDialog()

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

                    val customDialogManager = CustomDialogManager(context, CustomDialogTypes.SIMPLE_DIALOG, "Error!","There was a problem copying the folder, please try again.", R.drawable.warning)
                    customDialogManager.setSimpleDialogButtonText("Confirm")

                    customDialogManager.showCustomDialog()

                }
            }
        }

    }
}