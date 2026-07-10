package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// 1. Login ViewModel
class LoginViewModel(private val repository: BandRepository) : ViewModel() {
    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _showFingerprintDialog = MutableStateFlow(false)
    val showFingerprintDialog = _showFingerprintDialog.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError = _loginError.asStateFlow()

    fun onEmailChanged(value: String) {
        _email.value = value
        _loginError.value = null
    }

    fun onPasswordChanged(value: String) {
        _password.value = value
        _loginError.value = null
    }

    fun setFingerprintDialogVisible(visible: Boolean) {
        _showFingerprintDialog.value = visible
    }

    fun loginWithPassword() {
        if (_email.value.trim().lowercase() == "admin@taskgroup.com" || _email.value.trim().lowercase() == "juan@taskgroup.com" || _email.value.trim().isEmpty()) {
            _isLoggedIn.value = true
        } else {
            _loginError.value = "Usuario o contraseña incorrectos"
        }
    }

    fun loginWithFingerprint() {
        _isLoggedIn.value = true
        _showFingerprintDialog.value = false
    }

    fun registerMember(name: String, code: String, phone: String, instrument: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.addMember(
                Member(
                    name = name,
                    code = code,
                    phone = phone,
                    instrument = instrument
                )
            )
            onComplete()
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _email.value = ""
        _password.value = ""
    }
}

// Recent Activity Data Model
data class RecentActivity(
    val title: String,
    val description: String,
    val timeAgo: String,
    val iconType: String // "check", "payment", "user"
)

// 2. Dashboard ViewModel
class DashboardViewModel(private val repository: BandRepository) : ViewModel() {
    
