package com.visionlearn.presentation.screens.creator

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.visionlearn.ai.AIService
import com.visionlearn.domain.model.ModuleType
import com.visionlearn.domain.repository.CustomActivity
import com.visionlearn.domain.repository.CustomActivityImage
import com.visionlearn.domain.repository.CustomActivityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * Data class representing an image being added (UI state)
 */
data class ActivityImageState(
    val id: String,
    val emoji: String,
    val name: String,
    val category: String? = null,
    val aiDescription: String? = null,
    val complexityScore: Int? = null,
    val isAnalyzing: Boolean = false,
    val isAppropriate: Boolean = true
)

/**
 * Template data
 */
data class ActivityTemplate(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val type: ModuleType,
    val presetImages: List<TemplateImage>,
    val color: Long
)

data class TemplateImage(
    val emoji: String,
    val name: String,
    val category: String? = null
)

/**
 * State for Creator Screen
 */
data class CreatorState(
    // New Activity Tab
    val activityTitle: String = "",
    val selectedType: ModuleType? = null,
    val addedImages: List<ActivityImageState> = emptyList(),
    val isCreating: Boolean = false,
    val createError: String? = null,
    val showImageSourceDialog: Boolean = false,
    val isAnalyzingImages: Boolean = false,
    
    // My Content Tab
    val myActivities: List<CustomActivity> = emptyList(),
    val isLoadingActivities: Boolean = false,
    val activityToEdit: CustomActivity? = null,
    val activityToDelete: CustomActivity? = null,
    val showEditDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val editTitle: String = "",
    
    // Templates Tab
    val selectedTemplate: ActivityTemplate? = null,
    val showTemplatePreview: Boolean = false,
    
    // Success states
    val showSuccessMessage: Boolean = false,
    val successMessage: String = ""
)

/**
 * ScreenModel for Content Creator - now with database persistence
 */
