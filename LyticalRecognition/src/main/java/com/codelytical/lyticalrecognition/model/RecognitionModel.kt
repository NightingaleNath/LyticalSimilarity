package com.codelytical.lyticalrecognition.model

import android.graphics.Bitmap

data class CropResult(
    val croppedBitmap: Bitmap,
    val processingTime: Long
)

data class CompareResult(
    val similarity: Float,
    val isSamePerson: Boolean,
    val processingTime: Long
)

data class RecognitionResult(
    val similarity: Float? = null,
    val isSamePerson: Boolean? = null,
    val detectionTime: Long? = null,
    val comparisonTime: Long? = null,
    val error: String? = null
)

data class NamedFace(val name: String, val bitmap: Bitmap)

data class FaceMatchResult(
    val similarity: Float,
    val isMatch: Boolean,
    val name: String
)

data class AntiSpoofingResult(val isLive: Boolean, val score: Float, val message: String)