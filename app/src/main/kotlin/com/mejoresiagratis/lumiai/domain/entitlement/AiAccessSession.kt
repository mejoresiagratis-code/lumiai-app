package com.mejoresiagratis.lumiai.domain.entitlement

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.takeWhile

/** Do not open hardware before access is resolved; cancel work before reporting revocation. */
suspend fun Flow<Boolean>.whileAiAccess(
    onDenied: () -> Unit,
    runSession: suspend () -> Unit
) {
    distinctUntilChanged().takeWhile { it }.collectLatest { runSession() }
    onDenied()
}
