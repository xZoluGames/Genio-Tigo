package com.example.geniotecni.tigo.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.geniotecni.tigo.data.dao.TransactionDao
import com.example.geniotecni.tigo.data.entities.TransactionEntity
import com.example.geniotecni.tigo.utils.AppLogger

@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun transactionDao(): TransactionDao
    
    companion object {
        private const val DATABASE_NAME = "genio_tecni_database"
        private const val TAG = "AppDatabase"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = buildDatabase(context)
                INSTANCE = instance
                instance
            }
        }
        
        private fun buildDatabase(context: Context): AppDatabase {
            AppLogger.i(TAG, "Construyendo base de datos Room")
            
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
            .addCallback(DatabaseCallback())
            .addMigrations(*getAllMigrations())
            .fallbackToDestructiveMigration() // Solo para desarrollo, remover en producción
            .build()
        }
        
        private fun getAllMigrations(): Array<Migration> {
            return arrayOf(
                // Futuras migraciones irán aquí
            )
        }
        
        // Callback para inicialización de la base de datos
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                AppLogger.i(TAG, "Base de datos creada exitosamente")
                
                // Ejecutar inicialización en background
                AppDatabase.INSTANCE?.let { database ->
                    // Aquí se pueden agregar datos iniciales si es necesario
                    AppLogger.d(TAG, "Base de datos lista para usar")
                }
            }
            
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                AppLogger.d(TAG, "Base de datos abierta")
            }
        }
        
        // Para testing - permite crear una instancia temporal
        fun createInMemoryDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            )
            .allowMainThreadQueries()
            .build()
        }
        
        // Para limpiar la instancia (testing)
        fun clearInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}

