package com.example.etapa1.ui.components

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.etapa1.model.Child
import com.example.etapa1.model.ChildFullProfile
import com.example.etapa1.ui.theme.AlertRed
import com.example.etapa1.ui.theme.AlertRedBg
import com.example.etapa1.ui.theme.AppBackground
import com.example.etapa1.ui.theme.BorderSubtle
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.BrandBlueContainer
import com.example.etapa1.ui.theme.CardBackground
import com.example.etapa1.ui.theme.StatusPresentGreen
import com.example.etapa1.ui.theme.StatusPresentGreenBg
import com.example.etapa1.ui.theme.TextMuted
import com.example.etapa1.ui.theme.TextPrimary
import com.example.etapa1.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EditChildProfileDialog(
    child: Child,
    fullProfile: ChildFullProfile,
    onDismiss: () -> Unit,
    onSave: (
        pediatrician: String,
        pediatricianPhone: String,
        medicalNotes: String,
        habitsAndPedagogicalNotes: String,
        emergencyPhone: String,
        authorizedPickups: List<String>,
        medicalCertificateUri: String?,
        medicalCertificateName: String?,
        medicalCertificateDate: String?
    ) -> Unit
) {
    val context = LocalContext.current

    // Form fields
    var pediatrician by remember { mutableStateOf(fullProfile.pediatrician) }
    var pediatricianPhone by remember { mutableStateOf(fullProfile.pediatricianPhone) }
    var medicalNotes by remember { mutableStateOf(fullProfile.medicalNotes) }
    var habitsAndPedagogicalNotes by remember { mutableStateOf(fullProfile.generalNotes) }
    var emergencyPhone by remember { mutableStateOf(fullProfile.emergencyPhone) }

    val authorizedPickups = remember {
        mutableStateListOf<String>().apply { addAll(fullProfile.authorizedPickups) }
    }
    var newPersonName by remember { mutableStateOf("") }
    var newPersonRelationship by remember { mutableStateOf("") }
    var newPersonDni by remember { mutableStateOf("") }

    // Medical certificate state
    var certificateUri by remember { mutableStateOf(fullProfile.medicalCertificateUri) }
    var certificateName by remember { mutableStateOf(fullProfile.medicalCertificateName) }
    var certificateDate by remember { mutableStateOf(fullProfile.medicalCertificateDate) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            certificateUri = uri.toString()
            var name = "Certificado_Medico.pdf"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }
            } catch (_: Exception) { }
            certificateName = name
            val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            certificateDate = "Certificado el $today"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = CardBackground,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header fijo
                Surface(
                    color = CardBackground,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ChildAvatar(
                                initials = child.avatarInitials,
                                bgColor = Color(child.avatarBgColor),
                                size = 38.dp,
                                showStatusDot = false
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Modificar Datos",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue
                                )
                                Text(
                                    text = child.fullName,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                // Formulario Scrolleable
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sección 1: Pediatra de referencia
                    FormSection(
                        title = "Pediatra de Referencia",
                        icon = Icons.Default.LocalHospital
                    ) {
                        OutlinedTextField(
                            value = pediatrician,
                            onValueChange = { pediatrician = it },
                            label = { Text("Nombre del pediatra") },
                            placeholder = { Text("Ej: Dra. Sofía Morales") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = outlinedTextFieldColors()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = pediatricianPhone,
                            onValueChange = { pediatricianPhone = it },
                            label = { Text("Teléfono del pediatra") },
                            placeholder = { Text("+34 555 111 222") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = outlinedTextFieldColors()
                        )
                    }

                    // Sección 2: Observaciones Médicas y Alergias
                    FormSection(
                        title = "Observaciones Médicas",
                        icon = Icons.Default.LocalHospital
                    ) {
                        OutlinedTextField(
                            value = medicalNotes,
                            onValueChange = { medicalNotes = it },
                            label = { Text("Alergias, medicación o cuidados médicos") },
                            placeholder = { Text("Ej: Alergia al huevo. Vacunación al día...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp),
                            colors = outlinedTextFieldColors()
                        )
                    }

                    // Sección 3: Certificado Médico Oficial
                    FormSection(
                        title = "Certificado Médico",
                        icon = Icons.Default.Description
                    ) {
                        Text(
                            text = "Adjunta un certificado médico oficial para certificar las observaciones médicas y alergias del alumno.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (certificateName != null) {
                            // Certificado actualmente adjuntado
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StatusPresentGreenBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, StatusPresentGreen.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = null,
                                            tint = StatusPresentGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = certificateName ?: "Certificado.pdf",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = StatusPresentGreen,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = certificateDate ?: "Certificado acreditado",
                                                fontSize = 11.5.sp,
                                                color = TextPrimary
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            certificateUri = null
                                            certificateName = null
                                            certificateDate = null
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Quitar certificado",
                                            tint = AlertRed
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { filePickerLauncher.launch("*/*") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BrandBlue.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandBlue)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.UploadFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reemplazar Certificado Médico", fontSize = 13.sp)
                            }
                        } else {
                            // Sin certificado adjunto
                            OutlinedButton(
                                onClick = { filePickerLauncher.launch("*/*") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BrandBlue),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = BrandBlueContainer.copy(alpha = 0.35f),
                                    contentColor = BrandBlue
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Adjuntar Certificado (PDF o Foto)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Sección 4: Hábitos y Observaciones Pedagógicas
                    FormSection(
                        title = "Hábitos y Observaciones Pedagógicas",
                        icon = Icons.Default.Face
                    ) {
                        OutlinedTextField(
                            value = habitsAndPedagogicalNotes,
                            onValueChange = { habitsAndPedagogicalNotes = it },
                            label = { Text("Hábitos de descanso, comida y desarrollo") },
                            placeholder = { Text("Ej: Duerme con su mantita. Come autónomamente...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp),
                            colors = outlinedTextFieldColors()
                        )
                    }

                    // Sección 5: Teléfono de Emergencia
                    FormSection(
                        title = "Contacto de Emergencia",
                        icon = Icons.Default.Phone
                    ) {
                        OutlinedTextField(
                            value = emergencyPhone,
                            onValueChange = { emergencyPhone = it },
                            label = { Text("Teléfono de emergencia") },
                            placeholder = { Text("+34 555 333 444") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = outlinedTextFieldColors()
                        )
                    }

                    // Sección 6: Personas Autorizadas para Recogida
                    FormSection(
                        title = "Personas Autorizadas para Recogida",
                        icon = Icons.Default.FamilyRestroom
                    ) {
                        Text(
                            text = "Personas autorizadas para recoger al niño en la escuela:",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Lista actual
                        if (authorizedPickups.isEmpty()) {
                            Text(
                                text = "No hay personas adicionales autorizadas.",
                                fontSize = 12.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            authorizedPickups.forEach { person ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = AppBackground,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = BrandBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = person,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TextPrimary
                                            )
                                        }

                                        IconButton(
                                            onClick = { authorizedPickups.remove(person) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Quitar",
                                                tint = AlertRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Formulario de nueva persona autorizada con campos separados: Nombre, Parentesco y DNI
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BrandBlue.copy(alpha = 0.04f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BrandBlue.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Añadir persona autorizada",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue
                                )

                                OutlinedTextField(
                                    value = newPersonName,
                                    onValueChange = { newPersonName = it },
                                    label = { Text("Nombre y apellidos") },
                                    placeholder = { Text("Ej: Carmen Morales García") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = outlinedTextFieldColors()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = newPersonRelationship,
                                        onValueChange = { newPersonRelationship = it },
                                        label = { Text("Parentesco") },
                                        placeholder = { Text("Ej: Abuela, Tío...") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = outlinedTextFieldColors()
                                    )

                                    OutlinedTextField(
                                        value = newPersonDni,
                                        onValueChange = { newPersonDni = it },
                                        label = { Text("DNI / NIE") },
                                        placeholder = { Text("12345678Z") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = outlinedTextFieldColors()
                                    )
                                }

                                val canAddPerson = newPersonName.isNotBlank() &&
                                    newPersonRelationship.isNotBlank() &&
                                    newPersonDni.isNotBlank()

                                Button(
                                    onClick = {
                                        if (canAddPerson) {
                                            val formatted = "${newPersonName.trim()} (${newPersonRelationship.trim()}) - DNI: ${newPersonDni.trim().uppercase()}"
                                            authorizedPickups.add(formatted)
                                            newPersonName = ""
                                            newPersonRelationship = ""
                                            newPersonDni = ""
                                        }
                                    },
                                    enabled = canAddPerson,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BrandBlue,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Añadir Persona Autorizada",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Footer de acciones fijas
                Surface(
                    color = CardBackground,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Text("Cancelar", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                if (newPersonName.isNotBlank() && newPersonRelationship.isNotBlank() && newPersonDni.isNotBlank()) {
                                    val formatted = "${newPersonName.trim()} (${newPersonRelationship.trim()}) - DNI: ${newPersonDni.trim().uppercase()}"
                                    authorizedPickups.add(formatted)
                                }
                                onSave(
                                    pediatrician,
                                    pediatricianPhone,
                                    medicalNotes,
                                    habitsAndPedagogicalNotes,
                                    emergencyPhone,
                                    authorizedPickups.toList(),
                                    certificateUri,
                                    certificateName,
                                    certificateDate
                                )
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandBlue,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guardar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlue
                )
            }
            content()
        }
    }
}

@Composable
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BrandBlue,
    unfocusedBorderColor = BorderSubtle,
    focusedLabelColor = BrandBlue,
    unfocusedLabelColor = TextSecondary,
    cursorColor = BrandBlue
)
