package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleLight
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextGray
import com.example.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onClose: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val saveMessage by viewModel.saveMessage.collectAsStateWithLifecycle()
    var currencyExpanded by remember { mutableStateOf(false) }
    val currencies = listOf("S/ PEN", "$ USD", "€ EUR")
    val initial = profile.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Cerrar perfil", tint = TextDark)
            }
            Text(
                text = "Perfil",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextDark,
                modifier = Modifier.weight(1f)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .background(BrandPurpleLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (profile.photoUrl.isBlank()) {
                        Text(
                            text = initial,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandPurple
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = BrandPurple,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(30.dp)
                            .background(BrandPurple, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(profile.name.ifBlank { "Usuario" }, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(profile.email.ifBlank { "Sin correo registrado" }, fontSize = 13.sp, color = TextGray, textAlign = TextAlign.Center)
            }
        }

        SectionTitle("Datos del usuario")
        ProfileTextField(
            value = profile.name,
            onValueChange = viewModel::onNameChanged,
            label = "Nombre",
            icon = Icons.Outlined.AccountCircle,
            testTag = "profile_name_input"
        )
        ProfileTextField(
            value = profile.email,
            onValueChange = {},
            label = "Correo",
            icon = Icons.Outlined.Email,
            readOnly = true,
            testTag = "profile_email_input"
        )
        ProfileTextField(
            value = profile.phone,
            onValueChange = viewModel::onPhoneChanged,
            label = "Celular",
            icon = Icons.Outlined.Phone,
            keyboardType = KeyboardType.Phone,
            testTag = "profile_phone_input"
        )
        ProfileTextField(
            value = profile.instrument,
            onValueChange = viewModel::onInstrumentChanged,
            label = "Instrumento o cargo",
            icon = Icons.Default.MusicNote,
            testTag = "profile_instrument_input"
        )
        ProfileTextField(
            value = profile.photoUrl,
            onValueChange = viewModel::onPhotoUrlChanged,
            label = "Foto de perfil",
            icon = Icons.Outlined.Link,
            placeholder = "Pega un enlace de imagen",
            testTag = "profile_photo_input"
        )

        SectionTitle("Configuracion de la banda")
        ProfileTextField(
            value = profile.bandName,
            onValueChange = viewModel::onBandNameChanged,
            label = "Nombre de banda",
            icon = Icons.Outlined.Groups,
            testTag = "profile_band_input"
        )
        ExposedDropdownMenuBox(
            expanded = currencyExpanded,
            onExpandedChange = { currencyExpanded = !currencyExpanded }
        ) {
            OutlinedTextField(
                value = profile.currency,
                onValueChange = {},
                readOnly = true,
                label = { Text("Moneda") },
                leadingIcon = { Icon(Icons.Outlined.AttachMoney, contentDescription = null, tint = BrandPurple) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .testTag("profile_currency_dropdown"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandPurple,
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                )
            )
            ExposedDropdownMenu(
                expanded = currencyExpanded,
                onDismissRequest = { currencyExpanded = false }
            ) {
                currencies.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text(currency) },
                        onClick = {
                            viewModel.onCurrencyChanged(currency)
                            currencyExpanded = false
                        }
                    )
                }
            }
        }
        ProfileTextField(
            value = profile.role,
            onValueChange = {},
            label = "Rol",
            icon = Icons.Outlined.Badge,
            readOnly = true,
            testTag = "profile_role_input"
        )

        if (saveMessage != null) {
            Text(
                text = saveMessage.orEmpty(),
                color = BrandPurple,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = viewModel::saveProfile,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_profile_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text("Guardar cambios", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = {
                viewModel.clearSaveMessage()
                onLogout()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("logout_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB91C1C))
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text("Cerrar sesion", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = TextDark,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    readOnly: Boolean = false,
    testTag: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        label = { Text(label) },
        placeholder = {
            if (placeholder.isNotBlank()) {
                Text(placeholder)
            }
        },
        leadingIcon = { Icon(icon, contentDescription = null, tint = BrandPurple) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrandPurple,
            unfocusedBorderColor = Color(0xFFE5E7EB),
            disabledBorderColor = Color(0xFFE5E7EB)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}
