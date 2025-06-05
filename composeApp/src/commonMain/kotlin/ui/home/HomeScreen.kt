package ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import data.model.Post
import data.model.User
import data.model.UserRole
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import navigation.PostDetail
import org.koin.compose.koinInject
import ui.components.RefreshableContainer
import ui.components.rememberRefreshHandler
import utils.isDesktop
import viewmodel.HomeViewModel
import viewmodel.PostViewModel

@Composable
fun NewPostDialog(onDismiss: () -> Unit, onPostCreated: (content: String) -> Unit) {
    var postContent by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Post") },
        text = {
            OutlinedTextField(
                value = postContent,
                onValueChange = { postContent = it },
                label = { Text("Post content") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onPostCreated(postContent.trim()) },
                enabled = postContent.trim().isNotBlank()
            ) {
                Text("Post")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TimeAgoText(timestamp: String, modifier: Modifier = Modifier) {
    val timeAgo = remember(timestamp) {
        calculateTimeAgo(timestamp)
    }

    Text(
        text = timeAgo,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = modifier
    )
}

fun calculateTimeAgo(timestamp: String): String {
    if (timestamp.isBlank()) {
        return "recently"
    }

    try {
        val createdInstant = Instant.parse(timestamp)
        val now = Clock.System.now()
        val diffSeconds = (now - createdInstant).inWholeSeconds

        // Handle future timestamps
        if (diffSeconds < 0) {
            return "just now"
        }

        // Time unit constants
        val SECONDS_IN_MINUTE = 60
        val SECONDS_IN_HOUR = 60 * SECONDS_IN_MINUTE
        val SECONDS_IN_DAY = 24 * SECONDS_IN_HOUR
        val SECONDS_IN_MONTH = 30 * SECONDS_IN_DAY // Approximate
        val SECONDS_IN_YEAR = 365 * SECONDS_IN_DAY // Approximate

        return when {
            diffSeconds < SECONDS_IN_MINUTE -> "just now"
            diffSeconds < SECONDS_IN_HOUR -> "${diffSeconds / SECONDS_IN_MINUTE}m ago"
            diffSeconds < SECONDS_IN_DAY -> "${diffSeconds / SECONDS_IN_HOUR}h ago"
            diffSeconds < SECONDS_IN_MONTH -> "${diffSeconds / SECONDS_IN_DAY}d ago"
            diffSeconds < SECONDS_IN_YEAR -> "${diffSeconds / SECONDS_IN_MONTH}mo ago"
            else -> "${diffSeconds / SECONDS_IN_YEAR}y ago"
        }
    } catch (e: IllegalArgumentException) {
        return "recently"
    }
}

@Composable
fun LikeButton(
    isLiked: Boolean,
    likeCount: Int,
    onLikeClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .padding(vertical = 4.dp)
    ) {
        IconButton(onClick = onLikeClicked) {
            Icon(
                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isLiked) "Unlike" else "Like",
                tint = if (isLiked) Color.Red else LocalContentColor.current,
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = likeCount.toString(),
            style = MaterialTheme.typography.bodyMedium
        )

    }
}

@Composable
fun PostCard(
    post: Post,
    isLiked: Boolean,
    onLikeClicked: () -> Unit,
    onPostClicked: () -> Unit,
    currentUser: User? = null,
    onReportClicked: (String) -> Unit = {},
    onDeleteClicked: () -> Unit = {},
) {
    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    // Check if user can delete this post
    val canDelete = currentUser != null && (
        post.authorId == currentUser.id ||
        currentUser.globalRole == UserRole.ADMIN ||
        currentUser.globalRole == UserRole.SUPER_ADMIN
    )

    if (showReportDialog) {
        ReportDialog(
            onDismiss = { showReportDialog = false },
            onReport = { reason ->
                onReportClicked(reason)
                showReportDialog = false
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        onClick = onPostClicked
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Author info row with username and timestamp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Author info (left side)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Display name
                    Text(
                        text = post.authorName.ifEmpty { "Unknown User" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Add a dot separator
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Display username with @ symbol
                    Text(
                        text = "@${post.authorName.lowercase().replace(" ", "_")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Time ago and menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimeAgoText(timestamp = post.createdAt)

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            // Report option - available to all users
                            DropdownMenuItem(
                                text = { Text("Report post") },
                                leadingIcon = { Icon(Icons.Default.Close, "Report") },
                                onClick = {
                                    showMenu = false
                                    showReportDialog = true
                                }
                            )

                            // Delete option - only for author or admins
                            if (canDelete) {
                                DropdownMenuItem(
                                    text = { Text("Delete post") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onDeleteClicked()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Post content
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Like button with count
            LikeButton(
                isLiked = isLiked,
                likeCount = post.likeCount,
                onLikeClicked = onLikeClicked
            )
        }
    }
}

@Composable
fun ReportDialog(onDismiss: () -> Unit, onReport: (String) -> Unit) {
    var reportReason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Post") },
        text = {
            Column {
                Text("Please tell us why you're reporting this post:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reportReason,
                    onValueChange = { reportReason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onReport(reportReason) },
                enabled = reportReason.trim().isNotBlank()
            ) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = koinInject(),
    postViewModel: PostViewModel = koinInject()
) {
    val currentUserSnapshot by homeViewModel.currentUser.collectAsState()
    val posts by postViewModel.posts.collectAsState(emptyList())
    val isLoading by postViewModel.isLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showNewPostDialog by remember { mutableStateOf(false) }

    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(isLoading) {
        if (!isLoading) isRefreshing = false
    }

    val refreshHandler = rememberRefreshHandler(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            postViewModel.getAllPosts(forceRefresh = true)
        }
    )

    LaunchedEffect(Unit) {
        postViewModel.getAllPosts()
    }

    if (showNewPostDialog) {
        NewPostDialog(
            onDismiss = { showNewPostDialog = false },
            onPostCreated = { content: String ->
                postViewModel.createPost(content)
                showNewPostDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Post created!")
                }
                postViewModel.getAllPosts(forceRefresh = true)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feed") },
                actions = {
                    if (isDesktop()) {
                        IconButton(onClick = { refreshHandler.refresh() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewPostDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New post")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        val localCurrentUser = currentUserSnapshot

        if (localCurrentUser == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading feed…")
            }
            return@Scaffold
        }

        if (isLoading && !isRefreshing) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        RefreshableContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            refreshHandler = refreshHandler
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 72.dp) // For FAB
            ) {
                item {
                    Text(
                        text = "Welcome back, ${localCurrentUser.name}!",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                    HorizontalDivider(
                        Modifier.padding(horizontal = 16.dp),
                        DividerDefaults.Thickness, color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(8.dp))
                }

                items(posts) { post ->
                    PostCard(
                        post = post,
                        isLiked = post.isLikedByCurrentUser,
                        onLikeClicked = { postViewModel.likePost(post.id) },
                        onPostClicked = { navController.navigate(PostDetail(post.id)) },
                        currentUser = localCurrentUser,
                        onReportClicked = { reason ->
                            postViewModel.reportPost(post.id, reason)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Report submitted")
                            }
                        },
                        onDeleteClicked = {
                            postViewModel.deletePost(post.id)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Post deleted")
                            }
                        }
                    )
                }

                if (posts.isEmpty() && !isLoading) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No posts yet. Create one!")
                        }
                    }
                }
            }
        }
    }
}