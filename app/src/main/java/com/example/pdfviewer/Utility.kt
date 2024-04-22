package com.example.pdfviewer

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.core.content.FileProvider
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream

class Utility {
    companion object {
        fun isInternetAvailable(context: Context): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return connectivityManager.activeNetworkInfo != null && connectivityManager.activeNetworkInfo!!
                .isConnected
        }

        fun cachePDF(directory: String, fileName: String,smbFile: com.hierynomus.smbj.share.File): File {
            val localFilePath = "${directory}/${fileName}"
            val localFile = File(localFilePath)
            if (!localFile.exists())
            {
                val inputStream = BufferedInputStream(smbFile.inputStream)
                val outputStream = FileOutputStream(localFile)

                val buffer = ByteArray(1024)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } > 0) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                inputStream.close()
                outputStream.close()
            }
            return localFile
        }


         fun openPDF(filePath: String, context: Context) {
            val file = File(filePath)
            if(file.exists() && file.isFile)
            {
                val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", file)
                val pdfIntent = Intent(Intent.ACTION_VIEW)
                pdfIntent.addCategory(Intent.CATEGORY_DEFAULT)
                pdfIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                pdfIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                pdfIntent.setDataAndType(uri, "application/pdf")
                try {
                    context.startActivity(pdfIntent)
                } catch (e: ActivityNotFoundException) {
                    // In case there is no PDF viewer app installed, handle the exception here.
                    // You can prompt the user to install a PDF viewer app from the Play Store.
                    // For example:
                    ToastHelper.showToast(context,"There is no PDF application")
                }
            }
            else
            {
                ToastHelper.showToast(context,"File path not found")
            }
        }
    }
}