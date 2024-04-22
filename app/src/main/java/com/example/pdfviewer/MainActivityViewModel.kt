package com.example.pdfviewer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.smbj.share.File

class MainActivityViewModel: ViewModel() {

    private val _filename = MutableLiveData<ArrayList<java.io.File>>()
    val filename: LiveData<ArrayList<java.io.File>> = _filename

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _folders = MutableLiveData<ArrayList<String>>()
    val folders : LiveData<ArrayList<String>> = _folders


    fun addFiles(filenames: ArrayList<java.io.File>)
    {
        _filename.postValue(filenames)
    }

    fun addFolders(folders: ArrayList<String>)
    {
        _folders.postValue(folders)
    }

    fun getError(error: String?)
    {
        _error.postValue(error)
    }

    fun removeObserver()
    {
        _folders.value = null
    }

    fun removeError()
    {
        _error.value = null
    }
}