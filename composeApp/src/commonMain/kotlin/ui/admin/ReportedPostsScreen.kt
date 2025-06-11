package ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import data.model.Post
import data.model.Report
import data.model.ReportStatus
import org.koin.compose.koinInject
import viewmodel.PostViewModel

@Composable
fun ReportedPostsScreen(
    navController: NavController,
    postViewModel: PostViewModel = koinInject()
) {
    val reportedPosts by postViewModel.reportedPosts.collectAsState()
    val isLoading by postViewModel.isLoading.collectAsState()
    var showOnlyPending by remember { mutableStateOf(true) }

    LaunchedEffect(showOnlyPending) {
        postViewModel.getReportedPosts(showOnlyPending)
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Filter toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reported Posts",
                    style = MaterialTheme.typography.titleLarge
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Show only pending",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Switch(
                        checked = showOnlyPending,
                        onCheckedChange = {
                            showOnlyPending = it
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (reportedPosts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // Show different message based on filter state
                    Text(
                        if (showOnlyPending) "No pending reports found"
                        else "No reports found"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(reportedPosts) { (post, report) ->
                        ReportedPostItem(
                            post = post,
                            report = report,
                            postViewModel = postViewModel,
                            showingAll = !showOnlyPending
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReportedPostItem(
    post: Post,
    report: Report,
    postViewModel: PostViewModel,
    showingAll: Boolean
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showIgnoreDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = when (report.status) {
            ReportStatus.PENDING -> CardDefaults.cardColors()
            ReportStatus.ACCEPTED -> CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            )
            ReportStatus.IGNORED -> CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            ReportStatus.REVIEWED -> CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // If showing all statuses, display the status
            if (showingAll) {
                ReportStatusChip(report.status)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = "Post by ${post.authorName}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Report Reason:",
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = report.reason,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Only show action buttons for pending reports
            if (report.status == ReportStatus.PENDING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete Post")
                    }

                    Button(
                        onClick = { showIgnoreDialog = true }
                    ) {
                        Text("Ignore Report")
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Post") },
            text = { Text("Are you sure you want to delete this post? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        postViewModel.deleteReportedPost(post.id, report.id, !showingAll)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showIgnoreDialog) {
        AlertDialog(
            onDismissRequest = { showIgnoreDialog = false },
            title = { Text("Ignore Report") },
            text = { Text("Are you sure you want to ignore this report?") },
            confirmButton = {
                Button(
                    onClick = {
                        showIgnoreDialog = false
                        postViewModel.ignoreReport(report.id, !showingAll)
                    }
                ) {
                    Text("Ignore")
                }
            },
            dismissButton = {
                Button(onClick = { showIgnoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ReportStatusChip(status: ReportStatus) {
    val (color, text) = when (status) {
        ReportStatus.PENDING -> MaterialTheme.colorScheme.primary to "Pending"
        ReportStatus.ACCEPTED -> MaterialTheme.colorScheme.error to "Action Taken"
        ReportStatus.IGNORED -> MaterialTheme.colorScheme.outline to "Ignored"
        ReportStatus.REVIEWED -> MaterialTheme.colorScheme.secondary to "Reviewed"
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}