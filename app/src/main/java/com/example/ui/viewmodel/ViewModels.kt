package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.notifications.AdminNotificationManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ServerValue
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
    private val localAdminEmail = "bomarty28@gmail.com"
    private val localAdminPassword = "bomarty"
    private val localAdminName = "bomarty"

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
        prefs.getBoolean("biometric_login_enabled", false) &&
                (auth.currentUser != null || prefs.getString("last_login_type", "") == "local_admin")
    )
    val canUseBiometricLogin = _canUseBiometricLogin.asStateFlow()

    private val _savedLoginEmail = MutableStateFlow(prefs.getString("last_login_email", "") ?: "")
    val savedLoginEmail = _savedLoginEmail.asStateFlow()
    private val _registrationRequestStatus = MutableStateFlow<String?>(null)
    val registrationRequestStatus = _registrationRequestStatus.asStateFlow()
    private var isUsingAnotherAccount = false

    init {
        observeRegistrationRequest(prefs.getString("registration_request_id", null))
    }

    fun onEmailChanged(value: String) {
        _email.value = value
        _loginError.value = null
    }

    fun onPasswordChanged(value: String) {
        _password.value = value
        _loginError.value = null
    }

    fun prepareForAnotherAccount() {
        isUsingAnotherAccount = true
        _email.value = ""
        _password.value = ""
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
        isUsingAnotherAccount = false
    }

    private fun rememberLoginType(type: String) {
        prefs.edit().putString("last_login_type", type).apply()
    }

    fun loginWithPassword() {
        val email = _email.value.trim().ifEmpty {
            if (isUsingAnotherAccount) "" else _savedLoginEmail.value.trim()
        }
        val password = _password.value

        if (email.isEmpty() || password.isEmpty()) {
            _loginError.value = "Ingresa tu correo y contrasena"
            return
        }

        if (email.equals(localAdminEmail, ignoreCase = true) && password == localAdminPassword) {
            loginAsLocalAdmin()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                _loginError.value = null
                rememberLoginEmail(email)
                rememberLoginType("firebase")
                enableBiometricForThisDevice()
                _isLoggedIn.value = true
            }
            .addOnFailureListener { error ->
                _loginError.value = error.localizedMessage ?: "Usuario o contrasena incorrectos"
            }
    }

    private fun loginAsLocalAdmin() {
        auth.signOut()
        _loginError.value = null
        rememberLoginEmail(localAdminEmail)
        rememberLoginType("local_admin")
        enableBiometricForThisDevice()
        _isLoggedIn.value = true
    }

    fun loginWithFingerprint() {
        val isLocalAdmin = prefs.getString("last_login_type", "") == "local_admin" &&
                _savedLoginEmail.value.equals(localAdminEmail, ignoreCase = true)
        if (_canUseBiometricLogin.value && (auth.currentUser != null || isLocalAdmin)) {
            _loginError.value = null
            if (isLocalAdmin) {
                rememberLoginEmail(localAdminEmail)
                rememberLoginType("local_admin")
            }
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
                                        code = "Cuenta Firebase",
                                        phone = cleanPhone,
                                        instrument = instrument
                                    )
                                )
                            }.onSuccess {
                                _loginError.value = null
                                rememberLoginEmail(cleanEmail)
                                rememberLoginType("firebase")
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

    fun submitRegistrationRequest(
        name: String,
        email: String,
        phone: String,
        instrument: String,
        onComplete: () -> Unit
    ) {
        if (name.isBlank() || email.isBlank() || phone.isBlank() || instrument == "Seleccionar instrumento") {
            _loginError.value = "Completa nombre, correo, celular e instrumento"
            return
        }
        val request = mapOf(
            "name" to name.trim(), "email" to email.trim(), "phone" to phone.trim(),
            "instrument" to instrument, "status" to "pending", "createdAt" to ServerValue.TIMESTAMP
        )
        val reference = database.child("registrationRequests").push()
        reference.setValue(request)
            .addOnSuccessListener {
                prefs.edit()
                    .putString("registration_request_id", reference.key)
                    .putString("registration_name", name.trim())
                    .putString("registration_email", email.trim())
                    .putString("registration_phone", phone.trim())
                    .putString("registration_instrument", instrument)
                    .apply()
                observeRegistrationRequest(reference.key)
                _loginError.value = null
                onComplete()
            }
            .addOnFailureListener { error ->
                _loginError.value = error.localizedMessage ?: "No se pudo enviar la solicitud"
            }
    }

    private fun observeRegistrationRequest(requestId: String?) {
        if (requestId.isNullOrBlank()) return
        database.child("registrationRequests").child(requestId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    _registrationRequestStatus.value = snapshot.child("status").getValue(String::class.java)
                }
                override fun onCancelled(error: DatabaseError) = Unit
            })
    }

    fun pendingRegistrationDraft() = RegistrationDraft(
        name = prefs.getString("registration_name", "").orEmpty(),
        email = prefs.getString("registration_email", "").orEmpty(),
        phone = prefs.getString("registration_phone", "").orEmpty(),
        instrument = prefs.getString("registration_instrument", "Seleccionar instrumento").orEmpty()
    )

    fun clearRegistrationRequest() {
        prefs.edit()
            .remove("registration_request_id")
            .remove("registration_name")
            .remove("registration_email")
            .remove("registration_phone")
            .remove("registration_instrument")
            .apply()
        _registrationRequestStatus.value = null
    }
}

