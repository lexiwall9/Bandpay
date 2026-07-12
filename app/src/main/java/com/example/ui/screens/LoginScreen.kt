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
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleLight
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextGray
import com.example.ui.viewmodel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onBiometricLoginRequested: () -> Unit,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()
    val canUseBiometricLogin by viewModel.canUseBiometricLogin.collectAsStateWithLifecycle()
    val savedLoginEmail by viewModel.savedLoginEmail.collectAsStateWithLifecycle()

    var isKeypadMode by remember { mutableStateOf(false) }
    var isRegisterMode by remember { mutableStateOf(false) }
    var isRegisterSuccess by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var pinValue by remember { mutableStateOf("") }
    var autoBiometricPromptShown by remember { mutableStateOf(false) }

    var regName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regInstrument by remember { mutableStateOf("Seleccionar instrumento") }
    var regInstrumentExpanded by remember { mutableStateOf(false) }
    val instrumentsList = listOf("Tarola", "Trompeta", "Baritono", "Platillo", "Clarinete", "Saxofon", "Otros")

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) onLoginSuccess()
    }

    LaunchedEffect(canUseBiometricLogin, savedLoginEmail, isRegisterMode, isKeypadMode) {
        if (
            canUseBiometricLogin &&
            savedLoginEmail.isNotBlank() &&
            !isRegisterMode &&
            !isKeypadMode &&
            !autoBiometricPromptShown
        ) {
            autoBiometricPromptShown = true
            onBiometricLoginRequested()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF3E8FF), Color.White)))
    ) {
        when {
            isRegisterSuccess -> RegisterSuccess(
                email = regEmail,
                onAccept = {
                    isRegisterSuccess = false
                    isRegisterMode = false
                    viewModel.onEmailChanged(regEmail)
                }
            )
            isRegisterMode -> RegisterForm(
                name = regName,
                email = regEmail,
                phone = regPhone,
                password = regPassword,
                instrument = regInstrument,
                instrumentExpanded = regInstrumentExpanded,
                instrumentsList = instrumentsList,
                loginError = loginError,
                onNameChange = { regName = it },
                onEmailChange = { regEmail = it },
                onPhoneChange = { regPhone = it },
                onPasswordChange = { regPassword = it },
                onInstrumentExpandedChange = { regInstrumentExpanded = it },
                onInstrumentSelected = {
                    regInstrument = it
                    regInstrumentExpanded = false
                },
                onBack = { isRegisterMode = false },
                onLoginClick = { isRegisterMode = false },
                onSubmit = {
                    viewModel.registerMember(
                        name = regName,
                        email = regEmail,
                        password = regPassword,
                        phone = regPhone,
                        instrument = regInstrument
                    ) {
                        isRegisterSuccess = true
                    }
                }
            )
            !isKeypadMode -> PasswordLogin(
                email = email,
                savedEmail = savedLoginEmail,
                password = password,
                passwordVisible = passwordVisible,
                loginError = loginError,
                canUseBiometricLogin = canUseBiometricLogin,
                onEmailChange = viewModel::onEmailChanged,
                onPasswordChange = viewModel::onPasswordChanged,
                onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                onPasswordLogin = viewModel::loginWithPassword,
                onBiometricMode = { isKeypadMode = true },
                onRegister = { isRegisterMode = true }
            )
            else -> BiometricLogin(
                pinValue = pinValue,
                loginError = loginError,
                onBack = { isKeypadMode = false },
                onBiometricLoginRequested = onBiometricLoginRequested,
                onPinChanged = { pinValue = it },
                onPinUnavailable = {
                    viewModel.setLoginError("El PIN rapido aun no esta configurado")
                    pinValue = ""
                }
            )
        }
    }
}

