package com.ozgurbaykal.hostmobile.view

import android.content.Context
import android.text.InputFilter
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.ozgurbaykal.hostmobile.R
import com.ozgurbaykal.hostmobile.model.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class ExpandableListAdapter(
    private val context: Context,
    private val folderList: List<String?>,
    private var folderFilesMap: Map<String?, List<Pair<String, String>>> // Update the data type for folderFilesMap
) : BaseExpandableListAdapter() {

    override fun getGroup(groupPosition: Int): String? {
        return folderList[groupPosition]
    }



    override fun getGroupCount(): Int {
        return folderList.size
    }



    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }



    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_expandable_list_item_1, parent, false)
        val groupTextView = view.findViewById<TextView>(android.R.id.text1)
        val folderName = getGroup(groupPosition)
        Log.i("ExpandableListAdapter", "getGroupView()")
        GlobalScope.launch(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(context)
            val dao = database.folderDao()
            val selectedFolder = dao.getSelectedFolder()

            MainActivity.getInstance()?.runOnUiThread {
                Log.i("ExpandableListAdapter", "selectedFolder?.folderName: " + selectedFolder?.folderName +  " folderName: " + folderName)
                val drawable = if (selectedFolder?.folderName == folderName) {
                    ContextCompat.getDrawable(context, R.drawable.checked_mini) // Icon for selected
                } else {
                    ContextCompat.getDrawable(context, R.drawable.rec) // Icon for unselected
                }

                drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                groupTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
            }
        }



        groupTextView.text = if ((folderName?.length ?: 0) > 19) folderName?.substring(0, 18) else folderName

        return view
    }


    override fun getChildrenCount(groupPosition: Int): Int {
        val folderName = getGroup(groupPosition)
        return folderFilesMap[folderName]?.size ?: 0
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        val folderName = getGroup(groupPosition)
        return folderFilesMap[folderName]?.get(childPosition) ?: ""
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }


    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        Log.i("ExpandableListAdapter", "getChildView()")
        val view = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false)
        val childTextView = view.findViewById<TextView>(android.R.id.text1)
        val folderName = getGroup(groupPosition)
        val (fileName, _) = getChild(groupPosition, childPosition) as Pair<String, String> // Destructuring to get the file name from the Pair object

        childTextView.setTextColor(ContextCompat.getColor(context, R.color.custom_gray))
        childTextView.text = fileName
        childTextView.text = if ((fileName?.length ?: 0) > 19) fileName?.substring(0, 18) else fileName

        // Get the selected file for the current group item from the SQLite database


        GlobalScope.launch(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(context)
            val dao = database.folderDao()
            val selectedFile = dao.getSelectedFile(folderName)

            if (fileName == selectedFile) {

            MainActivity.getInstance()?.runOnUiThread {
                val drawable = ContextCompat.getDrawable(context, R.drawable.checked_mini)
                drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                childTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
            }

            } else {
                MainActivity.getInstance()?.runOnUiThread {
                    val drawable = ContextCompat.getDrawable(context, R.drawable.rec)
                    drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                    childTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)                }
            }
        }

        return view
    }

    fun updateChildSelectedFile(groupPosition: Int, childPosition: Int, newSelectedFile: String?, newSelectedFilePath: String?) {
        val folderName = folderList[groupPosition]
        val filesList = folderFilesMap[folderName]

        if (filesList != null && childPosition >= 0 && childPosition < filesList.size) {
            val (selectedFileName, selectedFilePath) = filesList[childPosition]

            if (selectedFileName == newSelectedFile) {
                // Eğer yeni seçilen dosya zaten mevcut dosyaysa, bu dosyayı seçili yapın.
                GlobalScope.launch(Dispatchers.IO) {
                    val database = AppDatabase.getDatabase(context)
                    val dao = database.folderDao()
                    dao.updateSelectedFile(folderName, newSelectedFile, newSelectedFilePath)
                    dao.updateSelectedFolder(true, folderName)
                    dao.updateOtherFolders(false, folderName)

                }
            } else {
                // Eğer yeni seçilen dosya farklıysa, eski seçilen dosyayı kaldırın ve yeni seçilen dosyayı seçili yapın.
                GlobalScope.launch(Dispatchers.IO) {
                    val database = AppDatabase.getDatabase(context)
                    val dao = database.folderDao()
                    dao.clearSelectedFile(folderName)
                    dao.updateSelectedFile(folderName, newSelectedFile, newSelectedFilePath)
                    dao.updateSelectedFolder(true, folderName)
                    dao.updateOtherFolders(false, folderName)
                    notifyDataSetChanged()
                }
            }
            notifyDataSetChanged()
        }
    }


    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }
}
