package com.example.geniotecni.tigo.di

import android.content.Context
import com.example.geniotecni.tigo.data.repository.ServiceRepository
import com.example.geniotecni.tigo.data.repository.OptimizedServiceRepository
import com.example.geniotecni.tigo.data.processors.TransactionDataProcessor
import com.example.geniotecni.tigo.managers.*
import com.example.geniotecni.tigo.helpers.*
// import com.example.geniotecni.tigo.helpers.HistoryExporter // Temporarily disabled
import com.example.geniotecni.tigo.helpers.PhoneValidator
import com.example.geniotecni.tigo.events.AppEventBus
import com.example.geniotecni.tigo.events.EventLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 🔧 MÓDULO PRINCIPAL DE HILT - Inyección de Dependencias Centralizada
 * 
 * PROPÓSITO:
 * - Configuración centralizada de todas las dependencias de la aplicación
 * - Provisión de singletons para managers y repositories
 * - Inyección automática en actividades, fragmentos y ViewModels
 * - Desacoplamiento completo entre componentes
 * 
 * DEPENDENCIAS PROPORCIONADAS:
 * - Repositories: ServiceRepository, OptimizedServiceRepository
 * - Managers: PreferencesManager, PrintDataManager, BluetoothManager, etc.
 * - Helpers: USSDIntegrationHelper, AnimationHelper, NetworkHelper, etc.
 * - Processors: TransactionDataProcessor para lógica de datos
 * - Event System: AppEventBus, EventLogger para comunicación desacoplada
 * 
 * BENEFICIOS ARQUITECTÓNICOS:
 * - Eliminación de dependencias hardcoded
 * - Facilita testing con mocks automáticos
 * - Gestión automática del ciclo de vida
 * - Inyección type-safe en tiempo de compilación
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideServiceRepository(): ServiceRepository {
        return ServiceRepository.getInstance()
    }

    @Provides
    @Singleton
    fun provideOptimizedServiceRepository(): OptimizedServiceRepository {
        return OptimizedServiceRepository.getInstance()
    }

    @Provides
    @Singleton
    fun provideTransactionDataProcessor(): TransactionDataProcessor {
        return TransactionDataProcessor.getInstance()
    }

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun providePrintDataManager(@ApplicationContext context: Context): PrintDataManager {
        return PrintDataManager(context)
    }

    @Provides
    @Singleton
    fun provideBluetoothManager(@ApplicationContext context: Context): BluetoothManager {
        return BluetoothManager(context)
    }

    @Provides
    @Singleton
    fun provideUSSDIntegrationHelper(@ApplicationContext context: Context): USSDIntegrationHelper {
        return USSDIntegrationHelper(context)
    }

    @Provides
    @Singleton
    fun provideAmountUsageManager(@ApplicationContext context: Context): AmountUsageManager {
        return AmountUsageManager(context)
    }

    @Provides
    @Singleton
    fun providePrintCooldownManager(@ApplicationContext context: Context): PrintCooldownManager {
        return PrintCooldownManager(context)
    }

    @Provides
    @Singleton
    fun provideStatisticsManager(@ApplicationContext context: Context): StatisticsManager {
        return StatisticsManager(context)
    }

    // BackupManager temporarily removed - needs implementation
    // @Provides
    // @Singleton
    // fun provideBackupManager(@ApplicationContext context: Context): BackupManager {
    //     return BackupManager(context)
    // }

    @Provides
    fun provideAnimationHelper(): AnimationHelper {
        return AnimationHelper()
    }

    @Provides
    fun provideNetworkHelper(@ApplicationContext context: Context): NetworkHelper {
        return NetworkHelper(context)
    }

    @Provides
    fun providePhoneValidator(): PhoneValidator {
        return PhoneValidator
    }

    @Provides
    fun provideValidationHelper(): ValidationHelper {
        return ValidationHelper()
    }

    @Provides
    fun provideExportHelper(@ApplicationContext context: Context): ExportHelper {
        return ExportHelper(context)
    }

    // HistoryExporter temporarily removed - needs implementation
    // @Provides
    // fun provideHistoryExporter(@ApplicationContext context: Context): HistoryExporter {
    //     return HistoryExporter(context)
    // }

    @Provides
    fun provideBackupHelper(@ApplicationContext context: Context): BackupHelper {
        return BackupHelper(context)
    }

    // LoadingAnimationHelper requiere Activity específica, se crea localmente en Activities

    // Event System Components
    @Provides
    @Singleton
    fun provideAppEventBus(): AppEventBus {
        return AppEventBus()
    }

    @Provides
    @Singleton
    fun provideEventLogger(appEventBus: AppEventBus): EventLogger {
        return EventLogger(appEventBus)
    }
}