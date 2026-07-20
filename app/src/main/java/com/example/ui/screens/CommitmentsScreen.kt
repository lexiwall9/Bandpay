package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Commitment
import com.example.ui.theme.*
import com.example.ui.viewmodel.CommitmentsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommitmentsScreen(
    viewModel: CommitmentsViewModel,
    onNavigateToDetail: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val upcomingCommitments by viewModel.upcomingCommitments.collectAsStateWithLifecycle()
    val pastCommitments by viewModel.pastCommitments.collectAsStateWithLifecycle()
    val isCreatedSuccess by viewModel.isCreatedSuccess.collectAsStateWithLifecycle()
    val lastCreatedCommitment by viewModel.lastCreatedCommitment.collectAsStateWithLifecycle()

    var screenState by remember { mutableStateOf("list") } // "list", "create"

    // When a commitment is created successfully, show success sub-view
    LaunchedEffect(isCreatedSuccess) {
        if (isCreatedSuccess) {
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
                    contentWindowInsets = WindowInsets(0.dp),
                    topBar = {
                        TopAppBar(
                            windowInsets = WindowInsets.statusBars,
                            title = {
                                Column {
                                    Text("Compromisos", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                    Text("Gestiona tus tareas y eventos grupales.", fontSize = 13.sp, color = TextGray)
                                }
                            },
                            actions = {
                                IconButton(onClick = {}) {
                                    Icon(Icons.Default.Search, contentDescription = "Buscar", tint = TextDark)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { screenState = "create" },
                            containerColor = BrandPurple,
                            contentColor = Color.White,
                            modifier = Modifier.testTag("add_commitment_fab")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Crear Compromiso")
                        }
                    },
                    containerColor = BackgroundLight
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Tabs Row (Próximos | Historial) - Custom Card Segmented Control to match Figma
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .background(Color(0xFFF3F4F6), RoundedCornerShape(10.dp))
                                    .padding(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Próximos Tab
                                val isTab0Selected = selectedTab == 0
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isTab0Selected) BrandPurple else Color.Transparent)
                                        .clickable { viewModel.setSelectedTab(0) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Próximos",
                                        color = if (isTab0Selected) Color.White else TextGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                // Historial Tab
                                val isTab1Selected = selectedTab == 1
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isTab1Selected) BrandPurple else Color.Transparent)
                                        .clickable { viewModel.setSelectedTab(1) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Historial",
                                        color = if (isTab1Selected) Color.White else TextGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        val activeList = if (selectedTab == 0) upcomingCommitments else pastCommitments

                        if (activeList.isEmpty()) {
                            // Empty State
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.EventBusy, contentDescription = null, tint = TextGray, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No hay compromisos", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                Text(
                                    "Comienza agregando un ensayo, reunión o evento con el botón +",
                                    fontSize = 14.sp,
                                    color = TextGray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        } else {
                            // Lazy List of commitments
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(activeList) { item ->
                                    CommitmentRow(
                                        commitment = item,
                                        onClick = { onNavigateToDetail(item.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            "create" -> {
                val newTitle by viewModel.newTitle.collectAsStateWithLifecycle()
                val newDescription by viewModel.newDescription.collectAsStateWithLifecycle()
                val newDate by viewModel.newDate.collectAsStateWithLifecycle()
                val newTime by viewModel.newTime.collectAsStateWithLifecycle()
                val newLocation by viewModel.newLocation.collectAsStateWithLifecycle()

                Scaffold(
                    contentWindowInsets = WindowInsets(0.dp),
                    topBar = {
                        TopAppBar(
                            windowInsets = WindowInsets.statusBars,
                            title = {
                                Text("NUEVO COMPROMISO", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
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
                            text = "Crea un nuevo compromiso",
                            fontSize = 15.sp,
                            color = TextGray
                        )

                        // Title Textfield
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { viewModel.newTitle.value = it },
                            label = { Text("Título del compromiso") },
                            placeholder = { Text("ej. Ensayo General: Gala Anual") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = BrandPurple) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("commitment_title_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandPurple,
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                focusedLabelColor = BrandPurple,
                                unfocusedLabelColor = TextGray,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            singleLine = true
                        )

                        // Description Textfield
                        OutlinedTextField(
                            value = newDescription,
                            onValueChange = { viewModel.newDescription.value = it },
                            label = { Text("Descripción (opcional)") },
                            placeholder = { Text("ej. Coordinación de repertorio y trajes") },
                            leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = BrandPurple) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandPurple,
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                focusedLabelColor = BrandPurple,
                                unfocusedLabelColor = TextGray,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        // Date and Time Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = newDate,
                                onValueChange = { viewModel.newDate.value = it },
                                label = { Text("Fecha") },
                                placeholder = { Text("mm/dd/yyyy") },
                                leadingIcon = { Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = BrandPurple) },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("commitment_date_input"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandPurple,
                                    unfocusedBorderColor = Color(0xFFE5E7EB),
                                    focusedLabelColor = BrandPurple,
                                    unfocusedLabelColor = TextGray,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = newTime,
                                onValueChange = { viewModel.newTime.value = it },
                                label = { Text("Hora") },
                                placeholder = { Text("--:--") },
                                leadingIcon = { Icon(Icons.Outlined.Schedule, contentDescription = null, tint = BrandPurple) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("commitment_time_input"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandPurple,
                                    unfocusedBorderColor = Color(0xFFE5E7EB),
                                    focusedLabelColor = BrandPurple,
                                    unfocusedLabelColor = TextGray,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                singleLine = true
                            )
                        }

                        // Location Textfield
                        OutlinedTextField(
                            value = newLocation,
                            onValueChange = { viewModel.newLocation.value = it },
                            label = { Text("Lugar") },
                            placeholder = { Text("Lugar del evento") },
                            leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = BrandPurple) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("commitment_location_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandPurple,
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                focusedLabelColor = BrandPurple,
                                unfocusedLabelColor = TextGray,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Next Steps Info Card
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BrandPurpleLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Outlined.Info, contentDescription = null, tint = BrandPurple)
                                Column {
                                    Text(
                                        text = "Próximos Pasos",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = BrandPurple
                                    )
                                    Text(
                                        text = "Se enviará una notificación a todos los integrantes.",
                                        fontSize = 12.sp,
                                        color = TextDark
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Submit Button
                        Button(
                            onClick = { viewModel.createCommitment() },
                            enabled = newTitle.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("create_commitment_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                        ) {
                            Text("Crear compromiso", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            "success" -> {
                val lastCreated = lastCreatedCommitment
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color(0xFFF3E8FF), Color(0xFFFFFFFF))
                            )
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Custom Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            viewModel.resetSuccess()
                            screenState = "list"
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = TextDark)
                        }
                        Text(
                            text = "Compromiso creado",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Double overlapping badges (Calendar + Checkmark)
                        Box(
                            modifier = Modifier.size(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .background(BrandPurple, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    tint = Color.White,
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
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Éxito",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "¡Compromiso creado!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Tu compromiso ha sido creado correctamente.",
                            fontSize = 14.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // High fidelity ticket-style Card with Icon rows
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Compromiso Row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(BrandPurpleLight, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Assignment, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text("COMPROMISO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextGray, letterSpacing = 0.5.sp)
                                        Text(lastCreated?.title ?: "", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                    }
                                }

                                Divider(color = Color(0xFFF3F4F6), thickness = 1.dp)

                                // Fecha Row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(BrandPurpleLight, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text("FECHA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextGray, letterSpacing = 0.5.sp)
                                        Text("${lastCreated?.date ?: ""} - ${lastCreated?.time ?: ""}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                    }
                                }

                                Divider(color = Color(0xFFF3F4F6), thickness = 1.dp)

                                // Lugar Row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(BrandPurpleLight, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text("LUGAR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextGray, letterSpacing = 0.5.sp)
                                        Text(lastCreated?.location ?: "", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                    }
                                }
                            }
                        }
                    }

                    // Button return to list
                    Button(
                        onClick = {
                            viewModel.resetSuccess()
                            screenState = "list"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("view_commitments_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                    ) {
                        Text("Ver mis compromisos", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun CommitmentRow(
    commitment: Commitment,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("commitment_row_${commitment.id}"),
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
            // Left Date Block
            Column(
                modifier = Modifier
                    .size(56.dp)
                    .background(BrandPurpleLight, RoundedCornerShape(12.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val dateParts = commitment.date.split(" ")
                val day = dateParts.firstOrNull() ?: "00"
                val month = dateParts.getOrNull(1)?.uppercase() ?: "EVT"
                
                Text(
                    text = day,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandPurple
                )
                Text(
                    text = month,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPurple
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Middle info block
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = commitment.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = TextGray, modifier = Modifier.size(13.dp))
                    Text(
                        text = "${commitment.date} - ${commitment.time}",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = TextGray, modifier = Modifier.size(13.dp))
                    Text(
                        text = commitment.location,
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }
            }

            // Right Status Badge
            val badgeBg = if (commitment.isCompleted) AccentGreenLight else AccentOrangeLight
            val badgeColor = if (commitment.isCompleted) AccentGreen else AccentOrange
            val badgeText = if (commitment.isCompleted) "Confirmado" else "Pendiente"

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextGray,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
    }
}
