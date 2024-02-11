package com.codelytical.lyticalrecognition

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import com.codelytical.lyticalrecognition.mobilefacenet.MobileFaceNet
import com.codelytical.lyticalrecognition.model.CompareResult

class FaceComparator(private val context: Context) {

    private var mfn: MobileFaceNet? = MobileFaceNet(context.assets)

    fun compareFaces(bitmap1: Bitmap, bitmap2: Bitmap): CompareResult? {
        return try {
            if (mfn == null) {
                Toast.makeText(context, "MobileFaceNet not initialized", Toast.LENGTH_LONG).show()
                return null
            }
            val start = System.currentTimeMillis()
            val similarity = mfn!!.compare(bitmap1, bitmap2)
            val end = System.currentTimeMillis()
            CompareResult(similarity, similarity > MobileFaceNet.THRESHOLD, end - start)
        } catch (e: Exception) {
            Toast.makeText(context, "Error during face comparison: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            null
        }
    }
}