@Composable
private fun RegisterSuccess(email: String, onAccept: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(BrandPurpleLight, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(46.dp))
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("Registro exitoso", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Tu cuenta fue creada en Firebase con:", fontSize = 14.sp, color = TextGray, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(4.dp))
        Text(email, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BrandPurple, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = onAccept,
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterForm(
    name: String,
    email: String,
    phone: String,
    password: String,
    instrument: String,
    instrumentExpanded: Boolean,
    instrumentsList: List<String>,
    loginError: String?,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onInstrumentExpandedChange: (Boolean) -> Unit,
    onInstrumentSelected: (String) -> Unit,
    onBack: () -> Unit,
    onLoginClick: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Atras", tint = TextDark)
            }
            Text("Crear Cuenta", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark, modifier = Modifier.padding(start = 8.dp))
        }
        Text(
            "Completa tus datos de integrante para registrarte.",
            fontSize = 14.sp,
            color = TextGray,
            modifier = Modifier.fillMaxWidth()
        )
        LoginTextField(name, onNameChange, "Nombres", "Ingresa nombres", Icons.Outlined.Person, "reg_name_input")
        LoginTextField(email, onEmailChange, "Email", "ejemplo@bandaspuno.com", Icons.Outlined.Email, "reg_email_input")
        LoginTextField(phone, onPhoneChange, "Celular", "+51 987 654 321", Icons.Outlined.Phone, "reg_phone_input", KeyboardType.Phone)
        LoginTextField(password, onPasswordChange, "Contrasena", "Ingresa tu clave", Icons.Outlined.Lock, "reg_password_input", visualTransformation = PasswordVisualTransformation())

        ExposedDropdownMenuBox(
            expanded = instrumentExpanded,
            onExpandedChange = { onInstrumentExpandedChange(!instrumentExpanded) },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = instrument,
                onValueChange = {},
                readOnly = true,
                label = { Text("Instrumento") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = instrumentExpanded) },
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
            ExposedDropdownMenu(expanded = instrumentExpanded, onDismissRequest = { onInstrumentExpandedChange(false) }) {
                instrumentsList.forEach { selection ->
                    DropdownMenuItem(text = { Text(selection) }, onClick = { onInstrumentSelected(selection) })
                }
            }
        }

        ErrorText(loginError)

        Button(
            onClick = onSubmit,
            enabled = name.isNotEmpty() && email.isNotEmpty() && phone.isNotEmpty() && password.isNotEmpty() && instrument != "Seleccionar instrumento",
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("submit_register_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
        ) {
            Text("Registrarse", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("Ya tienes una cuenta? ", color = TextGray, fontSize = 14.sp)
            Text(
                "Inicia sesion",
                color = BrandPurple,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onLoginClick() }
            )
        }
    }
}

@Composable
private fun PasswordLogin(
    email: String,
    savedEmail: String,
    password: String,
    passwordVisible: Boolean,
    loginError: String?,
    canUseBiometricLogin: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onPasswordLogin: () -> Unit,
    onBiometricMode: () -> Unit,
    onRegister: () -> Unit
) {
    val hasSavedEmail = savedEmail.isNotBlank()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(BrandPurpleLight, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = "Band Logo", tint = BrandPurple, modifier = Modifier.size(50.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(if (hasSavedEmail) "Hola de nuevo" else "Iniciar sesion", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextDark, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            if (hasSavedEmail) "Ingresa tu contrasena o usa tu huella." else "Ingresa por primera vez con tu correo.",
            fontSize = 15.sp,
            color = TextGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        if (hasSavedEmail) {
            Text("Cuenta guardada", fontSize = 13.sp, color = TextGray)
            Text(savedEmail, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BrandPurple, textAlign = TextAlign.Center)
        } else {
            LoginTextField(email, onEmailChange, "Email", "admin@bandaspuno.com", Icons.Outlined.Email, "email_input")
        }
        Spacer(modifier = Modifier.height(16.dp))
        LoginTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Contrasena",
            placeholder = "********",
            icon = Icons.Outlined.Lock,
            testTag = "password_input",
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = if (passwordVisible) "Ocultar" else "Mostrar"
                    )
                }
            }
        )
        ErrorText(loginError)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onPasswordLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("login_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
        ) {
            Text("Ingresar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        if (canUseBiometricLogin) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Usar huella",
                color = BrandPurple,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onBiometricMode() }
                    .padding(8.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Registrarse",
            color = TextGray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clickable { onRegister() }
                .padding(8.dp)
        )
    }
}

@Composable
private fun BiometricLogin(
    pinValue: String,
    loginError: String?,
    onBack: () -> Unit,
    onBiometricLoginRequested: () -> Unit,
    onPinChanged: (String) -> Unit,
    onPinUnavailable: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = BrandPurple)
            }
            Text("Ayuda", color = TextGray, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(BrandPurpleLight)
                    .clickable { onBiometricLoginRequested() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = "Avatar", tint = BrandPurple, modifier = Modifier.size(50.dp))
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
            Text("Usa tu huella", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Tambien puedes usar PIN rapido", fontSize = 14.sp, color = TextGray)
            ErrorText(loginError)
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                for (i in 1..5) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(
                                color = if (pinValue.length >= i) BrandPurple else BrandPurple.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("fingerprint", "0", "backspace")
            )
            keys.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .clickable {
                                    when (key) {
                                        "backspace" -> if (pinValue.isNotEmpty()) onPinChanged(pinValue.dropLast(1))
                                        "fingerprint" -> onBiometricLoginRequested()
                                        else -> {
                                            val newPin = pinValue + key
                                            onPinChanged(newPin.take(5))
                                            if (newPin.length >= 5) onPinUnavailable()
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when (key) {
                                "backspace" -> Icon(Icons.Default.Backspace, contentDescription = "Borrar", tint = BrandPurple)
                                "fingerprint" -> Icon(Icons.Default.Fingerprint, contentDescription = "Huella", tint = BrandPurple, modifier = Modifier.size(32.dp))
                                else -> Text(key, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Volver", fontSize = 13.sp, color = TextGray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().clickable { onBack() })
        }
    }
}

@Composable
private fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = BrandPurple) },
        trailingIcon = trailingIcon,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrandPurple,
            unfocusedBorderColor = Color(0xFFE5E7EB)
        ),
        singleLine = true
    )
}

@Composable
private fun ErrorText(message: String?) {
    if (message != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, color = Color.Red, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
