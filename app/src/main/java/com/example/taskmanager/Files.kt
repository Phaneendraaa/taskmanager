package com.example.taskmanager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast

/** Reads name, size and type of a file the user picked and keeps read access to it. */
fun readAttachment(context: Context, uri: Uri): Attachment {
    try {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (e: Exception) {
        // some providers do not allow persistable access; the file may need re-attaching later
    }
    var name = "File"
    var size = 0L
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val si = c.getColumnIndex(OpenableColumns.SIZE)
            if (c.moveToFirst()) {
                if (ni >= 0) name = c.getString(ni) ?: name
                if (si >= 0 && !c.isNull(si)) size = c.getLong(si)
            }
        }
    } catch (e: Exception) {
        // keep defaults
    }
    val mime = context.contentResolver.getType(uri) ?: ""
    return Attachment(uri.toString(), name, size, mime)
}

fun openAttachment(context: Context, att: Attachment) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(att.uri), if (att.mime.isNotEmpty()) att.mime else "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Can't open this file", Toast.LENGTH_SHORT).show()
    }
}
