package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class Repository(private val database: AppDatabase) {
    val allClients: Flow<List<Client>> = database.clientDao().getAllClients()
    val clientCount: Flow<Int> = database.clientDao().getClientCount()
    
    suspend fun insertClient(client: Client): Long = database.clientDao().insertClient(client)
    suspend fun deleteClient(client: Client) = database.clientDao().deleteClient(client)

    val activeServices: Flow<List<Service>> = database.serviceDao().getActiveServices()
    val allServices: Flow<List<Service>> = database.serviceDao().getAllServices()
    
    suspend fun insertService(service: Service) = database.serviceDao().insertService(service)
    suspend fun deleteService(service: Service) = database.serviceDao().deleteService(service)

    val allAppointments: Flow<List<Appointment>> = database.appointmentDao().getAllAppointments()
    
    suspend fun insertAppointment(appointment: Appointment) = database.appointmentDao().insertAppointment(appointment)
    suspend fun deleteAppointment(appointment: Appointment) = database.appointmentDao().deleteAppointment(appointment)
    
    val allWallets: Flow<List<Wallet>> = database.walletDao().getAllWallets()
    suspend fun insertWallet(wallet: Wallet) = database.walletDao().insertWallet(wallet)
    suspend fun deleteWallet(wallet: Wallet) = database.walletDao().deleteWallet(wallet)

    fun getAppointmentsForToday(): Flow<List<Appointment>> {
        val (start, end) = getTodayRange()
        return database.appointmentDao().getAppointmentsForDay(start, end)
    }

    fun getAppointmentCountForToday(): Flow<Int> {
        val (start, end) = getTodayRange()
        return database.appointmentDao().getAppointmentCountForDay(start, end)
    }
    
    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis
        
        return Pair(startOfDay, endOfDay)
    }
}
