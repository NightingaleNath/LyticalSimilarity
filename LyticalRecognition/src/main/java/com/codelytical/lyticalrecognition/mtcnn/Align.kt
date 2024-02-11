package com.codelytical.lyticalrecognition.mtcnn

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Point


object Align {
    /**
     * 仿射变换
     * @param bitmap 原图片
     * @param landmarks landmarks
     * @return 变换后的图片
     */
    fun face_align(bitmap: Bitmap, landmarks: Array<Point>): Bitmap {
        val diffEyeX = (landmarks[1].x - landmarks[0].x).toFloat()
        val diffEyeY = (landmarks[1].y - landmarks[0].y).toFloat()
        val fAngle: Float
        fAngle = if (Math.abs(diffEyeY) < 1e-7) {
            0f
        } else {
            (Math.atan((diffEyeY / diffEyeX).toDouble()) * 180.0f / Math.PI).toFloat()
        }
        val matrix = Matrix()
        matrix.setRotate(-fAngle)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}

