package com.example.pythagoros.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.pythagoros.data.history.HistoryRepository
import com.example.pythagoros.domain.model.SolutionHistoryEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    val historyEntries: Flow<List<SolutionHistoryEntry>> =
        historyRepository.observeAll()

    suspend fun saveHistoryEntry(entry: SolutionHistoryEntry) {
        historyRepository.save(entry)
    }
}
