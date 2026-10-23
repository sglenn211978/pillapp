package com.pillease.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pillease.app.data.*
import com.pillease.app.receiver.AlarmReceiver
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class MedicationViewModel(
    private val repository: MedicationRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val allActiveMedications = repository.allActiveMedications
    val allLogs = repository.allLogs

    // Dynamic daily doses based on active meds, selected date logs, and current time
    val dailyDoses: StateFlow<List<DailyDoseItem>> = _selectedDate
        .flatMapLatest { date ->
            combine(
                repository.allActiveMedications,
                repository.getLogsForDate(date)
            ) { meds, logs ->
                repository.computeDosesForDate(meds, logs, date, LocalTime.now())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // As Needed (PRN) medications
    val asNeededMeds: StateFlow<List<Medication>> = repository.allActiveMedications
        .map { list -> list.filter { it.scheduleType == ScheduleType.AS_NEEDED_PRN } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    // 1-Tap Mark Taken
    fun markDoseTaken(item: DailyDoseItem) {
        viewModelScope.launch {
            repository.markDoseTaken(item.medication.id, item.scheduledDateTime)
        }
    }

    // 1-Tap Undo / Toggle Taken
    fun toggleDose(item: DailyDoseItem) {
        viewModelScope.launch {
            if (item.isTaken) {
                repository.undoDose(item.medication.id, item.scheduledDateTime)
            } else {
                repository.markDoseTaken(item.medication.id, item.scheduledDateTime)
            }
        }
    }

    // Mark Skipped
    fun markDoseSkipped(item: DailyDoseItem, reason: String = "") {
        viewModelScope.launch {
            repository.markDoseSkipped(item.medication.id, item.scheduledDateTime, reason)
        }
    }

    // 1-Tap "Take All Due Now"
    fun takeAllDueDoses() {
        val currentItems = dailyDoses.value
        val dueItems = currentItems.filter { it.isDueNow }
        viewModelScope.launch {
            for (item in dueItems) {
                repository.markDoseTaken(item.medication.id, item.scheduledDateTime)
            }
        }
    }

    // 1-Tap Log PRN (As Needed)
    fun logAsNeeded(med: Medication) {
        viewModelScope.launch {
            repository.logAsNeededDose(med.id)
        }
    }

    // Save medication and schedule/update alarm
    fun saveMedication(context: Context, medication: Medication, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val savedId = repository.saveMedication(medication)
            val updatedMed = medication.copy(id = savedId)

            // Cancel existing alarm and reschedule with new settings
            AlarmReceiver.cancelAlarmsForMedication(context, savedId)
            if (updatedMed.isActive && updatedMed.scheduleType != ScheduleType.AS_NEEDED_PRN) {
                AlarmReceiver.scheduleNextAlarmForMedication(context, updatedMed)
            }
            onComplete()
        }
    }

    fun deleteMedication(context: Context, medication: Medication) {
        viewModelScope.launch {
            AlarmReceiver.cancelAlarmsForMedication(context, medication.id)
            repository.deleteMedication(medication)
        }
    }
}

class MedicationViewModelFactory(private val repository: MedicationRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MedicationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MedicationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