data class RegistrationDraft(val name: String, val email: String, val phone: String, val instrument: String)

data class UserProfileUi(
    val name: String = "Juan",
    val email: String = "",
    val phone: String = "",
    val instrument: String = "",
    val role: String = "Integrante",
    val photoUrl: String = "",
    val bandName: String = "Banda Musical de Puno",
    val currency: String = "S/ PEN"
)

class ProfileViewModel(context: Context) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val prefs = context.getSharedPreferences("bandpay_settings", Context.MODE_PRIVATE)
    private val authPrefs = context.getSharedPreferences("bandpay_auth", Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(
        UserProfileUi(
            email = auth.currentUser?.email.orEmpty(),
            bandName = prefs.getString("band_name", "Banda Musical de Puno") ?: "Banda Musical de Puno",
            currency = prefs.getString("currency", "S/ PEN") ?: "S/ PEN"
        )
    )
    val profile = _profile.asStateFlow()

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage = _saveMessage.asStateFlow()

    private var userListener: ValueEventListener? = null
    private var observedUserId: String? = null

    init {
        observeUserProfile()
    }

    override fun onCleared() {
        val uid = observedUserId
        val listener = userListener
        if (uid != null && listener != null) {
            database.child("users").child(uid).removeEventListener(listener)
        }
        super.onCleared()
    }

    fun onNameChanged(value: String) = updateProfile { it.copy(name = value) }
    fun onPhoneChanged(value: String) = updateProfile { it.copy(phone = value) }
    fun onInstrumentChanged(value: String) = updateProfile { it.copy(instrument = value) }
    fun onBandNameChanged(value: String) = updateProfile { it.copy(bandName = value) }
    fun onCurrencyChanged(value: String) = updateProfile { it.copy(currency = value) }
    fun onPhotoUrlChanged(value: String) = updateProfile { it.copy(photoUrl = value) }

    fun refresh() {
        observeUserProfile()
    }

    fun clearSaveMessage() {
        _saveMessage.value = null
    }

    fun saveProfile() {
        val current = _profile.value
        prefs.edit()
            .putString("band_name", current.bandName)
            .putString("currency", current.currency)
            .putString("profile_name", current.name)
            .putString("profile_phone", current.phone)
            .putString("profile_instrument", current.instrument)
            .putString("profile_photo_url", current.photoUrl)
            .apply()

        val uid = auth.currentUser?.uid
        if (uid == null) {
            _saveMessage.value = "Configuracion guardada en este dispositivo"
            return
        }

        val updates = mapOf(
            "name" to current.name.trim(),
            "email" to current.email,
            "phone" to current.phone.trim(),
            "instrument" to current.instrument.trim(),
            "role" to current.role.lowercase(),
            "photoUrl" to current.photoUrl.trim()
        )

        database.child("users").child(uid).updateChildren(updates)
            .addOnSuccessListener {
                _saveMessage.value = "Perfil actualizado"
            }
            .addOnFailureListener { error ->
                _saveMessage.value = error.localizedMessage ?: "No se pudo guardar el perfil"
            }
    }

    private fun observeUserProfile() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            if (authPrefs.getString("last_login_type", "") == "local_admin") {
                _profile.value = _profile.value.copy(
                    name = prefs.getString("profile_name", "bomarty") ?: "bomarty",
                    email = "bomarty28@gmail.com",
                    phone = prefs.getString("profile_phone", "") ?: "",
                    instrument = prefs.getString("profile_instrument", "Admin") ?: "Admin",
                    role = "Admin",
                    photoUrl = prefs.getString("profile_photo_url", "") ?: "",
                    bandName = prefs.getString("band_name", "Banda Musical de Puno") ?: "Banda Musical de Puno",
                    currency = prefs.getString("currency", "S/ PEN") ?: "S/ PEN"
                )
            }
            return
        }
        userListener?.let { listener ->
            observedUserId?.let { oldUid ->
                database.child("users").child(oldUid).removeEventListener(listener)
            }
        }
        val ref = database.child("users").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val current = _profile.value
                _profile.value = current.copy(
                    name = snapshot.child("name").getValue(String::class.java)?.ifBlank { current.name } ?: current.name,
                    email = snapshot.child("email").getValue(String::class.java)?.ifBlank { current.email }
                        ?: auth.currentUser?.email.orEmpty(),
                    phone = snapshot.child("phone").getValue(String::class.java).orEmpty(),
                    instrument = snapshot.child("instrument").getValue(String::class.java).orEmpty(),
                    role = snapshot.child("role").getValue(String::class.java)?.replaceFirstChar { it.uppercase() }
                        ?: current.role,
                    photoUrl = snapshot.child("photoUrl").getValue(String::class.java).orEmpty()
                )
            }

            override fun onCancelled(error: DatabaseError) {
                _saveMessage.value = error.message
            }
        }
        ref.addValueEventListener(listener)
        userListener = listener
        observedUserId = uid
    }

    private fun updateProfile(transform: (UserProfileUi) -> UserProfileUi) {
        _profile.value = transform(_profile.value)
        _saveMessage.value = null
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
    private val attendance = repository.allAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val upcomingCommitments = commitments.map { list ->
        list.filter { !it.isCompleted }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalPaymentsPending = attendance.map { list ->
        list.filter { !it.isPaid }.sumOf { it.paymentAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val pendingPaymentsCount = attendance.map { list ->
        list.filter { !it.isPaid }.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pendingTasksCount = attendance.map { list ->
        list.count { it.status.equals("Pendiente", ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completedCommitmentsCount = commitments.map { list -> list.count { it.isCompleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completionProgress = combine(commitments, completedCommitmentsCount) { list, completed ->
        if (list.isEmpty()) 0f else completed.toFloat() / list.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val nextCommitment = upcomingCommitments.map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentActivities = combine(commitments, attendance, activeMembersCount) { eventList, attendanceList, membersCount ->
        buildList {
            eventList.firstOrNull { it.isCompleted }?.let {
                add(RecentActivity("Evento completado", it.title, "Actual", "check"))
            }
            attendanceList.firstOrNull { it.isPaid }?.let {
                add(RecentActivity("Pago registrado", "Un pago fue marcado como recibido", "Actual", "payment"))
            }
            if (membersCount > 0) add(RecentActivity("Integrantes", "$membersCount integrantes registrados", "Actual", "user"))
        }.take(3)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
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
data class RegistrationRequest(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val instrument: String
)

class MembersViewModel(
    private val repository: BandRepository,
    private val context: Context
) : ViewModel() {
    companion object {
        private const val MEMBER_CREATION_APP = "member-creation"
    }

    private val memberAuth: FirebaseAuth by lazy {
        val app = FirebaseApp.getApps(context).firstOrNull { it.name == MEMBER_CREATION_APP }
            ?: requireNotNull(
                FirebaseApp.initializeApp(
                    context,
                    FirebaseApp.getInstance().options,
                    MEMBER_CREATION_APP
                )
            )
        FirebaseAuth.getInstance(app)
    }
    private val memberDatabase by lazy {
        FirebaseDatabase.getInstance(memberAuth.app).reference
    }
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedInstrument = MutableStateFlow("Todos")
    val selectedInstrument = _selectedInstrument.asStateFlow()

    // Form inputs
    val newMemberName = MutableStateFlow("")
    val newMemberEmail = MutableStateFlow("")
    val newMemberCode = MutableStateFlow("")
    val newMemberPhone = MutableStateFlow("")
    val newMemberInstrument = MutableStateFlow("Seleccionar instrumento")
    val newAccountRole = MutableStateFlow("Musico")

    private val _isAddedSuccess = MutableStateFlow(false)
    val isAddedSuccess = _isAddedSuccess.asStateFlow()

    private val _lastInvitedEmail = MutableStateFlow("juan.nuevo@email.com")
    val lastInvitedEmail = _lastInvitedEmail.asStateFlow()

    private val _addError = MutableStateFlow<String?>(null)
    val addError = _addError.asStateFlow()

    private val _pendingRequests = MutableStateFlow<List<RegistrationRequest>>(emptyList())
    val pendingRequests = _pendingRequests.asStateFlow()
    private val notificationPrefs = context.getSharedPreferences("bandpay_notifications", Context.MODE_PRIVATE)

    val members = repository.allMembers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        FirebaseDatabase.getInstance().reference.child("registrationRequests")
            .orderByChild("status").equalTo("pending")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val requests = snapshot.children.mapNotNull { child ->
                        child.key?.let { id ->
                            RegistrationRequest(
                                id = id,
                                name = child.child("name").getValue(String::class.java).orEmpty(),
                                email = child.child("email").getValue(String::class.java).orEmpty(),
                                phone = child.child("phone").getValue(String::class.java).orEmpty(),
                                instrument = child.child("instrument").getValue(String::class.java).orEmpty()
                            )
                        }
                    }
                    val notifiedIds = notificationPrefs.getStringSet("notified_request_ids", emptySet()).orEmpty()
                    val newRequests = requests.filter { it.id !in notifiedIds }
                    notifyAdministrator(newRequests, notifiedIds)
                    _pendingRequests.value = requests
                }
                override fun onCancelled(error: DatabaseError) { _addError.value = error.message }
            })
    }

    private fun isLocalAdmin(): Boolean =
        context.getSharedPreferences("bandpay_auth", Context.MODE_PRIVATE)
            .getString("last_login_type", "") == "local_admin"

    private fun notifyAdministrator(
        requests: List<RegistrationRequest>,
        notifiedIds: Set<String>
    ) {
        if (requests.isEmpty()) return

        fun notify() {
            requests.forEach { AdminNotificationManager.showRegistrationRequest(context, it.name) }
            notificationPrefs.edit()
                .putStringSet("notified_request_ids", notifiedIds + requests.map { it.id })
                .apply()
        }

        if (isLocalAdmin()) {
            notify()
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().reference.child("users").child(uid).child("role").get()
            .addOnSuccessListener { role ->
                if (role.getValue(String::class.java).equals("admin", ignoreCase = true)) notify()
            }
    }

    fun approveRegistrationRequest(request: RegistrationRequest) {
        FirebaseDatabase.getInstance().reference.child("registrationRequests").child(request.id)
            .updateChildren(mapOf("status" to "approved", "reviewedAt" to ServerValue.TIMESTAMP))
            .addOnFailureListener { error -> _addError.value = error.localizedMessage ?: "No se pudo aprobar" }
    }

    fun rejectRegistrationRequest(request: RegistrationRequest) {
        FirebaseDatabase.getInstance().reference.child("registrationRequests").child(request.id)
            .updateChildren(mapOf("status" to "rejected", "reviewedAt" to ServerValue.TIMESTAMP))
            .addOnFailureListener { error -> _addError.value = error.localizedMessage ?: "No se pudo rechazar" }
    }

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
        val name = newMemberName.value.trim()
        val email = newMemberEmail.value.trim()
        val password = newMemberCode.value
        val phone = newMemberPhone.value.trim()
        val accountRole = newAccountRole.value
        val firebaseRole = if (accountRole.equals("Admin", ignoreCase = true)) "admin" else "musico"

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            _addError.value = "Completa nombre, correo, celular y contrasena"
            return
        }
        if (password.length < 6) {
            _addError.value = "La contrasena debe tener al menos 6 caracteres"
            return
        }

        val member = Member(
            name = name,
            // La contraseña solo se envía a Firebase Auth; nunca se guarda en la base de datos.
            code = "Cuenta Firebase",
            phone = phone,
            instrument = if (firebaseRole == "admin") {
                "Admin"
            } else if (newMemberInstrument.value == "Seleccionar instrumento") {
                "Otros"
            } else {
                newMemberInstrument.value
            }
        )

        _addError.value = null
        memberAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: run {
                    _addError.value = "No se pudo crear la cuenta"
                    return@addOnSuccessListener
                }
                val profile = mapOf(
                    "name" to name,
                    "email" to email,
                    "phone" to phone,
                    "instrument" to member.instrument,
                    "role" to firebaseRole,
                    "biometricLoginEnabled" to false
                )
                memberDatabase.child("users").child(uid).setValue(profile)
                    .addOnSuccessListener {
                        if (firebaseRole == "admin") {
                            finishAccountCreation(email)
                        } else {
                            val memberId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
                            val savedMember = member.copy(id = memberId)
                            memberDatabase.child("members").child(memberId.toString()).setValue(savedMember)
                                .addOnSuccessListener {
                                        finishAccountCreation(email)
                                }
                                .addOnFailureListener { error ->
                                    _addError.value = error.localizedMessage ?: "No se pudo guardar el integrante"
                                }
                        }
                    }
                    .addOnFailureListener { error ->
                        _addError.value = error.localizedMessage ?: "No se pudo guardar el perfil"
                    }
            }
            .addOnFailureListener { error ->
                _addError.value = error.localizedMessage ?: "No se pudo crear la cuenta"
            }
    }

    private fun finishAccountCreation(email: String) {
        memberAuth.signOut()
        _lastInvitedEmail.value = email
        _isAddedSuccess.value = true
        newMemberName.value = ""
        newMemberEmail.value = ""
        newMemberCode.value = ""
        newMemberPhone.value = ""
        newMemberInstrument.value = "Seleccionar instrumento"
        newAccountRole.value = "Musico"
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
