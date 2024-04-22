package com.example.pdfviewer

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.example.pdfviewer.databinding.AcitivityPdfListBinding
import com.example.pdfviewer.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.google.zxing.*
import com.hierynomus.mssmb2.SMBApiException
import com.hierynomus.protocol.transport.TransportException
import kotlinx.coroutines.*


import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.collections.ArrayList

class MainActivity: AppCompatActivity(){


    lateinit var binding: ActivityMainBinding
    private val REQUEST_READ_EXTERNAL_STORAGE = 1001
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Initializing the ViewPagerAdapter
        val adapter = ViewPagerAdapter(supportFragmentManager, lifecycle)

        // Adding the Adapter to the ViewPager
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabs, binding.viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = Constants.manualScreen
                1 -> tab.text = Constants.qrScreen
            }
        }.attach()
        if(checkPermission())
        {
            binding.viewPager.setCurrentItem(0, false);
        }
        else {
            // Permission is not granted. Request the permission from the user.
            requestPermission()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                // Perform search operation here based on the query string
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                // Perform search operation here based on the new query string (as the user types)
                val fragmentManager: FragmentManager = supportFragmentManager
                val fragmentTag = "f" + binding.viewPager.currentItem
                val currentFragment =  fragmentManager.findFragmentByTag(fragmentTag)
                if (currentFragment is ManualFragment) {
                    currentFragment.performSearch(newText)
                }
                return true
            }
        })

        return true
    }

    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val result =
                ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE)
            val result1 =
                ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
            return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermission() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data =
                    Uri.parse(String.format("package:%s", this.applicationContext.packageName))
                startActivityForResult(intent, 2296)
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivityForResult(intent, 2296)
            }
        } else {
            //below android 11
            ActivityCompat.requestPermissions(
                this,
                arrayOf(READ_EXTERNAL_STORAGE),
                REQUEST_READ_EXTERNAL_STORAGE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            // Check if the permission is granted or not.
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                displayPdfFiles()
//                accessRemoteFile()
            } else {
                // Permission is denied. Handle this situation accordingly.
                // For example, you can show a message or disable certain functionality.
                Toast.makeText(this, "Allow permission for storage access!", Toast.LENGTH_SHORT).show()
            }
        }

    }
}