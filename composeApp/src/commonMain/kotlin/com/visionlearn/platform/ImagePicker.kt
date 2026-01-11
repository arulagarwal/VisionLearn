package com.visionlearn.platform

import kotlinx.coroutines.flow.Flow

/**
 * Result from image picking
 */
data class PickedImage(
    val path: String,
    val bytes: ByteArray,
    val mimeType: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as PickedImage
        return path == other.path && bytes.contentEquals(other.bytes) && mimeType == other.mimeType
    }
    
    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}

/**
 * Multiplatform image picker interface
 */
expect class ImagePicker {
    /**
     * Launch image picker from gallery
     * @return Flow emitting picked image or null if cancelled
     */
    suspend fun pickImage(): PickedImage?
    
    /**
     * Launch camera to take a photo
     * @return Flow emitting captured image or null if cancelled
     */
    suspend fun takePhoto(): PickedImage?
    
    /**
     * Pick multiple images from gallery
     * @param maxCount Maximum number of images to pick
     * @return List of picked images
     */
    suspend fun pickMultipleImages(maxCount: Int = 10): List<PickedImage>
    
    /**
     * Check if camera is available
     */
    val isCameraAvailable: Boolean
}

/**
 * State for image picker UI
 */
sealed class ImagePickerState {
    object Idle : ImagePickerState()
    object Loading : ImagePickerState()
    data class Success(val images: List<PickedImage>) : ImagePickerState()
    data class Error(val message: String) : ImagePickerState()
}
