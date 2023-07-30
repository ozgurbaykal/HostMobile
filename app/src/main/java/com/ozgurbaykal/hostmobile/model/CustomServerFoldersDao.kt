package com.ozgurbaykal.hostmobile.model

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CustomServerFoldersDao {
    @Query("SELECT * FROM customserverfolders")
    fun getAll(): List<CustomServerFolders>

    @Insert
    fun insert(folder: CustomServerFolders)

    @Delete
    fun delete(folder: CustomServerFolders)
    @Query("SELECT * FROM customserverfolders WHERE is_selected = 1 LIMIT 1")
    fun getSelectedFolderLiveData(): LiveData<CustomServerFolders?>

    @Query("SELECT selected_file FROM CustomServerFolders WHERE folder_name = :folderName")
    fun getSelectedFile(folderName: String?): String?

    @Query("UPDATE customserverfolders SET selected_file = NULL WHERE folder_name = :folderName")
    suspend fun clearSelectedFile(folderName: String?)

    @Query("UPDATE customserverfolders SET selected_file = :selectedFile, selected_file_path = :selectedFilePath WHERE folder_name = :folderName")
    suspend fun updateSelectedFile(folderName: String?, selectedFile: String?, selectedFilePath: String?)


    @Query("UPDATE customserverfolders SET selected_file_path = :selectedFilePath WHERE folder_name = :folderName")
    fun updateSelectedFilePath(folderName: String?, selectedFilePath: String?)

    @Query("SELECT selected_file_path FROM CustomServerFolders WHERE folder_name = :folderName")
    fun getSelectedFilePath(folderName: String?): String?
}