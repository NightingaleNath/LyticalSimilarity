package com.codelytical.lyticalrecognition

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import com.codelytical.lyticalrecognition.faceantispoofing.FaceAntiSpoofing
import com.codelytical.lyticalrecognition.model.AntiSpoofingResult

class FaceLiveliness (private val context: Context) {

    private var fas: FaceAntiSpoofing? = FaceAntiSpoofing(context.assets)

    fun checkAntiSpoofing(bitmap: Bitmap?): AntiSpoofingResult? {
        if (bitmap == null) {
            Toast.makeText(context, "Bitmap is null, please provide a valid bitmap.", Toast.LENGTH_LONG).show()
            return null
        }

        return try {
            val laplacian = fas!!.laplacian(bitmap)
            if (laplacian < FaceAntiSpoofing.LAPLACIAN_THRESHOLD) {
                AntiSpoofingResult(false, 0f, "Image not clear enough")
            } else {
                val start = System.currentTimeMillis()
                val score = fas!!.antiSpoofing(bitmap)
                val end = System.currentTimeMillis()
                val isLive = score < FaceAntiSpoofing.THRESHOLD
                val message = if (isLive) "Live" else "Spoof"
                AntiSpoofingResult(isLive, score, "$message, Time taken: ${end - start}ms")
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error during anti-spoofing check: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            null
        }
    }

}