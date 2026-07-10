package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Member
import com.example.ui.theme.*
import com.example.ui.viewmodel.MembersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    viewModel: MembersViewModel,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedInstrument by viewModel.selectedInstrument.collectAsStateWithLifecycle()
    val filteredMembers by viewModel.filteredMembers.collectAsStateWithLifecycle()
    val isAddedSuccess by viewModel.isAddedSuccess.collectAsStateWithLifecycle()
    val lastInvitedEmail by viewModel.lastInvitedEmail.collectAsStateWithLifecycle()

    var screenState by remember { mutableStateOf("list") } // "list", "add"

    LaunchedEffect(isAddedSuccess) {
        if (isAddedSuccess) {
            screenState = "success"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        when (screenState) {
            "list" -> {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text("Lista de integrantes", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                    Text("Total: ${filteredMembers.size} integrantes", fontSize = 13.sp, color = TextGray)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { screenState = "add" },
                            containerColor = BrandPurple,
                            contentColor = Color.White,
                            modifier = Modifier.testTag("add_member_fab")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Agregar Integrante")
                        }
                    },
                    containerColor = BackgroundLight
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Buscar integrante...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextGray) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("search_member_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandPurple,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        // Filter chips scroll list
                        val chips = listOf("Todos", "Tarola", "Trompeta", "Barítono", "Platillo")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            items(chips) { chip ->
                                val isSelected = selectedInstrument == chip
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setSelectedInstrument(chip) },
                                    label = { Text(chip, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandPurple,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color.White,
                                        labelColor = TextGray
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = Color(0xFFE5E7EB),
                                        selectedBorderColor = BrandPurple
                                    )
                                )
                            }
                        }

                        if (filteredMembers.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.PersonOff, contentDescription = null, tint = TextGray, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No se encontraron integrantes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                Text("Intenta cambiar el buscador o el filtro de instrumentos", fontSize = 14.sp, color = TextGray, textAlign = TextAlign.Center)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(filteredMembers) { item ->
                                    MemberRow(member = item)
                                }
                            }
                        }
                    }
                }
            }

            "add" -> {
                val name by viewModel.newMemberName.collectAsStateWithLifecycle()
                val code by viewModel.newMemberCode.collectAsStateWithLifecycle()
                val phone by viewModel.newMemberPhone.collectAsStateWithLifecycle()
                val instrument by viewModel.newMemberInstrument.collectAsStateWithLifecycle()

                var expandedDropdown by remember { mutableStateOf(false) }
                val instrumentsList = listOf("Tarola", "Trompeta", "Barítono", "Platillo", "Clarinete", "Saxofón", "Otros")

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text("AGREGAR INTEGRANTE", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            },
                            navigationIcon = {
                                IconButton(onClick = { screenState = "list" }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = TextDark)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                        )
                    },
                    containerColor = BackgroundLight
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Completa los datos del nuevo integrante para que pueda unirse a tu equipo de trabajo.",
                            fontSize = 14.sp,
                            color = TextGray
                        )

                        // Name field
                        OutlinedTextField(
                            value = name,
                            onValueChange = { viewModel.newMemberName.value = it },
                            label = { Text("Nombres") },
                            placeholder = { Text("Ingresa nombres") },
                            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = BrandPurple) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("member_name_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // Pin/Code field
                        OutlinedTextField(
                            value = code,
                            onValueChange = { viewModel.newMemberCode.value = it },
                            label = { Text("Clave") },
                            placeholder = { Text("Ingresa clave") },
                            leadingIcon = { Icon(Icons.Outlined.VpnKey, contentDescription = null, tint = BrandPurple) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("member_code_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // Phone field
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { viewModel.newMemberPhone.value = it },
                            label = { Text("Celular") },
                            placeholder = { Text("+51 987 654 321") },
                            leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null, tint = BrandPurple) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("member_phone_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // Instrument Selector Custom Field (DropdownMenu style)
                        ExposedDropdownMenuBox(
                            expanded = expandedDropdown,
                            onExpandedChange = { expandedDropdown = !expandedDropdown },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = instrument,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Instrumento") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                                leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null, tint = BrandPurple) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                                    .testTag("member_instrument_dropdown"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false }
                            ) {
                                instrumentsList.forEach { selection ->
                                    DropdownMenuItem(
                                        text = { Text(selection) },
                                        onClick = {
                                            viewModel.newMemberInstrument.value = selection
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Submit Button
                        Button(
                            onClick = { viewModel.addMember() },
                            enabled = name.isNotEmpty() && phone.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("create_member_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                        ) {
                            Text("Agregar integrante", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            "success" -> {
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
                        text = "¡Invitación enviada!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Hemos enviado una invitación a:",
                        fontSize = 14.sp,
                        color = TextGray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = lastInvitedEmail,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPurple,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "El integrante recibirá un correo electrónico para unirse al grupo de trabajo y comenzar a colaborar.",
                        fontSize = 14.sp,
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    // Dismiss Button
                    Button(
                        onClick = {
                            viewModel.resetSuccess()
                            screenState = "list"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("dismiss_invitation_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                    ) {
                        Text("Aceptar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun MemberRow(
    member: Member
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("member_row_${member.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Initials Circle
            val initials = member.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(BrandPurpleLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPurple
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = member.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = member.phone,
                    fontSize = 12.sp,
                    color = TextGray
                )
            }

            // Instrument Label Badge
            val badgeBg = when (member.instrument) {
                "Platillo" -> Color(0xFFE0F2FE)
                "Trompeta", "Trompeta Principal", "Trompeta 2" -> Color(0xFFF3E8FF)
                "Barítono" -> Color(0xFFD1FAE5)
                "Tarola", "Tarolas" -> Color(0xFFFEF3C7)
                "Clarinete" -> Color(0xFFE0F7FA)
                "Saxofón" -> Color(0xFFFCE7F3)
                else -> Color(0xFFF3F4F6)
            }

            val badgeColor = when (member.instrument) {
                "Platillo" -> Color(0xFF0369A1)
                "Trompeta", "Trompeta Principal", "Trompeta 2" -> BrandPurple
                "Barítono" -> Color(0xFF047857)
                "Tarola", "Tarolas" -> Color(0xFFB45309)
                "Clarinete" -> Color(0xFF00838F)
                "Saxofón" -> Color(0xFFBE185D)
                else -> TextGray
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = member.instrument,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
        }
    }
}
