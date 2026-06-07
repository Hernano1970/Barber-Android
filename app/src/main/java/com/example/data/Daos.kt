package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY fullName ASC")
    fun getAllClients(): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE id = :id")
    fun getClientById(id: Int): Flow<Client?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client)

    @Delete
    suspend fun deleteClient(client: Client)
    
    @Query("SELECT COUNT(*) FROM clients")
    fun getClientCount(): Flow<Int>
}

@Dao
interface ServiceDao {
    @Query("SELECT * FROM services WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveServices(): Flow<List<Service>>

    @Query("SELECT * FROM services ORDER BY name ASC")
    fun getAllServices(): Flow<List<Service>>

    @Query("SELECT * FROM services WHERE id = :id")
    fun getServiceById(id: Int): Flow<Service?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: Service)

    @Delete
    suspend fun deleteService(service: Service)
}

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments ORDER BY dateTimestamp ASC")
    fun getAllAppointments(): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE dateTimestamp >= :startOfDay AND dateTimestamp < :endOfDay ORDER BY dateTimestamp ASC")
    fun getAppointmentsForDay(startOfDay: Long, endOfDay: Long): Flow<List<Appointment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: Appointment)

    @Delete
    suspend fun deleteAppointment(appointment: Appointment)
    
    @Query("SELECT COUNT(*) FROM appointments WHERE dateTimestamp >= :startOfDay AND dateTimestamp < :endOfDay")
    fun getAppointmentCountForDay(startOfDay: Long, endOfDay: Long): Flow<Int>
}
