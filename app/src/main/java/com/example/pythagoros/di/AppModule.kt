package com.example.pythagoros.di

import android.content.Context
import androidx.room.Room
import com.example.pythagoros.data.ai.BackendPremiumAiSolver
import com.example.pythagoros.data.auth.BackendAuthClient
import com.example.pythagoros.data.history.HistoryDao
import com.example.pythagoros.data.history.PythagorosDatabase
import com.example.pythagoros.domain.ai.PremiumAiSolver
import com.example.pythagoros.domain.solver.CasSolver
import com.example.pythagoros.domain.solver.LocalSolver
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
    fun provideDatabase(@ApplicationContext context: Context): PythagorosDatabase =
        Room.databaseBuilder(
            context,
            PythagorosDatabase::class.java,
            "pythagoros.db",
        ).build()

    @Provides
    fun provideHistoryDao(database: PythagorosDatabase): HistoryDao =
        database.historyDao()

    @Provides
    @Singleton
    fun provideLocalSolver(): LocalSolver = LocalSolver()

    @Provides
    @Singleton
    fun provideCasSolver(): CasSolver = CasSolver()

    @Provides
    @Singleton
    fun providePremiumAiSolver(): PremiumAiSolver = BackendPremiumAiSolver()

    @Provides
    @Singleton
    fun provideBackendAuthClient(): BackendAuthClient = BackendAuthClient()
}