    val activeMembersCount = repository.allMembers.map { it.size }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    val commitments = repository.allCommitments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val upcomingCommitments = commitments.map { list ->
        list.filter { !it.isCompleted }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalPaymentsPending = repository.allAttendance.map { list ->
        list.filter { !it.isPaid }.sumOf { it.paymentAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val pendingPaymentsCount = repository.allAttendance.map { list ->
        list.filter { !it.isPaid }.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentActivities = flowOf(
        listOf(
            RecentActivity("María completó \"Revisión de Gastos\"", "Aprobación de viáticos", "hace 15 min", "check"),
            RecentActivity("Nuevo pago registrado: Suscripción SaaS", "Mantenimiento del servidor cloud", "hace 2 horas", "payment"),
            RecentActivity("Carlos se unió al equipo", "Instrumentista de saxofón", "Ayer, 18:30", "user")
        )
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

// 3. Commitments ViewModel
class CommitmentsViewModel(private val repository: BandRepository) : ViewModel() {
    private val _selectedTab = MutableStateFlow(0) // 0: Próximos, 1: Historial
    val selectedTab = _selectedTab.asStateFlow()

    val commitments = repository.allCommitments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingCommitments = commitments.map { list ->
        list.filter { !it.isCompleted }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pastCommitments = commitments.map { list ->
        list.filter { it.isCompleted }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // New commitment form state
    val newTitle = MutableStateFlow("")
    val newDescription = MutableStateFlow("")
    val newDate = MutableStateFlow("")
    val newTime = MutableStateFlow("")
    val newLocation = MutableStateFlow("")

    private val _isCreatedSuccess = MutableStateFlow(false)
    val isCreatedSuccess = _isCreatedSuccess.asStateFlow()
    
    private val _lastCreatedCommitment = MutableStateFlow<Commitment?>(null)
    val lastCreatedCommitment = _lastCreatedCommitment.asStateFlow()

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun createCommitment() {
        if (newTitle.value.isEmpty()) return
        
        val commitment = Commitment(
            title = newTitle.value,
            description = newDescription.value,
            date = if (newDate.value.isEmpty()) "25 May 2026" else newDate.value,
            time = if (newTime.value.isEmpty()) "10:00 AM" else newTime.value,
            location = if (newLocation.value.isEmpty()) "Sala 101" else newLocation.value,
            isCompleted = false
        )

        viewModelScope.launch {
            val id = repository.addCommitment(commitment)
            _lastCreatedCommitment.value = commitment.copy(id = id.toInt())
            _isCreatedSuccess.value = true
            
            // Reset fields
            newTitle.value = ""
            newDescription.value = ""
            newDate.value = ""
            newTime.value = ""
            newLocation.value = ""
        }
    }

    fun resetSuccess() {
        _isCreatedSuccess.value = false
    }
}

// 4. Members ViewModel
class MembersViewModel(private val repository: BandRepository) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedInstrument = MutableStateFlow("Todos")
    val selectedInstrument = _selectedInstrument.asStateFlow()

    // Form inputs
    val newMemberName = MutableStateFlow("")
    val newMemberCode = MutableStateFlow("")
    val newMemberPhone = MutableStateFlow("")
    val newMemberInstrument = MutableStateFlow("Seleccionar instrumento")

    private val _isAddedSuccess = MutableStateFlow(false)
    val isAddedSuccess = _isAddedSuccess.asStateFlow()

    private val _lastInvitedEmail = MutableStateFlow("juan.nuevo@email.com")
    val lastInvitedEmail = _lastInvitedEmail.asStateFlow()

    val members = repository.allMembers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredMembers = combine(members, searchQuery, selectedInstrument) { list, query, instrument ->
        list.filter { m ->
            val matchesQuery = m.name.contains(query, ignoreCase = true) || m.instrument.contains(query, ignoreCase = true)
            val matchesInstrument = instrument == "Todos" || m.instrument.equals(instrument, ignoreCase = true) ||
                    (instrument == "Trompeta" && (m.instrument.contains("Trompeta", ignoreCase = true))) ||
                    (instrument == "Platillo" && (m.instrument.contains("Platillo", ignoreCase = true)))
            matchesQuery && matchesInstrument
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedInstrument(instrument: String) {
        _selectedInstrument.value = instrument
    }

    fun addMember() {
        if (newMemberName.value.isEmpty()) return
        
        val member = Member(
            name = newMemberName.value,
            code = if (newMemberCode.value.isEmpty()) "m123" else newMemberCode.value,
            phone = if (newMemberPhone.value.isEmpty()) "987654321" else newMemberPhone.value,
            instrument = if (newMemberInstrument.value == "Seleccionar instrumento") "Otros" else newMemberInstrument.value
        )

        viewModelScope.launch {
            repository.addMember(member)
            _lastInvitedEmail.value = "${member.name.replace(" ", "").lowercase()}@email.com"
            _isAddedSuccess.value = true
            
            // Reset
            newMemberName.value = ""
            newMemberCode.value = ""
            newMemberPhone.value = ""
            newMemberInstrument.value = "Seleccionar instrumento"
        }
    }

    fun resetSuccess() {
        _isAddedSuccess.value = false
    }
}

// Combined model for Attendance Screen UI
data class MemberAttendanceInfo(
    val attendance: Attendance,
    val member: Member
)

// 5. Payment Detail ViewModel
class PaymentDetailViewModel(private val repository: BandRepository) : ViewModel() {
    private val _currentCommitment = MutableStateFlow<Commitment?>(null)
    val currentCommitment = _currentCommitment.asStateFlow()

    private val _attendanceList = MutableStateFlow<List<MemberAttendanceInfo>>(emptyList())
    val attendanceList = _attendanceList.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: Integrantes, 1: Pagos
    val selectedTab = _selectedTab.asStateFlow()

    val totalPaid = _attendanceList.map { list ->
        list.filter { it.attendance.isPaid }.sumOf { it.attendance.paymentAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPending = _attendanceList.map { list ->
        list.filter { !it.attendance.isPaid }.sumOf { it.attendance.paymentAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun loadCommitment(commitmentId: Int) {
        viewModelScope.launch {
            val commitment = repository.getCommitmentById(commitmentId)
            _currentCommitment.value = commitment
            
            if (commitment != null) {
                // Combine members and attendance
                combine(repository.allMembers, repository.getAttendanceForCommitment(commitmentId)) { members, attendance ->
                    attendance.mapNotNull { att ->
                        val m = members.find { it.id == att.memberId }
                        if (m != null) MemberAttendanceInfo(att, m) else null
                    }
                }.collect { list ->
                    _attendanceList.value = list
                }
            }
        }
    }

    fun updateAttendanceStatus(memberId: Int, status: String) {
        val currentList = _attendanceList.value
        val item = currentList.find { it.member.id == memberId } ?: return
        
        viewModelScope.launch {
            val updated = item.attendance.copy(status = status)
            repository.updateAttendance(updated)
            
            // Reactive DB changes will automatically update our combined flow
        }
    }

    fun togglePaymentPaid(memberId: Int) {
        val currentList = _attendanceList.value
        val item = currentList.find { it.member.id == memberId } ?: return
        
        viewModelScope.launch {
            val updated = item.attendance.copy(isPaid = !item.attendance.isPaid)
            repository.updateAttendance(updated)
        }
    }

    fun updatePaymentAmount(memberId: Int, amount: Double) {
        val currentList = _attendanceList.value
        val item = currentList.find { it.member.id == memberId } ?: return
        
        viewModelScope.launch {
            val updated = item.attendance.copy(paymentAmount = amount)
            repository.updateAttendance(updated)
        }
    }

    fun markCommitmentAsFinished() {
        val commitment = _currentCommitment.value ?: return
        viewModelScope.launch {
            val updated = commitment.copy(isCompleted = true)
            repository.updateCommitment(updated)
            _currentCommitment.value = updated
        }
    }
}
