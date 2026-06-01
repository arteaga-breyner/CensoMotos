package com.putumayo.censomotos.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.putumayo.censomotos.data.dao.MotocicletaDao
import com.putumayo.censomotos.data.entity.Motocicleta

/**
 * Base de datos principal de la aplicación usando Room.
 * Singleton thread-safe con soporte de migraciones.
 */
@Database(
    entities = [Motocicleta::class],
    version = 1,
    exportSchema = false
)
abstract class CensoDatabase : RoomDatabase() {

    abstract fun motocicletaDao(): MotocicletaDao

    companion object {
        private const val DATABASE_NAME = "censo_motos.db"

        @Volatile
        private var INSTANCE: CensoDatabase? = null

        fun getInstance(context: Context): CensoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CensoDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(DatabaseCallback())
                    // Las migraciones se agregan aquí cuando suba la versión:
                    // .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Migración de ejemplo para versión futura (1 → 2)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // ALTER TABLE motocicletas ADD COLUMN nuevo_campo TEXT DEFAULT ''
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Índices para consultas rápidas por municipio y marca
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_municipio ON motocicletas(municipio)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_marca ON motocicletas(marca)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_timestamp ON motocicletas(timestamp)")
            }
        }
    }
}
