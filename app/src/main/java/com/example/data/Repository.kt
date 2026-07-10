package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class BandRepository(private val bandDao: BandDao) {

    val allMembers: Flow<List<Member>> = bandDao.getAllMembers()
    val allCommitments: Flow<List<Commitment>> = bandDao.getAllCommitments()
    val allAttendance: Flow<List<Attendance>> = bandDao.getAllAttendance()

    fun getAttendanceForCommitment(commitmentId: Int): Flow<List<Attendance>> {
        return bandDao.getAttendanceForCommitment(commitmentId)
    }

    suspend fun addMember(member: Member): Long {
        return bandDao.insertMember(member)
    }

    suspend fun addCommitment(commitment: Commitment): Long {
        val id = bandDao.insertCommitment(commitment)
        
        // When creating a new commitment, create default attendance records for all active members
        val membersList = bandDao.getAllMembers().first()
        for (m in membersList) {
            val defaultAmount = when (m.instrument) {
                "Platillo", "Tarola" -> 120.0
                "Trompeta", "Trompeta Principal", "Trompeta 2" -> 130.0
                "Teclados", "Guitarra Eléctrica" -> 120.0
                "Batería" -> 150.0
                else -> 100.0
            }
            bandDao.insertAttendance(
                Attendance(
                    commitmentId = id.toInt(),
                    memberId = m.id,
                    status = "Pendiente",
                    paymentAmount = defaultAmount,
                    isPaid = false
                )
            )
        }
        return id
    }

    suspend fun updateCommitment(commitment: Commitment) {
        bandDao.updateCommitment(commitment)
    }

    suspend fun updateAttendance(attendance: Attendance) {
        bandDao.updateAttendance(attendance)
    }

    suspend fun getCommitmentById(id: Int): Commitment? {
        return bandDao.getCommitmentById(id)
    }

    suspend fun prepopulateIfEmpty() {
        val existingMembers = bandDao.getAllMembers().first()
        if (existingMembers.isEmpty()) {
            // Insert exact members from the high fidelity screens
            val initialMembers = listOf(
                Member(name = "María Fernández", code = "maria123", phone = "986532147", instrument = "Platillo"),
                Member(name = "Juan Pérez", code = "juan123", phone = "987456321", instrument = "Barítono"),
                Member(name = "Ana Gómez", code = "ana123", phone = "953951210", instrument = "Trompeta"),
                Member(name = "Luis Rodríguez", code = "luis123", phone = "987451521", instrument = "Clarinete"),
                Member(name = "Carla Ruiz", code = "carla123", phone = "986532111", instrument = "Saxofón"),
                Member(name = "Pedro Sánchez", code = "pedro123", phone = "985574121", instrument = "Tarola"),
                
                // Extra members for the commitment detail / payment screens
                Member(name = "Marco Aurelio", code = "marco123", phone = "912345678", instrument = "Trompeta Principal"),
                Member(name = "Elena Rojas", code = "elena123", phone = "923456789", instrument = "Tarolas"),
                Member(name = "Santiago Velez", code = "santiago123", phone = "934567890", instrument = "Bombos"),
                Member(name = "Luis Mendez", code = "luism123", phone = "945678901", instrument = "Trompeta 2"),
                Member(name = "Alex Rivera", code = "alex123", phone = "956789012", instrument = "Batería"),
                Member(name = "Elena Gómez", code = "elenag123", phone = "967890123", instrument = "Guitarra Eléctrica"),
                Member(name = "Marcos Ruiz", code = "marcos123", phone = "978901234", instrument = "Teclados")
            )

            val memberIds = mutableMapOf<String, Int>()
            for (m in initialMembers) {
                val id = bandDao.insertMember(m)
                memberIds[m.name] = id.toInt()
            }

            // Insert default commitments from screenshots
            val commitments = listOf(
                Commitment(
                    title = "Ensayo General: Gala Anual",
                    description = "Ensayo de la banda municipal para la Gala Anual.",
                    date = "15 Oct 2026",
                    time = "18:00",
                    location = "Teatro Central",
                    isCompleted = false
                ),
                Commitment(
                    title = "Reunión de planificación",
                    description = "Coordinación interna de presupuestos y ensayos.",
                    date = "25 May 2026",
                    time = "10:00 AM",
                    location = "Sala 101",
                    isCompleted = false
                ),
                Commitment(
                    title = "Entrega de proyecto",
                    description = "Entrega y revisión de carpetas musicales.",
                    date = "02 Jun 2026",
                    time = "05:00 PM",
                    location = "Virtual",
                    isCompleted = false
                ),
                Commitment(
                    title = "Presentación final",
                    description = "Gran concierto regional de Puno.",
                    date = "10 Jun 2026",
                    time = "09:00 AM",
                    location = "Auditorio",
                    isCompleted = true
                )
            )

            for (c in commitments) {
                val commitmentId = bandDao.insertCommitment(c).toInt()

                // Insert specific attendance states for "Ensayo General: Gala Anual" to match screenshot precisely
                if (c.title == "Ensayo General: Gala Anual") {
                    val attendanceRecords = listOf(
                        Attendance(
                            commitmentId = commitmentId,
                            memberId = memberIds["Marco Aurelio"] ?: 0,
                            status = "Aceptado",
                            paymentAmount = 120.0,
                            isPaid = false
                        ),
                        Attendance(
                            commitmentId = commitmentId,
                            memberId = memberIds["Elena Rojas"] ?: 0,
                            status = "Pendiente",
                            paymentAmount = 120.0,
                            isPaid = false
                        ),
                        Attendance(
                            commitmentId = commitmentId,
                            memberId = memberIds["Santiago Velez"] ?: 0,
                            status = "Rechazado",
                            paymentAmount = 120.0,
                            isPaid = false
                        ),
                        Attendance(
                            commitmentId = commitmentId,
                            memberId = memberIds["Luis Mendez"] ?: 0,
                            status = "Pendiente",
                            paymentAmount = 120.0,
                            isPaid = false
                        ),
                        Attendance(
                            commitmentId = commitmentId,
                            memberId = memberIds["Alex Rivera"] ?: 0,
                            status = "Aceptado",
                            paymentAmount = 150.0,
                            isPaid = true
                        ),
                        Attendance(
                            commitmentId = commitmentId,
                            memberId = memberIds["Elena Gómez"] ?: 0,
                            status = "Aceptado",
                            paymentAmount = 120.0,
                            isPaid = false
                        ),
                        Attendance(
                            commitmentId = commitmentId,
                            memberId = memberIds["Marcos Ruiz"] ?: 0,
                            status = "Aceptado",
                            paymentAmount = 120.0,
                            isPaid = false
                        )
                    )
                    for (att in attendanceRecords) {
                        bandDao.insertAttendance(att)
                    }

                    // For the remaining members, create default pending records
                    for (m in initialMembers) {
                        if (m.name !in listOf("Marco Aurelio", "Elena Rojas", "Santiago Velez", "Luis Mendez", "Alex Rivera", "Elena Gómez", "Marcos Ruiz")) {
                            bandDao.insertAttendance(
                                Attendance(
                                    commitmentId = commitmentId,
                                    memberId = memberIds[m.name] ?: 0,
                                    status = "Pendiente",
                                    paymentAmount = 100.0,
                                    isPaid = false
                                )
                            )
                        }
                    }
                } else {
                    // Create default records for other commitments
                    for (m in initialMembers) {
                        bandDao.insertAttendance(
                            Attendance(
                                commitmentId = commitmentId,
                                memberId = memberIds[m.name] ?: 0,
                                status = if (c.isCompleted) "Aceptado" else "Pendiente",
                                paymentAmount = 120.0,
                                isPaid = c.isCompleted
                            )
                        )
                    }
                }
            }
        }
    }
}
