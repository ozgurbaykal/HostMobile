package com.ozgurbaykal.hostmobile.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index(value = ["folder_name"], unique = true)])
class CustomServerFolders (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "folder_name") val folderName: String?,
    @ColumnInfo(name = "is_selected") val isSelected: Boolean = false,
    @ColumnInfo(name = "selected_file") val selectedFile: String? = null,
    @ColumnInfo(name = "selected_file_path") val selectedFilePath: String? = null
)