class CreatorScreenModel(
    private val aiService: AIService,
    private val customActivityRepository: CustomActivityRepository
) : ScreenModel {
    
    private val _state = MutableStateFlow(CreatorState())
    val state: StateFlow<CreatorState> = _state.asStateFlow()
    
    init {
        loadMyActivities()
    }
    
    // ==================== NEW ACTIVITY TAB ====================
    
    fun updateTitle(title: String) {
        _state.update { it.copy(activityTitle = title) }
    }
    
    fun selectType(type: ModuleType) {
        _state.update { it.copy(selectedType = type) }
    }
    
    fun showImageSourceDialog() {
        _state.update { it.copy(showImageSourceDialog = true) }
    }
    
    fun hideImageSourceDialog() {
        _state.update { it.copy(showImageSourceDialog = false) }
    }
    
    /**
     * Add a demo/placeholder image
     */
    fun addDemoImage(emoji: String, name: String, category: String? = null) {
        val image = ActivityImageState(
            id = "img_${Clock.System.now().toEpochMilliseconds()}",
            emoji = emoji,
            name = name,
            category = category,
            isAnalyzing = true
        )
        
        _state.update { 
            it.copy(
                addedImages = it.addedImages + image,
                showImageSourceDialog = false
            ) 
        }
        
        // Simulate AI analysis
        analyzeImage(image.id)
    }
    
    private fun analyzeImage(imageId: String) {
        screenModelScope.launch {
            _state.update { it.copy(isAnalyzingImages = true) }
            
            try {
                // Simulate AI analysis delay
                kotlinx.coroutines.delay(1200)
                
                val complexity = (1..4).random()
                val descriptions = listOf(
                    "Clear object with good contrast",
                    "Simple shape, easy to identify",
                    "Familiar everyday item",
                    "High contrast on dark background"
                )
                
                _state.update { state ->
                    state.copy(
                        addedImages = state.addedImages.map { img ->
                            if (img.id == imageId) {
                                img.copy(
                                    isAnalyzing = false,
                                    aiDescription = descriptions.random(),
                                    complexityScore = complexity,
                                    isAppropriate = complexity <= 4
                                )
                            } else img
                        },
                        isAnalyzingImages = state.addedImages.any { it.isAnalyzing && it.id != imageId }
                    )
                }
            } catch (e: Exception) {
                _state.update { state ->
                    state.copy(
                        addedImages = state.addedImages.map { img ->
                            if (img.id == imageId) {
                                img.copy(isAnalyzing = false, complexityScore = 2)
                            } else img
                        },
                        isAnalyzingImages = false
                    )
                }
            }
        }
    }
    
    fun removeImage(imageId: String) {
        _state.update { 
            it.copy(addedImages = it.addedImages.filter { img -> img.id != imageId }) 
        }
    }
    
    fun createActivity() {
        val currentState = _state.value
        if (currentState.activityTitle.isBlank() || currentState.selectedType == null) {
            _state.update { it.copy(createError = "Please fill in all required fields") }
            return
        }
        
        screenModelScope.launch {
            _state.update { it.copy(isCreating = true, createError = null) }
            
            try {
                val now = Clock.System.now().toEpochMilliseconds()
                
                val activity = CustomActivity(
                    id = "activity_$now",
                    title = currentState.activityTitle,
                    moduleType = currentState.selectedType,
                    images = currentState.addedImages.map { img ->
                        CustomActivityImage(
                            id = img.id,
                            emoji = img.emoji,
                            name = img.name,
                            category = img.category,
                            aiDescription = img.aiDescription,
                            complexityScore = img.complexityScore ?: 2
                        )
                    },
                    createdAt = now,
                    isFromTemplate = false
                )
                
                // Save to database
                val result = customActivityRepository.createActivity(activity)
                
                result.onSuccess {
                    // Reset form and show success
                    _state.update {
                        it.copy(
                            activityTitle = "",
                            selectedType = null,
                            addedImages = emptyList(),
                            isCreating = false,
                            showSuccessMessage = true,
                            successMessage = "Activity '${activity.title}' created! ✓"
                        )
                    }
                    
                    // Reload activities
                    loadMyActivities()
                    
                    // Auto-hide success message
                    kotlinx.coroutines.delay(3000)
                    _state.update { it.copy(showSuccessMessage = false) }
                    
                }.onFailure { e ->
                    _state.update { 
                        it.copy(isCreating = false, createError = "Failed to save: ${e.message}") 
                    }
                }
                
            } catch (e: Exception) {
                _state.update { 
                    it.copy(isCreating = false, createError = e.message) 
                }
            }
        }
    }
    
    // ==================== MY CONTENT TAB ====================
    
    private fun loadMyActivities() {
        screenModelScope.launch {
            _state.update { it.copy(isLoadingActivities = true) }
            try {
                customActivityRepository.getAllActivities()
                    .catch { e ->
                        emit(emptyList())
                    }
                    .collect { activities ->
                        _state.update { it.copy(myActivities = activities, isLoadingActivities = false) }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update { it.copy(isLoadingActivities = false) }
            }
        }
    }
    
    fun showEditDialog(activity: CustomActivity) {
        _state.update { 
            it.copy(
                activityToEdit = activity,
                showEditDialog = true,
                editTitle = activity.title
            ) 
        }
    }
    
    fun hideEditDialog() {
        _state.update { 
            it.copy(activityToEdit = null, showEditDialog = false, editTitle = "") 
        }
    }
    
    fun updateEditTitle(title: String) {
        _state.update { it.copy(editTitle = title) }
    }
    
    fun saveActivityEdit() {
        val activity = _state.value.activityToEdit ?: return
        val newTitle = _state.value.editTitle
        if (newTitle.isBlank()) return
        
        screenModelScope.launch {
            val updated = activity.copy(title = newTitle)
            val result = customActivityRepository.updateActivity(updated)
            
            result.onSuccess {
                _state.update {
                    it.copy(
                        showEditDialog = false,
                        activityToEdit = null,
                        editTitle = "",
                        showSuccessMessage = true,
                        successMessage = "Activity updated! ✓"
                    )
                }
                loadMyActivities()
                kotlinx.coroutines.delay(2000)
                _state.update { it.copy(showSuccessMessage = false) }
            }
        }
    }
    
    fun showDeleteDialog(activity: CustomActivity) {
        _state.update { 
            it.copy(activityToDelete = activity, showDeleteDialog = true) 
        }
    }
    
    fun hideDeleteDialog() {
        _state.update { 
            it.copy(activityToDelete = null, showDeleteDialog = false) 
        }
    }
    
    fun confirmDelete() {
        val activity = _state.value.activityToDelete ?: return
        
        screenModelScope.launch {
            val result = customActivityRepository.deleteActivity(activity.id)
            
            result.onSuccess {
                _state.update {
                    it.copy(
                        showDeleteDialog = false,
                        activityToDelete = null,
                        showSuccessMessage = true,
                        successMessage = "Activity deleted"
                    )
                }
                loadMyActivities()
                kotlinx.coroutines.delay(2000)
                _state.update { it.copy(showSuccessMessage = false) }
            }
        }
    }
    
    // ==================== TEMPLATES TAB ====================
    
    fun selectTemplate(template: ActivityTemplate) {
        _state.update { 
            it.copy(selectedTemplate = template, showTemplatePreview = true) 
        }
    }
    
    fun hideTemplatePreview() {
        _state.update { 
            it.copy(selectedTemplate = null, showTemplatePreview = false) 
        }
    }
    
    fun useTemplate() {
        val template = _state.value.selectedTemplate ?: return
        
        screenModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            
            val images = template.presetImages.mapIndexed { index, preset ->
                CustomActivityImage(
                    id = "img_template_$index",
                    emoji = preset.emoji,
                    name = preset.name,
                    category = preset.category,
                    aiDescription = "Template: ${preset.name}",
                    complexityScore = 2
                )
            }
            
            val activity = CustomActivity(
                id = "activity_$now",
                title = template.title,
                moduleType = template.type,
                images = images,
                createdAt = now,
                isFromTemplate = true,
                templateId = template.id
            )
            
            val result = customActivityRepository.createActivity(activity)
            
            result.onSuccess {
                _state.update {
                    it.copy(
                        showTemplatePreview = false,
                        selectedTemplate = null,
                        showSuccessMessage = true,
                        successMessage = "Created from '${template.title}' template! ✓"
                    )
                }
                loadMyActivities()
                kotlinx.coroutines.delay(3000)
                _state.update { it.copy(showSuccessMessage = false) }
            }
        }
    }
    
    fun dismissError() {
        _state.update { it.copy(createError = null) }
    }
}

