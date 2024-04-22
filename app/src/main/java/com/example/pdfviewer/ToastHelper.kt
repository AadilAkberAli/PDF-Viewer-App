package com.example.pdfviewer

import android.content.Context
import android.widget.Toast

object ToastHelper {
    fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, message, duration).show()
    }
}
