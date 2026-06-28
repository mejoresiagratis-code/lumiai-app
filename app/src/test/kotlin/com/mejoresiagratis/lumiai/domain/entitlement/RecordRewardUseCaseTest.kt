package com.mejoresiagratis.lumiai.domain.entitlement

import com.mejoresiagratis.lumiai.domain.repository.RewardProgressRepository
import com.mejoresiagratis.lumiai.domain.repository.TemporaryUnlockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeRewardProgressRepository(initial: Int = 0) : RewardProgressRepository {
    private val state = MutableStateFlow(initial)
    override val count: Flow<Int> = state
    override suspend fun set(value: Int) { state.value = value.coerceAtLeast(0) }
}

private class FakeTemporaryUnlockRepository : TemporaryUnlockRepository {
    val extendCalls = mutableListOf<Long>()
    private val state = MutableStateFlow(0L)
    override val proUntilMillis: Flow<Long> = state
    override suspend fun extend(durationMillis: Long) {
        extendCalls += durationMillis
        state.value += durationMillis
    }
    override suspend fun clear() { state.value = 0L }
}

class RecordRewardUseCaseTest {

    @Test
    fun primer_anuncio_no_concede_hora() = runTest {
        val progress = FakeRewardProgressRepository(0)
        val unlock = FakeTemporaryUnlockRepository()
        val out = RecordRewardUseCase(progress, unlock)()
        assertEquals(1, out.newCount)
        assertFalse(out.grantsUnlock)
        assertEquals(1, progress.count.first())
        assertTrue(unlock.extendCalls.isEmpty())
    }

    @Test
    fun segundo_anuncio_concede_una_hora_y_reinicia() = runTest {
        val progress = FakeRewardProgressRepository(RewardProgress.ADS_PER_GRANT - 1)
        val unlock = FakeTemporaryUnlockRepository()
        val out = RecordRewardUseCase(progress, unlock)()
        assertTrue(out.grantsUnlock)
        assertEquals(0, progress.count.first())
        assertEquals(listOf(TemporaryUnlock.HOUR_MS), unlock.extendCalls)
    }

    @Test
    fun dos_anuncios_consecutivos_desde_cero_conceden_una_vez() = runTest {
        val progress = FakeRewardProgressRepository(0)
        val unlock = FakeTemporaryUnlockRepository()
        val useCase = RecordRewardUseCase(progress, unlock)
        useCase()
        val out = useCase()
        assertTrue(out.grantsUnlock)
        assertEquals(listOf(TemporaryUnlock.HOUR_MS), unlock.extendCalls)
    }
}
