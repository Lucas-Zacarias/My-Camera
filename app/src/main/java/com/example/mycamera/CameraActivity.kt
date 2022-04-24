package com.example.mycamera

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.mycamera.databinding.ActivityCameraBinding
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraActivity : AppCompatActivity() {

    private var previewView : PreviewView? = null
    private var cameraProvider : ProcessCameraProvider? = null
    private var cameraSelector : CameraSelector? = null
    private var previewUseCase : Preview? = null
    private var qrAnalysisUseCase : ImageAnalysis? = null
    private var imageCapture: ImageCapture? = null
    private lateinit var binding:ActivityCameraBinding
    private lateinit var cameraTakePhotoBtn : ImageButton
    private  var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        getViews()
        requestPermissions(arrayOf(Manifest.permission.CAMERA),REQUEST_CODE_PERMISSIONS)
        setListeners()
    }

    private fun getViews() {
        previewView = binding.viewFinder
        cameraTakePhotoBtn = binding.cameraTakePhotoButton
    }

    private fun setListeners() {
        cameraTakePhotoBtn.setOnClickListener {
            takePhoto()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        //Set camera selector
        cameraSelector = CameraSelector.Builder().requireLensFacing(LENS_FACING_BACK).build()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()


            //Bind camera use cases
            bindCameraUseCases()

        },ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases(){
        bindPreviewUseCase()
        bindAnalyseUseCase()
        bindTakePhotoUseCase()
    }

    private fun bindPreviewUseCase() {
        if(cameraProvider == null){
            return
        }
        if(cameraProvider != null){
            cameraProvider!!.unbind(previewUseCase)
        }
        previewUseCase = Preview.Builder().setTargetRotation(previewView!!.display.rotation).build()
        previewUseCase!!.setSurfaceProvider(previewView!!.surfaceProvider)

        try {
            cameraProvider!!.bindToLifecycle(
                this,
                cameraSelector!!,
                previewUseCase
            )
        } catch (illegalStateException: IllegalStateException) {
            Log.e(TAG, illegalStateException.message ?: "IllegalStateException")
        } catch (illegalArgumentException: IllegalArgumentException) {
            Log.e(TAG, illegalArgumentException.message ?: "IllegalArgumentException")
        }
    }


    private fun bindAnalyseUseCase() {
        val options = BarcodeScannerOptions
            .Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        val barcodeScanner : BarcodeScanner  = BarcodeScanning.getClient(options)

        if(cameraProvider == null){
            return
        }
        if(cameraProvider != null){
            cameraProvider!!.unbind(qrAnalysisUseCase)
        }

        qrAnalysisUseCase = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(previewView!!.display.rotation)
            .build()

        qrAnalysisUseCase?.setAnalyzer(
            cameraExecutor,
            ImageAnalysis.Analyzer { imageProxy: ImageProxy ->
                processImageProxy(barcodeScanner,imageProxy)
            }
        )

        try {
            cameraProvider!!.bindToLifecycle(
                this,
                cameraSelector!!,
                qrAnalysisUseCase
            )
        } catch (illegalStateException: IllegalStateException) {
            Log.e(TAG, illegalStateException.message ?: "IllegalStateException")
        } catch (illegalArgumentException: IllegalArgumentException) {
            Log.e(TAG, illegalArgumentException.message ?: "IllegalArgumentException")
        }
    }

    private fun bindTakePhotoUseCase(){
        if(cameraProvider == null){
            return
        }
        if(cameraProvider != null){
            cameraProvider!!.unbind(imageCapture)
        }

        imageCapture = ImageCapture.Builder().build()

        try{
            cameraProvider!!.bindToLifecycle(
                this,
                cameraSelector!!,
                imageCapture
            )
        }catch (illegalStateException: IllegalStateException) {
            Log.e(TAG, illegalStateException.message ?: "IllegalStateException")
        } catch (illegalArgumentException: IllegalArgumentException) {
            Log.e(TAG, illegalArgumentException.message ?: "IllegalArgumentException")
        }
    }

    private fun takePhoto(){
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(barcodeScanner: BarcodeScanner, imageProxy: ImageProxy) {
        val imageInput = InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(imageInput).addOnSuccessListener { barcodes ->
            val barcode = barcodes.firstOrNull()
            if(barcode !=null){
                Toast.makeText(this, "URL: ${barcode.url!!.url}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {exception ->
            Log.e("EXCEPTIONN",   "${exception.message}")
        }.addOnCompleteListener {
            imageProxy.close()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}