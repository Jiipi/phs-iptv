package vn.phs.iptv.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Hilt module — reserved for manual @Provides bindings.
// ProvisioningDataStore is auto-bound via @Inject constructor + @Singleton.
// NetworkModule handles all Retrofit / OkHttp bindings.
@Module
@InstallIn(SingletonComponent::class)
object AppModule
