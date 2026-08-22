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
    /**
     * Resultado de la ultima compra, SIN traducir (22-ago). Antes el ViewModel fabricaba el
     * texto ya escrito — y en español fijo, asi que un usuario ingles veia "Ya tenias la
     * suscripcion activa" justo al pagar. La capa de presentacion no debe producir texto
     * visible: ahora entrega el hecho y la interfaz resuelve la string del idioma que toque.
     */
    val lastOutcome: PurchaseOutcome? = null
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val repo: SubscriptionRepository
) : ViewModel() {

    private val _purchasing = MutableStateFlow(false)
    private val _lastOutcome = MutableStateFlow<PurchaseOutcome?>(null)

    val ui: StateFlow<SubscriptionUiState> = combine(
        repo.product, _purchasing, _lastOutcome
    ) { product, purchasing, outcome ->
        SubscriptionUiState(product = product, purchasing = purchasing, lastOutcome = outcome)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), SubscriptionUiState())

    fun purchase(activity: Activity?) {
        if (activity == null || _purchasing.value) return
        _purchasing.value = true
        viewModelScope.launch {
            val outcome = repo.purchase(activity)
            // Cancelar no es un error: el usuario cerró el flujo a propósito y no merece aviso.
            _lastOutcome.value = outcome.takeIf { it !is PurchaseOutcome.UserCancelled }
            _purchasing.value = false
        }
    }

    fun consumeMessage() { _lastOutcome.value = null }
}
