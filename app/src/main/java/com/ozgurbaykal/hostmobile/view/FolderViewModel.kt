package com.ozgurbaykal.hostmobile.view

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.ozgurbaykal.hostmobile.model.AppDatabase
import com.ozgurbaykal.hostmobile.model.CustomServerFolders

class FolderViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(getApplication())
    private val dao = database.folderDao()
    var selectedFolder: LiveData<CustomServerFolders?> = dao.getSelectedFolderLiveData()

    fun refreshSelectedFolder() {
        selectedFolder = dao.getSelectedFolderLiveData()
    }
}
