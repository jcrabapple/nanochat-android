package com.nanogpt.chat.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * Extension function to copy text to clipboard
 */
fun Context.copyToClipboard(text: String, label: String = "Text") {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}

/**
 * Extension function to get text from clipboard
 */
fun Context.getFromClipboard(): CharSequence? {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    return clipboard.primaryClip?.getItemAt(0)?.text
}
