package com.nowdex.android.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nowdex.android.data.AppContainer
import com.nowdex.android.data.model.ServiceQuota
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StatusViewModel : ViewModel() {

    private val repository = AppContainer.usageRepository

    val quotas: StateFlow<List<ServiceQuota>> = repository.observeQuotas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refresh() {
        viewModelScope.launch {
            repository.refreshQuotas()
        }
    }
}
