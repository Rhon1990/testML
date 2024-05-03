package com.example.testmlandroid

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.YuvImage
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    private lateinit var ivPicture: ImageView
    private lateinit var tvResult: TextView
    private lateinit var btnChoosePicture: Button

    private val CAMERA_PERMISSION_CODE = 123
    private val READ_MEDIA_IMAGE_PERMISSION_CODE = 113
    private val WRITE_STORAGE_PERMISSION_CODE = 113

    private val TAG="Mi ML"

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>

    private lateinit var inputImage: InputImage

    private lateinit var imagelabeler: ImageLabeler
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ivPicture = findViewById(R.id.ivPicture)
        tvResult = findViewById(R.id.tvresult)
        btnChoosePicture = findViewById(R.id.btnChoosePicture)

        //me permite variar el porcentaje de confianza del etiquetado
        //en este caso que solo me acepte el porcentaje superior al 80%
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.8f)
            .build()

        imagelabeler = ImageLabeling.getClient(options)

        cameraLauncher=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result?.data
            try {
                val photo = data?.extras?.get("data") as Bitmap
                ivPicture.setImageBitmap(photo)
                inputImage = InputImage.fromBitmap(photo, 0)
                processImage()
            } catch (e: Exception) {
                Log.d(TAG, "onActivityResult: ${e.message}")
            }
        }

        galleryLauncher=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            try {
                inputImage = InputImage.fromFilePath(this@MainActivity, data?.data!!)
                ivPicture.setImageURI(data?.data)
                processImage()
            } catch (e: Exception) {

            }
        }

        btnChoosePicture.setOnClickListener {
            val options = arrayOf("Camara", "Galeria")
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Seleccione una opción")

            builder.setItems(options, DialogInterface.OnClickListener{
                dialog, which ->
                if(which==0){
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    cameraLauncher.launch(cameraIntent)
                }else{
                    val storageIntent = Intent()
                    storageIntent.setType("image/*")
                    storageIntent.setAction(Intent.ACTION_GET_CONTENT)
                    galleryLauncher.launch(storageIntent)
                }
            })
            builder.show()
        }
    }

    private fun processImage() {
        imagelabeler.process(inputImage)
            .addOnSuccessListener {
                var result = ""
                for (label in it) {
                    result = result + "\n"+ label.text + " -> " + label.confidence * 100 + "%"
                }
                tvResult.text = result
            }.addOnFailureListener {
                Log.d(TAG, "procesando Imagen: ${it.message}")
            }
    }

    override fun onResume() {
        super.onResume()
        checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE)
    }

    private fun checkPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(this, permission)
            == PackageManager.PERMISSION_DENIED) {
           ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE)
            } else {
                // Permiso denegado
                Toast.makeText(this, "CAMARA Permisos denegado", Toast.LENGTH_SHORT).show()
            }
        } else if(requestCode == READ_MEDIA_IMAGE_PERMISSION_CODE){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkPermission(Manifest.permission.READ_MEDIA_IMAGES, READ_MEDIA_IMAGE_PERMISSION_CODE)
            } else {
                // Permiso denegado
                Toast.makeText(this, "GALERIA Permisos denegado", Toast.LENGTH_SHORT).show()
            }
        } else if(requestCode == WRITE_STORAGE_PERMISSION_CODE){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_STORAGE_PERMISSION_CODE)
            } else {
                // Permiso denegado
                Toast.makeText(this, "ESCRIBIR Permisos denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
