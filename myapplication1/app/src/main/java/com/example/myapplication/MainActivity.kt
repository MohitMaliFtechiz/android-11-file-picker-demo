package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() {
    var isCopySelected = false
    val REQUEST_SELECT_FOLDER_PERMISSION = 4
    lateinit var title: TextView
    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val button: Button = findViewById(R.id.saveZimFile)
        val canReadZimFile: Button = findViewById(R.id.canReadZimFile)
        val canReadZimFile1: Button = findViewById(R.id.canReadZimFile1)
        val select_folder_to_save: Button = findViewById(R.id.select_folder_to_save)
        val previewWithCopy: Button = findViewById(R.id.previewWithCopy)
        title = findViewById(R.id.title)
        button.setOnClickListener {
            try {
                if (checkPermissions()) {
                    val loadFileStream =
                        assets.open("testzim.zim")
                    val zimFile = File(sharedPreferences.getString("STORE_PATH", ""), "testzim.zim")
                    if (zimFile.exists()) zimFile.delete()
                    zimFile.createNewFile()
                    loadFileStream.use { inputStream ->
                        val outputStream: OutputStream = FileOutputStream(zimFile)
                        outputStream.use { it ->
                            val buffer = ByteArray(inputStream.available())
                            var length: Int
                            while (inputStream.read(buffer).also { length = it } > 0) {
                                it.write(buffer, 0, length)
                            }
                            title.text = "File Saved at ${zimFile.path}"
                        }.also {
                            sharedPreferences.edit {
                                putString("path", zimFile.path)
                            }
                        }
                    }
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
        canReadZimFile.setOnClickListener {
            isCopySelected = false
            showFileChooser()
        }
        canReadZimFile1.setOnClickListener {
            isCopySelected = false
            showFileChooser()
        }
        previewWithCopy.setOnClickListener {
            isCopySelected = true
            showFileChooser()
        }

        select_folder_to_save.setOnClickListener {
            if (checkPermissions()) {
                selectFolder()
            }
        }
    }

    private fun showFileChooser() {
        val intent = Intent().apply {
            action = Intent.ACTION_OPEN_DOCUMENT
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        try {
            startActivityForResult(
                Intent.createChooser(intent, "Select a zim file"),
                100
            )
        } catch (ex: ActivityNotFoundException) {
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            100 -> {
                data?.data?.let { uri ->
                    val filePath = if (!isCopySelected) Constants.getPathFromUri(this, uri)
                    else BraverDocPathUtils.getSourceDocPath(this, uri)
                    title.text = "is this file read by app = ${File(filePath).canRead()}"
                    //openDocument(this, filepath)
                }
            }
            REQUEST_SELECT_FOLDER_PERMISSION -> {
                val storePath = data?.let {
                    getPathFromUri(this, it)
                }
                sharedPreferences.edit {
                    putString("STORE_PATH", storePath)
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun checkPermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    100
                )
            }
            return false
        } else {
            return true
        }
    }

    fun openDocument(context: Context, filePath: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".provider",
                File(filePath)
            )
            val intent = Intent(Intent.ACTION_VIEW)
            if (filePath.contains(".doc") || filePath.contains(".docx")) {
                intent.setDataAndType(uri, "application/msword")
            } else if (filePath.contains(".pdf")) {
                intent.setDataAndType(uri, "application/pdf")
            } else if (filePath.contains(".ppt") || filePath.contains(".pptx")) {
                intent.setDataAndType(uri, "application/vnd.ms-powerpoint")
            } else if (filePath.contains(".xls") || filePath.contains(".xlsx")) {
                intent.setDataAndType(uri, "application/vnd.ms-excel")
            } else if (filePath.contains(".zip") || filePath.contains(".rar")) {
                intent.setDataAndType(uri, "application/x-wav")
            } else if (filePath.contains(".rtf")) {
                intent.setDataAndType(uri, "application/rtf")
            } else if (filePath.contains(".wav") || filePath.contains(".mp3")) {
                intent.setDataAndType(uri, "audio/x-wav")
            } else if (filePath.contains(".gif")) {
                intent.setDataAndType(uri, "image/gif")
            } else if (filePath.contains(".jpg") || filePath.contains(".jpeg") || filePath.contains(
                    ".png"
                )
            ) {
                intent.setDataAndType(uri, "image/jpeg")
            } else if (filePath.contains(".txt")) {
                intent.setDataAndType(uri, "text/plain")
            } else if (filePath.contains(".3gp") || filePath.contains(".mpg") || filePath.contains(".mpeg") || filePath.contains(
                    ".mpe"
                ) || filePath.contains(".mp4") || filePath.contains(".avi")
            ) {
                intent.setDataAndType(uri, "video/*")
            } else {
                intent.setDataAndType(uri, "*/*")
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {

        }
    }

    private fun selectFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        )
        startActivityForResult(intent, REQUEST_SELECT_FOLDER_PERMISSION)
    }


    @SuppressLint("WrongConstant")
    fun getPathFromUri(
        activity: Activity,
        data: Intent
    ): String? {
        val uri: Uri? = data.data
        val takeFlags: Int = data.flags and (
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
        uri?.let {
            activity.grantUriPermission(
                activity.packageName, it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            activity.contentResolver.takePersistableUriPermission(it, takeFlags)

            val dFile = DocumentFile.fromTreeUri(activity, it)
            if (dFile != null) {
                dFile.uri.path?.let { file ->
                    val originalPath = file.substring(
                        file.lastIndexOf(":") + 1
                    )
                    val path = "${activity.getExternalFilesDirs("")[0]}"
                    return@getPathFromUri path.substringBefore(
                        "/Android"
                    )
                        .plus(File.separator).plus(originalPath)
                }
            }
            Log.e("TAG", "getPathFromUri: sysytem Unable to grant permission")
        } ?: run {
            Log.e("TAG", "getPathFromUri: sysytem Unable to grant permission")
        }
        return null
    }
}