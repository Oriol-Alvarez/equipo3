package com.example.etapa1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.model.UserRole
import com.example.etapa1.ui.components.DaycareLogo
import com.example.etapa1.ui.theme.AlertRed
import com.example.etapa1.ui.theme.AlertRedBg
import com.example.etapa1.ui.theme.AlertRedBorder
import com.example.etapa1.ui.theme.AppBackground
import com.example.etapa1.ui.theme.BorderSubtle
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.BrandBlueContainer
import com.example.etapa1.ui.theme.CardBackground
import com.example.etapa1.ui.theme.TextMuted
import com.example.etapa1.ui.theme.TextPrimary
import com.example.etapa1.ui.theme.TextSecondary
import com.example.etapa1.ui.theme.Etapa1Theme
import androidx.compose.ui.tooling.preview.Preview
import com.example.etapa1.ui.state.LoginViewModel

import androidx.compose.runtime.collectAsState

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (UserRole) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LoginContent(
        uiState = uiState,
        onRoleSelected = viewModel::onRoleSelected,
        onEmailChange = viewModel::onEmailChanged,
        onPasswordChange = viewModel::onPasswordChanged,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onErrorToggle = viewModel::toggleError,
        onSubmitLogin = {
            val role = viewModel.submitLogin()
            if (role != null) {
                onLoginSuccess(role)
            }
        }
    )
}

@Composable
fun LoginScreen(
    onLoginSuccess: (UserRole) -> Unit = {}
) {
    var selectedRole by remember { mutableStateOf(UserRole.EDUCADORA) }
    var email by remember { mutableStateOf("correo@ejemplo.com") }
    var password by remember { mutableStateOf("••••••••") }
    var passwordVisible by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    val state = com.example.etapa1.ui.state.LoginUiState(
        selectedRole = selectedRole,
        email = email,
        password = password,
        passwordVisible = passwordVisible,
        hasError = hasError
    )

    LoginContent(
        uiState = state,
        onRoleSelected = { selectedRole = it },
        onEmailChange = { email = it; hasError = false },
        onPasswordChange = { password = it; hasError = false },
        onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
        onErrorToggle = { hasError = !hasError },
        onSubmitLogin = {
            if (email.isBlank() || password.isBlank()) {
                hasError = true
            } else {
                onLoginSuccess(selectedRole)
            }
        }
    )
}

@Composable
fun LoginContent(
    uiState: com.example.etapa1.ui.state.LoginUiState,
    onRoleSelected: (UserRole) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onErrorToggle: () -> Unit,
    onSubmitLogin: () -> Unit
) {
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

            // Logo & Header
            DaycareLogo(size = 76.dp)

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

            Spacer(modifier = Modifier.height(26.dp))

            // Selector Educadora / Familiar
            RoleToggle(
                selectedRole = uiState.selectedRole,
                onRoleSelected = onRoleSelected
            )

            Spacer(modifier = Modifier.height(24.dp))

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
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AlertRedBg)
                        .border(1.dp, AlertRedBorder, RoundedCornerShape(16.dp))
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Correo o contraseña incorrectos",
                        color = AlertRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón Iniciar Sesión
            Button(
                onClick = onSubmitLogin,
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
                    text = "Iniciar Sesión",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Link Recuperar contraseña
            Text(
                text = "Recuperar contraseña",
                fontSize = 13.sp,
                color = BrandBlue,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { /* Simulación de recuperación */ }
                    .padding(4.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Switch sutil para probar estado de error vs normal
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (uiState.hasError) "Quitar error" else "Simular error de login",
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier
                        .clickable { onErrorToggle() }
                        .padding(6.dp)
                )
            }
        }
    }
}

@Composable
private fun RoleToggle(
    selectedRole: UserRole,
    onRoleSelected: (UserRole) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(BrandBlueContainer)
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selectedRole == UserRole.EDUCADORA) BrandBlue else Color.Transparent)
                    .clickable { onRoleSelected(UserRole.EDUCADORA) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Educadora",
                    color = if (selectedRole == UserRole.EDUCADORA) Color.White else BrandBlue,
                    fontSize = 13.sp,
                    fontWeight = if (selectedRole == UserRole.EDUCADORA) FontWeight.Bold else FontWeight.Medium
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selectedRole == UserRole.FAMILIAR) BrandBlue else Color.Transparent)
                    .clickable { onRoleSelected(UserRole.FAMILIAR) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Familiar",
                    color = if (selectedRole == UserRole.FAMILIAR) Color.White else BrandBlue,
                    fontSize = 13.sp,
                    fontWeight = if (selectedRole == UserRole.FAMILIAR) FontWeight.Bold else FontWeight.Medium
                )
            }
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

