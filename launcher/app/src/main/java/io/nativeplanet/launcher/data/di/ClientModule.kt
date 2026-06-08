package io.nativeplanet.launcher.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.nativeplanet.launcher.data.provider.ProviderNativePlanetClient
import io.nativeplanet.launcher.domain.NativePlanetClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ClientModule {

    @Binds
    @Singleton
    abstract fun bindNativePlanetClient(
        impl: ProviderNativePlanetClient
    ): NativePlanetClient
}
