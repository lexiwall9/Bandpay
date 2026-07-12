package com.example.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class BandRepository(private val bandDao: BandDao) {
    private val database = FirebaseDatabase.getInstance().reference

    val allMembers: Flow<List<Member>> = observeList("members") { snapshot ->
        snapshot.toMember()
    }

    val allCommitments: Flow<List<Commitment>> = observeList("commitments") { snapshot ->
        snapshot.toCommitment()
    }

    val allAttendance: Flow<List<Attendance>> = observeList("attendance") { snapshot ->
        snapshot.toAttendance()
    }

    fun getAttendanceForCommitment(commitmentId: Int): Flow<List<Attendance>> = callbackFlow {
        val ref = database.child("attendance")
            .orderByChild("commitmentId")
            .equalTo(commitmentId.toDouble())
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.children.mapNotNull { it.toAttendance() })
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun addMember(member: Member): Long {
        val id = member.id.takeIf { it != 0 } ?: newFirebaseIntId()
        database.child("members").child(id.toString()).setValueAwait(member.copy(id = id))
        return id.toLong()
    }

    suspend fun addCommitment(commitment: Commitment): Long {
        val id = commitment.id.takeIf { it != 0 } ?: newFirebaseIntId()
        database.child("commitments").child(id.toString()).setValueAwait(commitment.copy(id = id))
        createDefaultAttendanceForCommitment(id)
        return id.toLong()
    }

    suspend fun updateCommitment(commitment: Commitment) {
        database.child("commitments").child(commitment.id.toString()).setValueAwait(commitment)
    }

    suspend fun updateAttendance(attendance: Attendance) {
        val id = attendance.id.takeIf { it != 0 } ?: newFirebaseIntId()
        database.child("attendance").child(id.toString()).setValueAwait(attendance.copy(id = id))
    }

    suspend fun getCommitmentById(id: Int): Commitment? = suspendCancellableCoroutine { continuation ->
        val ref = database.child("commitments").child(id.toString())
        ref.get()
            .addOnSuccessListener { snapshot ->
                continuation.resume(snapshot.toCommitment())
            }
            .addOnFailureListener {
                continuation.resume(null)
            }
    }

    suspend fun prepopulateIfEmpty() = Unit

    suspend fun ensureAttendanceForCommitment(commitmentId: Int) {
        val existing = getAttendanceForCommitment(commitmentId).first()
        if (existing.isEmpty()) {
            createDefaultAttendanceForCommitment(commitmentId)
        }
    }

    private fun <T> observeList(
        path: String,
        mapper: (DataSnapshot) -> T?
    ): Flow<List<T>> = callbackFlow {
        val ref = database.child(path)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.children.mapNotNull(mapper))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    private fun DataSnapshot.toMember(): Member? {
        val id = child("id").asInt() ?: key?.toIntOrNull() ?: return null
        return Member(
            id = id,
            name = child("name").asString(),
            code = child("code").asString(),
            phone = child("phone").asString(),
            instrument = child("instrument").asString()
        )
    }

    private fun DataSnapshot.toCommitment(): Commitment? {
        val id = child("id").asInt() ?: key?.toIntOrNull() ?: return null
        return Commitment(
            id = id,
            title = child("title").asString(),
            description = child("description").asString(),
            date = child("date").asString(),
            time = child("time").asString(),
            location = child("location").asString(),
            isCompleted = child("completed").asBoolean() ?: child("isCompleted").asBoolean() ?: false
        )
    }

    private fun DataSnapshot.toAttendance(): Attendance? {
        val id = child("id").asInt() ?: key?.toIntOrNull() ?: return null
        val commitmentId = child("commitmentId").asInt() ?: return null
        val memberId = child("memberId").asInt() ?: return null
        return Attendance(
            id = id,
            commitmentId = commitmentId,
            memberId = memberId,
            status = child("status").asString("Pendiente"),
            paymentAmount = child("paymentAmount").asDouble() ?: 0.0,
            isPaid = child("paid").asBoolean() ?: child("isPaid").asBoolean() ?: false
        )
    }

    private fun DataSnapshot.asString(default: String = ""): String {
        return getValue(String::class.java) ?: default
    }

    private fun DataSnapshot.asInt(): Int? {
        return when (val value = value) {
            is Long -> value.toInt()
            is Int -> value
            is Double -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
    }

    private fun DataSnapshot.asDouble(): Double? {
        return when (val value = value) {
            is Double -> value
            is Long -> value.toDouble()
            is Int -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }

    private fun DataSnapshot.asBoolean(): Boolean? {
        return when (val value = value) {
            is Boolean -> value
            is String -> value.toBooleanStrictOrNull()
            else -> null
        }
    }

    private fun newFirebaseIntId(): Int {
        return (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
    }

    private suspend fun createDefaultAttendanceForCommitment(commitmentId: Int) {
        val members = allMembers.first()
        members.forEachIndexed { index, member ->
            val defaultAmount = when {
                member.instrument.contains("Platillo", ignoreCase = true) -> 120.0
                member.instrument.contains("Tarola", ignoreCase = true) -> 120.0
                member.instrument.contains("Trompeta", ignoreCase = true) -> 130.0
                member.instrument.contains("Teclado", ignoreCase = true) -> 120.0
                member.instrument.contains("Guitarra", ignoreCase = true) -> 120.0
                member.instrument.contains("Bateria", ignoreCase = true) -> 150.0
                member.instrument.contains("Batería", ignoreCase = true) -> 150.0
                else -> 100.0
            }
            val attendanceId = ((commitmentId.toLong() * 1000L + index + 1L) % Int.MAX_VALUE).toInt()
            updateAttendance(
                Attendance(
                    id = attendanceId,
                    commitmentId = commitmentId,
                    memberId = member.id,
                    status = "Pendiente",
                    paymentAmount = defaultAmount,
                    isPaid = false
                )
            )
        }
    }

    private suspend fun DatabaseReference.setValueAwait(value: Any?) {
        suspendCancellableCoroutine { continuation ->
            setValue(value)
                .addOnSuccessListener {
                    continuation.resume(Unit)
                }
                .addOnFailureListener { error ->
                    continuation.resumeWithException(error)
                }
        }
    }
}
