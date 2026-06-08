package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = Repository(database)
    val appSettings = AppSettings(application.applicationContext)

    val clients: StateFlow<List<Client>> = repository.allClients.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeServices: StateFlow<List<Service>> = repository.activeServices.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val todayAppointments: StateFlow<List<Appointment>> = repository.getAppointmentsForToday().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allAppointments: StateFlow<List<Appointment>> = repository.allAppointments.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val todayAppointmentCount: StateFlow<Int> = repository.getAppointmentCountForToday().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )
    
    val totalClientCount: StateFlow<Int> = repository.clientCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    private val _businessName = kotlinx.coroutines.flow.MutableStateFlow(appSettings.businessName)
    val businessName: StateFlow<String> = _businessName

    fun updateBusinessName(newName: String) {
        appSettings.businessName = newName
        _businessName.value = newName
    }

    // Example Initial Data
    init {
        viewModelScope.launch {
            // Uncomment to populate initial data for testing
            /*
            if (repository.allServices.first().isEmpty()) {
                repository.insertService(Service(name = "Corte Clásico", price = 15.0, durationMinutes = 30, description = "Corte a tijera o máquina tradicional."))
                repository.insertService(Service(name = "Corte + Barba", price = 25.0, durationMinutes = 45, description = "Corte y perfilado de barba."))
            }
            */
        }
    }

    fun addClient(fullName: String, phone: String, observations: String) {
        viewModelScope.launch {
            repository.insertClient(Client(fullName = fullName, phone = phone, observations = observations))
        }
    }

    fun updateClient(client: Client) {
        viewModelScope.launch {
            repository.insertClient(client)
        }
    }

    fun deleteClient(client: Client) {
        viewModelScope.launch {
            repository.deleteClient(client)
        }
    }

    fun addService(name: String, price: Double, duration: Int, description: String) {
        viewModelScope.launch {
            repository.insertService(Service(name = name, price = price, durationMinutes = duration, description = description))
        }
    }

    fun addAppointment(clientId: Int, serviceId: Int, date: Long, observations: String) {
        viewModelScope.launch {
            repository.insertAppointment(
                Appointment(
                    clientId = clientId,
                    serviceId = serviceId,
                    dateTimestamp = date,
                    observations = observations,
                    status = "Pendiente"
                )
            )
        }
    }

    fun addAppointmentWithNewClient(clientName: String, clientPhone: String, clientObs: String, serviceId: Int, date: Long, apptObs: String) {
        viewModelScope.launch {
            val clientId = repository.insertClient(Client(fullName = clientName, phone = clientPhone, observations = clientObs)).toInt()
            repository.insertAppointment(
                Appointment(
                    clientId = clientId,
                    serviceId = serviceId,
                    dateTimestamp = date,
                    observations = apptObs,
                    status = "Pendiente"
                )
            )
        }
    }

    fun deleteAppointment(appointment: Appointment) {
        viewModelScope.launch {
            repository.deleteAppointment(appointment)
        }
    }

    fun updateAppointment(appointment: Appointment) {
        viewModelScope.launch {
            repository.insertAppointment(appointment)
        }
    }
}
