package ui.post

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import data.model.Post
import kotlinx.coroutines.launch
import navigation.PostDetail
import org.koin.compose.koinInject
import ui.components.rememberRefreshHandler
import ui.home.PostCard
import ui.home.TimeAgoText
import viewmodel.PostViewModel
import ui.components.RefreshableContainer
import viewmodel.HomeViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    navController: NavController,
    postViewModel: PostViewModel = koinInject(),
    homeViewModel: HomeViewModel = koinInject()
) {
    val comments by postViewModel.comments.collectAsState(emptyList())
    val isLoading by postViewModel.isLoading.collectAsState()
    var commentText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val currentPost by postViewModel.currentPost.collectAsState()
    val parentPost by postViewModel.parentPost.collectAsState()

    LaunchedEffect(postId) {
        println("PostDetailScreen: Received postId: $postId")
        postViewModel.getPostById(postId)
        postViewModel.getCommentsForPost(postId)
        postViewModel.getParentPostIfExists(postId)
    }

    LaunchedEffect(currentPost) {
        println("Current post: $currentPost")
    }

    Scaffold(
        snackbarHost = { SnackbarHost(
            snackbarHostState,
            modifier = Modifier.padding(bottom = 72.dp),
            snackbar = { snackbarData ->
                Snackbar(
                    snackbarData = snackbarData,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    actionColor = MaterialTheme.colorScheme.secondary
                )
            }
        ) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val refreshHandler = rememberRefreshHandler(
                isRefreshing = isLoading,
                onRefresh = {
                    postViewModel.getCommentsForPost(postId)
                }
            )

            RefreshableContainer(
                refreshHandler = refreshHandler,
                modifier = Modifier.weight(1f)
            ){
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    // Parent post (if this is a reply)
                    parentPost?.let { parent ->
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Replying to",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "Replying to post by ${parent.authorName}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(
                                        text = parent.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TimeAgoText(timestamp = parent.createdAt)
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .padding(start = 32.dp)
                                    .height(24.dp)
                                    .width(2.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            )
                        }
                    }

                    // Current post
                    if (currentPost != null) {
                        item {
                            currentPost?.let { post ->
                                PostCard(
                                    post = post,
                                    isLiked = post.isLikedByCurrentUser,
                                    onLikeClicked = { postViewModel.likePost(post.id) },
                                    onPostClicked = {}
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Create,
                                        contentDescription = "Comments",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Comments (${comments.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    } else if (!isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Create,
                                        contentDescription = "Post not found",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "Post not found",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        "This post may have been deleted or does not exist.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Comments
                    if (comments.isNotEmpty()) {
                        items(comments) { comment ->
                            CommentItem(
                                comment = comment,
                                onLikeClicked = { postViewModel.likePost(comment.id) },
                                onCommentClicked = {
                                    navController.navigate(PostDetail(comment.id))
                                },
                                currentUser = homeViewModel.currentUser.collectAsState().value,
                                onReportClicked = { reason ->
                                    postViewModel.reportPost(comment.id, reason)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Report submitted")
                                    }
                                },
                                onDeleteClicked = {
                                    postViewModel.deletePost(comment.id)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Comment deleted")
                                    }
                                }
                            )
                        }
                    } else if (!isLoading && currentPost != null) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Create,
                                        contentDescription = "No comments",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "No comments yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        "Be the first to comment!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }

                    // Loading indicator
                    if (isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }


            // Comment input (shown only if post is loaded or loading)
            if (currentPost != null || isLoading) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text("Add a comment...") },
                            modifier = Modifier.weight(1f),
                            maxLines = 3,
                            shape = MaterialTheme.shapes.medium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val trimmedComment = commentText.trim()
                                if (trimmedComment.isNotBlank() && postId.isNotBlank()) {
                                    postViewModel.createPost(
                                        content = trimmedComment,
                                        parentPostId = postId
                                    )
                                    commentText = ""
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Comment posted!")
                                    }
                                }
                            },
                            enabled = commentText.isNotBlank(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Post comment")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Post")
                        }
                    }
                }
            }
        }
    }
}