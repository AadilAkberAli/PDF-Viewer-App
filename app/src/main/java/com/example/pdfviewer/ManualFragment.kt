package com.example.pdfviewer

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.pdfviewer.Utility.Companion.cachePDF
import com.example.pdfviewer.Utility.Companion.isInternetAvailable
import com.example.pdfviewer.databinding.AcitivityPdfListBinding
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.mssmb2.SMBApiException
import com.hierynomus.protocol.transport.TransportException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class ManualFragment : Fragment(){

    lateinit var mainActivityViewModel: MainActivityViewModel
    lateinit var binding: AcitivityPdfListBinding
    var pdfFile = ArrayList<File>()
    val folders = ArrayList<String>()
    var username = ""
    var password = ""
    var fileName = ""
    var folderNamed =""
    var isPDFExist = false
    lateinit var adapter: CustomAdapter
    var fileadapter: CustomFileAdapter?=null
    // This method is called when the fragment is created

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = AcitivityPdfListBinding.inflate(inflater, container, false)
        val root: View = binding.root
        mainActivityViewModel = MainActivityViewModel()
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        binding.noItem.visibility = View.VISIBLE
        binding.noItem.text = "No Folder Selected"
        binding.filePath.setOnClickListener {
            binding.noItem.visibility = View.GONE
            binding.pdfListView.visibility = View.VISIBLE
            binding.pdfFileListView.visibility = View.GONE
            binding.filePath.visibility = View.GONE
            adapter = CustomAdapter(requireContext(),folders)
            binding.pdfListView.adapter = adapter
        }

        mainActivityViewModel.folders.observe(viewLifecycleOwner)
        {
            if(it != null)
            {
                binding.filePath.visibility = View.GONE
                binding.noItem.visibility = View.GONE
                binding.progressBar.visibility = View.GONE
                binding.pdfListView.visibility = View.VISIBLE
                binding.pdfFileListView.visibility = View.GONE
                adapter = CustomAdapter(requireContext(),it)
                binding.pdfListView.adapter = adapter
                binding.pdfListView.setOnItemClickListener { _, _, position, _ ->
                    // Handle item click. You can open the selected PDF file here.
                    // Example: openPdfFile(pdfFiles[position])
                    folderNamed = adapter.getItemAtParticularPosition(position)
                    binding.progressBar.visibility = View.VISIBLE
                    binding.pdfListView.visibility = View.GONE
                    binding.noItem.visibility = View.GONE
                    mainActivityViewModel.removeObserver()
                    smbClientConnection("File")
                }
            }
        }

        mainActivityViewModel.error.observe(viewLifecycleOwner){
            if (it != null) {
                if(it.isNotEmpty()) {
                    binding.progressBar.visibility = View.GONE
                    binding.noItem.visibility = View.VISIBLE
                    binding.noItem.text = it.toString()
                    binding.pdfListView.visibility = View.GONE
                    binding.filePath.visibility = View.VISIBLE
                    binding.filePath.text = "$folderNamed"
                }
            }
        }
        mainActivityViewModel.filename.observe(viewLifecycleOwner){
            if(it.isNotEmpty())
            {
                binding.filePath.visibility = View.VISIBLE
                binding.filePath.text = "$folderNamed"
                binding.noItem.visibility = View.GONE
                binding.progressBar.visibility = View.GONE
                binding.pdfListView.visibility = View.GONE
                binding.pdfFileListView.visibility = View.VISIBLE
                fileadapter = CustomFileAdapter(requireContext(), it)
                binding.pdfFileListView.adapter = fileadapter
                binding.pdfFileListView.setOnItemClickListener { _, _, position, _ ->
                    // Handle item click. You can open the selected PDF file here.
                    // Example: openPdfFile(pdfFiles[position])
                    if (fileadapter != null)
                    {
                        fileadapter?.getItemAtParticularPosition(position)
                            ?.let { it1 -> Utility.openPDF(it1.absolutePath,requireContext()) }
                    }
                }
            }
            else if(it.size == 0)
            {
                binding.progressBar.visibility = View.GONE
                binding.noItem.visibility = View.VISIBLE
                binding.pdfListView.visibility = View.GONE

            }
        }
        accessRemoteFileSMB2()
        return  root
    }

    fun performSearch(query: String)
    {
        adapter.filterList(query)
        fileadapter?.filterList(query)
    }

    fun accessRemoteFileSMB2()
    {
        if(isInternetAvailable(requireContext()))
        {
            credentialDialog()
        }
        else
        {
            binding.progressBar.visibility = View.GONE
            getUndismissableAlert(requireContext(), "No Internet","Internet is not available. please check internet and then retry",{accessRemoteFileSMB2()},{ activity?.finish()},"Retry", "Exit")
        }
    }

    fun credentialDialog()
    {
        val dialogBuilder = AlertDialog.Builder(requireContext())
        val alertDialog = dialogBuilder.create()
        alertDialog.show()
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.credential_dialog, null)
        alertDialog.window?.setContentView(dialogView)
        alertDialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        alertDialog.setCancelable(false)
        val buttonSubmit= dialogView.findViewById<View>(R.id.buttonSubmit) as Button
        val userName = dialogView.findViewById<View>(R.id.userName) as EditText
        val paSSword = dialogView.findViewById<View>(R.id.password) as EditText
        buttonSubmit.setOnClickListener {
            if(userName.text.toString().isNotEmpty() && paSSword.text.toString().isNotEmpty())
            {
                val sharedPreferences = context?.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
                val editor = sharedPreferences?.edit()
                username = userName.text.toString()
                password = paSSword.text.toString()
                editor?.putString("username", username)
                editor?.putString("password", password)
                editor?.apply()
                alertDialog.dismiss()
                getFolderName()
            }
        }
    }

    fun getFolderName()
    {
        pdfFile.clear()
        binding.progressBar.visibility = View.VISIBLE
        binding.pdfListView.visibility = View.GONE
        binding.noItem.visibility = View.GONE
        smbClientConnection("Folder")
    }

    fun isDirectory(fileInfo: FileIdBothDirectoryInformation): Boolean {
        val fileAttributes = fileInfo.fileAttributes.toInt()
        val directoryFlag = 0x10 // This is the value of the FILE_ATTRIBUTE_DIRECTORY flag (0x10)
        return (fileAttributes and directoryFlag) != 0 && !fileInfo.fileName.equals(".") && !fileInfo.fileName.equals("..")
    }


    private fun smbClientConnection(type: String)
    {
        GlobalScope.launch(Dispatchers.IO) {
            val smb2Connection = SMB2Connection(Constants.server, username, password, Constants.shareName)
            try {
                smb2Connection.connect()
                try {
                            if(type == "Folder")
                            {
                                smb2Connection.listFiles("${Constants.defaultFilePath}")?.forEach {
                                    if(isDirectory(it)) {
                                        folders.add(it.fileName)
                                    }
                                }
                            }
                            else {
                                pdfFile = ArrayList()
                                smb2Connection.listFiles("${Constants.defaultFilePath}/$folderNamed")?.forEach {
                                    if(it.fileName.endsWith(".pdf", true)) {
                                        isPDFExist = true
                                        // Print the names of the files in the shared folder.
                                        val smbFile = smb2Connection.openFile("${Constants.defaultFilePath}/${folderNamed}/${it.fileName}")
                                        if (smbFile != null) {
                                            writePDF(it.fileName, smbFile)
                                        }
                                    }
                                    else
                                    {
                                        isPDFExist = false
                                    }
                                }
                                if(!isPDFExist)
                                {
                                    mainActivityViewModel.getError("PDF file doesn't exist")
                                }
                                if(pdfFile.size > 0)
                                {
                                    mainActivityViewModel.addFiles(pdfFile)
                                }
                            }


                    if(type == "Folder")
                    {
                        if(folders.size > 0)
                        {
                            mainActivityViewModel.addFolders(folders)
                        }
                        else
                        {
                            mainActivityViewModel.getError("No Folder exist in this directory")
                        }
                    }
                }
                catch (e: SMBApiException)
                {
                    if(type == "Folder")
                    {
                        mainActivityViewModel.getError("Folder Does Not Exist")
                    }
                    else
                    {
                        mainActivityViewModel.getError("No PDF Found")
                    }
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

        }
    }

    private fun writePDF(
        fileName: String,
        smbFile: com.hierynomus.smbj.share.File
    ) {
        val documentsDirectory = requireActivity().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (documentsDirectory != null) {
            if(documentsDirectory.exists()) {
                val filePDF = cachePDF(documentsDirectory.absolutePath,fileName, smbFile)
                pdfFile.add(filePDF)
            }
        }

    }

    fun getUndismissableAlert(
        context: Context,
        title:String,
        message:String,
        onPositiveButton: () -> Unit,
        onNegativeButton: () -> Unit,
        positiveText: String,
        negativeText: String) : AlertDialog.Builder
    {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message )
        builder.setCancelable(false)
        builder.setPositiveButton(positiveText) { dialog, _ ->
            onPositiveButton()
            dialog.dismiss()
        }

        builder.setNegativeButton(negativeText) { dialog, _ ->
            onNegativeButton()
            dialog.dismiss()
        }
        builder.show()
        return builder
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2296) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
//                    displayPdfFiles()
//                    accessRemoteFile()
                    accessRemoteFileSMB2()
                } else {
                    Toast.makeText(requireContext(), "Allow permission for storage access!", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }




}