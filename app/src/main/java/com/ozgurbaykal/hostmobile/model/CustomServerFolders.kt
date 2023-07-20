package com.ozgurbaykal.hostmobile.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class CustomServerFolders (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "folder_name") val folderName: String?,
)