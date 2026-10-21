package com.example.etapa1.ui.components

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
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
        emergencyPhoneRelation: String,
        authorizedPickups: List<String>,
        medicalCertificateUri: String?,
        medicalCertificateName: String?,
        medicalCertificateDate: String?,
        bloodType: String
    ) -> Unit
) {
    val context = LocalContext.current

    // Campos editables (solo aquellos que tiene sentido modificar por la familia)
    var bloodType by remember { mutableStateOf(fullProfile.bloodType.ifBlank { "O+" }) }
    var pediatrician by remember { mutableStateOf(fullProfile.pediatrician) }
    var pediatricianPhone by remember { mutableStateOf(fullProfile.pediatricianPhone) }
    var medicalNotes by remember { mutableStateOf(fullProfile.medicalNotes) }
    var habitsAndPedagogicalNotes by remember { mutableStateOf(fullProfile.generalNotes) }
    var emergencyPhone by remember { mutableStateOf(fullProfile.emergencyPhone) }
    var emergencyPhoneRelation by remember { mutableStateOf(fullProfile.emergencyPhoneRelation) }
    var emergencyRelationError by remember { mutableStateOf(false) }

    // Personas autorizadas (México: Nombre, Parentesco, CURP/INE y Teléfono)
    val authorizedPickups = remember {
        mutableStateListOf<String>().apply { addAll(fullProfile.authorizedPickups) }
    }
    var newPersonName by remember { mutableStateOf("") }
    var newPersonRelationship by remember { mutableStateOf("") }
    var newPersonCurp by remember { mutableStateOf("") }
    var newPersonPhone by remember { mutableStateOf("") }

    // Estado del certificado médico
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
                                size = 40.dp,
                                showStatusDot = false
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Actualizar Ficha del Alumno",
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue
                                )
                                Text(
                                    text = child.fullName,
                                    fontSize = 12.5.sp,
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
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Datos Oficiales SEP (Solo lectura: no modificables por la familia)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = BrandBlue.copy(alpha = 0.04f)),
                        border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = BrandBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Datos Oficiales de Matrícula",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandBlue
                                    )
                                }
                                Surface(
                                    color = BrandBlue.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Solo Lectura",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BrandBlue,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            ReadOnlyRowItem("Nombre del menor:", child.fullName)
                            ReadOnlyRowItem("CURP / Matrícula:", fullProfile.studentId)
                            ReadOnlyRowItem("Fecha de nacimiento:", "${fullProfile.birthDate} (${child.ageText})")
                            ReadOnlyRowItem("Sala y grupo:", "${child.roomText} • ${child.groupText}")

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "El nombre oficial, CURP y sala asignada son registros escolares oficiales ante la SEP. Para solicitar una corrección de datos legales, acude a la dirección del plantel.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    // Sección 1: Salud y Observaciones Médicas
                    FormSection(
                        title = "Salud y Cuidados Médicos",
                        icon = Icons.Default.LocalHospital
                    ) {
                        Text(
                            text = "Grupo sanguíneo del menor:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Selector rápido de grupo sanguíneo común en México
                        val bloodTypes = listOf("O+", "A+", "B+", "AB+", "O-", "A-")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            bloodTypes.forEach { type ->
                                val isSelected = bloodType.equals(type, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) BrandBlue else AppBackground,
                                    border = BorderStroke(1.dp, if (isSelected) BrandBlue else BorderSubtle),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { bloodType = type }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = type,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else TextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = medicalNotes,
                            onValueChange = { medicalNotes = it },
                            label = { Text("Alergias, intolerancias y cuidados de salud") },
                            placeholder = { Text("Ej: Esquema de vacunación nacional al día. Intolerancia leve a la lactosa...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp),
                            colors = outlinedTextFieldColors()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = pediatrician,
                            onValueChange = { pediatrician = it },
                            label = { Text("Pediatra / Hospital o Clínica") },
                            placeholder = { Text("Ej: Dra. Sofía Morales (Hospital Ángeles)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = outlinedTextFieldColors()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = pediatricianPhone,
                            onValueChange = { pediatricianPhone = it },
                            label = { Text("Teléfono del pediatra o consultorio") },
                            placeholder = { Text("+52 55 1234 5678") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = outlinedTextFieldColors()
                        )
                    }

                    // Sección 2: Certificado Médico Oficial
                    FormSection(
                        title = "Certificado Médico Oficial",
                        icon = Icons.Default.Description
                    ) {
                        Text(
                            text = "Adjunta el certificado médico expedido por médico con cédula profesional para acreditar la salud y esquema de vacunación del alumno.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (certificateName != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StatusPresentGreenBg,
                                border = BorderStroke(1.dp, StatusPresentGreen.copy(alpha = 0.35f)),
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
                                border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.5f)),
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
                            OutlinedButton(
                                onClick = { filePickerLauncher.launch("*/*") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BrandBlue),
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

                    // Sección 3: Hábitos y Recomendaciones Pedagógicas
                    FormSection(
                        title = "Hábitos y Recomendaciones Pedagógicas",
                        icon = Icons.Default.Face
                    ) {
                        OutlinedTextField(
                            value = habitsAndPedagogicalNotes,
                            onValueChange = { habitsAndPedagogicalNotes = it },
                            label = { Text("Hábitos de descanso, comidas y adaptación") },
                            placeholder = { Text("Ej: Duerme siesta con su mantita de apego. Come de forma autónoma...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp),
                            colors = outlinedTextFieldColors()
                        )
                    }

                    // Sección 4: Contacto de Emergencia
                    FormSection(
                        title = "Contacto de Emergencia",
                        icon = Icons.Default.Phone
                    ) {
                        OutlinedTextField(
                            value = emergencyPhoneRelation,
                            onValueChange = {
                                emergencyPhoneRelation = it
                                if (it.isNotBlank()) emergencyRelationError = false
                            },
                            label = { Text("Parentesco / Relación *") },
                            placeholder = { Text("Ej: Madre, Padre, Abuela materna, Tutor legal") },
                            isError = emergencyRelationError,
                            supportingText = if (emergencyRelationError) {
                                { Text("Debes indicar el parentesco del contacto de emergencia", color = AlertRed) }
                            } else null,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = outlinedTextFieldColors()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = emergencyPhone,
                            onValueChange = { emergencyPhone = it },
                            label = { Text("Teléfono de emergencia") },
                            placeholder = { Text("+52 55 9876 5432") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = outlinedTextFieldColors()
                        )
                    }

                    // Sección 5: Personas Autorizadas para Recogida (México)
                    FormSection(
                        title = "Personas Autorizadas para Recogida",
                        icon = Icons.Default.FamilyRestroom
                    ) {
                        Text(
                            text = "Personas autorizadas para retirar al menor del plantel educativo (con CURP o INE):",
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
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = BrandBlue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = person,
                                                fontSize = 12.5.sp,
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Formulario de nueva persona autorizada (Nombre, Parentesco, CURP/INE y Teléfono)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BrandBlue.copy(alpha = 0.04f)),
                            border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.2f))
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
                                        value = newPersonCurp,
                                        onValueChange = { newPersonCurp = it },
                                        label = { Text("CURP / INE") },
                                        placeholder = { Text("CURP o INE") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = outlinedTextFieldColors()
                                    )
                                }

                                OutlinedTextField(
                                    value = newPersonPhone,
                                    onValueChange = { newPersonPhone = it },
                                    label = { Text("Teléfono de contacto") },
                                    placeholder = { Text("+52 55 1234 5678") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = outlinedTextFieldColors()
                                )

                                val canAddPerson = newPersonName.isNotBlank() &&
                                    newPersonRelationship.isNotBlank() &&
                                    newPersonCurp.isNotBlank()

                                Button(
                                    onClick = {
                                        if (canAddPerson) {
                                            val phoneSuffix = if (newPersonPhone.isNotBlank()) " • Tel: ${newPersonPhone.trim()}" else ""
                                            val formatted = "${newPersonName.trim()} (${newPersonRelationship.trim()}) - CURP: ${newPersonCurp.trim().uppercase()}$phoneSuffix"
                                            authorizedPickups.add(formatted)
                                            newPersonName = ""
                                            newPersonRelationship = ""
                                            newPersonCurp = ""
                                            newPersonPhone = ""
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
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Text("Cancelar", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                if (emergencyPhone.isNotBlank() && emergencyPhoneRelation.isBlank()) {
                                    emergencyRelationError = true
                                    return@Button
                                }
                                if (newPersonName.isNotBlank() && newPersonRelationship.isNotBlank() && newPersonCurp.isNotBlank()) {
                                    val phoneSuffix = if (newPersonPhone.isNotBlank()) " • Tel: ${newPersonPhone.trim()}" else ""
                                    val formatted = "${newPersonName.trim()} (${newPersonRelationship.trim()}) - CURP: ${newPersonCurp.trim().uppercase()}$phoneSuffix"
                                    authorizedPickups.add(formatted)
                                }
                                onSave(
                                    pediatrician,
                                    pediatricianPhone,
                                    medicalNotes,
                                    habitsAndPedagogicalNotes,
                                    emergencyPhone,
                                    emergencyPhoneRelation,
                                    authorizedPickups.toList(),
                                    certificateUri,
                                    certificateName,
                                    certificateDate,
                                    bloodType
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
                            Text("Guardar Cambios", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyRowItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
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
