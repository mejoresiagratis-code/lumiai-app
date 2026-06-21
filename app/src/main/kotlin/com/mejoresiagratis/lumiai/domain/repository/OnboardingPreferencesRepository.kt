package com.mejoresiagratis.lumiai.domain.repository

import kotlinx.coroutines.flow.Flow

interface OnboardingPreferencesRepository {
    val completed: Flow<Boolean>
    suspend fun setCompleted()
}
