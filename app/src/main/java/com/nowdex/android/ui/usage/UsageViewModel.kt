package com.nowdex.android.ui.usage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nowdex.android.data.AppContainer
import com.nowdex.android.data.model.DailyUsage
import com.nowdex.android.data.model.UsageStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class UsageViewModel : ViewModel() {

    private val repository = AppContainer.usageRepository

    val todayUsage: StateFlow<DailyUsage?> = repository.observeTodayUsage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val history: StateFlow<List<DailyUsage>> = repository.observeDailyHistory(365)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<UsageStats> = repository.observeUsageStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UsageStats(0, 0.0, 0, 0.0))
}
