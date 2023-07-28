package com.ozgurbaykal.hostmobile.view

import android.content.Context
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
    private var folderFilesMap: Map<String?, List<String>> // Add this map to store child data
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
        groupTextView.isSingleLine = true
        groupTextView.text = getGroup(groupPosition)
        return view
    }

    fun updateChildData(dataMap: Map<String?, List<String>>) {
        folderFilesMap = dataMap // Update the map with the new data
        notifyDataSetChanged()
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
        val view = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false)
        val childTextView = view.findViewById<TextView>(android.R.id.text1)
        val folderName = getGroup(groupPosition)
        val fileName = getChild(groupPosition, childPosition) as String
        childTextView.isSingleLine = true
        childTextView.setTextColor(ContextCompat.getColor(context, R.color.custom_gray))
        childTextView.text = fileName

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



    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }
}
