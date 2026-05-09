package com.duck.photopicker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore

class RedirectActivity : Activity() {

    companion object {
        private const val REQUEST_CODE_DOCSUI = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val original = intent

            // Determine if multiple selection is allowed.
            // - PICK_IMAGES / USER_SELECT uses EXTRA_PICK_IMAGES_MAX (int)
            // - GET_CONTENT has no such extra → max = 1
            val max = original.getIntExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, 1)
            val allowMultiple = max > 1

            // Build explicit intent to DocumentsUI picker
            val docsIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                setClassName(
                    "com.android.documentsui",
                    "com.android.documentsui.picker.PickActivity"
                )
                // Preserve MIME type
                type = original.type

                // Tell DocumentsUI whether to allow multiple selection
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)

                // Forward any other extras the caller might have attached
                // (e.g., EXTRA_LOCAL_ONLY) but strip our intercepted extras
                val safeExtras = Bundle(original.extras).apply {
                    remove(MediaStore.EXTRA_PICK_IMAGES_MAX)
                    remove(Intent.EXTRA_ALLOW_MULTIPLE)
                }
                putExtras(safeExtras)
            }

            startActivityForResult(docsIntent, REQUEST_CODE_DOCSUI)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_DOCSUI) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                // The app that originally called us
                val callerPackage = callingPackage ?: packageName
                val result = Intent()

                // Grant persistent-ish read permission to the caller for all returned URIs
                val readFlag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                result.flags = readFlag

                // Check if DocumentsUI returned a ClipData (multiple URIs)
                if (data.clipData != null) {
                    val clip = data.clipData!!
                    for (i in 0 until clip.itemCount) {
                        clip.getItemAt(i).uri?.let { uri ->
                            grantUriPermission(callerPackage, uri, readFlag)
                        }
                    }
                    result.clipData = clip
                } else {
                    val singleUri = data.data
                    if (singleUri != null) {
                        grantUriPermission(callerPackage, singleUri, readFlag)
                        result.data = singleUri
                    }
                }

                setResult(Activity.RESULT_OK, result)
            } else {
                setResult(Activity.RESULT_CANCELED)
            }
            finish()
        }
    }
}