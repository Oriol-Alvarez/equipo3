package com.example.etapa1.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.etapa1.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.ui.theme.AlertRed
import com.example.etapa1.ui.theme.AlertRedBg
import com.example.etapa1.ui.theme.AlertRedBorder
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.BrandBlueContainer
import com.example.etapa1.ui.theme.BrandBlueLight
import com.example.etapa1.ui.theme.CardBackground
import com.example.etapa1.ui.theme.StatusAbsentOrange
import com.example.etapa1.ui.theme.StatusPresentGreen
import com.example.etapa1.ui.theme.TextMuted
import com.example.etapa1.ui.theme.TextPrimary
import com.example.etapa1.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

/**
 * Logo de 'Sonrisas de Cristal' cargado desde el Image Asset logo (R.mipmap.logo)
 */
@Composable
fun DaycareLogo(
    modifier: Modifier = Modifier,
    size: Dp = 82.dp
) {
    val context = LocalContext.current
    val drawable = remember {
        ContextCompat.getDrawable(context, R.mipmap.logo)
    }
    val painter = remember(drawable) {
        if (drawable != null) {
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 300
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 300
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            BitmapPainter(bitmap.asImageBitmap())
        } else {
            null
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, Color(0xFFE0E7F1), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (painter != null) {
                Image(
                    painter = painter,
                    contentDescription = "Logo Sonrisas de Cristal",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

/**
 * Barra de navegación inferior
 */
@Composable
fun AppBottomBar(
    selectedTab: Int = 0,
    showMessagesTab: Boolean = true,
    onTabSelected: (Int) -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBackground,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 12.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 0: Inicio
            BottomNavItem(
                modifier = Modifier.weight(1f),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Inicio",
                        tint = if (selectedTab == 0) BrandBlue else TextMuted
                    )
                },
                label = "Inicio",
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) }
            )

            // Tab 1 (opcional): Mensajes
            if (showMessagesTab) {
                BottomNavItem(
                    modifier = Modifier.weight(1f),
                    icon = {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MailOutline,
                                contentDescription = "Mensajes",
                                tint = if (selectedTab == 1) BrandBlue else TextMuted
                            )
                            // Notification dot
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE53935))
                            )
                        }
                    },
                    label = "Mensajes",
                    isSelected = selectedTab == 1,
                    onClick = { onTabSelected(1) }
                )
            }

            // Tab Sugerencias (índice 2 con mensajes, índice 1 sin mensajes)
            val suggestionsTabIndex = if (showMessagesTab) 2 else 1
            BottomNavItem(
                modifier = Modifier.weight(1f),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = "Buzón de sugerencias",
                        tint = if (selectedTab == suggestionsTabIndex) BrandBlue else TextMuted
                    )
                },
                label = "Sugerencias",
                isSelected = selectedTab == suggestionsTabIndex,
                onClick = { onTabSelected(suggestionsTabIndex) }
            )

            // Tab Objetos perdidos (índice 3 con mensajes, índice 2 sin mensajes)
            val lostObjectsTabIndex = if (showMessagesTab) 3 else 2
            BottomNavItem(
                modifier = Modifier.weight(1f),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = "Objetos perdidos",
                        tint = if (selectedTab == lostObjectsTabIndex) BrandBlue else TextMuted
                    )
                },
                label = "Obj. perdidos",
                isSelected = selectedTab == lostObjectsTabIndex,
                onClick = { onTabSelected(lostObjectsTabIndex) }
            )

            // Tab Más (índice 4 con mensajes, índice 3 sin mensajes)
            val moreTabIndex = if (showMessagesTab) 4 else 3
            BottomNavItem(
                modifier = Modifier.weight(1f),
                icon = {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Más",
                        tint = if (selectedTab == moreTabIndex) BrandBlue else TextMuted
                    )
                },
                label = "Más",
                isSelected = selectedTab == moreTabIndex,
                onClick = { onTabSelected(moreTabIndex) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 30.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isSelected) BrandBlueContainer else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) BrandBlue else TextMuted,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * Avatar circular de alumno con iniciales e indicador de estado
 */
@Composable
fun ChildAvatar(
    initials: String,
    bgColor: Color,
    size: Dp = 48.dp,
    showStatusDot: Boolean = false,
    isPresent: Boolean = true
) {
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(bgColor)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                fontSize = (size.value * 0.38f).sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        if (showStatusDot) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(if (isPresent) StatusPresentGreen else StatusAbsentOrange)
                    .border(1.5.dp, Color.White, CircleShape)
            )
        }
    }
}

/**
 * Banner de alerta médica (Alergia a Nuez)
 */
@Composable
fun MedicalAlertBanner(
    text: String = "Alergia a Nuez (No Certificado)",
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AlertRedBg)
            .border(1.dp, AlertRedBorder, RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Alerta médica",
            tint = AlertRed,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AlertRed
        )
        if (onClick != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Ver detalle",
                tint = AlertRed,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Recuadro para previsualizar o indicar claramente que va una imagen allí.
 */
@Composable
fun ImagePlaceholderBox(
    modifier: Modifier = Modifier,
    imageUri: String? = null,
    height: Dp = 190.dp,
    placeholderLabel: String = "Fotografía",
    isError: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val bitmap = remember(imageUri) {
        if (!imageUri.isNullOrBlank()) {
            try {
                val uri = Uri.parse(imageUri)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    val shape = RoundedCornerShape(14.dp)
    val borderColor = if (isError) AlertRedBorder else Color(0xFFD1D5DB)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(Color(0xFFF3F4F6))
            .border(
                width = if (isError) 1.5.dp else 1.dp,
                color = borderColor,
                shape = shape
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = placeholderLabel,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(BrandBlueContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Recuadro de imagen",
                        tint = BrandBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = placeholderLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }
        }
    }
}

