package ui.auth


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import navigation.ForgotPassword
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import playscore.composeapp.generated.resources.Res
import playscore.composeapp.generated.resources.transparent_playscore_logo
import ui.components.AuthButton
import ui.components.AuthDivider
import ui.components.AuthMessage
import ui.components.AuthTextField
import ui.components.EmailVerificationBanner
import ui.theme.AppTheme
import viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = koinInject()
) {
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loginMessage by viewModel.loginMessage.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isEmailVerificationRequired by viewModel.isEmailVerificationRequired.collectAsState()

    // State for showing banned dialog
    var showBannedDialog by remember { mutableStateOf(false) }

    // Check if login message indicates banned status
    LaunchedEffect(loginMessage) {
        if (loginMessage?.contains("banned") == true) {
            showBannedDialog = true
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            navController.navigate("navigation.Home") {
                // Use route-based navigation instead of ID-based
                popUpTo("navigation.Login") {
                    inclusive = true
                }
            }
            viewModel.resetLoginState()
        }
    }

    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + slideInVertically(
                        initialOffsetY = { -50 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Email verification banner at the top
                        EmailVerificationBanner(
                            isVisible = isEmailVerificationRequired,
                            onResendClick = { viewModel.resendVerificationEmail() },
                            isLoading = isLoading
                        )

                        // App Logo
                        Image(
                            painter = painterResource(Res.drawable.transparent_playscore_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(180.dp)
                                .padding(bottom = 10.dp)
                        )

                        Text(
                            text = "Welcome Back",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Sign in to continue",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AuthTextField(
                            value = email,
                            onValueChange = viewModel::onEmailChanged,
                            label = "Email",
                            leadingIcon = Icons.Filled.Email,
                            keyboardType = KeyboardType.Email
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        AuthTextField(
                            value = password,
                            onValueChange = viewModel::onPasswordChanged,
                            label = "Password",
                            leadingIcon = Icons.Filled.Lock,
                            isPassword = true,
                            imeAction = ImeAction.Done,
                            onImeAction = {
                                if (email.isNotBlank() && password.isNotBlank()) {
                                    viewModel.login()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        AuthButton(
                            text = "Sign In",
                            onClick = { viewModel.login() },
                            isLoading = isLoading,
                            enabled = email.isNotBlank() && password.isNotBlank()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        loginMessage?.let { message ->
                            AuthMessage(
                                message = message,
                                isError = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(onClick = { navController.navigate(ForgotPassword) }) {
                            Text(
                                text = "Forgot Password?",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                AuthDivider()

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { navController.navigate("navigation.Register") }) {
                    Text(
                        text = "Don't have an account? Sign Up",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    // Banned user dialog
    if (showBannedDialog) {
        AlertDialog(
            onDismissRequest = { showBannedDialog = false },
            title = { Text("Account Suspended") },
            text = {
                Text("Your account has been banned due to violation of our terms of service. " +
                        "If you believe this is an error, please contact support.")
            },
            confirmButton = {
                Button(onClick = { showBannedDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}