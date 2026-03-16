package com.shotmetrics.app.di

import android.content.Context
import com.shotmetrics.app.data.local.AppDatabase
import com.shotmetrics.app.data.local.SessionDao
import com.shotmetrics.app.data.preferences.UserPreferences
import com.shotmetrics.app.data.repository.SessionRepository
import com.shotmetrics.app.data.repository.SettingsRepository
import com.shotmetrics.app.domain.calculator.ATZCalculator
import com.shotmetrics.app.domain.calculator.BallisticsCalculator
import com.shotmetrics.app.domain.calculator.UnitConverter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.create(context)

    @Provides
    @Singleton
    fun provideSessionDao(database: AppDatabase): SessionDao =
        database.sessionDao()

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences =
        UserPreferences(context)

    @Provides
    @Singleton
    fun provideSettingsRepository(prefs: UserPreferences): SettingsRepository =
        SettingsRepository(prefs)

    @Provides
    @Singleton
    fun provideSessionRepository(dao: SessionDao): SessionRepository =
        SessionRepository(dao)

    @Provides
    @Singleton
    fun provideUnitConverter(): UnitConverter = UnitConverter()

    @Provides
    @Singleton
    fun provideBallisticsCalculator(unitConverter: UnitConverter): BallisticsCalculator =
        BallisticsCalculator(unitConverter)

    @Provides
    @Singleton
    fun provideATZCalculator(unitConverter: UnitConverter): ATZCalculator =
        ATZCalculator(unitConverter)
}
