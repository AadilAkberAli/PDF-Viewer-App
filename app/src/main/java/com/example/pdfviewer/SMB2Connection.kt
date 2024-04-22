package com.example.pdfviewer
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.share.File
import java.util.*

class SMB2Connection(private val serverAddress: String, private val username: String, private val password: String, private val shareName: String) {

    private  var smbClient: SMBClient?=null
    private var session: Session?= null
    private  var share: DiskShare? = null

    fun connect() {
        smbClient = SMBClient()

        val connection = smbClient?.connect(serverAddress)
        val ac = AuthenticationContext(username, password.toCharArray(), "")
        session = connection?.authenticate(ac)

        share = session?.connectShare(shareName) as DiskShare
    }

    fun listFiles(directory: String): MutableList<FileIdBothDirectoryInformation>? {
        return share?.list(directory)
    }


    fun openFile(directory: String): File? {
        return share?.openFile(directory,
            EnumSet.of(AccessMask.GENERIC_READ),
            EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
            SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OPEN,
            null)
    }

    fun disconnect() {
        share?.close()
        session?.close()
        smbClient?.close()
    }
}
