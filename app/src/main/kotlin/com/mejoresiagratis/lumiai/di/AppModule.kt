package com.mejoresiagratis.lumiai.di

import com.mejoresiagratis.lumiai.data.settings.DataStoreFlashStateRepository
import com.mejoresiagratis.lumiai.data.settings.DataStoreOnboardingPreferencesRepository
import com.mejoresiagratis.lumiai.data.auth.FirebaseAuthRepository
import com.mejoresiagratis.lumiai.data.entitlement.DataStoreRewardProgressRepository
import com.mejoresiagratis.lumiai.data.entitlement.DataStoreTemporaryUnlockRepository
import com.mejoresiagratis.lumiai.data.entitlement.DefaultEntitlementRepository
import com.mejoresiagratis.lumiai.data.settings.DataStoreThemePreferencesRepository
import com.mejoresiagratis.lumiai.data.torch.Camera2TorchController
import com.mejoresiagratis.lumiai.data.torch.ServiceEngineController
import com.mejoresiagratis.lumiai.data.torch.TorchController
import com.mejoresiagratis.lumiai.domain.flash.EngineController
import com.mejoresiagratis.lumiai.domain.repository.AuthRepository
import com.mejoresiagratis.lumiai.domain.repository.EntitlementRepository
import com.mejoresiagratis.lumiai.domain.repository.TemporaryUnlockRepository
import com.mejoresiagratis.lumiai.domain.repository.FlashStateRepository
import com.mejoresiagratis.lumiai.domain.repository.OnboardingPreferencesRepository
import com.mejoresiagratis.lumiai.domain.repository.RewardProgressRepository
import com.mejoresiagratis.lumiai.domain.repository.ThemePreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindTorch(impl: Camera2TorchController): TorchController

    @Binds
    @Singleton
    abstract fun bindRepo(impl: DataStoreFlashStateRepository): FlashStateRepository

    @Binds
    @Singleton
    abstract fun bindEngineController(impl: ServiceEngineController): EngineController

    @Binds
    @Singleton
    abstract fun bindThemeRepo(impl: DataStoreThemePreferencesRepository): ThemePreferencesRepository

    @Binds
    @Singleton
    abstract fun bindOnboardingRepo(impl: DataStoreOnboardingPreferencesRepository): OnboardingPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepo(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindEntitlementRepo(impl: DefaultEntitlementRepository): EntitlementRepository

    @Binds
    @Singleton
    abstract fun bindTemporaryUnlockRepo(impl: DataStoreTemporaryUnlockRepository): TemporaryUnlockRepository

    @Binds
    @Singleton
    abstract fun bindRewardProgressRepo(impl: DataStoreRewardProgressRepository): RewardProgressRepository
}
