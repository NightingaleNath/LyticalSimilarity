package com.codelytical.lyticalsimilarity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.codelytical.lyticalrecognition.FaceRecognitionManager
import com.codelytical.lyticalrecognition.model.NamedFace
import com.codelytical.lyticalrecognition.utils.drawableToBitmap
import com.codelytical.lyticalsimilarity.databinding.ActivityMainBinding
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private lateinit var faceRecognitionManager: FaceRecognitionManager

    private var bitmap1: Bitmap? = null
    private var bitmap2: Bitmap? = null

    private lateinit var imageCapture: ImageCapture

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        faceRecognitionManager = FaceRecognitionManager(this)

        binding.imageButton1.setOnClickListener {
            openGallery(1)
        }
        binding.imageButton2.setOnClickListener {
            openGallery(2)
        }

        binding.cropBtn.setOnClickListener {
            bitmap1?.let { it1 -> bitmap2?.let { it2 -> compareFaces(it1, it2) } }
        }

        val facesList = listOf(
            NamedFace("na", drawableToBitmap(this, R.drawable.na)),
            NamedFace("download", drawableToBitmap(this, R.drawable.download)),
            NamedFace("image", drawableToBitmap(this, R.drawable.image))
            // Continue for all faces
        )

        binding.btnCapture.setOnClickListener {
            takePhoto()
        }

        binding.compareBtn.setOnClickListener {
//            val mostSimilarFaceResult =
//                bitmap1?.let { targetBitmap -> faceRecognitionManager.findMostSimilarFace(targetBitmap, facesList) }
//
//            // Process the result
//            if (mostSimilarFaceResult != null) {
//                val similarityScore = mostSimilarFaceResult.similarity
//                val isMatch = similarityScore > MobileFaceNet.THRESHOLD
//                val imageName = mostSimilarFaceResult.name
//
//                if (isMatch) {
//                    binding.resultTextView.text = "Recognized face detected: $imageName with a similarity score of $similarityScore"
//                    Log.d("FaceComparison", "Recognized face detected: $imageName with a similarity score of $similarityScore")
//                } else {
//                    binding.resultTextView.text = "No recognized face detected. Closest match: $imageName with a similarity score of $similarityScore, but below the threshold."
//                    Log.d("FaceComparison", "No recognized face detected. Closest match: $imageName with a similarity score of $similarityScore, but below the threshold.")
//                }
//            } else {
//                Log.d("FaceComparison", "No similar face found or error occurred.")
//                // Handle error or notify user
//            }

            bitmap1?.let { it1 -> checkForSpoofing(it1) }

        }

    }

    private fun checkForSpoofing(bitmap: Bitmap) {
        val result = faceRecognitionManager.checkForSpoofing(bitmap)
        binding.resultTextView.text = "${result.message} (Score: ${result.score}) ${result.isLive}"
        // Adjust the TextView color based on whether it's live or spoof
        if (result.isLive) {
            binding.resultTextView.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            binding.resultTextView.setTextColor(getColor(android.R.color.holo_red_dark))
        }
    }

    private fun openGallery(requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.setType("image/*")
        startActivityForResult(intent, requestCode)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null && data.data != null) {
            val uri = data.data
            try {
                val selectedImage = rotateImageIfRequired(this, uri)
                if (requestCode == 1) {
                    binding.imageButton1.setImageBitmap(selectedImage)
                    bitmap1 = selectedImage
                } else if (requestCode == 2) {
                    binding.imageButton2.setImageBitmap(selectedImage)
                    bitmap2 = selectedImage
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @Throws(IOException::class)
    private fun rotateImageIfRequired(context: Context, selectedImageUri: Uri?): Bitmap {
        val inputStream = context.contentResolver.openInputStream(selectedImageUri!!)
        assert(inputStream != null)
        val exif = ExifInterface(inputStream!!)
        val orientation =
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> {
                inputStream.close()
                return MediaStore.Images.Media.getBitmap(context.contentResolver, selectedImageUri)
            }
        }
        val originalBitmap =
            MediaStore.Images.Media.getBitmap(context.contentResolver, selectedImageUri)
        val rotatedBitmap = Bitmap.createBitmap(
            originalBitmap,
            0,
            0,
            originalBitmap.width,
            originalBitmap.height,
            matrix,
            true
        )
        originalBitmap.recycle()
        inputStream.close()
        return rotatedBitmap
    }

    private fun compareFaces(bitmap1: Bitmap, bitmap2: Bitmap) {
        val recognitionResult = faceRecognitionManager.recognizeFaces(bitmap1, bitmap2)

        // Check if there was an error
        if (recognitionResult.error != null) {
            Toast.makeText(this, recognitionResult.error, Toast.LENGTH_LONG).show()
            binding.resultTextView.text = recognitionResult.error
        } else {
            // Process the recognition result
            val message = if (recognitionResult.isSamePerson == true) {
                "The faces are of the same person. Similarity: ${recognitionResult.similarity}"
            } else {
                "The faces are of different people. Similarity: ${recognitionResult.similarity}"
            }

            // Example of how you might display the result
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            binding.resultTextView.text = message
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch(exc: Exception) {
                Toast.makeText(this, "Failed to bind use cases", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun takePhoto() {
        val imageCapture = imageCapture

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            @OptIn(ExperimentalGetImage::class) override fun onCaptureSuccess(image: ImageProxy) {
                val rotationDegrees = image.imageInfo.rotationDegrees
                //val bitmap = image.toBitmap(rotationDegrees)
                runOnUiThread {
                    binding.imageButton1.apply {
                        setImageBitmap(image.image?.toBitmap(rotationDegrees))
                    }
                    bitmap1 = image.image?.toBitmap(rotationDegrees)
                }
                image.close() // Don't forget to close the ImageProxy
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraXApp", "Photo capture failed: ${exception.message}", exception)
            }
        })
    }

    fun Image.toBitmap(rotationDegrees: Int): Bitmap {
        val buffer = planes[0].buffer
        buffer.rewind()
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size).rotate(rotationDegrees)
    }

    // Helper function to convert ImageProxy to Bitmap
    /*fun ImageProxy.toBitmap(rotationDegrees: Int): Bitmap? {
        val yBuffer = planes[0].buffer // Y
        val uBuffer = planes[1].buffer // U
        val vBuffer = planes[2].buffer // V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size).rotate(rotationDegrees)
    }*/

    // Helper function to rotate a Bitmap
    fun Bitmap.rotate(degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

}









