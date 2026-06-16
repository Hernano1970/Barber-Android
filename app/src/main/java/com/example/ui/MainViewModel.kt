package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.flow.map
import java.text.Collator
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = Repository(database)
    val appSettings = AppSettings(application.applicationContext)

    private val collator = Collator.getInstance(Locale("es", "ES")).apply {
        strength = Collator.PRIMARY
    }

    val clients: StateFlow<List<Client>> = repository.allClients.map { list ->
        list.sortedWith { c1, c2 -> collator.compare(c1.fullName, c2.fullName) }
    }.stateIn(
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

    val wallets: StateFlow<List<Wallet>> = repository.allWallets.stateIn(
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
        viewModelScope.launch(Dispatchers.IO) {
            com.example.BackupHelper.checkAutoBackup(getApplication())
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

    fun updateService(service: Service) {
        viewModelScope.launch {
            repository.insertService(service)
        }
    }

    fun deleteService(service: Service) {
        viewModelScope.launch {
            repository.deleteService(service)
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
            com.example.NotificationHelper.scheduleAll(getApplication())
        }
    }

    fun addAppointmentWithNewClient(clientName: String, clientPhone: String, clientObs: String, serviceId: Int, date: Long, apptObs: String, isPermanent: Boolean = true) {
        viewModelScope.launch {
            val clientId = repository.insertClient(Client(fullName = clientName, phone = clientPhone, observations = clientObs, isPermanent = isPermanent)).toInt()
            repository.insertAppointment(
                Appointment(
                    clientId = clientId,
                    serviceId = serviceId,
                    dateTimestamp = date,
                    observations = apptObs,
                    status = "Pendiente"
                )
            )
            com.example.NotificationHelper.scheduleAll(getApplication())
        }
    }

    fun deleteAppointment(appointment: Appointment) {
        viewModelScope.launch {
            if (appointment.isPaid) {
                repository.insertAppointment(appointment.copy(status = "Eliminado"))
            } else {
                repository.deleteAppointment(appointment)
            }
        }
    }

    fun updateAppointment(appointment: Appointment) {
        viewModelScope.launch {
            repository.insertAppointment(appointment)
            com.example.NotificationHelper.scheduleAll(getApplication())
        }
    }
    
    fun scheduleNotifications() {
        com.example.NotificationHelper.scheduleAll(getApplication())
    }

    fun updateWallet(wallet: Wallet) {
        viewModelScope.launch {
            repository.insertWallet(wallet)
        }
    }

    fun deleteWallet(wallet: Wallet) {
        viewModelScope.launch {
            repository.deleteWallet(wallet)
        }
    }
}
