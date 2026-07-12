package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// 1. Login ViewModel
class LoginViewModel(
    private val repository: BandRepository,
    context: Context
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val prefs = context.getSharedPreferences("bandpay_auth", Context.MODE_PRIVATE)
    private val adminEmail = "admin@bandaspuno.com"
    private val adminPassword = "admin123456"

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

    private val _canUseBiometricLogin = MutableStateFlow(
        auth.currentUser != null && prefs.getBoolean("biometric_login_enabled", false)
    )
    val canUseBiometricLogin = _canUseBiometricLogin.asStateFlow()

    private val _savedLoginEmail = MutableStateFlow(prefs.getString("last_login_email", "") ?: "")
    val savedLoginEmail = _savedLoginEmail.asStateFlow()

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

    fun setLoginError(message: String?) {
        _loginError.value = message
    }

    private fun enableBiometricForThisDevice() {
        prefs.edit().putBoolean("biometric_login_enabled", true).apply()
        _canUseBiometricLogin.value = true
    }

    private fun rememberLoginEmail(email: String) {
        prefs.edit().putString("last_login_email", email).apply()
        _savedLoginEmail.value = email
        _email.value = email
    }

    fun loginWithPassword() {
        val email = _email.value.trim().ifEmpty { _savedLoginEmail.value.trim() }
        val password = _password.value

        if (email.isEmpty() || password.isEmpty()) {
            _loginError.value = "Ingresa tu contrasena"
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                _loginError.value = null
                rememberLoginEmail(email)
                enableBiometricForThisDevice()
                _isLoggedIn.value = true
            }
            .addOnFailureListener { error ->
                if (email.equals(adminEmail, ignoreCase = true) && password == adminPassword) {
                    createAdminAccountAndLogin()
                } else {
                    _loginError.value = error.localizedMessage ?: "Usuario o contrasena incorrectos"
                }
            }
    }

    private fun createAdminAccountAndLogin() {
        auth.createUserWithEmailAndPassword(adminEmail, adminPassword)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    _loginError.value = "No se pudo crear la cuenta admin"
                    return@addOnSuccessListener
                }

                val profile = mapOf(
                    "name" to "Administrador",
                    "email" to adminEmail,
                    "phone" to "",
                    "instrument" to "Admin",
                    "role" to "admin",
                    "biometricLoginEnabled" to false
                )

                database.child("users").child(uid).setValue(profile)
                    .addOnSuccessListener {
                        _loginError.value = null
                        rememberLoginEmail(adminEmail)
                        enableBiometricForThisDevice()
                        _isLoggedIn.value = true
                    }
                    .addOnFailureListener { error ->
                        _loginError.value = error.localizedMessage ?: "No se pudo guardar la cuenta admin en Firebase"
                    }
            }
            .addOnFailureListener { error ->
                _loginError.value = error.localizedMessage ?: "No se pudo crear la cuenta admin en Firebase"
            }
    }

    fun loginWithFingerprint() {
        if (_canUseBiometricLogin.value && auth.currentUser != null) {
            _loginError.value = null
            _isLoggedIn.value = true
            _showFingerprintDialog.value = false
        } else {
            _loginError.value = "Primero inicia sesion con contrasena en este celular"
        }
    }

    fun registerMember(
        name: String,
        email: String,
        password: String,
        phone: String,
        instrument: String,
        onComplete: () -> Unit
    ) {
        val cleanEmail = email.trim()
        val cleanName = name.trim()
        val cleanPhone = phone.trim()

        if (cleanName.isEmpty() || cleanEmail.isEmpty() || password.isEmpty() || cleanPhone.isEmpty()) {
            _loginError.value = "Completa todos los datos para registrarte"
            return
        }
        if (password.length < 6) {
            _loginError.value = "La contrasena debe tener al menos 6 caracteres"
            return
        }

        auth.createUserWithEmailAndPassword(cleanEmail, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    _loginError.value = "No se pudo crear el usuario en Firebase"
                    return@addOnSuccessListener
                }

                val profile = mapOf(
                    "name" to cleanName,
                    "email" to cleanEmail,
                    "phone" to cleanPhone,
                    "instrument" to instrument,
                    "role" to "member",
                    "biometricLoginEnabled" to false
                )

                database.child("users").child(uid).setValue(profile)
                    .addOnSuccessListener {
                        viewModelScope.launch {
                            runCatching {
                                repository.addMember(
                                    Member(
                                        name = cleanName,
                                        code = password,
                                        phone = cleanPhone,
                                        instrument = instrument
                                    )
                                )
                            }.onSuccess {
                                _loginError.value = null
                                rememberLoginEmail(cleanEmail)
                                enableBiometricForThisDevice()
                                onComplete()
                            }.onFailure { error ->
                                _loginError.value = error.localizedMessage ?: "No se pudo guardar el integrante en Firebase"
                            }
                        }
                    }
                    .addOnFailureListener { error ->
                        _loginError.value = error.localizedMessage ?: "No se pudo guardar el perfil en Firebase"
                    }
            }
            .addOnFailureListener { error ->
                _loginError.value = error.localizedMessage ?: "No se pudo registrar el usuario en Firebase"
            }
    }

    fun logout() {
        _isLoggedIn.value = false
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

    val recentActivities = flowOf(emptyList<RecentActivity>())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

// 3. Commitments ViewModel
class CommitmentsViewModel(private val repository: BandRepository) : ViewModel() {
    private val _selectedTab = MutableStateFlow(0) // 0: PrÃƒÆ’Ã‚Â³ximos, 1: Historial
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
            runCatching {
                repository.addCommitment(commitment)
            }.onSuccess { id ->
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
            runCatching {
                repository.addMember(member)
            }.onSuccess {
                _lastInvitedEmail.value = "${member.name.replace(" ", "").lowercase()}@email.com"
                _isAddedSuccess.value = true

                // Reset
                newMemberName.value = ""
                newMemberCode.value = ""
                newMemberPhone.value = ""
                newMemberInstrument.value = "Seleccionar instrumento"
            }
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

    val membersCount = repository.allMembers
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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
                repository.ensureAttendanceForCommitment(commitmentId)
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
