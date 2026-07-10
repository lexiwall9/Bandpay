package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToCommitments: () -> Unit,
    onNavigateToMembers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeMembersCount by viewModel.activeMembersCount.collectAsState()
    val upcomingCount by viewModel.upcomingCommitments.collectAsState()
    val totalPendingAmount by viewModel.totalPaymentsPending.collectAsState()
    val pendingPaymentsCount by viewModel.pendingPaymentsCount.collectAsState()
    val activities by viewModel.recentActivities.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(40.dp)
                            .background(BrandPurple, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("J", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notificaciones", tint = TextDark)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Search, contentDescription = "Buscar", tint = TextDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = BackgroundLight,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Greeting Header
            Text(
                text = "¡Hola, Juan!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = "Hoy tienes ${upcomingCount.size} compromisos pendientes.",
                fontSize = 15.sp,
                color = TextGray,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Summary Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Eventos Metric Card
                MetricCard(
                    title = "Eventos",
                    value = upcomingCount.size.toString(),
                    containerColor = Color(0xFFEFF6FF),
                    contentColor = Color(0xFF1D4ED8),
                    modifier = Modifier.weight(1f)
                )
                // Pagos Metric Card
                MetricCard(
                    title = "Pagos",
                    value = pendingPaymentsCount.toString(),
                    containerColor = Color(0xFFFEF2F2),
                    contentColor = Color(0xFFB91C1C),
                    modifier = Modifier.weight(1f)
                )
                // Tareas Metric Card
                MetricCard(
                    title = "Tareas",
                    value = "12",
                    containerColor = Color(0xFFF5F3FF),
                    contentColor = Color(0xFF6D28D9),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Banner Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("progress_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BrandPurple)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Eventos completados",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "65%",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = 0.65f,
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "¡Buen trabajo! Estás por encima del promedio del grupo de Puno.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Next commitments section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Compromisos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = "Próxima: 14:00",
                    fontSize = 13.sp,
                    color = BrandPurple,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { onNavigateToCommitments() }
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Access Grid (Integrantes & Pagos)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick Access Integrantes
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToMembers() }
                        .testTag("quick_members"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(BrandPurpleLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.People, contentDescription = null, tint = BrandPurple)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Integrantes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextDark
                        )
                        Text(
                            text = "$activeMembersCount personas activas",
                            fontSize = 12.sp,
                            color = TextGray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Quick Access Pagos
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToCommitments() } // or directly to payment commitment
                        .testTag("quick_payments"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFFFE0E6), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFFE91E63))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Pagos",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextDark
                        )
                        Text(
                            text = "$pendingPaymentsCount pendientes",
                            fontSize = 12.sp,
                            color = AccentRed,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Activity Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Actividad Reciente",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = "Ver todo",
                    fontSize = 13.sp,
                    color = BrandPurple,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Activities Feed
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    activities.forEach { act ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val bgAndIcon = when (act.iconType) {
                                "check" -> Pair(AccentGreenLight, Icons.Default.CheckCircle)
                                "payment" -> Pair(BrandPurpleLight, Icons.Default.Receipt)
                                else -> Pair(Color(0xFFEFF6FF), Icons.Default.PersonAdd)
                            }
                            val iconColor = when (act.iconType) {
                                "check" -> AccentGreen
                                "payment" -> BrandPurple
                                else -> Color(0xFF2563EB)
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(bgAndIcon.first, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = bgAndIcon.second,
                                    contentDescription = null,
                                    tint = iconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = act.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = act.description,
                                    fontSize = 12.sp,
                                    color = TextGray
                                )
                            }

                            Text(
                                text = act.timeAgo,
                                fontSize = 11.sp,
                                color = TextGray,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                color = contentColor.copy(alpha = 0.8f),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor
            )
        }
    }
}
