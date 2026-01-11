package com.visionlearn.platform

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android implementation of ImagePicker using Photo Picker API
 */
actual class ImagePicker(private val context: Context) {
    
    private var pickImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>? = null
    private var pickMultipleLauncher: ActivityResultLauncher<PickVisualMediaRequest>? = null
    private var takePictureLauncher: ActivityResultLauncher<Uri>? = null
    
    private var pendingImageCallback: ((PickedImage?) -> Unit)? = null
    private var pendingMultipleCallback: ((List<PickedImage>) -> Unit)? = null
    
    /**
     * Register activity result launchers - call from Activity.onCreate()
     */
    fun registerLaunchers(activity: ComponentActivity) {
        pickImageLauncher = activity.registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            val image = uri?.let { uriToPickedImage(it) }
            pendingImageCallback?.invoke(image)
            pendingImageCallback = null
        }
        
        pickMultipleLauncher = activity.registerForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia()
        ) { uris ->
            val images = uris.mapNotNull { uriToPickedImage(it) }
            pendingMultipleCallback?.invoke(images)
            pendingMultipleCallback = null
        }
    }
    
    actual suspend fun pickImage(): PickedImage? = suspendCancellableCoroutine { cont ->
        pendingImageCallback = { image ->
            cont.resume(image)
        }
        
        pickImageLauncher?.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        ) ?: cont.resume(null)
        
        cont.invokeOnCancellation {
            pendingImageCallback = null
        }
    }
    
    actual suspend fun takePhoto(): PickedImage? {
        // For now, return null - would need camera permission and temp file
        // Full implementation would use CameraX or MediaStore
        return null
    }
    
    actual suspend fun pickMultipleImages(maxCount: Int): List<PickedImage> = 
        suspendCancellableCoroutine { cont ->
            pendingMultipleCallback = { images ->
                cont.resume(images.take(maxCount))
            }
            
            pickMultipleLauncher?.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            ) ?: cont.resume(emptyList())
            
            cont.invokeOnCancellation {
                pendingMultipleCallback = null
            }
        }
    
    actual val isCameraAvailable: Boolean
        get() = context.packageManager.hasSystemFeature(
            android.content.pm.PackageManager.FEATURE_CAMERA_ANY
        )
    
    private fun uriToPickedImage(uri: Uri): PickedImage? {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            
            PickedImage(
                path = uri.toString(),
                bytes = bytes,
                mimeType = mimeType
            )
        } catch (e: Exception) {
            null
        }
    }
}
