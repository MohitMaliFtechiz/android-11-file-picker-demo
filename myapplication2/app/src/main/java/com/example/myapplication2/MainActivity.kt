package com.example.myapplication2

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() {
    val REQUEST_SELECT_FOLDER_PERMISSION = 4
    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val button: Button = findViewById(R.id.saveZimFile)
        val canReadZimFile: Button = findViewById(R.id.canReadZimFile)
        val canReadZimFile1: Button = findViewById(R.id.canReadZimFile1)
        val select_folder_to_save: Button = findViewById(R.id.select_folder_to_save)
        val title: TextView = findViewById(R.id.title)
        button.setOnClickListener {
            if (checkPermissions()) {
                val loadFileStream =
                    assets.open("testzim.zim")
                val zimFile = File(sharedPreferences.getString("STORE_PATH", ""), "testzim1.zim")
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
        }
        canReadZimFile.setOnClickListener {
            val file = File(sharedPreferences.getString("path", "") + "1")
            title.text = "is this file read by app = ${file.canRead()}"
        }
        canReadZimFile1.setOnClickListener {
            val file = File(sharedPreferences.getString("path", ""))
            title.text = "is this file read by app = ${file.canRead()}"
        }
        select_folder_to_save.setOnClickListener {
            if (checkPermissions()) {
                selectFolder()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
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