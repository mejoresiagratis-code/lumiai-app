package com.mejoresiagratis.lumiai.ui.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mejoresiagratis.lumiai.domain.billing.PurchaseOutcome
import com.mejoresiagratis.lumiai.domain.billing.SubscriptionProduct
import com.mejoresiagratis.lumiai.domain.billing.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionUiState(
    val product: SubscriptionProduct? = null,
    val purchasing: Boolean = false,
    val lastMessage: String? = null
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val repo: SubscriptionRepository
) : ViewModel() {

    private val _purchasing = MutableStateFlow(false)
    private val _lastMessage = MutableStateFlow<String?>(null)

    val ui: StateFlow<SubscriptionUiState> = combine(
        repo.product, _purchasing, _lastMessage
    ) { product, purchasing, message ->
        SubscriptionUiState(product = product, purchasing = purchasing, lastMessage = message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), SubscriptionUiState())

    fun purchase(activity: Activity?) {
        if (activity == null || _purchasing.value) return
        _purchasing.value = true
        viewModelScope.launch {
            when (val outcome = repo.purchase(activity)) {
                PurchaseOutcome.Success -> _lastMessage.value = "¡Ya eres Pro! Gracias por tu apoyo."
                PurchaseOutcome.UserCancelled -> { /* silencio: el usuario cerró el flujo, no es un error */ }
                PurchaseOutcome.AlreadyOwned -> _lastMessage.value = "Ya tenías la suscripción activa."
                PurchaseOutcome.Pending -> _lastMessage.value = "Pago pendiente de confirmación."
                is PurchaseOutcome.Error -> _lastMessage.value = outcome.message
            }
            _purchasing.value = false
        }
    }

    fun consumeMessage() { _lastMessage.value = null }
}
