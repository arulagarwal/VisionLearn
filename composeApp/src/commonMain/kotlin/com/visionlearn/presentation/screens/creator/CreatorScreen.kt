package com.visionlearn.presentation.screens.creator

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.visionlearn.di.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.visionlearn.domain.model.ModuleType
import com.visionlearn.domain.repository.CustomActivity
import com.visionlearn.presentation.theme.CVIColors

class CreatorScreen : Screen {
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<CreatorScreenModel>()
        val state by screenModel.state.collectAsState()
        var selectedTab by remember { mutableStateOf(0) }
        
        val snackbarHostState = remember { SnackbarHostState() }
        
        LaunchedEffect(state.showSuccessMessage) {
            if (state.showSuccessMessage) {
                snackbarHostState.showSnackbar(state.successMessage, duration = SnackbarDuration.Short)
            }
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Content Creator") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("New") },
                        icon = { Icon(Icons.Default.Add, null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("My Content") },
                        icon = { Icon(Icons.Default.List, null) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Templates") },
                        icon = { Icon(Icons.Default.Star, null) }
                    )
                }
                
                when (selectedTab) {
                    0 -> NewActivityTab(screenModel, state)
                    1 -> MyContentTab(screenModel, state)
                    2 -> TemplatesTab(screenModel, state)
                }
            }
        }
        
        // Dialogs
        if (state.showImageSourceDialog) {
            ImageSourceDialog(
                onDismiss = { screenModel.hideImageSourceDialog() },
                onDemoClick = { emoji, name -> screenModel.addDemoImage(emoji, name) }
            )
        }
        
        if (state.showEditDialog && state.activityToEdit != null) {
            EditDialog(
                activity = state.activityToEdit!!,
                editTitle = state.editTitle,
                onTitleChange = { screenModel.updateEditTitle(it) },
                onDismiss = { screenModel.hideEditDialog() },
                onSave = { screenModel.saveActivityEdit() }
            )
        }
        
        if (state.showDeleteDialog && state.activityToDelete != null) {
            DeleteDialog(
                activity = state.activityToDelete!!,
                onDismiss = { screenModel.hideDeleteDialog() },
                onConfirm = { screenModel.confirmDelete() }
            )
        }
        
        if (state.showTemplatePreview && state.selectedTemplate != null) {
            TemplatePreviewDialog(
                template = state.selectedTemplate!!,
                onDismiss = { screenModel.hideTemplatePreview() },
                onUse = { screenModel.useTemplate() }
            )
        }
    }
}

