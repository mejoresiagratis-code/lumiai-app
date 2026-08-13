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
    override suspend fun resetIfVersionChanged(currentVersionCode: Int) { /* no ejercitado aquí */ }
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

    // ── Regla de producto (QA 13-ago): con Pro ACTIVO los anuncios no cuentan ni extienden ──

    @Test
    fun con_pro_activo_el_anuncio_no_cuenta_ni_extiende() = runTest {
        val progress = FakeRewardProgressRepository(0)
        val unlock = FakeTemporaryUnlockRepository().apply { extend(60_000L) } // activo
        val out = RecordRewardUseCase(progress, unlock)(now = 1_000L)          // 1s < 60s
        assertEquals(0, out.newCount)
        assertFalse(out.grantsUnlock)
        assertEquals(0, progress.count.first())                 // contador intacto
        assertEquals(listOf(60_000L), unlock.extendCalls)       // solo el extend del setup
    }

    @Test
    fun con_pro_caducado_el_anuncio_vuelve_a_contar() = runTest {
        val progress = FakeRewardProgressRepository(0)
        val unlock = FakeTemporaryUnlockRepository().apply { extend(60_000L) }
        val out = RecordRewardUseCase(progress, unlock)(now = 60_001L)          // ya caducado
        assertEquals(1, out.newCount)
        assertFalse(out.grantsUnlock)
        assertEquals(1, progress.count.first())
    }

    @Test
    fun umbral_alcanzado_con_pro_activo_NO_concede_otra_hora() = runTest {
        val progress = FakeRewardProgressRepository(RewardProgress.ADS_PER_GRANT - 1)
        val unlock = FakeTemporaryUnlockRepository().apply { extend(3_600_000L) }
        val out = RecordRewardUseCase(progress, unlock)(now = 5_000L)
        assertFalse(out.grantsUnlock)
        assertEquals(listOf(3_600_000L), unlock.extendCalls)    // sin segunda extension
    }

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