/**
 * Pre-defined templates
 */
val activityTemplates = listOf(
    ActivityTemplate(
        id = "template_colors",
        title = "Basic Colors",
        description = "Learn primary colors with simple objects",
        emoji = "🎨",
        type = ModuleType.RECOGNITION,
        presetImages = listOf(
            TemplateImage("🔴", "Red"),
            TemplateImage("🟡", "Yellow"),
            TemplateImage("🔵", "Blue"),
            TemplateImage("🟢", "Green"),
            TemplateImage("🟠", "Orange"),
            TemplateImage("🟣", "Purple")
        ),
        color = 0xFFE53935
    ),
    ActivityTemplate(
        id = "template_shapes",
        title = "Shapes",
        description = "Circle, square, triangle recognition",
        emoji = "🔷",
        type = ModuleType.RECOGNITION,
        presetImages = listOf(
            TemplateImage("⬛", "Square"),
            TemplateImage("🔺", "Triangle"),
            TemplateImage("⚫", "Circle"),
            TemplateImage("💠", "Diamond"),
            TemplateImage("⭐", "Star")
        ),
        color = 0xFF1E88E5
    ),
    ActivityTemplate(
        id = "template_numbers",
        title = "Numbers 1-5",
        description = "Count with pictures",
        emoji = "🔢",
        type = ModuleType.SEQUENCING,
        presetImages = listOf(
            TemplateImage("1️⃣", "One"),
            TemplateImage("2️⃣", "Two"),
            TemplateImage("3️⃣", "Three"),
            TemplateImage("4️⃣", "Four"),
            TemplateImage("5️⃣", "Five")
        ),
        color = 0xFF43A047
    ),
    ActivityTemplate(
        id = "template_animals",
        title = "Animals",
        description = "Common pets and animals",
        emoji = "🐕",
        type = ModuleType.RECOGNITION,
        presetImages = listOf(
            TemplateImage("🐕", "Dog"),
            TemplateImage("🐱", "Cat"),
            TemplateImage("🐰", "Rabbit"),
            TemplateImage("🐦", "Bird"),
            TemplateImage("🐟", "Fish"),
            TemplateImage("🐢", "Turtle")
        ),
        color = 0xFFFF9800
    ),
    ActivityTemplate(
        id = "template_food",
        title = "Food Sorting",
        description = "Fruits and vegetables",
        emoji = "🍎",
        type = ModuleType.SORTING,
        presetImages = listOf(
            TemplateImage("🍎", "Apple", "Fruits"),
            TemplateImage("🍌", "Banana", "Fruits"),
            TemplateImage("🥕", "Carrot", "Vegetables"),
            TemplateImage("🥦", "Broccoli", "Vegetables"),
            TemplateImage("🍇", "Grapes", "Fruits"),
            TemplateImage("🌽", "Corn", "Vegetables")
        ),
        color = 0xFFFFEB3B
    ),
    ActivityTemplate(
        id = "template_family",
        title = "Family",
        description = "Family member recognition",
        emoji = "👨‍👩‍👧",
        type = ModuleType.RECOGNITION,
        presetImages = listOf(
            TemplateImage("👨", "Dad"),
            TemplateImage("👩", "Mom"),
            TemplateImage("👧", "Sister"),
            TemplateImage("👦", "Brother"),
            TemplateImage("👴", "Grandpa"),
            TemplateImage("👵", "Grandma")
        ),
        color = 0xFF9C27B0
    ),
    ActivityTemplate(
        id = "template_vehicles",
        title = "Vehicles",
        description = "Cars, buses, and more",
        emoji = "🚗",
        type = ModuleType.MATCHING,
        presetImages = listOf(
            TemplateImage("🚗", "Car"),
            TemplateImage("🚌", "Bus"),
            TemplateImage("🚁", "Helicopter"),
            TemplateImage("🚂", "Train"),
            TemplateImage("🚢", "Ship"),
            TemplateImage("✈️", "Airplane")
        ),
        color = 0xFF607D8B
    ),
    ActivityTemplate(
        id = "template_weather",
        title = "Weather",
        description = "Sun, rain, and seasons",
        emoji = "☀️",
        type = ModuleType.CAUSE_EFFECT,
        presetImages = listOf(
            TemplateImage("☀️", "Sunny"),
            TemplateImage("🌧️", "Rainy"),
            TemplateImage("⛈️", "Stormy"),
            TemplateImage("❄️", "Snowy"),
            TemplateImage("🌈", "Rainbow"),
            TemplateImage("☁️", "Cloudy")
        ),
        color = 0xFF00BCD4
    )
)
