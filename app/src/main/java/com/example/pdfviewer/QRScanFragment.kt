package com.example.pdfviewer

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.zxing.Result
import com.hierynomus.mssmb2.SMBApiException
import com.hierynomus.protocol.transport.TransportException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.dm7.barcodescanner.zxing.ZXingScannerView

class QRScanFragment:  Fragment(), ZXingScannerView.ResultHandler {

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 101
    }

    var isPDFExist = false
    private lateinit var scannerView: ZXingScannerView
    lateinit var mainActivityViewModel: MainActivityViewModel
    var progressDialog: ProgressDialog?=null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mainActivityViewModel = MainActivityViewModel()
        mainActivityViewModel.error.observe(viewLifecycleOwner){
            if (it != null) {
                if(it.isNotEmpty()) {
                    hideProgressDialog()
                    scannerView.resumeCameraPreview(this)
                    Toast.makeText(requireContext(), it.toString(), Toast.LENGTH_SHORT).show()
                    mainActivityViewModel.removeError()
                }
            }
            else
            {
                hideProgressDialog()
            }
        }
        return inflater.inflate(R.layout.fragment_qr_code_scanner, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val frameLayout = view.findViewById<FrameLayout>(R.id.scanner_container)
        scannerView = ZXingScannerView(requireContext())
        frameLayout.addView(scannerView)

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request camera permission if not granted
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            // Camera permission granted, start the scanner
            startScanner()
        }
    }


    fun progressDialog(fileName: String)
    {
        progressDialog = ProgressDialog(requireContext())
        progressDialog?.setTitle("$fileName")
        progressDialog?.setCancelable(false)
        progressDialog?.setMessage("Loading PDF...")
        progressDialog?.show()
    }

    fun hideProgressDialog()
    {
        progressDialog?.hide()
    }

    private fun startScanner() {
        scannerView.setResultHandler(this)
        scannerView.startCamera()
    }

    override fun onResume() {
        super.onResume()
        startScanner()
    }

    override fun onPause() {
        super.onPause()
        scannerView.stopCamera()
    }

    override fun handleResult(result: Result) {
        // Handle the QR code result here
        if (result != null) {
            val qrCodeText = result.text
            Log.d("QRCodeScanner", "Scanned QR Code: $qrCodeText")
            val splitData = qrCodeText.toString().split("/")
            scannerView.stopCameraPreview()
            if(splitData.size == 2)
            {
                smbClientConnection(splitData[0], splitData[1])
            }
            else
            {
                mainActivityViewModel.getError("Invalid QR code")
            }
        }
//        // Resume scanning for more QR codes
//        scannerView.resumeCameraPreview(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted, start the scanner
                startScanner()
            } else {
                // Camera permission denied, handle the error or show a message to the user
                Toast.makeText(requireContext(), "Allow permission for camera access!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun smbClientConnection(folderName: String, fileName: String)
    {
        progressDialog(fileName)
        // Get an instance of SharedPreferences
        val sharedPreferences = context?.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        val username = sharedPreferences?.getString("username", "default_username")
        val password = sharedPreferences?.getString("password", "default_password")
        GlobalScope.launch(Dispatchers.IO) {
            val smb2Connection = SMB2Connection(Constants.server, username.toString(), password.toString(), Constants.shareName)
            try {
                smb2Connection.connect()
                try {
                    var files = smb2Connection.listFiles("${Constants.defaultFilePath}/${folderName}")
                    if (files != null) {
                        for (fileInfo in files) {
                            if(fileInfo.fileName.endsWith(".pdf", true) && fileName == fileInfo.fileName) {
                                isPDFExist = true
                                val smbFile = smb2Connection.openFile("${Constants.defaultFilePath}/${folderName}/${fileInfo.fileName}")
                                if (smbFile != null) {
                                    val documentsDirectory = requireActivity().getExternalFilesDir(
                                        Environment.DIRECTORY_DOWNLOADS)
                                    if (documentsDirectory != null) {
                                        if(documentsDirectory.exists()) {
                                            val filePDF = Utility.cachePDF(
                                                documentsDirectory.absolutePath,
                                                fileName,
                                                smbFile
                                            )
                                            mainActivityViewModel.getError(null)
                                            Utility.openPDF(filePDF.absolutePath,requireContext())
                                        }
                                    }
                                }
                            }
                            else
                            {
                                isPDFExist = false
                            }
                            Log.e("files","File Name: ${fileInfo.fileName}")
                        }
                    }

                    if(!isPDFExist)
                    {
                        mainActivityViewModel.getError("PDF file doesn't exist")
                    }
                }
                catch (e: SMBApiException)
                {
                    mainActivityViewModel.getError("Folder Does Not Exist")
                }

            } catch (e: TransportException) {
                e.printStackTrace()
            }
            catch (e: Exception) {
                // Handle any other general exception that might occur
                if(e is SMBApiException)
                {
                    if(e.statusCode == 3221225581)
                    {
                        mainActivityViewModel.getError("Authentication failed. Invalid username or password.")
                    }
                    else
                    {
                        mainActivityViewModel.getError("Url not found")
                    }
                }
                else
                {
                    mainActivityViewModel.getError("Something went wrong")
                }
            }
            finally {
                // Close the SMBClientConnection when done.
                smb2Connection.disconnect()
            }
        }
    }

}