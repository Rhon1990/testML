package com.example.testmlandroid

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

class MainActivity : AppCompatActivity() {

    // Componentes de la interfaz de usuario
    private lateinit var ivPicture: ImageView
    private lateinit var tvResult: TextView
    private lateinit var btnChoosePicture: Button

    // Códigos de permisos
    private val CAMERA_PERMISSION_CODE = 123
    private val READ_MEDIA_IMAGE_PERMISSION_CODE = 113
    private val WRITE_STORAGE_PERMISSION_CODE = 113

    // Tag para logging
    private val TAG = "Mi ML"

    // Lanzadores de resultados de actividades
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>

    // Componentes de ML Kit
    private lateinit var inputImage: InputImage
    private lateinit var imageLabeler: ImageLabeler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar componentes de la interfaz de usuario
        ivPicture = findViewById(R.id.ivPicture)
        tvResult = findViewById(R.id.tvresult)
        btnChoosePicture = findViewById(R.id.btnChoosePicture)

        // Registrar lanzadores de resultados de actividades
        setupActivityResultLaunchers()

        // Establecer listener de clic para el botón
        btnChoosePicture.setOnClickListener {
            showImageSourceDialog()
        }
    }

    // Configurar los lanzadores de resultados de actividades
    private fun setupActivityResultLaunchers() {
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleCameraResult(result)
        }

        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleGalleryResult(result)
        }
    }

    // Manejar el resultado de la cámara
    private fun handleCameraResult(result: ActivityResult?) {
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

    // Manejar el resultado de la galería
    private fun handleGalleryResult(result: ActivityResult?) {
        val data = result?.data
        try {
            inputImage = InputImage.fromFilePath(this@MainActivity, data?.data!!)
            ivPicture.setImageURI(data?.data)
            processImage()
        } catch (e: Exception) {
            Log.d(TAG, "handleGalleryResult: ${e.message}")
        }
    }

    // Mostrar un diálogo para elegir la fuente de la imagen
    private fun showImageSourceDialog() {
        val options = arrayOf("Camara", "Galeria")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Seleccione una opción")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> launchCamera()
                1 -> launchGallery()
            }
        }
        builder.show()
    }

    // Lanzar la intención de la cámara
    private fun launchCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(cameraIntent)
    }

    // Lanzar la intención de la galería
    private fun launchGallery() {
        val storageIntent = Intent()
        storageIntent.setType("image/*")
        storageIntent.setAction(Intent.ACTION_GET_CONTENT)
        galleryLauncher.launch(storageIntent)
    }

    // Procesar la imagen seleccionada con ML Kit
    private fun processImage() {
        // 1. Configurar opciones del etiquetador de imágenes
        //me permite variar el porcentaje de confianza del etiquetado
        //en este caso que solo me acepte el porcentaje superior al 60%
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.6f)
            .build()
        //2. Crea una instancia del etiquetador de imagenes con las opciones configuradas anteriormente
        imageLabeler = ImageLabeling.getClient(options)

        //3. Procesamiento de la imagen
        imageLabeler.process(inputImage)
            .addOnSuccessListener { labels ->
                //si el procesamiento es exitoso, recorrer las etiquetas generadas
                var result = ""
                for (label in labels) {
                    //concatenamos el texto de la etiqueta y su nivel de confianza en porcentaje
                    result += "\n${label.text} -> ${label.confidence * 100}%"
                }
                //mostramos resultados
                tvResult.text = result
            }
            .addOnFailureListener { e ->
                //si el proceso falla, registrar el error en los logs
                Log.d(TAG, "procesando Imagen: ${e.message}")
            }
    }

    override fun onResume() {
        super.onResume()
        checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE)
    }

    // Verificar y solicitar los permisos necesarios
    private fun checkPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                handlePermissionResult(grantResults, "CAMARA")
            }
            READ_MEDIA_IMAGE_PERMISSION_CODE -> {
                handlePermissionResult(grantResults, "GALERIA")
            }
            WRITE_STORAGE_PERMISSION_CODE -> {
                handlePermissionResult(grantResults, "ESCRIBIR")
            }
        }
    }

    // Manejar el resultado de la solicitud de permisos
    private fun handlePermissionResult(grantResults: IntArray, permissionName: String) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE)
        } else {
            Toast.makeText(this, "$permissionName Permisos denegado", Toast.LENGTH_SHORT).show()
        }
    }
}