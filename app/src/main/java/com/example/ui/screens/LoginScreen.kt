package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.viewmodel.LoginViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val showFingerprintDialog by viewModel.showFingerprintDialog.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()

    var isKeypadMode by remember { mutableStateOf(false) }
    var isRegisterMode by remember { mutableStateOf(false) }
    var isRegisterSuccess by remember { mutableStateOf(false) }
    
    var passwordVisible by remember { mutableStateOf(false) }
    var pinValue by remember { mutableStateOf("") }
    
    // Biometric animation state
    var isBiometricSuccess by remember { mutableStateOf(false) }

    // Register Form Fields
    var regName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regInstrument by remember { mutableStateOf("Seleccionar instrumento") }
    var regInstrumentExpanded by remember { mutableStateOf(false) }
    val instrumentsList = listOf("Tarola", "Trompeta", "Barítono", "Platillo", "Clarinete", "Saxofón", "Otros")

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            onLoginSuccess()
        }
    }

    LaunchedEffect(isBiometricSuccess) {
        if (isBiometricSuccess) {
            delay(1200)
            viewModel.loginWithFingerprint()
            isBiometricSuccess = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF3E8FF), Color(0xFFFFFFFF))
                )
            )
    ) {
        if (isRegisterSuccess) {
            // Register Success Screen (Matching high fidelity "Invitación enviada")
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Success Envelope Icon
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(BrandPurpleLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(BrandPurple.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Mail,
                            contentDescription = "Enviado",
                            tint = BrandPurple,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .background(AccentGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "¡Registro exitoso!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Hemos enviado un correo de confirmación a:",
                    fontSize = 14.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = regEmail,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPurple,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Tu usuario ha sido registrado correctamente en la base de datos de la Banda Municipal.",
                    fontSize = 14.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = {
                        isRegisterSuccess = false
                        isRegisterMode = false
                        // Prepopulate email field for login convenience
                        viewModel.onEmailChanged(regEmail)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("dismiss_register_success"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                ) {
                    Text("Aceptar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        } else if (isRegisterMode) {
            // Register Mode Form Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { isRegisterMode = false }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = TextDark)
                    }
                    Text(
                        text = "Crear Cuenta",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Text(
                    text = "Completa tus datos de integrante para registrarte y unirte al equipo de trabajo.",
                    fontSize = 14.sp,
                    color = TextGray,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Name field
                OutlinedTextField(
                    value = regName,
                    onValueChange = { regName = it },
                    label = { Text("Nombres") },
                    placeholder = { Text("Ingresa nombres") },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = BrandPurple) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPurple,
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    ),
                    singleLine = true
                )

                // Email field
                OutlinedTextField(
                    value = regEmail,
                    onValueChange = { regEmail = it },
                    label = { Text("Email") },
                    placeholder = { Text("ejemplo@taskgroup.com") },
                    leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = BrandPurple) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_email_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPurple,
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    ),
                    singleLine = true
                )

                // Phone field
                OutlinedTextField(
                    value = regPhone,
                    onValueChange = { regPhone = it },
                    label = { Text("Celular") },
                    placeholder = { Text("+51 987 654 321") },
                    leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null, tint = BrandPurple) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_phone_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPurple,
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    ),
                    singleLine = true
                )

                // Password field
                OutlinedTextField(
                    value = regPassword,
                    onValueChange = { regPassword = it },
                    label = { Text("Contraseña") },
                    placeholder = { Text("Ingresa tu clave de acceso") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = BrandPurple) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_password_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPurple,
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )

                // Instrument Dropdown
                ExposedDropdownMenuBox(
                    expanded = regInstrumentExpanded,
                    onExpandedChange = { regInstrumentExpanded = !regInstrumentExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = regInstrument,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Instrumento") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = regInstrumentExpanded) },
                        leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null, tint = BrandPurple) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("reg_instrument_dropdown"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                            focusedBorderColor = BrandPurple,
                            unfocusedBorderColor = Color(0xFFE5E7EB)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = regInstrumentExpanded,
                        onDismissRequest = { regInstrumentExpanded = false }
                    ) {
                        instrumentsList.forEach { selection ->
                            DropdownMenuItem(
                                text = { Text(selection) },
                                onClick = {
                                    regInstrument = selection
                                    regInstrumentExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Registration Button
                Button(
                    onClick = {
                        viewModel.registerMember(
                            name = regName,
                            code = regPassword,
                            phone = regPhone,
                            instrument = regInstrument
                        ) {
                            isRegisterSuccess = true
                        }
                    },
                    enabled = regName.isNotEmpty() && regEmail.isNotEmpty() && regPhone.isNotEmpty() && regPassword.isNotEmpty() && regInstrument != "Seleccionar instrumento",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_register_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                ) {
                    Text("Registrarse", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "¿Ya tienes una cuenta? ", color = TextGray, fontSize = 14.sp)
                    Text(
                        text = "Inicia sesión",
                        color = BrandPurple,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { isRegisterMode = false }
                    )
                }
            }
        } else if (!isKeypadMode) {
            // Screen 1: Welcome Email/Password Login
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(40.dp))
                
                // App Logo / Symbol (Band Icon / Shield)
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(BrandPurpleLight, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Band Logo",
                        tint = BrandPurple,
                        modifier = Modifier.size(50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "¡Hola de nuevo!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Inicia sesión para gestionar tus compromisos y tareas grupales.",
                    fontSize = 15.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { viewModel.onEmailChanged(it) },
                    label = { Text("Email") },
                    placeholder = { Text("ejemplo@taskgroup.com") },
                    leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = BrandPurple) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("email_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPurple,
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { viewModel.onPasswordChanged(it) },
                    label = { Text("Contraseña") },
                    placeholder = { Text("••••••••") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = BrandPurple) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = if (passwordVisible) "Ocultar" else "Mostrar"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPurple,
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    ),
                    singleLine = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "¿Olvidaste tu contraseña?",
                        color = BrandPurple,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { }
                    )
                }

                if (loginError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = loginError ?: "",
                        color = Color.Red,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Login Button
                Button(
                    onClick = { viewModel.loginWithPassword() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("login_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                ) {
                    Text("Iniciar sesión", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Switch to Pin Keypad Mode
                Text(
                    text = "Iniciar con huella / PIN rápido",
                    color = BrandPurple,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { isKeypadMode = true }
                        .padding(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Registrarse Option
                Text(
                    text = "¿No tienes cuenta? Regístrate aquí",
                    color = TextGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { isRegisterMode = true }
                        .padding(8.dp)
                )
            }
        } else {
            // Screen 2: Passcode Keypad Screen (POLISHED LIGHT THEME!)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { isKeypadMode = false }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = BrandPurple)
                    }
                    Text(
                        text = "Ayuda",
                        color = TextGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { }
                    )
                }

                // Profile and Title
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Profile Image placeholder
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(BrandPurpleLight)
                            .clickable { viewModel.setFingerprintDialogVisible(true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar",
                            tint = BrandPurple,
                            modifier = Modifier.size(50.dp)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .background(AccentGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "¡Hola, Juan!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Ingrese su contraseña de PIN",
                        fontSize = 14.sp,
                        color = TextGray
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // PIN Dots - 5 dots to match Figma
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..5) {
                            val isActive = pinValue.length >= i
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(
                                        color = if (isActive) BrandPurple else BrandPurple.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }

                // Keyboard grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("fingerprint", "0", "backspace")
                    )

                    for (row in keys) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (key in row) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            when (key) {
                                                "backspace" -> {
                                                    if (pinValue.isNotEmpty()) {
                                                        pinValue = pinValue.dropLast(1)
                                                    }
                                                }
                                                "fingerprint" -> {
                                                    viewModel.setFingerprintDialogVisible(true)
                                                }
                                                else -> {
                                                    if (pinValue.length < 5) {
                                                        pinValue += key
                                                        if (pinValue.length == 5) {
                                                            // Auto login on 5 digits
                                                            viewModel.loginWithFingerprint()
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (key) {
                                        "backspace" -> Icon(Icons.Default.Backspace, contentDescription = "Borrar", tint = BrandPurple)
                                        "fingerprint" -> Icon(Icons.Default.Fingerprint, contentDescription = "Huella", tint = BrandPurple, modifier = Modifier.size(32.dp))
                                        else -> Text(
                                            text = key,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "¿OLVIDASTE TU CLAVE?",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPurple,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Cerrar sesión",
                        fontSize = 13.sp,
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isKeypadMode = false }
                    )
                }
            }
        }

        // Biometrics Fingerprint Dialog Overlay
        if (showFingerprintDialog) {
            Dialog(onDismissRequest = { 
                if (!isBiometricSuccess) {
                    viewModel.setFingerprintDialogVisible(false) 
                }
            }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedContent(
                            targetState = isBiometricSuccess,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "biometric_status_animation"
                        ) { success ->
                            if (success) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .background(AccentGreenLight, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Success",
                                            tint = AccentGreen,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "¡Biométrico correcto!",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "La huella dactilar ha sido autenticada de forma correcta.",
                                        fontSize = 14.sp,
                                        color = TextGray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .background(BrandPurpleLight, CircleShape)
                                            .clickable { isBiometricSuccess = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = "Fingerprint Sensor",
                                            tint = BrandPurple,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "Confirma tu huella digital",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "Coloca el dedo sobre el sensor de tu celular (o toca el icono arriba) para verificar tu huella digital.",
                                        fontSize = 14.sp,
                                        color = TextGray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        if (!isBiometricSuccess) {
                            Text(
                                text = "CANCELAR",
                                color = BrandPurple,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { viewModel.setFingerprintDialogVisible(false) }
                                    .padding(8.dp)
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = AccentGreen,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

