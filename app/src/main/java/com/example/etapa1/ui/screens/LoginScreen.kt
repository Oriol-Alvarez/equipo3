package com.example.etapa1.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.SonrisasApplication
import com.example.etapa1.data.remote.NetworkClient
import com.example.etapa1.model.UserRole
import com.example.etapa1.ui.components.DaycareLogo
import com.example.etapa1.ui.theme.AlertRed
import com.example.etapa1.ui.theme.AlertRedBg
import com.example.etapa1.ui.theme.AlertRedBorder
import com.example.etapa1.ui.theme.AppBackground
import com.example.etapa1.ui.theme.BorderSubtle
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.CardBackground
import com.example.etapa1.ui.theme.TextMuted
import com.example.etapa1.ui.theme.TextPrimary
import com.example.etapa1.ui.theme.TextSecondary
import com.example.etapa1.ui.theme.Etapa1Theme
import androidx.compose.ui.tooling.preview.Preview
import com.example.etapa1.ui.state.LoginViewModel

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (UserRole) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    LoginContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChanged,
        onPasswordChange = viewModel::onPasswordChanged,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onSubmitLogin = {
            scope.launch {
                viewModel.submitLogin()?.let(onLoginSuccess)
            }
        }
    )
}

@Composable
fun LoginScreen(
    onLoginSuccess: (UserRole) -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val state = com.example.etapa1.ui.state.LoginUiState(
        email = email,
        password = password,
        passwordVisible = passwordVisible,
        hasError = hasError,
        errorMessage = errorMessage
    )

    LoginContent(
        uiState = state,
        onEmailChange = { email = it; hasError = false; errorMessage = null },
        onPasswordChange = { password = it; hasError = false; errorMessage = null },
        onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
        onSubmitLogin = {
            if (email.isBlank() || password.isBlank()) {
                hasError = true
                errorMessage = "Por favor, introduce tu correo y contraseña."
            } else {
                val role = when {
                    email.contains("familiar", ignoreCase = true) -> UserRole.FAMILIAR
                    email.contains("admin", ignoreCase = true) || email.contains("direccion", ignoreCase = true) -> UserRole.ADMINISTRADOR
                    else -> UserRole.EDUCADORA
                }
                onLoginSuccess(role)
            }
        }
    )
}

@Composable
fun LoginContent(
    uiState: com.example.etapa1.ui.state.LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmitLogin: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var showCodeDialog by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }
    var adminCodeInput by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf<String?>(null) }
    var serverUrlInput by remember { mutableStateOf(NetworkClient.baseUrl) }
    var serverUrlError by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Logo & Header (mantener presionado para configurar el servidor)
            DaycareLogo(
                size = 76.dp,
                modifier = Modifier
                    .clip(CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                adminCodeInput = ""
                                codeError = null
                                showCodeDialog = true
                            }
                        )
                    }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Sonrisas de Cristal",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = BrandBlue
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Conectando momentos de cuidado",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Campo Correo electrónico
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Correo electrónico",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = onEmailChange,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    singleLine = true,
                    isError = uiState.hasError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground,
                        focusedBorderColor = if (uiState.hasError) AlertRed else BrandBlue,
                        unfocusedBorderColor = if (uiState.hasError) AlertRed else BorderSubtle,
                        errorBorderColor = AlertRed
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo Contraseña
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Contraseña",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    singleLine = true,
                    isError = uiState.hasError,
                    visualTransformation = if (uiState.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = onTogglePasswordVisibility) {
                            Icon(
                                imageVector = if (uiState.passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (uiState.passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground,
                        focusedBorderColor = if (uiState.hasError) AlertRed else BrandBlue,
                        unfocusedBorderColor = if (uiState.hasError) AlertRed else BorderSubtle,
                        errorBorderColor = AlertRed
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }

            // Banner de error (si está activo)
            if (uiState.hasError) {
                val errorText = uiState.errorMessage ?: "Correo o contraseña incorrectos"
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AlertRedBg)
                        .border(1.dp, AlertRedBorder, RoundedCornerShape(16.dp))
                        .padding(vertical = 10.dp, horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorText,
                        color = AlertRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón Iniciar Sesión
            Button(
                onClick = onSubmitLogin,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandBlue,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (uiState.isLoading) "Entrando…" else "Iniciar Sesión",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

        }

        if (showCodeDialog) {
            AlertDialog(
                onDismissRequest = { showCodeDialog = false },
                containerColor = CardBackground,
                title = {
                    Text(
                        text = "Acceso de Administrador",
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Introduce el código para configurar la URL del servidor:",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = adminCodeInput,
                            onValueChange = {
                                adminCodeInput = it
                                codeError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            placeholder = { Text("Código de acceso") },
                            isError = codeError != null,
                            supportingText = {
                                if (codeError != null) {
                                    Text(text = codeError!!, color = AlertRed, fontSize = 12.sp)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = if (codeError != null) AlertRed else BrandBlue,
                                unfocusedBorderColor = if (codeError != null) AlertRed else BorderSubtle,
                                errorBorderColor = AlertRed
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (adminCodeInput.trim() == "admin_equipo3_13042005") {
                                showCodeDialog = false
                                serverUrlInput = NetworkClient.baseUrl
                                serverUrlError = null
                                showServerDialog = true
                            } else {
                                codeError = "Código incorrecto"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Verificar", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCodeDialog = false }) {
                        Text("Cancelar", color = TextSecondary)
                    }
                }
            )
        }

        if (showServerDialog) {
            AlertDialog(
                onDismissRequest = { showServerDialog = false },
                containerColor = CardBackground,
                title = {
                    Text(
                        text = "Configuración del Servidor",
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Introduce la nueva URL del backend (ej. Cloudflare, ngrok o IP local):",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = serverUrlInput,
                            onValueChange = {
                                serverUrlInput = it
                                serverUrlError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            label = { Text("URL del Servidor") },
                            placeholder = { Text("http://10.0.2.2:8000/api/v1/") },
                            isError = serverUrlError != null,
                            supportingText = {
                                if (serverUrlError != null) {
                                    Text(text = serverUrlError!!, color = AlertRed, fontSize = 12.sp)
                                } else {
                                    Text(
                                        text = "Actual: ${NetworkClient.baseUrl}",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = if (serverUrlError != null) AlertRed else BrandBlue,
                                unfocusedBorderColor = if (serverUrlError != null) AlertRed else BorderSubtle,
                                errorBorderColor = AlertRed
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    serverUrlInput = NetworkClient.DEFAULT_BASE_URL
                                    serverUrlError = null
                                }
                            ) {
                                Text(
                                    text = "Restablecer por defecto",
                                    fontSize = 12.sp,
                                    color = BrandBlue
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (serverUrlInput.isBlank()) {
                                serverUrlError = "La URL no puede estar vacía"
                            } else {
                                val updatedUrl = NetworkClient.updateBaseUrl(context, serverUrlInput)
                                (context.applicationContext as? SonrisasApplication)?.resetContainer()
                                Toast.makeText(
                                    context,
                                    "Servidor actualizado:\n$updatedUrl",
                                    Toast.LENGTH_LONG
                                ).show()
                                showServerDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Guardar", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showServerDialog = false }) {
                        Text("Cancelar", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    Etapa1Theme {
        LoginScreen(onLoginSuccess = {})
    }
}

