package com.codelytical.lyticalrecognition

import android.content.Context
import android.graphics.Bitmap
import com.codelytical.lyticalrecognition.faceantispoofing.FaceAntiSpoofing
import com.codelytical.lyticalrecognition.mobilefacenet.MobileFaceNet
import com.codelytical.lyticalrecognition.model.AntiSpoofingResult
import com.codelytical.lyticalrecognition.model.FaceMatchResult
import com.codelytical.lyticalrecognition.model.NamedFace
import com.codelytical.lyticalrecognition.model.RecognitionResult

class FaceRecognitionManager(context: Context) {
    private val faceDetector = FaceDetector(context)
    private val faceComparator = FaceComparator(context)
    private val faceAntiSpoofing = FaceLiveliness(context)

    fun recognizeFaces(bitmap1: Bitmap, bitmap2: Bitmap): RecognitionResult {
        // Detect and crop faces from both bitmaps
        val cropResult1 = faceDetector.cropFace(bitmap1)
        val cropResult2 = faceDetector.cropFace(bitmap2)

        if (cropResult1 == null || cropResult2 == null) {
            return RecognitionResult(error = "Face detection failed in one or both images.")
        }

        // Compare the cropped faces
        val compareResult = faceComparator.compareFaces(cropResult1.croppedBitmap, cropResult2.croppedBitmap)
        return if (compareResult != null) {
            RecognitionResult(
                similarity = compareResult.similarity,
                isSamePerson = compareResult.isSamePerson,
                detectionTime = cropResult1.processingTime + cropResult2.processingTime,
                comparisonTime = compareResult.processingTime
            )
        } else {
            RecognitionResult(error = "Error during face comparison.")
        }
    }

    fun findMostSimilarFace(targetFace: Bitmap, facesList: List<NamedFace>): FaceMatchResult? {
        var highestSimilarityScore = 0.0f
        var mostSimilarFace: NamedFace? = null

        val targetCropped = faceDetector.cropFace(targetFace)?.croppedBitmap ?: return null

        for (namedFace in facesList) {
            val faceCropped = faceDetector.cropFace(namedFace.bitmap)?.croppedBitmap ?: continue
            val compareResult = faceComparator.compareFaces(targetCropped, faceCropped)

            if (compareResult != null && compareResult.similarity > highestSimilarityScore) {
                highestSimilarityScore = compareResult.similarity
                mostSimilarFace = namedFace
            }
        }

        return mostSimilarFace?.let {
            FaceMatchResult(highestSimilarityScore, highestSimilarityScore > MobileFaceNet.THRESHOLD, it.name)
        }
    }

    fun checkForSpoofing(bitmap: Bitmap): AntiSpoofingResult {
        val cropResult = faceDetector.cropFace(bitmap)
            ?: return AntiSpoofingResult(
                isLive = false,
                score = 0f,
                message = "No face detected, cannot perform anti-spoofing check."
            )

        val antiSpoofingScore = faceAntiSpoofing.checkAntiSpoofing(cropResult.croppedBitmap)
        return if (antiSpoofingScore != null) {
            AntiSpoofingResult(
                isLive = antiSpoofingScore.isLive,
                score = antiSpoofingScore.score,
                message = antiSpoofingScore.message
            )
        } else {
            AntiSpoofingResult(message = "Error during face comparison.", isLive = false, score = 0F)
        }

    }
}