package com.codelytical.lyticalrecognition

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import com.codelytical.lyticalrecognition.model.CropResult
import com.codelytical.lyticalrecognition.mtcnn.Align
import com.codelytical.lyticalrecognition.mtcnn.MTCNN
import com.codelytical.lyticalrecognition.utils.MyUtil

class FaceDetector(private val context: Context) {

    private var mtcnn: MTCNN? = MTCNN(context.assets)

    fun cropFace(bitmap: Bitmap): CropResult? {
        return try {
            var bitmapTemp = bitmap.copy(bitmap.config, false)
            val start = System.currentTimeMillis()
            var boxes = mtcnn!!.detectFaces(bitmapTemp, bitmapTemp.width / 5)
            if (boxes.isEmpty()) {
                Toast.makeText(context, "No face detected", Toast.LENGTH_LONG).show()
                return null
            }
            val box = boxes[0]
            bitmapTemp = Align.face_align(bitmapTemp, box.landmark)
            boxes = mtcnn!!.detectFaces(bitmapTemp, bitmapTemp.width / 5)
            val boxAdjusted = boxes[0].apply {
                toSquareShape()
                limitSquare(bitmapTemp.width, bitmapTemp.height)
            }
            val rect = boxAdjusted.transform2Rect()
            val croppedBitmap = MyUtil.crop(bitmapTemp, rect)
            val end = System.currentTimeMillis()
            CropResult(croppedBitmap, end - start)
        } catch (e: Exception) {
            Toast.makeText(context, "Error during face cropping: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            null
        }
    }
}