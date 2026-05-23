package com.duck.photopicker

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore

/**
 * A transparent redirector that intercepts all image/video picking intents
 * and delegates to the standard DocumentsUI picker (or its trampoline).
 *
 * This activity never appears in recents, never shows a UI of its own,
 * and terminates immediately after returning the chosen URI(s) to the caller.
 */
class RedirectActivity : Activity() {

    companion object {
        private const val REQUEST_CODE_DOCSUI = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ----------------------------------------------------------------
        // GUARD: If we are being re‑created after process death,
        // DocumentsUI is already running (or has finished). We must not
        // launch a second picker, otherwise we'd stack multiple pickers
        // and lose the result chain.
        // ----------------------------------------------------------------
        if (savedInstanceState != null) {
            return
        }

        val original = intent

        // Determine which action we are intercepting so we can choose the
        // correct delegation strategy (trampoline vs. direct picker).
        val isGetContent = Intent.ACTION_GET_CONTENT == original.action

        // ----------------------------------------------------------------
        // BUG #1 (Opera Mini malformed extras) and general robustness:
        // Some apps send a Bundle whose internal lock object is null.
        // Copying such a Bundle via its copy constructor throws an NPE.
        // We wrap the copy in a try‑catch; if it fails, we fall back to
        // an empty Bundle, which is always safe.
        // ----------------------------------------------------------------
        fun copyExtrasSafely(source: Bundle?): Bundle {
            if (source == null) return Bundle()
            return try {
                Bundle(source)        // may throw NPE on corrupted source
            } catch (e: NullPointerException) {
                // Rare: the source Bundle is non‑null but internally corrupted.
                Bundle()
            }
        }

        // Build the explicit intent that will start DocumentsUI.
        val docsIntent: Intent

        if (isGetContent) {
            // ----------------------------------------------------------------
            // BUG #2 (trampoline bypass for ACTION_GET_CONTENT):
            // When the original action is ACTION_GET_CONTENT, the standard
            // flow is: caller → DocumentsUI TrampolineActivity → PickActivity.
            // The trampoline sets up the correct task stack, forwards
            // essential flags (e.g., FLAG_GRANT_READ_URI_PERMISSION),
            // and ensures the result is routed back to the original caller.
            //
            // Our earlier code launched PickActivity directly, skipping the
            // trampoline. This caused the picker to run in an inconsistent
            // task state, broke URI permission grants, and made the result
            // delivery unreliable, especially when the user switched to
            // another file browser inside DocumentsUI.
            //
            // FIX: Re‑use the exact incoming intent (including all flags,
            // extras, and the MIME type), but replace its component with the
            // trampoline. The trampoline will then do the rest correctly.
            // Because we set an explicit component, the intent resolver is
            // not consulted, so there is no risk of recursion back to us.
            // ----------------------------------------------------------------
            docsIntent = Intent(original).apply {
                component = ComponentName(
                    "com.android.documentsui",
                    "com.android.documentsui.picker.TrampolineActivity"
                )
            }
        } else {
            // ----------------------------------------------------------------
            // For PICK_IMAGES and USER_SELECT_IMAGES_FOR_APP, there is no
            // trampoline in the DocumentsUI protocol. These actions are
            // designed to be delivered directly to a picker activity.
            // We therefore construct a fresh ACTION_GET_CONTENT intent
            // targeted at the real PickActivity, and we manually copy
            // the relevant extras (max count, allow multiple, etc.).
            // ----------------------------------------------------------------
            val max = original.getIntExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, 1)
            val allowMultiple = max > 1

            docsIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                setClassName(
                    "com.android.documentsui",
                    "com.android.documentsui.picker.PickActivity"
                )
                type = original.type
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)

                // Forward other extras that the caller attached,
                // but remove the ones we are handling ourselves to avoid conflicts.
                val safeExtras = copyExtrasSafely(original.extras).apply {
                    remove(MediaStore.EXTRA_PICK_IMAGES_MAX)
                    remove(Intent.EXTRA_ALLOW_MULTIPLE)
                }
                putExtras(safeExtras)
            }
        }

        startActivityForResult(docsIntent, REQUEST_CODE_DOCSUI)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_DOCSUI) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                // Identify the app that originally called us.
                val callerPackage = callingPackage ?: packageName
                val result = Intent()

                // Grant the caller persistent read access to every returned URI.
                val readFlag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                result.flags = readFlag

                // Handle both single and multiple URI selections.
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
            // The redirector has done its job; it must disappear now.
            finish()
        }
    }
}
