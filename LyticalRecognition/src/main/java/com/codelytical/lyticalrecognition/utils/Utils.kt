package com.codelytical.lyticalrecognition.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap


fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
    try {
        // Check for and remove URI scheme if present
        val base64Image = base64Str.substringAfter("base64,")

        val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: IllegalArgumentException) {
        // Log the error or handle it as needed
        Log.e("ImageDecode", "Base64 string is not properly encoded: ${e.message}")
    } catch (e: Exception) {
        // General error handling, for any other unexpected errors
        Log.e("ImageDecode", "Error decoding Base64 string: ${e.message}")
    }
    return null
}

fun drawableToBitmap(context: Context, drawableId: Int): Bitmap {
    return ContextCompat.getDrawable(context, drawableId)?.toBitmap() ?: throw IllegalArgumentException("Drawable not found")
}
