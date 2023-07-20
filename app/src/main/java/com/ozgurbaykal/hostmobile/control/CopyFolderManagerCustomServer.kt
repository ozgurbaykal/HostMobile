package com.ozgurbaykal.hostmobile.control

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.ozgurbaykal.hostmobile.R
import com.ozgurbaykal.hostmobile.model.AppDatabase
import com.ozgurbaykal.hostmobile.model.CustomServerFolders
import com.ozgurbaykal.hostmobile.view.customdialog.CustomDialogManager
import com.ozgurbaykal.hostmobile.view.customdialog.CustomDialogTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class CopyFolderManagerCustomServer constructor(private val context: Context){
    private val TAG = "_CopyFolderManagerCustomServer"

     fun copyFilesFromUriToAppFolder(context: Context, uri: Uri, destFolder: File) {
        val documentId = DocumentsContract.getTreeDocumentId(uri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, documentId)
        context.contentResolver.query(childrenUri, null, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            while (cursor.moveToNext()) {
                val childId = cursor.getString(idColumn)
                val childUri = DocumentsContract.buildDocumentUriUsingTree(uri, childId)
                copyFileFromUriToFolder(context, childUri, destFolder)
            }
        }
    }

    fun copyFileFromUriToFolder(context: Context, uri: Uri, destFolder: File) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val destFile = File(destFolder, DocumentsContract.getDocumentId(uri))
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

                val customDialogManager = CustomDialogManager(context, CustomDialogTypes.SIMPLE_DIALOG, "Success!","The folder named \"$selectedFolderName\" has been successfully copied to the custom folder of the Custom Server.", R.drawable.check)
                customDialogManager.setSimpleDialogButtonText("Confirm")

                customDialogManager.showCustomDialog()


                //INSERT NEW FOLDER NAME TO ROOMDB
                GlobalScope.launch(Dispatchers.IO) {
                    val database = AppDatabase.getDatabase(context)
                    val dao = database.folderDao()
                    val customServerFolders = CustomServerFolders(id = 0, folderName = selectedFolderName)

                    dao.insert(customServerFolders)
                }



            } else {
                Log.e(TAG, "Folder cant copy: ${destFile.absolutePath}")

                val customDialogManager = CustomDialogManager(context, CustomDialogTypes.SIMPLE_DIALOG, "Error!","There was a problem copying the folder, please try again.", R.drawable.warning)
                customDialogManager.setSimpleDialogButtonText("Confirm")

                customDialogManager.showCustomDialog()

            }
        }
    }
}