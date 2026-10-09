package com.mejoresiagratis.lumiai.domain.entitlement

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transformWhile

/** Do not open hardware before access is resolved; cancel work before reporting revocation. */
@OptIn(ExperimentalCoroutinesApi::class)
suspend fun Flow<Boolean>.whileAiAccess(
    onDenied: () -> Unit,
    runSession: suspend () -> Unit
) {
    // Deliver false to collectLatest so it cancels and joins the running block.
    // Completing the upstream without this emission would wait for that block forever.
    distinctUntilChanged().transformWhile { allowed ->
        emit(allowed)
        allowed
    }.collectLatest { allowed ->
        if (allowed) runSession() else onDenied()
    }
}
