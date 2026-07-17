package com.ferhatozcelik.jetpackcomposetemplate.di

import android.content.Context
import android.telephony.TelephonyManager
import com.ferhatozcelik.jetpackcomposetemplate.data.telephony.TelephonyRepositoryImpl
import com.ferhatozcelik.jetpackcomposetemplate.domain.repository.TelephonyRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TelephonyModule {

    @Binds
    @Singleton
    abstract fun bindTelephonyRepository(impl: TelephonyRepositoryImpl): TelephonyRepository

    companion object {

        @Provides
        @Singleton
        fun provideTelephonyManager(@ApplicationContext context: Context): TelephonyManager {
            return context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        }
    }
}
