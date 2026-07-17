package com.ferhatozcelik.jetpackcomposetemplate.di

import com.ferhatozcelik.jetpackcomposetemplate.data.telephony.TelephonyRepositoryImpl
import com.ferhatozcelik.jetpackcomposetemplate.domain.repository.TelephonyRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * `@Binds` requires an abstract function, which a Kotlin `object` cannot
 * declare — hence this module is an `abstract class` with its pre-existing
 * `@Provides` function moved into a `companion object` (Dagger treats
 * companion-object `@Provides` functions the same as an `object` module).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindTelephonyRepository(impl: TelephonyRepositoryImpl): TelephonyRepository

    companion object {
        @ApplicationScope
        @Provides
        @Singleton
        fun provideApplicationScope(): CoroutineScope {
            return CoroutineScope(SupervisorJob())
        }
    }
}

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ApplicationScope