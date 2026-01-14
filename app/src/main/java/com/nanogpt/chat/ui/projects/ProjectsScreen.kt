package com.nanogpt.chat.ui.projects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nanogpt.chat.data.local.entity.ProjectEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: ProjectsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onProjectSelected: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }
    var conversationCount by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projects") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create Project")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(
                items = uiState.projects,
                key = { it.id }
            ) { project ->
                ProjectItem(
                    project = project,
                    onClick = { onProjectSelected(project.id) },
                    onEdit = { editingProject = project },
                    onDelete = {
                        projectToDelete = project
                        coroutineScope.launch {
                            conversationCount = viewModel.getConversationCountForProject(project.id)
                        }
                    }
                )
            }
        }
    }

    if (showCreateDialog) {
        ProjectDialog(
            onCreate = { name, description, systemPrompt, color ->
                viewModel.createProject(name, description, systemPrompt, color)
                showCreateDialog = false
            },
            onUpdate = { _, _, _, _ -> },
            onDismiss = { showCreateDialog = false }
        )
    }

    if (editingProject != null) {
        ProjectDialog(
            project = editingProject,
            onCreate = { _, _, _, _ -> },
            onUpdate = { name, description, systemPrompt, color ->
                viewModel.updateProject(editingProject!!.id, name, description, systemPrompt, color)
                editingProject = null
            },
            onDismiss = { editingProject = null }
        )
    }

    // Delete confirmation dialog
    if (projectToDelete != null) {
        DeleteProjectDialog(
            projectName = projectToDelete!!.name,
            conversationCount = conversationCount,
            onConfirm = { deleteConversations ->
                viewModel.deleteProjectWithConversations(projectToDelete!!, deleteConversations)
                projectToDelete = null
            },
            onDismiss = { projectToDelete = null }
        )
    }
}

@Composable
fun ProjectItem(
    project: ProjectEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Folder icon with color
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = if (project.color != null) {
                        Color(android.graphics.Color.parseColor(project.color))
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = if (project.color != null) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Actions
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
fun DeleteProjectDialog(
    projectName: String,
    conversationCount: Int,
    onConfirm: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var deleteConversations by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Delete Project?")
        },
        text = {
            Column {
                Text(
                    "Are you sure you want to delete \"$projectName\"?",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (conversationCount > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "This project has $conversationCount conversation(s).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = deleteConversations,
                            onCheckedChange = { deleteConversations = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Also delete conversations in this project",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(deleteConversations) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
