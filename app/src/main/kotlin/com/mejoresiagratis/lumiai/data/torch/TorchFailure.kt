package com.mejoresiagratis.lumiai.data.torch

import com.mejoresiagratis.lumiai.R

/** Stable UI reasons; platform exception messages are never shown to the user. */
enum class TorchFailure(val messageRes: Int) {
    BUSY(R.string.torch_error_busy),
    PERMISSION(R.string.torch_error_permission),
    DISABLED(R.string.torch_error_disabled),
    UNAVAILABLE(R.string.torch_error_unavailable),
    NO_FLASH(R.string.sa_no_flash),
    OFF_FAILED(R.string.torch_error_off)
}

class TorchOperationException(val failure: TorchFailure, cause: Exception? = null) :
    RuntimeException(failure.name, cause)
