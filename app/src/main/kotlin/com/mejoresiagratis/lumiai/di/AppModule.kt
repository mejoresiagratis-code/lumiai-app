package com.mejoresiagratis.lumiai.di

import com.mejoresiagratis.lumiai.data.settings.DataStoreFlashStateRepository
import com.mejoresiagratis.lumiai.data.torch.Camera2TorchController
import com.mejoresiagratis.lumiai.data.torch.ServiceEngineController
import com.mejoresiagratis.lumiai.data.torch.TorchController
import com.mejoresiagratis.lumiai.domain.flash.EngineController
import com.mejoresiagratis.lumiai.domain.repository.FlashStateRepository
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
}
