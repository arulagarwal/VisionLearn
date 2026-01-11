package com.visionlearn.platform

/**
 * iOS implementation of ImagePicker
 * Note: This is a stub implementation for MVP. Full implementation would use
 * PHPickerViewController or UIImagePickerController with proper UIViewController presentation.
 */
actual class ImagePicker {
    
    actual suspend fun pickImage(): PickedImage? {
        // Stub for MVP - would need UIViewController context for real implementation
        return null
    }
    
    actual suspend fun takePhoto(): PickedImage? {
        // Stub for MVP - would need camera permission and UIViewController context
        return null
    }
    
    actual suspend fun pickMultipleImages(maxCount: Int): List<PickedImage> {
        // Stub for MVP - would need PHPickerViewController
        return emptyList()
    }
    
    actual val isCameraAvailable: Boolean
        get() = false // Would check UIImagePickerController.isSourceTypeAvailable
}