@Composable
private fun NewActivityTab(screenModel: CreatorScreenModel, state: CreatorState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Create New Activity", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Build personalized learning content",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        item {
            OutlinedTextField(
                value = state.activityTitle,
                onValueChange = { screenModel.updateTitle(it) },
                label = { Text("Activity Title") },
                placeholder = { Text("e.g., Family Photos") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        
        item { Text("Activity Type", style = MaterialTheme.typography.titleMedium) }
        
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(activityTypes) { type ->
                    ActivityTypeCard(
                        type = type,
                        isSelected = state.selectedType == type.moduleType,
                        onClick = { screenModel.selectType(type.moduleType) }
                    )
                }
            }
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Images (${state.addedImages.size})", style = MaterialTheme.typography.titleMedium)
                if (state.isAnalyzingImages) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Analyzing...", style = MaterialTheme.typography.bodySmall, color = CVIColors.Blue)
                    }
                }
            }
        }
        
        if (state.addedImages.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.addedImages) { image ->
                        ImageCard(image = image, onRemove = { screenModel.removeImage(image.id) })
                    }
                }
            }
        }
        
        item {
            Card(
                onClick = { screenModel.showImageSourceDialog() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().height(100.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Add, "Add", Modifier.size(32.dp), MaterialTheme.colorScheme.primary)
                    Text("Tap to add images", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        
        state.createError?.let { error ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(error, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                        IconButton(onClick = { screenModel.dismissError() }) {
                            Icon(Icons.Default.Close, "Dismiss")
                        }
                    }
                }
            }
        }
        
        item {
            Button(
                onClick = { screenModel.createActivity() },
                enabled = state.activityTitle.isNotBlank() && state.selectedType != null && !state.isCreating,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CVIColors.Green)
            ) {
                if (state.isCreating) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Creating...")
                } else {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Create Activity")
                }
            }
        }
        
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun ActivityTypeCard(type: ActivityTypeInfo, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) type.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(if (isSelected) 3.dp else 1.dp, if (isSelected) type.color else MaterialTheme.colorScheme.outline),
        modifier = Modifier.width(120.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(type.emoji, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(4.dp))
            Text(type.title, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null, tint = type.color, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ImageCard(image: ActivityImageState, onRemove: () -> Unit) {
    Card(modifier = Modifier.width(100.dp)) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                Card(
                    modifier = Modifier.size(60.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (image.isAppropriate) CVIColors.Yellow.copy(0.2f) else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (image.isAnalyzing) {
                            CircularProgressIndicator(Modifier.size(24.dp))
                        } else {
                            Text(image.emoji, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.align(Alignment.TopEnd).size(20.dp).background(CVIColors.Red, CircleShape)
                ) {
                    Icon(Icons.Default.Close, "Remove", Modifier.size(14.dp), Color.White)
                }
            }
            Text(image.name, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            image.complexityScore?.let { score ->
                Text("$score/5", style = MaterialTheme.typography.labelSmall, color = if (image.isAppropriate) CVIColors.Success else CVIColors.Red)
            }
        }
    }
}

@Composable
private fun MyContentTab(screenModel: CreatorScreenModel, state: CreatorState) {
    if (state.myActivities.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📝", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.height(16.dp))
                Text("No activities yet", style = MaterialTheme.typography.headlineSmall)
                Text("Create your first activity!", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Your Activities (${state.myActivities.size})", style = MaterialTheme.typography.titleLarge) }
            
            items(state.myActivities) { activity ->
                ActivityCard(
                    activity = activity,
                    onEdit = { screenModel.showEditDialog(activity) },
                    onDelete = { screenModel.showDeleteDialog(activity) }
                )
            }
        }
    }
}

@Composable
private fun ActivityCard(activity: CustomActivity, onEdit: () -> Unit, onDelete: () -> Unit) {
    val color = getColorForModule(activity.moduleType)
    val emoji = activity.images.firstOrNull()?.emoji ?: "📚"
    
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = color.copy(0.2f)),
                modifier = Modifier.size(50.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(emoji, style = MaterialTheme.typography.headlineMedium)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(activity.title, style = MaterialTheme.typography.titleMedium)
                    if (activity.isFromTemplate) {
                        Spacer(Modifier.width(8.dp))
                        Surface(color = CVIColors.Purple.copy(0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text("T", style = MaterialTheme.typography.labelSmall, color = CVIColors.Purple, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
                Text("${activity.moduleType.displayName} • ${activity.images.size} items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit", tint = CVIColors.Blue) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = CVIColors.Red) }
        }
    }
}

@Composable
private fun TemplatesTab(screenModel: CreatorScreenModel, state: CreatorState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Activity Templates", style = MaterialTheme.typography.titleLarge)
            Text("Quick-start with pre-made activities", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        items(activityTemplates) { template ->
            TemplateCard(template = template, onClick = { screenModel.selectTemplate(template) })
        }
    }
}

@Composable
private fun TemplateCard(template: ActivityTemplate, onClick: () -> Unit) {
    val color = Color(template.color)
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = color.copy(0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = color.copy(0.2f)), modifier = Modifier.size(50.dp)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(template.emoji, style = MaterialTheme.typography.headlineMedium)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(template.title, style = MaterialTheme.typography.titleMedium)
                Text(template.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.padding(top = 4.dp)) {
                    Surface(color = color.copy(0.2f), shape = RoundedCornerShape(4.dp)) {
                        Text(template.type.displayName, style = MaterialTheme.typography.labelSmall, color = color, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("${template.presetImages.size} items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = color)
        }
    }
}

// Dialogs
@Composable
private fun ImageSourceDialog(onDismiss: () -> Unit, onDemoClick: (String, String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Image") },
        text = {
            Column {
                Text("Choose a demo image:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(demoImages) { (emoji, name) ->
                        Card(onClick = { onDemoClick(emoji, name) }, modifier = Modifier.size(50.dp)) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(emoji, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EditDialog(
    activity: CustomActivity,
    editTitle: String,
    onTitleChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Activity") },
        text = {
            Column {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = onTitleChange,
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text("Type: ${activity.moduleType.displayName}", style = MaterialTheme.typography.bodySmall)
                Text("Images: ${activity.images.size}", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = editTitle.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = CVIColors.Blue)) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DeleteDialog(activity: CustomActivity, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, null, tint = CVIColors.Red, modifier = Modifier.size(48.dp)) },
        title = { Text("Delete Activity?") },
        text = {
            Column {
                Text("Delete \"${activity.title}\"?")
                Spacer(Modifier.height(4.dp))
                Text("This cannot be undone.", style = MaterialTheme.typography.bodySmall, color = CVIColors.Red)
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = CVIColors.Red)) {
                Text("Delete")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun TemplatePreviewDialog(template: ActivityTemplate, onDismiss: () -> Unit, onUse: () -> Unit) {
    val color = Color(template.color)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(template.emoji)
                Spacer(Modifier.width(8.dp))
                Text(template.title)
            }
        },
        text = {
            Column {
                Text(template.description)
                Spacer(Modifier.height(12.dp))
                Text("Includes ${template.presetImages.size} items:", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(template.presetImages) { item ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Card(colors = CardDefaults.cardColors(containerColor = color.copy(0.1f)), modifier = Modifier.size(40.dp)) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(item.emoji, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                            Text(item.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onUse, colors = ButtonDefaults.buttonColors(containerColor = color)) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("Use Template")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// Helpers
private fun getColorForModule(type: ModuleType): Color = when (type) {
    ModuleType.RECOGNITION -> CVIColors.Yellow
    ModuleType.CAUSE_EFFECT -> CVIColors.Blue
    ModuleType.SORTING -> CVIColors.Green
    ModuleType.MATCHING -> CVIColors.Purple
    ModuleType.SEQUENCING -> CVIColors.Orange
    else -> CVIColors.Blue
}

private data class ActivityTypeInfo(val moduleType: ModuleType, val title: String, val emoji: String, val color: Color)

private val activityTypes = listOf(
    ActivityTypeInfo(ModuleType.RECOGNITION, "Recognition", "🖼️", CVIColors.Yellow),
    ActivityTypeInfo(ModuleType.SORTING, "Sorting", "📦", CVIColors.Green),
    ActivityTypeInfo(ModuleType.MATCHING, "Matching", "🎴", CVIColors.Purple),
    ActivityTypeInfo(ModuleType.CAUSE_EFFECT, "Cause & Effect", "✨", CVIColors.Blue),
    ActivityTypeInfo(ModuleType.SEQUENCING, "Sequencing", "📋", CVIColors.Orange)
)

private val demoImages = listOf(
    "🍎" to "Apple", "🍌" to "Banana", "🐕" to "Dog", "🐱" to "Cat",
    "🚗" to "Car", "🏠" to "House", "⭐" to "Star", "🌙" to "Moon",
    "🌺" to "Flower", "🌳" to "Tree", "🎈" to "Balloon", "📚" to "Book",
    "🔔" to "Bell", "⚽" to "Ball", "🎁" to "Gift", "🌈" to "Rainbow"
)
