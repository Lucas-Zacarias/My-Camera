package com.example.mycamera

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.mycamera.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(){


    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraBtn: ImageButton
    private lateinit var imgView: ImageView
    private lateinit var storageBtn : ImageButton
    private lateinit var resultLauncherForCamera : ActivityResultLauncher<Intent>
    private lateinit var resultLauncherForStorage : ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(R.layout.activity_main)

        getViews()
        setResultLauncher()
        setListeners()
    }


    private fun setListeners() {
        cameraBtn.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            resultLauncherForCamera.launch(intent)
        }

        storageBtn.setOnClickListener {
            resultLauncherForStorage.launch("image/*")
        }
    }

    private fun getViews() {
        cameraBtn = findViewById(R.id.cameraButton)
        imgView = findViewById(R.id.imageView)
        storageBtn = findViewById(R.id.storageButton)
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

}