package ui.register

import androidx.compose.ui.unit.dp
import viewmodel.RegisterViewModel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import ui.components.AuthButton
import ui.components.AuthDivider
import ui.components.AuthMessage
import ui.components.AuthTextField
import ui.theme.AppTheme

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onBackToLogin: () -> Unit,
) {
    val name = viewModel.name
    val email = viewModel.email
    val password = viewModel.password
    val isLoading = viewModel.isLoading
    val registerResult = viewModel.registerResult

    // Replace isRegistered with isRegistrationComplete to match ViewModel
    val isRegistrationComplete = viewModel.isRegistrationComplete

    // Track if animation should play (only on first composition)
    var shouldAnimate by remember { mutableStateOf(true) }

    // Reset animation after first render
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1000)
        shouldAnimate = false
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
                        Text(
                            text = "Create Account",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Sign up to get started",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AuthTextField(
                            value = name,
                            onValueChange = viewModel::onNameChanged,
                            label = "Full Name",
                            leadingIcon = Icons.Filled.Person
                        )

                        Spacer(modifier = Modifier.height(16.dp))

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
                                if (name.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
                                    viewModel.register()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        AuthButton(
                            text = "Create Account",
                            onClick = { viewModel.register() },
                            isLoading = isLoading,
                            enabled = name.isNotBlank() && email.isNotBlank() && password.isNotBlank()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        registerResult?.let {
                            AuthMessage(
                                message = it,
                                isError = !it.startsWith("Registered")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "By signing up, you agree to our Terms of Service and Privacy Policy",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                AuthDivider()

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onBackToLogin) {
                    Text(
                        "Already have an account? Sign In",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}