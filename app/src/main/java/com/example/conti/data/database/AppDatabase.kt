package com.example.conti.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.conti.data.database.dao.AbbonamentoDao
import com.example.conti.data.database.dao.ContoDao
import com.example.conti.data.database.dao.MovimentoDao
import com.example.conti.data.database.entities.Abbonamento
import com.example.conti.data.database.entities.Conto
import com.example.conti.data.database.entities.Movimento

/**
 * Database principale dell'applicazione Conti.
 *
 * Utilizza Room per gestire SQLite in modo type-safe e reattivo.
 *
 * Versione 1: Schema iniziale con 3 tabelle (conti, movimenti, abbonamenti)
 *
 * Pattern Singleton: una sola istanza del database per tutta l'app.
 * Thread-safe: utilizza @Volatile e synchronized per garantire sicurezza multi-thread.
 */
@Database(
    entities = [
        Conto::class,
        Movimento::class,
        Abbonamento::class
    ],
    version = 1,
    exportSchema = false // ✅ Disabilita export schema (più semplice per sviluppo)
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    // DAO accessibili dal database
    abstract fun contoDao(): ContoDao
    abstract fun movimentoDao(): MovimentoDao
    abstract fun abbonamentoDao(): AbbonamentoDao

    companion object {
        /**
         * Istanza singleton del database.
         * @Volatile garantisce che le modifiche siano visibili a tutti i thread.
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Nome del file del database SQLite.
         */
        private const val DATABASE_NAME = "conti_database"

        /**
         * Ottiene l'istanza del database (pattern Singleton).
         *
         * Thread-safe: usa synchronized per evitare che più thread creino
         * multiple istanze del database contemporaneamente.
         *
         * @param context Context dell'applicazione
         * @return L'istanza singleton di AppDatabase
         */
        fun getDatabase(context: Context): AppDatabase {
            // Se l'istanza già esiste, ritornala
            return INSTANCE ?: synchronized(this) {
                // Double-check: verifica di nuovo dentro il blocco synchronized
                val instance = INSTANCE ?: buildDatabase(context)
                INSTANCE = instance
                instance
            }
        }

        /**
         * Costruisce il database Room.
         *
         * Configurazioni:
         * - fallbackToDestructiveMigration(): se cambia la versione e non c'è una
         *   migrazione definita, ricrea il database da zero (⚠️ CANCELLA I DATI!)
         * - In produzione, dovresti definire migrazioni esplicite per preservare i dati.
         */
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                // ⚠️ ATTENZIONE: Questa opzione cancella tutti i dati se lo schema cambia
                // Usala solo in sviluppo. In produzione, implementa migrazioni esplicite.
                .fallbackToDestructiveMigration()

                // Per debugging: abilita questo per vedere tutte le query SQL nel log
                // .setQueryCallback({ sqlQuery, bindArgs ->
                //     Log.d("RoomQuery", "Query: $sqlQuery Args: $bindArgs")
                // }, Executors.newSingleThreadExecutor())

                .build()
        }

        /**
         * Metodo per chiudere il database (utile per testing).
         * In produzione, non è necessario chiamarlo.
         */
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}