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
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private val imagePickCode = 1000
    private val imageCaptureCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        imageView.setOnClickListener {
            val options = arrayOf("Usar Galería", "Tomar Foto")
            AlertDialog.Builder(this)
                .setTitle("Seleccionar Acción")
                .setItems(options) { dialog, which ->
                    when (which) {
                        0 -> checkGalleryPermissions()
                        1 -> checkCameraPermissions()
                    }
                }
                .show()
        }
    }
    private fun checkGalleryPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
            == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            requestStoragePermission()
        }
    }

    private fun checkCameraPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            takePicture()
        } else {
            requestCameraPermission()
        }
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
            imagePickCode)  // imagePickCode es un valor constante que define el código de solicitud del permiso de cámara
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.CAMERA),
            imageCaptureCode)  // cameraPermissionCode es un valor constante que define el código de solicitud del permiso de cámara
    }
    //permisos para leer
    private fun showExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permiso Requerido")
            .setMessage("Necesitamos acceso al almacenamiento para cargar imágenes desde la galería.")
            .setPositiveButton("OK") { dialog, which ->
                // Solicitar el permiso después de que el usuario entienda por qué es necesario
                requestStoragePermission()
            }
            .setNegativeButton("Cancelar") { dialog, which ->
                dialog.dismiss()
            }
            .create()
            .show()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d("Permissions", "requestCode: $requestCode, grantResults: ${grantResults.joinToString()}")
        if (requestCode == imagePickCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido
                openGallery()
            } else {
                // Permiso denegado
                Toast.makeText(this, "Permisos denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when {
            resultCode == Activity.RESULT_OK && requestCode == imagePickCode -> {
                val imageUri = data?.data  // Obtener la URI de la imagen
                imageView.setImageURI(imageUri)  // Mostrar la imagen seleccionada en el ImageView
            }
            resultCode == Activity.RESULT_OK && requestCode == imageCaptureCode -> {
                val imageBitmap = data?.extras?.get("data") as Bitmap
                imageView.setImageBitmap(imageBitmap)  // Mostrar la imagen de la cámara en el ImageView
            }
        }
    }
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, imagePickCode)
    }

    private fun takePicture(){
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, imageCaptureCode)
        } else {
            Toast.makeText(this, "No se encontró una aplicación de cámara", Toast.LENGTH_SHORT).show()
        }
    }
}
