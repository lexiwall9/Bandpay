package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mail
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
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDetailScreen(
    viewModel: PaymentDetailViewModel,
    commitmentId: Int,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val commitment by viewModel.currentCommitment.collectAsStateWithLifecycle()
    val attendanceList by viewModel.attendanceList.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val totalPaid by viewModel.totalPaid.collectAsStateWithLifecycle()
    val totalPending by viewModel.totalPending.collectAsStateWithLifecycle()

    var showEditAmountDialog by remember { mutableStateOf<Int?>(null) } // holds memberId
    var editAmountValue by remember { mutableStateOf("") }

    val selectedMemberIds = remember { mutableStateListOf<Int>() }
    var selectedFilterChip by remember { mutableStateOf("Todos") }
    val filterChips = listOf("Todos", "Bombos", "Tarolas", "Trompetas")
    var showNotificationSuccess by remember { mutableStateOf(false) }

    // Prepopulate selectedMemberIds with accepted/pending members on first load of attendanceList
    LaunchedEffect(attendanceList) {
        if (selectedMemberIds.isEmpty() && attendanceList.isNotEmpty()) {
            attendanceList.filter { it.attendance.status == "Aceptado" }.forEach {
                selectedMemberIds.add(it.member.id)
            }
        }
    }

    LaunchedEffect(commitmentId) {
        viewModel.loadCommitment(commitmentId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = TextDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = BackgroundLight,
        modifier = modifier
    ) { innerPadding ->
        commitment?.let { evt ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Header Area
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Band Region Category
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BrandPurpleLight)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Banda Municipal",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPurple
                                )
                            }

                            // Event Status badge
                            val badgeBg = if (evt.isCompleted) AccentGreenLight else AccentOrangeLight
                            val badgeColor = if (evt.isCompleted) AccentGreen else AccentOrange
                            val badgeText = if (evt.isCompleted) "Confirmado" else "Pendiente"

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

                        Spacer(modifier = Modifier.height(12.dp))

                        // Event Title
                        Text(
                            text = evt.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextDark
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Schedule info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = TextGray, modifier = Modifier.size(14.dp))
                                Text(text = "${evt.date} - ${evt.time}", fontSize = 12.sp, color = TextGray)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = TextGray, modifier = Modifier.size(14.dp))
                                Text(text = evt.location, fontSize = 12.sp, color = TextGray)
                            }
                        }
                    }
                }

                // Sub Navigation Tabs - Custom Card Segmented Control
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
                        // Integrantes Tab
                        val isTab0Selected = selectedTab == 0
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isTab0Selected) Color.White else Color.Transparent)
                                .clickable { viewModel.setSelectedTab(0) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Integrantes",
                                color = if (isTab0Selected) BrandPurple else TextGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // Pagos Tab
                        val isTab1Selected = selectedTab == 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isTab1Selected) Color.White else Color.Transparent)
                                .clickable { viewModel.setSelectedTab(1) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Pagos",
                                color = if (isTab1Selected) BrandPurple else TextGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                if (selectedTab == 0) {
                    // TAB 1: INTEGRANTES (ATTENDANCE)
                    Box(modifier = Modifier.weight(1f)) {
                        if (attendanceList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = BrandPurple)
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Instrument Filter Chips
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(filterChips) { chip ->
                                        val isSelected = selectedFilterChip == chip
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedFilterChip = chip },
                                            label = { Text(chip, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = BrandPurple,
                                                selectedLabelColor = Color.White,
                                                containerColor = Color(0xFFF3F4F6),
                                                labelColor = TextGray
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = isSelected,
                                                borderColor = Color(0xFFE5E7EB),
                                                selectedBorderColor = BrandPurple
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                    }
                                }

                                val filteredAttendance = remember(attendanceList, selectedFilterChip) {
                                    if (selectedFilterChip == "Todos") {
                                        attendanceList
                                    } else {
                                        attendanceList.filter {
                                            it.member.instrument.contains(selectedFilterChip, ignoreCase = true)
                                        }
                                    }
                                }

                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filteredAttendance) { item ->
                                        AttendanceMemberRow(
                                            info = item,
                                            onStatusChange = { newStatus ->
                                                viewModel.updateAttendanceStatus(item.member.id, newStatus)
                                            },
                                            isSelected = selectedMemberIds.contains(item.member.id),
                                            onSelectedChange = { isSelected ->
                                                if (isSelected) {
                                                    selectedMemberIds.add(item.member.id)
                                                } else {
                                                    selectedMemberIds.remove(item.member.id)
                                                }
                                            }
                                        )
                                    }
                                }

                                // Bottom notification trigger
                                Button(
                                    onClick = { showNotificationSuccess = true },
                                    enabled = selectedMemberIds.isNotEmpty(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .height(50.dp)
                                        .testTag("send_notification_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Enviar Notificación", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                } else {
                    // TAB 2: PAGOS (PAYMENTS LIQUIDATION)
                    Box(modifier = Modifier.weight(1f)) {
                        if (attendanceList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = BrandPurple)
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Metric summaries
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Total Pagado card
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = AccentGreenLight),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("Total Pagado", fontSize = 11.sp, color = AccentGreen, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = "S/ %.2f".format(totalPaid),
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = AccentGreen,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }

                                    // Pendiente card
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = AccentRedLight),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("Pendiente", fontSize = 11.sp, color = AccentRed, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = "S/ %.2f".format(totalPending),
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = AccentRed,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "INTEGRANTES & PARTICIPACIÓN",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextGray,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )

                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(attendanceList) { item ->
                                        PaymentMemberRow(
                                            info = item,
                                            onTogglePaid = {
                                                viewModel.togglePaymentPaid(item.member.id)
                                            },
                                            onEditAmount = {
                                                showEditAmountDialog = item.member.id
                                                editAmountValue = item.attendance.paymentAmount.toString()
                                            }
                                        )
                                    }
                                }

                                // Bottom Alert & Finish Button Block
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White)
                                        .padding(16.dp)
                                ) {
                                    // Alert info card
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(Icons.Outlined.Info, contentDescription = null, tint = Color(0xFF2563EB))
                                            Text(
                                                text = "Todos los montos deben estar marcados como Pagado para poder finalizar este compromiso y cerrar la contabilidad.",
                                                fontSize = 11.sp,
                                                color = Color(0xFF1E3A8A)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            viewModel.markCommitmentAsFinished()
                                            onNavigateBack()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("save_payments_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Guardar Cambios", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Edit Payment Amount Dialog Box
        if (showEditAmountDialog != null) {
            val memberId = showEditAmountDialog
            Dialog(onDismissRequest = { showEditAmountDialog = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Editar Monto de Pago",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = editAmountValue,
                            onValueChange = { editAmountValue = it },
                            label = { Text("Monto (S/)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_amount_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TextButton(
                                onClick = { showEditAmountDialog = null },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("CANCELAR", color = TextGray)
                            }
                            Button(
                                onClick = {
                                    val amt = editAmountValue.toDoubleOrNull()
                                    if (amt != null && memberId != null) {
                                        viewModel.updatePaymentAmount(memberId, amt)
                                    }
                                    showEditAmountDialog = null
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                            ) {
                                Text("GUARDAR", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // High fidelity Notification success Dialog
        if (showNotificationSuccess) {
            Dialog(onDismissRequest = { showNotificationSuccess = false }) {
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
                        // Success Envelope Icon with overlapping green Check badge
                        Box(
                            modifier = Modifier.size(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(BrandPurpleLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Mail,
                                    contentDescription = null,
                                    tint = BrandPurple,
                                    modifier = Modifier.size(40.dp)
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
                                    contentDescription = "Confirmado",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "¡Notificación enviada!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Hemos notificado exitosamente a los ${selectedMemberIds.size} integrantes seleccionados para coordinar su participación en este compromiso.",
                            fontSize = 14.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BrandPurpleLight)
                        ) {
                            Text(
                                text = "Cada integrante recibirá una alerta en tiempo real en su dispositivo móvil con las especificaciones del evento.",
                                fontSize = 12.sp,
                                color = BrandPurple,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { showNotificationSuccess = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                        ) {
                            Text("Aceptar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceMemberRow(
    info: MemberAttendanceInfo,
    onStatusChange: (String) -> Unit,
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("attendance_member_row_${info.member.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Initials Circle
            val initials = info.member.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(BrandPurpleLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandPurple)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Member Info
            Column(modifier = Modifier.weight(1f)) {
                Text(text = info.member.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(text = info.member.instrument, fontSize = 12.sp, color = TextGray)
                
                // Show "+ Reemplazar" action directly under the details for Rejected members
                if (info.attendance.status == "Rechazado") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = BrandPurple,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+ Reemplazar",
                            color = BrandPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Attendance Action / Status badge
            val statusBg = when (info.attendance.status) {
                "Aceptado" -> AccentGreenLight
                "Pendiente" -> AccentOrangeLight
                else -> AccentRedLight
            }
            val statusColor = when (info.attendance.status) {
                "Aceptado" -> AccentGreen
                "Pendiente" -> AccentOrange
                else -> AccentRed
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusBg)
                    .clickable {
                        // Cycles through: Aceptado -> Pendiente -> Rechazado -> Aceptado
                        val nextStatus = when (info.attendance.status) {
                            "Aceptado" -> "Pendiente"
                            "Pendiente" -> "Rechazado"
                            else -> "Aceptado"
                        }
                        onStatusChange(nextStatus)
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = info.attendance.status,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor
                    )
                    Icon(
                        imageVector = when (info.attendance.status) {
                            "Aceptado" -> Icons.Default.CheckCircle
                            "Pendiente" -> Icons.Default.Help
                            else -> Icons.Default.Cancel
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Custom high fidelity checkbox for Aceptado / Pendiente members
            if (info.attendance.status != "Rechazado") {
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) BrandPurple else Color.Transparent)
                        .border(
                            width = 2.dp,
                            color = if (isSelected) BrandPurple else Color(0xFFD1D5DB),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onSelectedChange(!isSelected) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Seleccionado",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentMemberRow(
    info: MemberAttendanceInfo,
    onTogglePaid: () -> Unit,
    onEditAmount: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("payment_member_row_${info.member.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile initials
            val initials = info.member.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(BrandPurpleLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandPurple)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = info.member.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(text = info.member.instrument, fontSize = 12.sp, color = TextGray)
            }

            // Price tag and edit button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "S/ %.2f".format(info.attendance.paymentAmount),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (info.attendance.isPaid) AccentGreen else TextDark
                )
                IconButton(onClick = onEditAmount, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Editar Monto",
                        tint = TextGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Switch component
            Switch(
                checked = info.attendance.isPaid,
                onCheckedChange = { onTogglePaid() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = BrandPurple,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFE5E7EB)
                ),
                modifier = Modifier.testTag("switch_${info.member.id}")
            )
        }
    }
}
