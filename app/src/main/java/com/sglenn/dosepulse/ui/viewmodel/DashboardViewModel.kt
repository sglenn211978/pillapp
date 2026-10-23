package com.sglenn.dosepulse.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sglenn.dosepulse.alarm.AlarmSoundService
import com.sglenn.dosepulse.alarm.DoseActionHandler
import com.sglenn.dosepulse.alarm.MedicationAlarmScheduler
import com.sglenn.dosepulse.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class DashboardViewModel(
    private val repository: MedicationRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Active live ringing alarm state for emergency banner
    val activeAlarm = AlarmSoundService.activeAlarmState

    // References & Categories
    val allReferences = repository.allReferences
    val referenceCategories = repository.referenceCategories

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredReferences: StateFlow<List<MedicationReference>> = _searchQuery
        .debounce(250)
        .flatMapLatest { query ->
            if (query.isBlank()) repository.allReferences
            else repository.searchReferences(query)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Daily Schedule computation
    val dailySchedule: StateFlow<List<ScheduleItem>> = _selectedDate
        .flatMapLatest { date ->
            combine(
                repository.allActiveMedications,
                repository.getLogsForDate(date)
            ) { meds, logs ->
                repository.computeSchedule(meds, logs, date, LocalTime.now())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val complianceStats: StateFlow<ComplianceStats> = dailySchedule
        .map { items -> repository.calculateCompliance(items) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ComplianceStats(0, 0, 0))

    val allLogs = repository.allLogs
    val allActiveMedications = repository.allActiveMedications
    val allMedications = repository.allMedications

    suspend fun getMedicationById(id: Long): Medication? = repository.getMedicationById(id)

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun markTaken(context: Context, item: ScheduleItem) {
        viewModelScope.launch {
            DoseActionHandler.take(context, repository, item.medication.id, item.scheduledDate, item.scheduledTime)
        }
    }

    fun toggleTaken(context: Context, item: ScheduleItem) {
        viewModelScope.launch {
            if (item.status == DoseStatus.TAKEN) {
                repository.undoDose(item.medication.id, item.scheduledDate, item.scheduledTime)
            } else {
                DoseActionHandler.take(context, repository, item.medication.id, item.scheduledDate, item.scheduledTime)
            }
        }
    }

    fun markSkipped(context: Context, item: ScheduleItem, reason: String) {
        viewModelScope.launch {
            DoseActionHandler.skip(context, repository, item.medication.id, item.scheduledDate, item.scheduledTime, reason)
        }
    }

    fun saveMedication(context: Context, medication: Medication, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val savedId = repository.saveMedication(medication)
            val updated = medication.copy(id = savedId)
            MedicationAlarmScheduler.cancelAlarms(context, savedId)
            val scheduled = !updated.isActive || MedicationAlarmScheduler.scheduleNextAlarm(context, updated)
            onComplete(scheduled)
        }
    }

    fun deleteMedication(context: Context, medication: Medication) {
        viewModelScope.launch {
            MedicationAlarmScheduler.cancelAlarms(context, medication.id)
            repository.deleteMedication(medication)
        }
    }

    // Trigger instant 5-second test alarm simulator
    fun testAlarm5s(context: Context): Boolean {
        return MedicationAlarmScheduler.scheduleTestAlarm(context, delaySeconds = 5)
    }

    // Silence active ringing alarm
    fun silenceAlarm(context: Context) {
        AlarmSoundService.stop(context)
    }
}

class DashboardViewModelFactory(private val repository: MedicationRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
