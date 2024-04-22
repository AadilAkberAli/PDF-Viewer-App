package com.example.pdfviewer

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.example.pdfviewer.databinding.AcitivityPdfListBinding
import com.example.pdfviewer.databinding.ActivityQrBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter

class QRActivity: AppCompatActivity(){

    lateinit var binding: ActivityQrBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val qrCodeWidth = 400 // Set the desired width of the QR code
        val qrCodeHeight = 400 // Set the desired height of the QR code

       binding.btnGenerate.setOnClickListener {
           val qrCodeText = binding.etText.text.toString()
           val qrCodeBitmap = generateQRCode(qrCodeText, qrCodeWidth, qrCodeHeight)
           binding.imageCode.setImageBitmap(qrCodeBitmap)
       }
    }

    fun generateQRCode(text: String, width: Int, height: Int): Bitmap? {
        val qrCodeWriter = QRCodeWriter()
        try {
            val bitMatrix: BitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            return bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            return null
        }
    }
}