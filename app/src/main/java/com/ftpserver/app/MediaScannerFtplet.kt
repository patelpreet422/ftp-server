package com.ftpserver.app

import android.content.Context
import android.media.MediaScannerConnection
import org.apache.ftpserver.ftplet.DefaultFtplet
import org.apache.ftpserver.ftplet.FtpRequest
import org.apache.ftpserver.ftplet.FtpSession
import org.apache.ftpserver.ftplet.FtpletResult
import java.io.File

/**
 * Ftplet that triggers Android's MediaScanner after file operations so that
 * files uploaded, created, deleted, or renamed via FTP become immediately
 * visible in the Android filesystem (Files app, Gallery, etc.).
 */
class MediaScannerFtplet(
    private val context: Context,
    private val homeDirectory: String
) : DefaultFtplet() {

    private fun resolveActualPath(session: FtpSession, argument: String?): String? {
        return try {
            val ftpFile = session.fileSystemView.getFile(argument ?: return null) ?: return null
            homeDirectory + ftpFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun scanPath(path: String) {
        MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
    }

    private fun scanFile(session: FtpSession, argument: String?) {
        val path = resolveActualPath(session, argument) ?: return
        scanPath(path)
    }

    private fun scanParentDirectory(session: FtpSession, argument: String?) {
        val path = resolveActualPath(session, argument) ?: return
        val parent = File(path).parent ?: return
        scanPath(parent)
    }

    override fun onUploadEnd(session: FtpSession, request: FtpRequest): FtpletResult {
        scanFile(session, request.argument)
        return super.onUploadEnd(session, request)
    }

    override fun onUploadUniqueEnd(session: FtpSession, request: FtpRequest): FtpletResult {
        try {
            val cwd = session.fileSystemView.workingDirectory
            scanPath(homeDirectory + cwd.absolutePath)
        } catch (_: Exception) { }
        return super.onUploadUniqueEnd(session, request)
    }

    override fun onAppendEnd(session: FtpSession, request: FtpRequest): FtpletResult {
        scanFile(session, request.argument)
        return super.onAppendEnd(session, request)
    }

    override fun onMkdirEnd(session: FtpSession, request: FtpRequest): FtpletResult {
        scanFile(session, request.argument)
        return super.onMkdirEnd(session, request)
    }

    override fun onDeleteEnd(session: FtpSession, request: FtpRequest): FtpletResult {
        scanParentDirectory(session, request.argument)
        return super.onDeleteEnd(session, request)
    }

    override fun onRmdirEnd(session: FtpSession, request: FtpRequest): FtpletResult {
        scanParentDirectory(session, request.argument)
        return super.onRmdirEnd(session, request)
    }

    override fun onRenameEnd(session: FtpSession, request: FtpRequest): FtpletResult {
        scanFile(session, request.argument)
        return super.onRenameEnd(session, request)
    }
}
