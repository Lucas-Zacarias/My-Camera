package com.example.mycamera

import android.Manifest
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.mycamera.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraBtn: ImageButton
    private lateinit var imgView: ImageView
    private lateinit var storageBtn : ImageButton
    private lateinit var resultLauncherForCamera : ActivityResultLauncher<Intent>
    private lateinit var resultLauncherForStorage : ActivityResultLauncher<String>
    private val STORAGE_COD = 1
    private  val CAMERA_COD = 2

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

     //   requestWriteExternalPermissionPermission()
        getViews()
        setResultLauncher()
        setListeners()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setListeners() {
        cameraBtn.setOnClickListener {
            requestCameraPermission()
        }

        storageBtn.setOnClickListener {
            resultLauncherForStorage.launch("image/*")
        }
    }

    private fun goToCamera() {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            resultLauncherForCamera.launch(intent)
    }

    private fun getViews() {
        cameraBtn = binding.cameraButton
          //  findViewById(R.id.cameraButton)
        imgView = binding.imageView
            //findViewById(R.id.imageView)
        storageBtn = binding.storageButton
           // findViewById(R.id.storageButton)
    }

    private fun setResultLauncher() {
        /*
        *  Trae el resultado de lo que haya capturado la camara y lo devuelve en el imageview de la activity
        */
        resultLauncherForCamera =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    imgView.setImageBitmap(data?.extras?.get("data") as Bitmap)
                }
            }

        /*
        *  Trae el resultado de lo que se haya elegido al abrir el apartado de almacenamiento de imágenes y lo devuelve
        * en el imageview de la activity
        */
        resultLauncherForStorage = registerForActivityResult(
            ActivityResultContracts.GetContent(),
            ActivityResultCallback {
                imgView.setImageURI(it)
            }
        )
    }

  /*  @RequiresApi(Build.VERSION_CODES.M)
    private fun requestWriteExternalPermissionPermission() {
        if (!hasWriteExternalStoragePermission()) {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun hasWriteExternalStoragePermission() =
        checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED*/

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestCameraPermission() {
        if (!hasCameraPermission()) {
           requestPermissions(arrayOf(Manifest.permission.CAMERA),CAMERA_COD)
        }
        goToCamera()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun hasCameraPermission() =
        checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(requestCode:Int, permissions:Array<String>, grantResults:IntArray){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == CAMERA_COD){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                goToCamera()
            }
        }
    }

}