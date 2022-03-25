package com.example.storage

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.example.storage.ui.theme.StorageTheme
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    lateinit var firebaseStorage: FirebaseStorage

    @SuppressLint("CoroutineCreationDuringComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseStorage = Firebase.storage

        setContent {
            StorageTheme {
                val check = remember { mutableStateOf(false) }
                val activityResult = activityResultLauncher(check)
                val scope = rememberCoroutineScope()
                val images = remember { mutableStateOf(listOf<String>()) }
                val image:MutableState<Bitmap?> = remember { mutableStateOf(null) }
                val downloading = remember { mutableStateOf(false) }
                if (check.value){
                    activityResult.launch("image/*")
                }
                if (downloading.value){
                    downloading(bitmap = image, boolean = downloading)
                }

                Column {
                    OutlinedButton(onClick = {
                        check.value = true
                    }) {
                        Text(text = "Push")
                    }

                    OutlinedButton(onClick = {
                        downloading.value = true
                    }) {
                        Text(text = "Downloading")
                    }

                    OutlinedButton(onClick = {
                        delete()
                    }) {
                        Text(text = "Delete")
                    }

                    OutlinedButton(onClick = {
                        listFiles(images)
                    }) {
                        Text(text = "List file")
                    }

                    image.value?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    LazyColumn(content = {
                        items(images.value){ item ->
                            Image(
                                painter = rememberImagePainter(data = item),
                                contentDescription = null,
                                modifier = Modifier.size(200.dp)
                            )
                        }
                    })
                }
            }
        }
    }

    private fun downloading(
        bitmap:MutableState<Bitmap?>,
        boolean: MutableState<Boolean>
    ) = CoroutineScope(Dispatchers.IO).launch {
        val maxDownloadSize = 5L * 1024 * 1024
        val filename = "0021321249"
        val byte = firebaseStorage.reference.child("image/$filename").getBytes(maxDownloadSize).await()
        withContext(Dispatchers.Main){
            bitmap.value =  BitmapFactory.decodeByteArray(byte, 0, byte.size)
            boolean.value = false
        }
    }

    private fun delete() = CoroutineScope(Dispatchers.IO).launch{
        val filename = "0002356268"
        firebaseStorage.reference.child("image/$filename").delete().await()
    }

    private fun listFiles(
        listString: MutableState<List<String>>
    ) = CoroutineScope(Dispatchers.IO).launch {
        val imageUrls = mutableListOf<String>()
        val images = firebaseStorage.reference.child("image/").listAll().await()
        for (image in images.items){
            val url = image.downloadUrl.await()
            imageUrls.add(url.toString())
            Log.e("StorageA:", url.toString())
            Log.e("StorageB:", imageUrls.toString())
        }
        withContext(Dispatchers.Main){
            Log.e("StorageC:", imageUrls.toString())
            listString.value = imageUrls
            Log.e("StorageD:", listString.value.toString())
        }
    }

    @Composable
    private fun activityResultLauncher(
        boolean: MutableState<Boolean>
    ): ManagedActivityResultLauncher<String, Uri> {
        return rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent(),
            onResult = {
                Log.e("Storage:", it.toString())
                var fileName = ""
                repeat(10){ int ->
                    fileName += (0..int).random().toString()
                }

                it?.let {
                    firebaseStorage.reference.child("image/$fileName").putFile(it)
                        .addOnSuccessListener{
                            boolean.value = false
                        }
                        .addOnFailureListener{ error ->
                            Log.e("Storage:", error.message.toString())
                        }
                }
        })
    }
}
